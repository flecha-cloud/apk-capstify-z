CAPSTIFY-ADMIN Z — PROYEK ANDROID NATIVE (TAHAP DASAR)

PENTING:
- Ini proyek Android native Kotlin + Jetpack Compose, bukan WebView/PWA.
- Aplikasi memakai Worker dan database D1 yang sudah ada. Tidak membuat database baru.
- URL backend sudah disetel ke:
  https://adminz-capstify.adminzcapstify.workers.dev/
- Login dan Dashboard dasar sudah dihubungkan ke API yang ada.
- Menu lain masih berupa placeholder pada paket tahap dasar ini dan belum boleh dianggap selesai.
- Jangan menaruh password admin, SETUP_TOKEN, atau BACKUP_API_TOKEN di repository.

CARA MEMASUKKAN KE REPOSITORY:
1. Ekstrak ZIP ini di HP.
2. Buka repository GitHub: flecha-cloud/apk-capstify-z
3. Upload isi folder proyek (bukan file ZIP-nya) ke root repository.
4. Pastikan folder .github/workflows ikut terunggah.
5. Setelah commit, buka tab Actions.
6. Pilih workflow "Build Capstify Android APK" lalu jalankan jika belum berjalan otomatis.
7. APK tersedia di bagian Artifacts pada hasil workflow yang sukses.

Catatan:
- Tahap ini untuk memastikan kerangka Android native, login, penyimpanan cookie sesi,
  koneksi dashboard, dan pipeline build sudah berfungsi.
- Modul Produk, Stok, Pesanan, Tempat Sampah, Rekap, dan Backup perlu diimplementasikan
  dan diuji satu per satu sebelum APK dipakai untuk operasional.
