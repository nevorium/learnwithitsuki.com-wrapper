# Learn With Itsuki — Android Kotlin WebView Wrapper

Proyek Android Studio berbasis **Kotlin** yang dihasilkan oleh **ItsukiWrapper Studio** untuk membungkus situs **https://learnwithitsuki.com**.

## Ringkasan Konfigurasi
- **Target URL**: `https://learnwithitsuki.com`
- **Application ID**: `com.learnwithitsuki.app`
- **Arsitektur UI**: Jetpack Compose + Material 3
- **Min / Target SDK**: API 24 / API 35
- **Versi**: 1.0.0 (code 1)
- **Ikon Aplikasi**: Sudah dibundel otomatis ke `app/src/main/res/mipmap-*/ic_launcher.png` & adaptive icon `mipmap-anydpi-v26/`.

## Opsi 1: Build APK Otomatis di Cloud (Tanpa Android Studio)
1. Upload isi folder ZIP ini ke repository GitHub baru.
2. Buka tab **Actions** -> jalankan **Build Android APK**.
3. Unduh file **`app-debug.apk`** dari bagian **Artifacts** dan langsung install di HP Android Anda.

## Opsi 2: Build Lokal dengan Android Studio
1. Ekstrak arsip ZIP ini ke folder kerja Anda.
2. Buka folder di **Android Studio Ladybug (2024.2+)** atau lebih baru.
3. Tunggu **Gradle Sync** selesai mengunduh dependensi, lalu klik **Run** ke HP atau jalankan:
   ```bash
   gradle assembleDebug
   ```
   Hasil APK berada di `app/build/outputs/apk/debug/app-debug.apk`.
