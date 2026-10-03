package com.example.util

object PengumumanTemplates {
    val TEMPLATES = mapOf(
        "Ulangan/UTS" to """
📢 *PENGUMUMAN ULANGAN / UTS*

Assalamualaikum wr. wb.
Bapak/Ibu Wali Murid Kelas [kelas],

📅 *Hari/Tanggal:* [hari], [tanggal]
📚 *Mata Pelajaran:* [mapel]
📖 *Materi:* [materi]

📝 Mohon bimbingan Bapak/Ibu agar ananda dapat mempersiapkan diri dengan baik dan beristirahat cukup.

📚 *Yang Perlu Dibawa:*
• Alat tulis lengkap (pensil 2B, penghapus, penggaris)
• Perlengkapan ujian

Terima kasih atas perhatian dan kerja samanya.

🙏 Wassalamualaikum wr. wb.
*[nama guru]*
_Guru Kelas [kelas] - [sekolah]_
        """.trimIndent(),

        "PR/Tugas" to """
📢 *INFORMASI PR / TUGAS RUMAH*

Assalamualaikum wr. wb.
Bapak/Ibu Wali Murid Kelas [kelas],

📅 *Batas Pengumpulan:* [hari/tanggal]
📚 *Mata Pelajaran:* [mapel]
📝 *Detail Tugas:* [materi/tugas]

Mohon bantuan Bapak/Ibu untuk mendampingi ananda menyelesaikan tugas rumah ini dengan mandiri dan teliti.

Terima kasih atas kerja samanya.

🙏 Wassalamualaikum wr. wb.
*[nama guru]*
_Guru Kelas [kelas] - [sekolah]_
        """.trimIndent(),

        "Bahan Bawaan" to """
📢 *PERLENGKAPAN / BAHAN PRAKTIKUM*

Assalamualaikum wr. wb.
Bapak/Ibu Wali Murid Kelas [kelas],

Untuk kegiatan pembelajaran/praktikum pada hari [hari], [tanggal], ananda dimohon membawa perlengkapan berikut:

🎒 *Bahan & Alat yang Dibawa:*
• [daftar perlengkapan]
• [bahan praktikum]

⚠️ _Catatan: Mohon pastikan semua perlengkapan sudah diberi label nama ananda._

Terima kasih atas perhatian Bapak/Ibu.

🙏 Wassalamualaikum wr. wb.
*[nama guru]*
_Guru Kelas [kelas] - [sekolah]_
        """.trimIndent(),

        "Kegiatan Sekolah" to """
📢 *PEMBERITAHUAN KEGIATAN SEKOLAH*

Assalamualaikum wr. wb.
Bapak/Ibu Wali Murid Kelas [kelas],

Memberitahukan bahwa sekolah akan menyelenggarakan kegiatan:
📌 *Kegiatan:* [nama kegiatan]
📅 *Hari/Tanggal:* [hari/tanggal]
⏰ *Waktu:* [jam kegiatan]
👕 *Pakaian:* [seragam/kostum]
📍 *Tempat:* [lokasi kegiatan]

Mohon ananda dapat hadir tepat waktu. Terima kasih atas dukungan Bapak/Ibu.

🙏 Wassalamualaikum wr. wb.
*[nama guru]*
_Guru Kelas [kelas] - [sekolah]_
        """.trimIndent(),

        "Libur" to """
📢 *PEMBERITAHUAN HARI LIBUR SEKOLAH*

Assalamualaikum wr. wb.
Bapak/Ibu Wali Murid Kelas [kelas],

Berdasarkan kalender pendidikan resmi, kami informasikan bahwa:
🗓️ *Hari Libur:* [tanggal libur]
📌 *Keterangan:* [alasan libur / hari besar]
🏫 *Masuk Kembali:* [hari dan tanggal masuk sekolah]

Selama libur, mohon ananda tetap menjaga kesehatan dan mengulang materi pembelajaran di rumah.

Terima kasih atas perhatian Bapak/Ibu.

🙏 Wassalamualaikum wr. wb.
*[nama guru]*
_Guru Kelas [kelas] - [sekolah]_
        """.trimIndent(),

        "Rapat Wali Murid" to """
📢 *UNDANGAN PERTEMUAN WALI MURID*

Assalamualaikum wr. wb.
Yth. Bapak/Ibu Orang Tua / Wali Murid Kelas [kelas],

Dengan hormat kami mengundang kehadiran Bapak/Ibu pada agenda pertemuan sekolah:
📅 *Hari/Tanggal:* [hari/tanggal]
⏰ *Waktu:* [pukul WIB]
📍 *Tempat:* [ruang kelas / aula sekolah]
📋 *Agenda:* [agenda rapat]

Mengingat pentingnya agenda ini untuk perkembangan ananda, kehadiran Bapak/Ibu sangat kami harapkan.

Terima kasih atas kerja sama dan perhatiannya.

🙏 Wassalamualaikum wr. wb.
*[nama guru]*
_Guru Kelas [kelas] - [sekolah]_
        """.trimIndent(),

        "Custom" to """
📢 *PENGUMUMAN KELAS [kelas]*

Assalamualaikum wr. wb.
Bapak/Ibu Wali Murid Kelas [kelas],

📝 *Informasi:*
[info tambahan]

Terima kasih atas perhatian dan kerja samanya.

🙏 Wassalamualaikum wr. wb.
*[nama guru]*
_Guru Kelas [kelas] - [sekolah]_
        """.trimIndent()
    )

    fun getTemplate(jenis: String): String {
        return TEMPLATES[jenis] ?: TEMPLATES["Custom"] ?: ""
    }
}
