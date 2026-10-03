package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.local.AsistenDatabase
import com.example.data.local.ConfigEntity
import com.example.data.local.JadwalEntity
import com.example.data.local.MateriEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object BackupHelper {

    sealed class RestoreResult {
        data class Success(val kelasCount: Int, val pertemuanCount: Int, val message: String) : RestoreResult()
        data class Error(val message: String) : RestoreResult()
    }

    suspend fun createBackupJson(context: Context, database: AsistenDatabase): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        root.put("version", "1.0")
        root.put("exported_at", System.currentTimeMillis().toString())

        val dataObj = JSONObject()

        // 1. Materi grouped by class and subject
        val materiList = database.materiDao().getAllMateriList()
        val materiObj = JSONObject()
        val groupedByKelas = materiList.groupBy { it.kelas }

        for (k in 1..6) {
            val listForK = groupedByKelas[k] ?: emptyList()
            val kObj = JSONObject()
            kObj.put("semester", "1")
            kObj.put("kelas", k)
            kObj.put("total_pertemuan", listForK.size)

            val mapelObj = JSONObject()
            val groupedByMapel = listForK.groupBy { it.mapel }
            for ((mapelName, pertemuans) in groupedByMapel) {
                val pArr = JSONArray()
                for (m in pertemuans) {
                    val pObj = JSONObject().apply {
                        put("pertemuan", m.pertemuan)
                        put("bab", m.bab)
                        put("materi", m.judul)
                        val bahanList = m.bahan.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val bArr = JSONArray()
                        bahanList.forEach { bArr.put(it) }
                        put("bahan", bArr)
                        put("tujuan", m.tujuan)
                        put("alokasi_jp", m.alokasiJp)
                        put("status", m.status ?: JSONObject.NULL)
                        put("tanggal_selesai", m.tanggalSelesai ?: JSONObject.NULL)
                    }
                    pArr.put(pObj)
                }
                mapelObj.put(mapelName, pArr)
            }
            kObj.put("mapel", mapelObj)
            materiObj.put("kelas_$k", kObj)
        }
        dataObj.put("materi", materiObj)

        // 2. Jadwal
        val jadwalList = database.jadwalDao().getAllJadwalList()
        val jadwalObj = JSONObject()
        val jadwalArray = JSONArray()
        for (j in jadwalList) {
            val obj = JSONObject().apply {
                put("id", j.id)
                put("kelas", j.kelas.toString())
                put("hari", j.hari)
                put("jam_mulai", j.jamMulai)
                put("jam_selesai", j.jamSelesai)
                put("mapel", j.mapel)
            }
            jadwalArray.put(obj)
        }
        jadwalObj.put("jadwal", jadwalArray)
        dataObj.put("jadwal", jadwalObj)

        // 3. Config
        val configList = database.configDao().getAllConfigList()
        val configMap = configList.associate { it.key to it.value }
        val configObj = JSONObject()

        val userObj = JSONObject().apply {
            put("name", configMap["user_name"] ?: "")
            put("role", configMap["user_role"] ?: "Guru SD")
            put("school", configMap["user_school"] ?: "")
            put("kelas_utama", configMap["user_kelas_utama"] ?: "4")
            val kArr = JSONArray()
            (configMap["user_kelas"] ?: "1,2,3,4,5,6").split(",").mapNotNull { it.trim().toIntOrNull() }.forEach {
                kArr.put(it)
            }
            put("kelas", kArr)
        }
        configObj.put("user", userObj)

        val assistantObj = JSONObject().apply {
            put("name", configMap["assistant_name"] ?: "Cici")
            val asstGreeting = configMap["assistant_greeting"]
            put("greeting", asstGreeting?.ifBlank { null } ?: "Halo! Cici siap membantu. Panggil aku kalau butuh sesuatu ya.")
        }
        configObj.put("assistant", assistantObj)
        dataObj.put("config", configObj)

        // 4. Visual Catalog & Kalender (load baseline if present from assets)
        try {
            val assetJson = context.assets.open("data_mengajar.json").bufferedReader().use { it.readText() }
            val assetRoot = JSONObject(assetJson)
            val assetData = if (assetRoot.has("data")) assetRoot.getJSONObject("data") else assetRoot
            if (assetData.has("visual_catalog")) {
                dataObj.put("visual_catalog", assetData.getJSONObject("visual_catalog"))
            } else {
                dataObj.put("visual_catalog", JSONObject())
            }
            if (assetData.has("kalender")) {
                dataObj.put("kalender", assetData.getJSONObject("kalender"))
            } else {
                dataObj.put("kalender", JSONObject())
            }
        } catch (_: Exception) {
            dataObj.put("visual_catalog", JSONObject())
            dataObj.put("kalender", JSONObject())
        }

        root.put("data", dataObj)
        root.toString(2)
    }

    suspend fun restoreFromJson(
        context: Context,
        jsonString: String,
        database: AsistenDatabase
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            if (jsonString.isBlank()) {
                return@withContext RestoreResult.Error("Isi file JSON kosong.")
            }

            // a & b. Parse JSON
            val root = try {
                JSONObject(jsonString)
            } catch (e: Exception) {
                return@withContext RestoreResult.Error("Format JSON tidak valid: ${e.localizedMessage}")
            }

            // c. Validasi struktur (harus ada field "data" dengan "materi", "jadwal", "config", "kalender", "visual_catalog")
            if (!root.has("data")) {
                return@withContext RestoreResult.Error("Struktur file tidak valid: Field 'data' tidak ditemukan.")
            }

            val dataObj = root.optJSONObject("data")
                ?: return@withContext RestoreResult.Error("Struktur file tidak valid: Objek 'data' kosong.")

            val requiredFields = listOf("materi", "jadwal", "config", "kalender", "visual_catalog")
            val missingFields = requiredFields.filter { !dataObj.has(it) }
            if (missingFields.isNotEmpty()) {
                return@withContext RestoreResult.Error(
                    "File tidak lengkap: field [${missingFields.joinToString(", ")}] tidak ditemukan di dalam 'data'."
                )
            }

            val materiVal = dataObj.opt("materi")
            if (materiVal !is JSONObject) {
                return@withContext RestoreResult.Error("Format field 'materi' tidak valid.")
            }

            // Validasi kelas di data.materi
            val kelasKeys = mutableListOf<String>()
            val iter = materiVal.keys()
            while (iter.hasNext()) {
                val key = iter.next()
                if (key.startsWith("kelas_") || key.toIntOrNull() != null) {
                    kelasKeys.add(key)
                }
            }

            if (kelasKeys.isEmpty()) {
                return@withContext RestoreResult.Error("File tidak lengkap: Tidak ada data kelas di dalam 'materi'.")
            }

            // Requirement 5: Kalau file JSON cuma punya 1 kelas, beri warning
            if (kelasKeys.size == 1) {
                return@withContext RestoreResult.Error("File tidak lengkap, hanya berisi 1 kelas (${kelasKeys[0]}).")
            }

            // d. HAPUS semua data lama di Room Database
            database.materiDao().deleteAll()
            database.jadwalDao().deleteAll()
            database.configDao().deleteAll()
            database.chatDao().clearHistory()
            database.kalenderDao().deleteAll()

            // e. INSERT data baru dari JSON ke Room Database
            // Loop SEMUA kelas di data.materi (kelas_1 s/d kelas_6)
            // Loop semua mapel di tiap kelas
            // Loop semua pertemuan di tiap mapel
            val materiList = mutableListOf<MateriEntity>()
            val sortedKelasKeys = kelasKeys.sortedBy { k ->
                k.replace("kelas_", "").toIntOrNull() ?: 99
            }

            var importedKelasCount = 0
            for (kelasKey in sortedKelasKeys) {
                val kelasNum = kelasKey.replace("kelas_", "").toIntOrNull() ?: 1
                val kelasObj = materiVal.getJSONObject(kelasKey)
                var hasPertemuanInClass = false

                val mapelObj = if (kelasObj.has("mapel")) {
                    kelasObj.getJSONObject("mapel")
                } else {
                    kelasObj
                }

                val mapelKeys = mapelObj.keys()
                while (mapelKeys.hasNext()) {
                    val mapelName = mapelKeys.next()
                    if (mapelName in listOf("semester", "kelas", "total_pertemuan", "catatan", "id")) continue

                    val pertemuanVal = mapelObj.opt(mapelName)
                    if (pertemuanVal is JSONArray) {
                        for (i in 0 until pertemuanVal.length()) {
                            val item = pertemuanVal.getJSONObject(i)
                            val pertemuan = item.optInt("pertemuan", i + 1)
                            val bab = item.optString("bab", "")
                            val judul = item.optString("materi", item.optString("judul", ""))
                            val bahanArr = item.optJSONArray("bahan")
                            val bahanString = if (bahanArr != null) {
                                val list = mutableListOf<String>()
                                for (j in 0 until bahanArr.length()) {
                                    list.add(bahanArr.getString(j))
                                }
                                list.joinToString(", ")
                            } else {
                                item.optString("bahan", "")
                            }
                            val tujuan = item.optString("tujuan", "")
                            val alokasiJp = item.optInt("alokasi_jp", item.optInt("jp", 2))
                            val status = if (item.isNull("status") || item.optString("status").isEmpty()) null else item.optString("status")
                            val tanggalSelesai = if (item.has("tanggal_selesai") && !item.isNull("tanggal_selesai") && item.optString("tanggal_selesai").isNotEmpty()) {
                                item.optString("tanggal_selesai")
                            } else if (status == "selesai") {
                                "Awal Semester"
                            } else {
                                null
                            }

                            materiList.add(
                                MateriEntity(
                                    kelas = kelasNum,
                                    mapel = mapelName,
                                    pertemuan = pertemuan,
                                    bab = bab,
                                    judul = judul,
                                    bahan = bahanString,
                                    tujuan = tujuan,
                                    alokasiJp = alokasiJp,
                                    status = status,
                                    tanggalSelesai = tanggalSelesai
                                )
                            )
                            hasPertemuanInClass = true
                        }
                    }
                }
                if (hasPertemuanInClass) {
                    importedKelasCount++
                }
            }

            if (materiList.isNotEmpty()) {
                database.materiDao().insertAll(materiList)
            }

            // Insert Jadwal
            val jadwalList = mutableListOf<JadwalEntity>()
            val jadwalVal = dataObj.opt("jadwal")
            val jadwalArray = if (jadwalVal is JSONArray) {
                jadwalVal
            } else if (jadwalVal is JSONObject && jadwalVal.has("jadwal")) {
                jadwalVal.optJSONArray("jadwal")
            } else null

            if (jadwalArray != null) {
                for (i in 0 until jadwalArray.length()) {
                    val obj = jadwalArray.getJSONObject(i)
                    jadwalList.add(
                        JadwalEntity(
                            kelas = obj.optString("kelas", "4").toIntOrNull() ?: 4,
                            hari = obj.optString("hari", "Senin"),
                            jamMulai = obj.optString("jam_mulai", "07:30"),
                            jamSelesai = obj.optString("jam_selesai", "09:15"),
                            mapel = obj.optString("mapel", "")
                        )
                    )
                }
                if (jadwalList.isNotEmpty()) {
                    database.jadwalDao().insertAll(jadwalList)
                }
            }

            // Insert Kalender
            val kalenderList = mutableListOf<com.example.data.local.KalenderEntity>()
            val kalenderObj = dataObj.optJSONObject("kalender")
            if (kalenderObj != null) {
                // 1. Libur Nasional
                val liburArr = kalenderObj.optJSONArray("libur")
                if (liburArr != null) {
                    for (i in 0 until liburArr.length()) {
                        val item = liburArr.getJSONObject(i)
                        kalenderList.add(
                            com.example.data.local.KalenderEntity(
                                tanggal = item.optString("tanggal", ""),
                                tanggalMulai = item.optString("tanggal_mulai", ""),
                                tanggalSelesai = item.optString("tanggal_selesai", ""),
                                keterangan = item.optString("keterangan", ""),
                                jenis = "libur_nasional"
                            )
                        )
                    }
                }
                // 2. Libur Semester
                val liburSemArr = kalenderObj.optJSONArray("libur_semester")
                if (liburSemArr != null) {
                    for (i in 0 until liburSemArr.length()) {
                        val item = liburSemArr.getJSONObject(i)
                        kalenderList.add(
                            com.example.data.local.KalenderEntity(
                                tanggal = item.optString("tanggal", ""),
                                tanggalMulai = item.optString("tanggal_mulai", ""),
                                tanggalSelesai = item.optString("tanggal_selesai", ""),
                                keterangan = item.optString("keterangan", ""),
                                jenis = "libur_semester"
                            )
                        )
                    }
                }
                // 3. Libur Khusus / Keagamaan
                val liburKhususArr = kalenderObj.optJSONArray("libur_khusus")
                if (liburKhususArr != null) {
                    for (i in 0 until liburKhususArr.length()) {
                        val item = liburKhususArr.getJSONObject(i)
                        kalenderList.add(
                            com.example.data.local.KalenderEntity(
                                tanggal = item.optString("tanggal", ""),
                                tanggalMulai = item.optString("tanggal_mulai", ""),
                                tanggalSelesai = item.optString("tanggal_selesai", ""),
                                keterangan = item.optString("keterangan", ""),
                                jenis = "libur_keagamaan"
                            )
                        )
                    }
                }
                // 4. Kegiatan Sekolah / Peringatan
                val kegiatanArr = kalenderObj.optJSONArray("kegiatan_sekolah")
                if (kegiatanArr != null) {
                    for (i in 0 until kegiatanArr.length()) {
                        val item = kegiatanArr.getJSONObject(i)
                        val j = item.optString("jenis", "kegiatan")
                        kalenderList.add(
                            com.example.data.local.KalenderEntity(
                                tanggal = item.optString("tanggal", ""),
                                tanggalMulai = item.optString("tanggal_mulai", ""),
                                tanggalSelesai = item.optString("tanggal_selesai", ""),
                                keterangan = item.optString("keterangan", ""),
                                jenis = if (j == "peringatan") "peringatan" else "kegiatan"
                            )
                        )
                    }
                }
                // 5. Ujian / Rapor
                val ujianArr = kalenderObj.optJSONArray("ujian")
                if (ujianArr != null) {
                    for (i in 0 until ujianArr.length()) {
                        val item = ujianArr.getJSONObject(i)
                        val j = item.optString("jenis", "ujian")
                        kalenderList.add(
                            com.example.data.local.KalenderEntity(
                                tanggal = item.optString("tanggal", ""),
                                tanggalMulai = item.optString("tanggal_mulai", ""),
                                tanggalSelesai = item.optString("tanggal_selesai", ""),
                                keterangan = item.optString("keterangan", ""),
                                jenis = if (j == "rapor") "rapor" else "ujian"
                            )
                        )
                    }
                }
            }
            if (kalenderList.isNotEmpty()) {
                database.kalenderDao().insertAll(kalenderList)
            }

            // f. Simpan config ke SharedPreferences dan Room Database
            val configEntities = mutableListOf<ConfigEntity>()
            val prefs = context.getSharedPreferences("asisten_mengajar_prefs", Context.MODE_PRIVATE)
            val editor = prefs.edit()

            val configVal = dataObj.opt("config")
            if (configVal is JSONObject) {
                if (configVal.has("user")) {
                    val userObj = configVal.getJSONObject("user")
                    val uName = userObj.optString("name", "")
                    if (uName.isNotBlank()) {
                        val uRole = userObj.optString("role", "Guru SD")
                        val uSchool = userObj.optString("school", "")
                        val uKelasUtama = userObj.optString("kelas_utama", "4")
                        val kelasArr = userObj.optJSONArray("kelas")
                        val uKelas = if (kelasArr != null) {
                            val list = mutableListOf<Int>()
                            for (k in 0 until kelasArr.length()) list.add(kelasArr.getInt(k))
                            list.joinToString(",")
                        } else "1,2,3,4,5,6"

                        configEntities.add(ConfigEntity("user_name", uName))
                        configEntities.add(ConfigEntity("user_role", uRole))
                        configEntities.add(ConfigEntity("user_school", uSchool))
                        configEntities.add(ConfigEntity("user_kelas", uKelas))
                        configEntities.add(ConfigEntity("user_kelas_utama", uKelasUtama))

                        editor.putString("user_name", uName)
                        editor.putString("user_role", uRole)
                        editor.putString("user_school", uSchool)
                        editor.putString("user_kelas", uKelas)
                        editor.putString("user_kelas_utama", uKelasUtama)
                    }
                }
                if (configVal.has("assistant")) {
                    val asstObj = configVal.getJSONObject("assistant")
                    val aName = asstObj.optString("name", "")
                    if (aName.isNotBlank()) {
                        val aGreeting = asstObj.optString("greeting", "Halo! $aName siap membantu.")
                        configEntities.add(ConfigEntity("assistant_name", aName))
                        configEntities.add(ConfigEntity("assistant_greeting", aGreeting))

                        editor.putString("assistant_name", aName)
                        editor.putString("assistant_greeting", aGreeting)
                    }
                }
            } else if (configVal is JSONArray) {
                for (i in 0 until configVal.length()) {
                    val obj = configVal.getJSONObject(i)
                    val k = obj.getString("key")
                    val v = obj.getString("value")
                    configEntities.add(ConfigEntity(k, v))
                    editor.putString(k, v)
                }
            }

            val exportVersion = root.optString("exported_at", root.optString("version", "1.0"))
            configEntities.add(ConfigEntity("data_imported_version", exportVersion))
            configEntities.add(ConfigEntity("notification_briefing_enabled", "true"))
            configEntities.add(ConfigEntity("notification_briefing_time", "20:00"))
            configEntities.add(ConfigEntity("notification_hydration_enabled", "true"))
            configEntities.add(ConfigEntity("voice_tts_enabled", "true"))

            editor.putString("data_imported_version", exportVersion)
            editor.apply()

            database.configDao().insertAll(configEntities)

            // g. Notifikasi sukses dengan jumlah data yang di-import
            val successMessage = "Berhasil import $importedKelasCount kelas, ${materiList.size} pertemuan"
            RestoreResult.Success(
                kelasCount = importedKelasCount,
                pertemuanCount = materiList.size,
                message = successMessage
            )
        } catch (e: Exception) {
            RestoreResult.Error("Terjadi kesalahan saat memulihkan data: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    suspend fun resetToDefaultData(
        context: Context,
        database: AsistenDatabase
    ): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.assets.open("data_mengajar.json").bufferedReader().use { it.readText() }
            restoreFromJson(context, jsonString, database)
        } catch (e: Exception) {
            RestoreResult.Error("Gagal mereset data: ${e.localizedMessage}")
        }
    }

    fun shareBackup(context: Context, backupJson: String) {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, backupJson)
            putExtra(Intent.EXTRA_TITLE, "Backup Asisten Mengajar.json")
            type = "application/json"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Simpan / Bagikan Backup Data")
        context.startActivity(shareIntent)
    }
}
