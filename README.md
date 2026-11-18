# Spicy Music

Pemutar musik Android tanpa login (sumber: Audius), dengan tema, CSS kustom, dan ekstensi ala Spicetify.

Cara dapat APK:
1. Bikin repo baru di GitHub, upload semua isi folder ini (termasuk folder .github).
2. Buka tab Actions, workflow "Build APK" jalan otomatis setelah push (atau jalankan manual).
3. Setelah hijau, download artifact "SpicyWeb-apk", extract, install app-debug.apk di HP.

Kustomisasi:
- Dari dalam app: tab Tema (preset, warna aksen, CSS kustom, ekstensi).
- Dari kode: app/src/main/assets/index.html (semua UI di satu file).
