# Asisten Mengajar 📚🇮🇩

Aplikasi Android Native pendamping cerdas untuk guru Sekolah Dasar (SD) di Indonesia, dirancang khusus dengan Kurikulum Merdeka untuk mendukung pengelolaan kegiatan belajar mengajar, administrasi jadwal, pemantauan progress kurikulum, dan integrasi kecerdasan buatan (Gemini AI).

**Profil Pendidik:**
- **Nama Guru:** Pak Druz
- **Sekolah:** SDN Cipeucang 01
- **Kelas Binaan:** Kelas 4 (Mendukung kelas 1–6)

---

## Fitur Utama

1. **🏠 Beranda (Dashboard)**
   - Salam personal ramah sesuai waktu (Pagi/Siang/Sore/Malam).
   - Kartu Jadwal Mengajar Hari Ini otomatis sesuai hari (Senin–Sabtu).
   - Indikator Progress Materi Kurikulum Merdeka per mata pelajaran.
   - Daftar checklist bahan ajar & alat praktikum yang perlu disiapkan hari ini.
   - Tombol cepat ke Asisten AI dan Progress Materi.

2. **💬 Tanya Asisten AI (Chatbot Interaktif Guru)**
   - Tampilan antarmuka pesan bergaya WhatsApp.
   - Input teks dan masukan suara (Voice Input) Bahasa Indonesia via `SpeechRecognizer`.
   - Otomatis membacakan balasan AI dengan Text-to-Speech (TTS) Bahasa Indonesia berkecepatan natural.
   - Integrasi Gemini API (`gemini-3.5-flash`) dengan prompt khusus pedagogi SD.
   - Fitur ilustrasi materi otomatis untuk topik visual (misal: fotosintesis, tata surya, pecahan, bagian tubuh tumbuhan).
   - Riwayat percakapan tersimpan otomatis di Room Database dengan tombol bersihkan histori.

3. **📊 Progress Materi Kurikulum**
   - Pemantauan pertemuan per mata pelajaran dan per kelas (1 sampai 6).
   - Penandaan status selesai (✅) atau belum (⏳) dengan timestamp tanggal penyelesaian.
   - Fitur "Mundur ke Pertemuan Ini" untuk menyesuaikan kembali dinamika kelas.
   - Filter cepat dropdown kelas & mata pelajaran.
   - Bar persentase ketuntasan materi ajar.

4. **📚 Bank Materi Ajar**
   - Tab navigasi untuk semua jenjang kelas 1–6.
   - Pengelompokan materi ajar per Bab.
   - Pencarian cepat (*search bar*) berdasarkan judul materi, bab, maupun bahan praktikum.
   - Lembar detail tujuan pembelajaran dan alokasi Jam Pelajaran (JP).
   - Tombol Bagikan/Ekspor ringkasan materi ajar ke WhatsApp, Catatan, atau dokumen cetak.

5. **⚙️ Pengaturan & Backup**
   - Pengaturan nama pendidik, nama sekolah, kelas utama, dan multi-pilihan kelas yang diampu.
   - Pengaturan Kunci Google Gemini API (terenkripsi / tersembunyi).
   - Notifikasi Briefing Harian otomatis (default jam 20:00) yang merangkum jadwal & bahan ajar esok hari.
   - Pengingat hidrasi & istirahat guru (Reminder Minum Air).
   - Cadangkan & Pulihkan data (Export/Import JSON) yang kompatibel dengan Google Drive.

---

## Arsitektur & Teknologi

- **Bahasa:** Kotlin 100%
- **UI Toolkit:** Jetpack Compose (Material You / Material 3)
- **Arsitektur:** Model-View-ViewModel (MVVM) + Repository Pattern
- **Penyimpanan Lokal:** Room Database (SQLite) dengan 4 entitas: `materi`, `jadwal`, `chat_history`, `config`
- **Jaringan:** Retrofit 2 + OkHttp 4 + Moshi
- **AI Engine:** Google Gemini API (`gemini-3.5-flash`)
- **Pemuatan Gambar:** Coil Compose
- **Notifikasi & Penjadwalan:** Android WorkManager + AlarmManager + NotificationChannel
- **Suara:** Android SpeechRecognizer + TextToSpeech (`id-ID`)

---

## Cara Build APK

### 1. Build via Terminal / Gradle

Pastikan JDK 17 atau JDK 21 terpasang:

```bash
# Debug APK
gradle assembleDebug

# Output APK akan berada di:
# app/build/outputs/apk/debug/app-debug.apk
```

Untuk build Release APK:
```bash
gradle assembleRelease

# Output APK akan berada di:
# app/build/outputs/apk/release/app-release-unsigned.apk
```

### 2. Ekspor & Unduh APK di AI Studio

1. Buka menu pengaturan di bilah sisi Google AI Studio Build.
2. Pilih opsi **Export / Download APK**.
3. Sistem akan mengompilasi dan mengunduh berkas `.apk` siap pasang ke ponsel pintar Android Anda.

---

## Konfigurasi Gemini API Key

1. Buka tab **Pengaturan (⚙️)** di dalam aplikasi.
2. Masukkan Gemini API Key Anda pada kolom *Kunci API Gemini AI*.
3. Tekan tombol **Simpan Key**.
4. Atau pada Google AI Studio, tambahkan rahasia `GEMINI_API_KEY` pada panel **Secrets**.
