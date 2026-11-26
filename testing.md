# Panduan Testing Manual — Bantuin

Dokumen ini berisi **daftar pengujian manual (fungsional, integrasi, UI/UX, dan ketahanan)** untuk seluruh fitur aplikasi Bantuin.

## 0. Tujuan & Referensi

| Referensi | Bagian yang dipakai |
|---|---|
| `PRD.md` | §3 Ruang Lingkup, §5 Konstanta Global, §6 Siklus Status & CRUD, §7 F1–F4, §8 Test Vector, §10.4 Navigasi, §11 Layar & Validasi Form, §13 Data Demo, §14 Skenario Demo & DoD, §16 Risiko |
| `implementation_plan.md` | Fase 1–6, Matriks Modul (`CRUD-0…4`, `F1…F4`, `RT-1`, `EXP-1`, `GPS-1`) dan Definition of Done tiap fase |
| `docs/DECISIONS.md` | §3 dual-mode repository, §8 bottom nav 4 tab, §10 pergantian peran, §11 QRIS & saldo mitra, §12 chat & konfirmasi geser, §13 sistem gerak |
| `docs/DEMO_SCENARIO_GUIDE.md` | Panduan eksekusi 5 skenario demo |
| `docs/LAPORAN_SKRIPSI_SUPPORT.md` | Kebutuhan bukti/tangkapan layar untuk Bab IV |

**Cara pakai:** centang kolom `Status` dengan `[x]` bila lulus, `[!]` bila ada bug (isi kolom Catatan), `[ - ]` bila tidak applicable. Untuk pengujian cloud, siapkan **dua perangkat/akun** (Pelanggan + Mitra) dan jaringan yang bisa dimatikan (airplane mode).

> **Catatan penting (mode cloud):** Jika beranda menampilkan **"Terjadi Kendala — Gagal memuat kategori layanan."**, itu bukan bug UI. Periksa hak akses Supabase: role `anon` **dan** `authenticated` harus memiliki `GRANT SELECT` pada tabel `public.*`. Jalankan `supabase/migrations/002_fix_auth_and_grants.sql`. Verifikasi cepat:
> ```bash
> curl -i "https://<project-id>.supabase.co/rest/v1/service_categories?select=id,name&order=id.asc" \
>   -H "apikey: <ANON_KEY>" -H "Authorization: Bearer <ANON_KEY>"
> ```
> Harus mengembalikan `200` dan 6 kategori. Lihat juga `DECISIONS.md §3`: kegagalan cloud **tidak** boleh diam-diam jatuh ke data demo.

---

## 1. Prasyarat Pengujian

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| 1.1 | Build debug: `./gradlew assembleDebug` | Build sukses, APK di `app/build/outputs/apk/debug/app-debug.apk` | [ ] | |
| 1.2 | Cek `local.properties`: ada `SUPABASE_URL` + `SUPABASE_ANON_KEY` non-placeholder | Mode cloud aktif. Jika kosong/placeholder → aplikasi diam-diam memakai `FakeBantuinRepository` (mode demo offline, tidak ada error Supabase) | [ ] | `.env` **tidak** dibaca Gradle |
| 1.3 | Migrasi `001 → 005` + `seed.sql` sudah dijalankan di SQL Editor Supabase | Tabel, RLS, policy, RPC, dan data demo ada | [ ] | `005_review_outcomes_and_trust.sql` wajib untuk RPC ulasan terbaru |
| 1.4 | Verifikasi RLS: insert langsung ke `bookings` dari klien anon | Ditolak (hanya lewat RPC) | [ ] | PRD P1 DoD |
| 1.5 | Siapkan akun demo: `pelanggan1@bantuin.test`, `pelanggan2@bantuin.test`, `mitra01@...` s.d. `mitra15@...`, password `Demo12345!` | Login berhasil di mode cloud maupun demo | [ ] | PRD §13 |
| 1.6 | Siapkan 2 perangkat/2 akun + aktifkan izin lokasi | Alur realtime dan lock bisa diuji | [ ] | |
| 1.7 | Catat konstanta uji (PRD §5) | Radius 10 km; bobot F1 50/35/15, penalti anomali ×0,7; Rp2.000/km; surge +20%; pembulatan Rp500; buffer 30 mnt; lock 10 mnt; expiry 15 mnt; min tarik Rp20.000; admin Rp2.500 | [ ] | |

---

## 2. Pemetaan Modul → Dokumen → Bagian Checklist

| ID | Modul | Referensi | Checklist |
|---|---|---|---|
| CRUD-0 | Auth & multi-peran | PRD §6.2, Fase 1 & 2 | A |
| CRUD-1 | Profil mitra & kategori | PRD §6.2, Fase 2 | B, K |
| CRUD-2 | Katalog layanan | PRD §6.2, Fase 2 | D, K |
| CRUD-3 | Ketersediaan & antrean | PRD §6.2, Fase 2–3 | E, J, K |
| CRUD-4 | Ulasan & riwayat | PRD §6.2, Fase 2 | G, I, N |
| F1 | Smart Matchmaking | PRD §7.1, §8.1, Fase 2 | B, C, I |
| F2 | Dynamic Cost Estimator | PRD §7.2, §8.2, Fase 2 | F |
| F3 | SLA & Conflict Detector | PRD §7.3, §8.3, Fase 2–3 | E |
| F4 | Trust Score & Anomaly | PRD §7.4, §8.4, Fase 2 | I, L |
| RT-1 | Realtime multi-perangkat | Plan Fase 3 | G, M, O |
| EXP-1 | Expiry 15 menit | Plan Fase 4 | G, J |
| GPS-1 | GPS + peta OSMDroid | Plan Fase 5 | B, K |
| — | QRIS, dompet, chat, notifikasi, tema | `DECISIONS.md §11–13` (di luar PRD awal) | H, L, M, N |

---

## 3. Checklist per Fitur

### A. Auth, Profil & Multi-Peran (CRUD-0 — PRD §6.2, Fase 1–2)

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| A.1 | Buka aplikasi tanpa sesi | Layar Login, field "Email Pengguna", "Kata Sandi", tombol "Masuk ke Aplikasi"; kartu demo menampilkan email & `Demo12345!` | [ ] | |
| A.2 | Login dengan email/password demo | Masuk ke Beranda sesuai peran; tombol berubah "Memproses..." saat loading | [ ] | |
| A.3 | Login email salah / password salah | Pesan "Email atau kata sandi salah."; mengetik ulang menghapus error | [ ] | |
| A.4 | Login email kosong, email tanpa `@`, password < 8 | "Email dan password wajib diisi" / "Format email tidak valid" | [ ] | |
| A.5 | Tekan ikon mata pada password | Sandi tampil/sembunyi; content description berubah | [ ] | |
| A.6 | Daftar akun baru: nama, email, HP `081234567890`, password 8+, centang "Daftar sekaligus sebagai Mitra Kerja" | Akun dibuat, langsung masuk ke Beranda Mitra | [ ] | |
| A.7 | Register dengan HP `12345`, password `1234567`, nama kosong | Error validasi sesuai aturan (nama wajib, password min. 8, "Nomor telepon Indonesia tidak valid (contoh: 081234567890)") | [ ] | PRD §11.2 |
| A.8 | Register email yang sudah dipakai | "Email sudah terdaftar." | [ ] | |
| A.9 | Akun dengan dua peran → Pengaturan → "Beralih ke Mode Mitra Kerja" / "Beralih ke Mode Pelanggan" | Bottom nav berganti (Pelanggan: Beranda/Pesanan/Chat/Profil; Mitra: Beranda/Pesanan/Dashboard/Profil), **tanpa logout** | [ ] | `DECISIONS.md §8, §10` |
| A.10 | Akun customer murni → "Daftar Jadi Mitra Kerja Bantuin" | Dialog "Aktivasi Akun Mitra Kerja"; isi alamat + pilih titik pangkalan + keahlian → "Aktifkan Mitra" | [ ] | |
| A.11 | Aktivasi dengan alamat/keahlian kosong | "Alamat pangkalan dan ringkasan keahlian wajib diisi." | [ ] | |
| A.12 | Force-stop aplikasi lalu buka lagi | Sesuai peran terakhir langsung masuk Beranda Mitra/Pelanggan (tidak perlu pilih peran) | [ ] | Fase 1 DoD |
| A.13 | "Keluar dari Akun" lalu buka ulang aplikasi | Kembali ke Login; draft order ikut ter-reset | [ ] | |

### B. Beranda Pelanggan, Pencarian & Lokasi (CRUD-1, F1, GPS-1 — PRD §11)

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| B.1 | Buka Beranda dengan koneksi normal | Loading "Memuat beranda layanan..." → sapaan "Halo, <nama>!", grid 6 kategori, banner darurat, kartu "Mitra Terdekat Siap Booking", garansi 7 hari | [ ] | |
| B.2 | Putus koneksi saat membuka Beranda | `ErrorView` "Terjadi Kendala" + "Gagal memuat kategori layanan." + tombol "Coba Lagi" (bukan force close) | [ ] | Juga terjadi jika GRANT Supabase belum ada |
| B.3 | Tekan "Coba Lagi" setelah koneksi kembali | Data beranda termuat | [ ] | Fase 5 DoD |
| B.4 | Ketik pencarian "AC" / "listrik" | Kategori & mitra terfilter; ikon clear muncul; hasil kosong → "Tidak ada kategori yang cocok..." | [ ] | |
| B.5 | Ketuk kategori (mis. "Teknisi Listrik") | Masuk Rekomendasi kategori tersebut | [ ] | |
| B.6 | Ketuk banner "DARURAT 24 JAM" → "Panggil" | Masuk rekomendasi kategori 2 (AC & Listrik) dengan flag `isUrgent = true`; nanti di Estimasi muncul label darurat | [ ] | Fitur tambahan |
| B.7 | Ketuk kartu lokasi → "Gunakan Lokasi GPS Saya" (izin diberikan, GPS aktif) | Label "Terdeteksi GPS akurat", alamat & koordinat terisi, peringkat mitra dihitung ulang | [ ] | GPS-1 |
| B.8 | Tolak izin lokasi / GPS mati | Error "Izin lokasi ditolak..." atau "Lokasi GPS tidak tersedia..."; masih bisa ketuk peta / preset "Alun-alun Malang", "UB / Suhat", "Sawojajar", "Dinoyo" | [ ] | PRD §10.5 |
| B.9 | Ketuk titik di peta OSMDroid, isi "Alamat Lengkap", "Simpan Lokasi" | Alamat tersimpan, label "Titik peta dipilih", peta menampilkan atribusi OpenStreetMap | [ ] | |
| B.10 | Alamat dikosongkan saat simpan lokasi | Fallback ke alamat berformat koordinat (tidak crash) | [ ] | |
| B.11 | Ada pesanan aktif (PENDING/DIKONFIRMASI/DALAM_PENGERJAAN) | Floating bar "Lacak" muncul **hanya** saat kolom pencarian kosong; kartu "Order Aktif" di header | [ ] | |
| B.12 | Ketuk lonceng notifikasi | Masuk "Notifikasi" | [ ] | |

### C. F1 — Rekomendasi & Filter Mitra (PRD §7.1, §8.1, Fase 2)

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| C.1 | Buka rekomendasi dari kategori | Judul kategori, "Radius 10 km • n mita siap", kartu terurut menurun **Match Score** (nama, jarak 1 desimal, rating, rasio selesai, badge, "Mulai Rp…") | [ ] | |
| C.2 | Ketuk ikon "Info Pemeringkatan" | Dialog "Cara Pemeringkatan Mitra": 50% jarak, 35% rating, 15% penyelesaian; tombol "Mengerti" | [ ] | |
| C.3 | Filter "<= 5 km", "Rating 4.8+", "Hanya Mitra Terpercaya", sort "Jarak Terdekat"/"Rating Tertinggi"/"Tarif Terendah"/"Ulasan Terbanyak" | Daftar & angka "Terapkan (n Mitra)" berubah langsung; "Reset" mengembalikan semua | [ ] | Filter sisi klien setelah ranking |
| C.4 | Perhatikan mitra dengan `flag_anomaly` | Tetap tampil dengan label peringatan, skor sudah dikali 0,7 → urutan turun | [ ] | PRD §7.1 aturan 4 |
| C.5 | Perhatikan mitra semua hari `OFF` dan mitra > 10 km | Mitra OFF tidak muncul; mitra > 10 km tidak masuk ranked (disebut di notifikasi "<n> mitra lain di luar radius 10 km") | [ ] | PRD §14 skenario 1 |
| C.6 | Terapkan filter yang tidak cocok | Empty "Tidak Ada Mitra yang Sesuai" + tombol "Reset Filter" | [ ] | |
| C.7 | Putus koneksi saat memuat rekomendasi | "Terjadi Kendala" + "Coba Lagi"; loading "Mencari mitra terbaik..." | [ ] | |

### D. Detail Mitra & Pemilihan Layanan (CRUD-1/2 — PRD §11.2b)

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| D.1 | Ketuk kartu mitra → "Pilih Mitra Ini" | "Detail Mitra & Layanan": nama, bio, rating, badge, alamat, daftar layanan aktif (tarif dasar & durasi), ulasan terbaru | [ ] | |
| D.2 | Mitra tanpa layanan aktif / tanpa ulasan | "Mitra belum mengaktifkan layanan untuk kategori ini." / "Belum ada ulasan dari pelanggan untuk mitra ini." | [ ] | |
| D.3 | Pilih satu layanan (tombol "Pilih Layanan") | Masuk "Pilih Jadwal Servis"; draft (mitra, layanan, durasi) tersimpan | [ ] | |
| D.4 | Putus koneksi / mitra tidak ditemukan | "Mitra Tidak Ditemukan" + "Kembali" atau `ErrorView` + "Coba Lagi" | [ ] | |

### E. F3 — Jadwal, Konflik & Slot Lock (PRD §7.3, §8.3, Fase 2–3)

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| E.1 | Buka "Pilih Jadwal Servis" | Strip 7 hari ("Hari ini"/"Libur"/"Tersedia"), kartu durasi + "+ 30m Buffer SLA", grid jam Pagi/Siang/Sore & Malam, info mode mitra (Libur / Operasional 24 Jam / Operasional HH:MM–HH:MM) | [ ] | |
| E.2 | Pilih jam yang bentrok dengan pesanan aktif | Card "Slot Tidak Tersedia" dengan alasan "Jadwal beririsan dengan pesanan aktif lain", chip "Rekomendasi Jam Bebas Konflik:" **maks 3** ("Paling Cepat"), tombol lanjut nonaktif | [ ] | PRD §14 skenario 2 |
| E.3 | Pilih jam tepat berurutan dengan blok existing (end == start) | Valid (bukan overlap), tombol "Lanjut ke Estimasi Biaya" aktif | [ ] | PRD §8.3 |
| E.4 | Pilih jam di luar jam operasional / hari libur / jam sudah lewat (hari ini) | "Jam yang dipilih berada di luar jam operasional mitra" / "Mitra libur pada hari yang dipilih" / "Jam yang dipilih sudah terlewat" | [ ] | |
| E.5 | Mitra `ALWAYS_ON`: pilih 23:00 durasi 30 / 23:30 durasi 30 | 23:00 valid (blok sampai 24:00); 23:30 `OUTSIDE_HOURS` | [ ] | PRD §8.3 |
| E.6 | Tekan "Lanjut ke Estimasi Biaya" ("Mengunci Slot...") | `acquire_slot_lock` berhasil; pindah ke Estimasi dengan info "Slot jadwal dikunci otomatis selama 10 menit" | [ ] | |
| E.7 | Perangkat kedua (akun lain) coba pilih slot yang sama | Ditolak: "Slot baru saja diambil, pilih jam lain" + penawaran slot alternatif | [ ] | RT-1 / PRD §14 skenario 2 |
| E.8 | Biarkan kunci 10 menit habis di layar Estimasi | "Kunci slot 10 menit telah habis"; tombol "Pilih Jadwal Ulang" mengembalikan ke Jadwal | [ ] | EXP/plan Fase 4 |
| E.9 | Keluar dari alur order sebelum konfirmasi | `release_slot_lock` dipanggil; slot kembali tersedia di perangkat kedua | [ ] | PRD §7.3 langkah 5 |
| E.10 | Jadwal gagal dimuat / mitra belum dipilih | "Tidak Ada Slot Tersedia" / "Data mitra atau layanan belum terpilih..." dengan aksi kembali | [ ] | |

### F. F2 — Estimasi Biaya & Pengiriman Order (PRD §7.2, §8.2, Fase 2)

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| F.1 | Lihat "Rincian Biaya Transparan" | Baris: "Tarif Dasar Jasa", "Biaya Jarak (<km> km x Rp2.000)", "Kompleksitas (<nama> <n>x)", "Subtotal Layanan", surge (jika ada), "Total Pembayaran", "Termasuk garansi servis 7 hari" | [ ] | |
| F.2 | Ganti kompleksitas Ringan/Sedang/Berat | Total berubah seketika ×1,00 / ×1,25 / ×1,50 **tanpa request jaringan** | [ ] | |
| F.3 | Aktifkan/nonaktifkan "Layanan Cepat Tanggap" | Surge +20% muncul/hilang dengan label "Layanan Cepat Tanggap (+20%)" | [ ] | |
| F.4 | Pilih jam sibuk 07:00–08:59 atau 16:00–18:59 | Label "Jam Sibuk (+20%)"; total = `roundUp500(raw × 1,2)` | [ ] | Lihat tabel test vector §5 |
| F.5 | Pilih jam 20:00–23:59 atau 00:00–05:59 | Label "Layanan Malam (+20%)" | [ ] | 06:00 = tarif normal, 05:59 = malam |
| F.6 | Jam sibuk + urgent (atau malam + urgent) | Tetap **satu** surge +20%, label "Jam Sibuk & Darurat (+20%)" / "Malam & Darurat (+20%)" — tidak menumpuk | [ ] | PRD §7.2 |
| F.7 | Ketik catatan > 500 karakter | Terpotong di 500 | [ ] | |
| F.8 | Pilih metode "QRIS" lalu "Konfirmasi Order" | Order dibuat dan langsung masuk "Pembayaran QRIS" | [ ] | `DECISIONS.md §11` |
| F.9 | Pilih "Tunai di Tempat (Cash)" lalu "Konfirmasi Order" | Order dibuat langsung masuk "Detail & Status Pesanan"; payment chip "Tunai • Bayar di Lokasi" | [ ] | |
| F.10 | Kunci habis / slot diambil akun lain saat submit | Error "Kunci slot telah berakhir..." / "Slot baru saja diambil, pilih jam lain"; tombol "Pilih Jadwal Ulang" | [ ] | |
| F.11 | Putus koneksi saat menekan "Konfirmasi Order" | Error inline, tombol kembali aktif, tidak crash; tidak ada pembelian dobel saat reconnect | [ ] | |

### G. Siklus Status, Tracking & Expiry (PRD §6.1, Fase 3–4, RT-1/EXP-1)

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| G.1 | Setelah order dibuat, cek status Pelanggan | "STATUS: MENUNGGU", catatan batas 15 menit + hitung mundur; timeline "Pesanan Dibuat" | [ ] | |
| G.2 | Mitra (perangkat 2) menerima ("Terima Pesanan") | Status Pelanggan berubah ke DIKONFIRMASI **tanpa refresh**; notifikasi "Pesanan Dikonfirmasi Mitra" | [ ] | PRD §14 skenario 4 |
| G.3 | Mitra mulai ("Mulai Pengerjaan") | Status DALAM_PENGERJAAN; timeline "Pengerjaan <layanan>"; notifikasi "Pengerjaan Berlangsung" | [ ] | |
| G.4 | Mitra selesai lewat "Proses Selesai & Tagihan" → geser konfirmasi | Status SELESAI; `finished_at` tercatat; notifikasi "Pesanan Telah Selesai" | [ ] | |
| G.5 | Mitra menolak ("Tolak") saat PENDING | Status DITOLAK, slot dilepaskan, notifikasi "Pesanan Ditolak Mitra" | [ ] | |
| G.6 | Pelanggan menekan "Batalkan Pesanan" saat PENDING/DIKONFIRMASI | Dialog konfirmasi → status DIBATALKAN_PELANGGAN; tombol tidak muncul di status lain | [ ] | PRD §6.1 |
| G.7 | Mitra menekan "Batalkan" saat DIKONFIRMASI | Status DIBATALKAN_MITRA; hanya status ini dihitung sebagai pembatalan mitra di F4 | [ ] | |
| G.8 | Pesanan PENDING dibiarkan 15 menit tanpa respons | Countdown mencapai 00:00 → status KEDALUWARSA, slot terbuka kembali, notifikasi "Pesanan Kedaluwarsa" | [ ] | Expiry butuh uji 15 mnt penuh |
| G.9 | Buka "Pesanan Saya": tab "Pesanan Aktif (n)" / "Riwayat Transaksi" | Pengelompokan sesuai status; chip pembayaran tepat; "Bayar QRIS" hanya untuk QRIS + menunggu bayar + aktif | [ ] | |
| G.10 | Detail pesanan: peta, "Chat Mitra", "Telepon" | Peta menampilkan marker mitra/lokasi layanan; chat terbuka; telepon membuka dialer atau toast "Nomor telepon mitra belum tersedia." | [ ] | |
| G.11 | Buka Notifikasi | Urutan terbaru di atas; kartu dengan `bookingId` bisa diketuk → "Lihat Detail"; kartu sistem tidak bisa diketuk | [ ] | |

### H. Pembayaran QRIS (DECISIONS §11 — di luar PRD awal)

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| H.1 | Buka "Pembayaran QRIS" dari order baru / daftar pesanan / detail pesanan | Loading "Menyiapkan kode QRIS..." → "ID #<8 karakter>", "Total Tagihan", grafis QRIS ("QRIS Nasional • Bantuin Pay", NMID), panduan simulasi | [ ] | QR bersifat dekoratif deterministik — **tidak** dipindai e-wallet nyata |
| H.2 | Tekan "Simulasi Pembayaran QRIS Berhasil" | "Memverifikasi..." → "Pembayaran Diterima" + "Dana ditahan aman hingga layanan selesai."; badge "QRIS • Lunas" | [ ] | |
| H.3 | Tekan "Bayar Nanti / Lihat Pesanan" | Kembali ke detail pesanan dengan status pembayaran "Menunggu Bayar" | [ ] | |
| H.4 | Perangkat Mitra membuka order yang sama saat pembayaran lunas | Badge pembayaran berubah otomatis ke "QRIS • Lunas" tanpa refresh | [ ] | RT-1 |
| H.5 | Putus koneksi lalu tekan simulasi bayar | Error inline (bukan crash); reconnect → tekan lagi → lunas, **tanpa dobel tagihan** | [ ] | |
| H.6 | Order tunai dibuka lewat route QRIS / order sudah dibatalkan | Error "Pesanan ini menggunakan metode tunai." / "Pesanan sudah tidak aktif." / "Tagihan Tidak Ditemukan" | [ ] | |
| H.7 | Aktifkan animator duration scale = 0 di pengembang | Animasi sapuan QR & bounce lunas tidak berjalan (reduced motion) | [ ] | `DECISIONS.md §13` |

### I. Ulasan, F4 & Feedback Loop (CRUD-4, F4 — PRD §6.1, §7.4, §14.5)

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| I.1 | Buka detail pesanan SELESAI → "Hasil Penyelesaian & Ulasan" | Chip hasil hanya "Selesai Tepat Waktu" / "Terlambat"; bintang 1–5, tag ("Tepat Waktu", "Pekerjaan Rapi", "Sopan & Ramah", "Harga Sesuai"), "Catatan Ulasan (n/300)" | [ ] | |
| I.2 | Pesanan PENDING/DIKONFIRMASI | Chip hasil "Dibatalkan" tersedia; muncul catatan "Menyimpan pilihan Dibatalkan akan membatalkan pesanan Anda." | [ ] | |
| I.3 | Pesanan DALAM_PENGERJAAN / DITOLAK / KEDALUWARSA | Tidak ada chip hasil yang bisa dipilih | [ ] | PRD `allowedOutcomes` |
| I.4 | Kirim ulasan tanpa pilih hasil / rating di luar 1–5 / komentar 301 karakter | "Pilih status penyelesaian pesanan." / "Pilih rating antara 1 hingga 5 bintang." / "Komentar ulasan maksimal 300 karakter." | [ ] | |
| I.5 | Kirim ulasan bintang 5 | Snackbar "Ulasan tersimpan"; ulasan muncul di Detail Mitra; metrik mitra berubah | [ ] | |
| I.6 | Ubah lalu "Simpan Perubahan", lalu "Hapus Ulasan" → dialog "Hapus ulasan?" | Perubahan tersimpan; snackbar "Ulasan dihapus"; ulasan hilang | [ ] | PRD §7.4 Screen 5 |
| I.7 | Ulasan bintang 1 + beberapa pembatalan mitra pada mitra uji | Trust Score turun; Dashboard Mitra menampilkan "Peringatan Anomali Akun" merah dengan alasan & "Pengali skor rekomendasi 0,7×" | [ ] | PRD §14 skenario 5 |
| I.8 | Kembali ke rekomendasi kategori mitra tersebut | Peringkat mitra turun, badge berubah dari "Mitra Terpercaya" menjadi label anomali | [ ] | Feedback loop F4 → F1 wajib |
| I.9 | Mitra dengan < 5 transaksi tetap aman meski 1 pembatalan | Tidak langsung anomali; skor awal 70, badge "Mitra Baru" | [ ] | PRD §16 mitigasi |

### J. Antrean Pesanan Mitra & Konfirmasi Selesai (CRUD-3, EXP-1)

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| J.1 | Buka "Antrean Pesanan Masuk" | Tab "Menunggu", "Terkonfirmasi", "Berjalan", "Riwayat"; kartu berisi nama, harga, layanan, lokasi, pembayaran, catatan, jadwal | [ ] | |
| J.2 | Kartu PENDING | Hitung mundur `mm:ss` 15 menit; tombol "Tolak" dan "Terima Pesanan" | [ ] | |
| J.3 | Terima setelah countdown habis | "Pesanan sudah kedaluwarsa"; status KEDALUWARSA | [ ] | |
| J.4 | Tab Terkonfirmasi | Tombol "Batalkan" → DIBATALKAN_MITRA; "Mulai Pengerjaan" → DALAM_PENGERJAAN | [ ] | |
| J.5 | Tab Berjalan → "Proses Selesai & Tagihan" | Layar "Selesaikan Pesanan": "Ringkasan Pekerjaan", "Verifikasi Pembayaran", catatan opsional maks 300 | [ ] | |
| J.6 | QRIS belum lunas lalu geser | "Pembayaran QRIS pelanggan belum lunas." | [ ] | |
| J.7 | Tunai tanpa centang konfirmasi lalu geser | "Centang konfirmasi penerimaan uang tunai terlebih dahulu." | [ ] | |
| J.8 | Geser < 85% lalu lepas | Tidak menyelesaikan; geser ≥ 85% → "Memproses penyelesaian..." → layar "Pesanan Berhasil Diselesaikan" + "Kembali ke Antrean Pesanan" | [ ] | `DECISIONS.md §12` |
| J.9 | Gunakan "Konfirmasi Tanpa Menggeser (Aksesibilitas)" | Pesanan tetap bisa diselesaikan | [ ] | |
| J.10 | Status bukan DALAM_PENGERJAAN saat membuka layar | "Pesanan harus berstatus Dalam Pengerjaan sebelum diselesaikan." | [ ] | |
| J.11 | Kosong / error | "Antrean Kosong" / "Terjadi Kendala" + "Coba Lagi" | [ ] | |

### K. Kalender, Katalog & Profil Mitra (CRUD-1/2/3 — PRD §11, Fase 2)

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| K.1 | Buka "Jadwal & Ketersediaan" | "Pesanan Aktif Terjadwal", template cepat, strip 7 hari, editor hari, "Ringkasan Jadwal Mingguan" | [ ] | |
| K.2 | Ganti mode per hari: "Jam Kerja" / "24 Jam" / "Libur" | Ringkasan menampilkan "08:00 - 17:00" / "Siaga 24 Jam" / "Libur"; perubahan tersimpan setelah "Simpan Perubahan Jadwal" → "Jadwal Tersimpan" | [ ] | |
| K.3 | Template "Sen–Jum 08–17", "Setiap Hari 08–20", "Siaga 24 Jam" + "Salin ke Sen–Jum"/"Salin ke Semua" | Semua hari terisi sesuai template | [ ] | |
| K.4 | Atur jam tutup ≤ jam buka | "Jam buka harus lebih awal dari jam tutup"; gagal simpan | [ ] | PRD §11.2 |
| K.5 | Pilih preset jam "08:00 - 17:00" dll | Field jam terisi; mode ON menampilkan input jam, OFF/ALWAYS_ON menyembunyikannya | [ ] | |
| K.6 | Tutup aplikasi saat ada jadwal belum disimpan, lalu rotasi layar | Draft jadwal di ViewModel tetap ada; dialog time picker boleh tertutup | [ ] | Fase 1 DoD |
| K.7 | "Katalog Layanan & Tarif" → "Tambah" | Dialog "Tambah Layanan Baru": nama, keterangan, chip kategori, "Tarif Dasar (Rp, kelipatan 500)", "Estimasi Durasi (Menit, 15 - 720)" | [ ] | |
| K.8 | Nama kosong, tarif negatif, tarif Rp1.000, durasi 10 / 721 | Error: "Nama layanan wajib diisi" / "Tarif dasar tidak boleh negatif" / "Tarif dasar harus kelipatan Rp 500 (contoh: Rp 0, Rp 75.000)" / "Durasi harus antara 15 hingga 720 menit" | [ ] | PRD §11.2 |
| K.9 | Tambah layanan lalu "Ubah Layanan", toggle aktif, "Hapus Layanan" | Perubahan tersimpan; dialog konfirmasi "Hapus Layanan?"; layanan muncul di Detail Mitra pelanggan | [ ] | |
| K.10 | Rotasi layar saat dialog layanan terbuka | Seluruh field tetap terisi (`rememberSaveable`) | [ ] | |
| K.11 | "Edit Biodata & Titik Pangkalan" | Field nama, bio, WhatsApp, alamat; peta interaktif + GPS + preset; simpan → snackbar "Profil mitra berhasil diperbarui" | [ ] | |
| K.12 | Nama kosong / HP tidak valid | "Nama lengkap tidak boleh kosong" / "Nomor telepon Indonesia tidak valid (contoh: 081234567890)" | [ ] | |
| K.13 | Profil mitra | Kartu identitas, RatingDisplay, badge, peta non-interaktif, menu ke layanan/kalender/evaluasi; toggle "Buka Order"/"Tutup Sementara" sinkron dengan Beranda Mitra | [ ] | |

### L. Dashboard Mitra, Evaluasi & Dompet (F4, DECISIONS §11)

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| L.1 | Buka "Dashboard Kinerja & Keuangan" | "Saldo Siap Cair", "Sudah Dicairkan", "Sedang Diproses", "Analisis Total Pendapatan" (porsi QRIS vs cash), "Provider Trust Score" dengan bobot 45/35/20 | [ ] | |
| L.2 | Tekan "Simulasikan Kondisi Anomali" | Muncul "Peringatan Anomali Akun", alasan, "Pengali skor rekomendasi 0,7×."; tombol "Kembali ke Data Aktual" memulihkan data | [ ] | Tidak mengubah data asli |
| L.3 | "Lihat Rincian Bobot Algoritma & Evaluasi" | "Evaluasi & Bobot Ranking Mitra": "STATUS EVALUASI TERKINI", "Alur Umpan Balik Otomatis", bobot F1 50/35/15, "Log Sinkronisasi Sistem", tombol "Hitung Ulang" → "Tersinkron" | [ ] | Bedakan bobot F4 vs F1 |
| L.4 | Buka "Dompet & Penarikan Saldo" | "Saldo Siap Cair", "Min. tarik Rp20.000 • Admin Rp2.500", channel BCA/BRI/Mandiri/DANA/GoPay/OVO, ringkasan "Dana Bersih Diterima" | [ ] | |
| L.5 | Coba nominal < Rp20.000 / saldo tidak cukup / rekening kosong | "Minimal penarikan dana adalah Rp20.000." / "Saldo siap cair tidak cukup." / "Lengkapi nomor rekening/HP dan nama pemilik akun." | [ ] | |
| L.6 | Ajukan penarikan valid ke BCA | Snackbar "Pencairan <Rp> ke BCA berhasil diproses.", input nominal kosong, riwayat bertambah status "Menunggu" | [ ] | |
| L.7 | Filter riwayat "Semua"/"Saldo Masuk (+)"/"Penarikan (-)" | Hanya transaksi sesuai filter; saldo masuk **hanya** dari Booking SELESAI + QRIS PAID (cash tidak masuk saldo) | [ ] | `DECISIONS.md §11` |
| L.8 | Selesaikan order QRIS lunas, cek saldo mitra | Saldo siap cair bertambah otomatis (realtime) | [ ] | |

### M. Chat Pesanan (DECISIONS §12 — di luar PRD awal)

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| M.1 | Buka tab "Chat" (Pelanggan) / "Pesan Masuk Pelanggan" (Mitra) | "Chat Pesanan"; kartu thread dengan status badge, nama layanan, pesan terakhir, bubble unread; badge angka di bottom nav | [ ] | |
| M.2 | Ketik di kolom "Cari nama atau layanan..." | Filter lokal (nama/layanan/pesan) case-insensitive; empty "Percakapan Tidak Ditemukan" + "Tidak ada obrolan yang cocok..." | [ ] | |
| M.3 | Belum ada pesanan aktif | "Belum Ada Percakapan" + "Obrolan dengan mitra atau pelanggan akan otomatis muncul di sini saat pesanan aktif." | [ ] | |
| M.4 | Buka ruang chat dari Pesanan/Detail/List | Header lawan bicara + status + chip pembayaran; quick reply sesuai peran; placeholder "Tulis pesan untuk mitra..." / "...pelanggan..." | [ ] | |
| M.5 | Kirim pesan kosong / > 1000 karakter | "Pesan tidak boleh kosong." / "Pesan maksimal 1000 karakter." | [ ] | |
| M.6 | Kirim pesan + balasan cepat | Bubble sendiri di kanan, lawan bicara di kiri; auto-scroll ke pesan terakhir | [ ] | |
| M.7 | Dua perangkat: kirim dari Pelanggan | Pesan muncul di Mitra tanpa refresh; badge unread bertambah | [ ] | RT-1 |
| M.8 | Putus koneksi saat mengirim | Error inline di atas input; reconnect lalu kirim ulang berhasil | [ ] | |
| M.9 | Ganti peran saat ruang chat terbuka | Alignment bubble dapat berubah (dihitung dari peran aktif) — pastikan tidak membingungkan | [ ] | `ChatRoomScreen.isMine` |

### N. Notifikasi & Pengaturan (PRD §11, DECISIONS §9–10)

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| N.1 | Buka "Notifikasi" | Kartu status pesanan + kartu sistem ("Garansi Pengerjaan 7 Hari", "Selamat Datang di Bantuin!"), urutan terbaru di atas | [ ] | |
| N.2 | Buka "Pengaturan" dari Profil | Halaman "Pengaturan" dengan Tampilan Aplikasi, Notifikasi, Peran Akun, Bantuan & Garansi, Keluar | [ ] | |
| N.3 | Pilih "Ikuti Sistem" / "Mode Terang" / "Mode Gelap" | Tema berubah langsung; force-stop & buka lagi → tema tetap | [ ] | Tema tersimpan di SharedPreferences |
| N.4 | Matikan "Pembaruan Status Pesanan", "Pengingat Jadwal Layanan", "Peringatan Antrean 15 Menit", "Notifikasi Pencairan Saldo" | Toggle berubah; setelah pindah peran & restart nilai tetap sama (_prefs global, bukan per-peran_) | [ ] | |
| N.5 | Ganti peran dari Pengaturan | Bottom nav & halaman beranda berganti tanpa logout | [ ] | |
| N.6 | Buka "Jaminan Garansi Layanan 7 Hari" | Dialog teks garansi (pengerjaan ulang 7 hari, gunakan chat pesanan) | [ ] | |
| N.7 | "Keluar dari Akun" | Kembali ke Login; sesi Supabase & draft order terhapus | [ ] | |

### O. Kualitas UI, Rotasi & Ketahanan (PRD §11.1, Fase 1/5/6)

| No | Langkah | Ekspektasi | Status | Catatan |
|---|---|---|---|---|
| O.1 | Rotasi portrait ↔ landscape saat: isi form register, edit jadwal, estimasi biaya, dialog layanan, edit profil | Draft tidak hilang; tidak ada `re-compose` yang mereset input | [ ] | Fase 1 DoD |
| O.2 | Rotasi saat dialog filter rekomendasi / cancel order / time picker terbuka | Dialog boleh tertutup, tapi tidak crash dan data tersimpan | [ ] | State lokal `remember` |
| O.3 | Cek seluruh layar dalam kondisi loading / empty / error | Semua punya state sesuai `"Memuat ..."`, `EmptyView`, `ErrorView` + "Coba Lagi" | [ ] | PRD §11.1 |
| O.4 | Cek seluruh teks UI | Bahasa Indonesia konsisten, tidak ada placeholder/inisialisme teknis (`SLOT_LOCKED`, dll) | [ ] | PRD DoD |
| O.5 | Cek target sentuh & kontras | Tombol minimum 48dp, teks terbaca di mode terang & gelap (WCAG AA) | [ ] | `DECISIONS.md §6` |
| O.6 | Aktifkan "Animasi" = 0 di opsi pengembang | Transisi/press-scale/animasi saldo dinonaktifkan | [ ] | `DECISIONS.md §13` |
| O.7 | Airplane mode di tiap layar utama (Beranda, Rekomendasi, Pesanan, Chat, QRIS, Dompet) | Tidak force close; pesan ramah + "Coba Lagi" | [ ] | Fase 5 DoD |
| O.8 | Matikan internet saat aplikasi terbuka lalu nyalakan lagi | Data pesanan & chat tersinkron otomatis saat koneksi kembali (Realtime reconnect) | [ ] | Catatan: library hanya log gagal langganan, tanpa retry eksplisit |
| O.9 | Cold start setelah force-stop | Sesi login, peran aktif, tema, dan 4 toggle notifikasi persist | [ ] | |
| O.10 | Tekan tombol Back berulang kali dari tengah alur order | Tidak ada navigasi ganda/overlap; lock dilepaskan; kembali ke halaman yang benar | [ ] | `NavGraph.safePopBackStack` |

---

## 4. Skenario Demo Wajib (PRD §14 — urutan rekaman video)

| # | Skenario | Langkah ringkas | Bukti | Status |
|---|---|---|---|---|
| 1 | **F1 Matchmaking** | Beranda → kategori → lihat urutan Match Score → buka filter (jarak 5 km, rating 4.8+, Terpercaya) → tunjukkan mitra anomali turun & mitra OFF tidak muncul | Tangkapan layar daftar + filter + info pemeringkatan | [ ] |
| 2 | **F3 Konflik & Lock** | Pilih jam bentrok → alasan + 3 slot alternatif → pilih alternatif → valid → lanjut; dari akun kedua coba slot sama → ditolak; tunggu/tunjukkan hitung mundur 10 menit | Tangkapanlayar dialog konflik + countdown + error akun kedua | [ ] |
| 3 | **F2 Biaya** | Jam normal vs jam sibuk (17:00) vs urgent → bandingkan rincian, surge +20% tunggal, pembulatan Rp500 | Tangkapan rincian biaya 3 varian | [ ] |
| 4 | **Order End-to-End (2 perangkat)** | Kirim QRIS → simulasi lunas → Mitra terima → mulai → selesai (geser) → status Pelanggan berubah realtime | Video 1080p + tangkapan timeline kedua perangkat | [ ] |
| 5 | **F4 Feedback Loop** | Ulasan bintang 1 / beberapa pembatalan mitra → Trust Score turun < 60 → Alert Card merah → peringkat mitra turun di F1 | Tangkapan dashboard anomali + rekomendasi sebelum/sesudah | [ ] |

---

## 5. Spot-Check Test Vector PRD §8 (bukan pengganti unit test)

> Unit test otomatis sudah ada di `app/src/test/...`. Gunakan tabel ini untuk memastikan angka di UI konsisten dengan teori.

**F2 — Dynamic Cost (PRD §8.2):**

| baseFare | Jarak | Kompleksitas | Jam | Urgent | Subtotal | Total UI |
|---|---|---|---|---|---|---|
| 100.000 | 3,0 | SEDANG | 10:00 | tidak | 132.500 | **132.500** |
| 100.000 | 3,0 | SEDANG | 17:00 | tidak | 132.500 | **159.000** (PEAK_HOUR) |
| 50.000 | 2,5 | RINGAN | 10:00 | ya | 55.000 | **66.000** (URGENT) |
| 75.000 | 1,3 | BERAT | 10:00 | tidak | 116.500 | **116.500** |
| 75.000 | 1,3 | BERAT | 21:00 | tidak | 116.500 | **140.000** (NIGHT) |
| 100.000 | 3,0 | SEDANG | 17:00 | ya | 132.500 | **159.000** (tidak menumpuk) |
| 100.000 | 0,0 | RINGAN | 06:00 | tidak | 100.000 | **100.000** |
| 100.000 | 0,0 | RINGAN | 05:59 | tidak | 100.000 | **120.000** (NIGHT) |

**F3 — Konflik (PRD §8.3, mitra ON 08:00–17:00, existing 09:00–11:30):** 10:00/60 mnt → `OVERLAP` + alternatif 11:30, 12:00, 12:30; 11:30/60 → valid; 16:00/60 → `OUTSIDE_HOURS`, alternatif kosong; 23:00/30 `ALWAYS_ON` → valid; 23:30/30 → `OUTSIDE_HOURS`; hari `OFF` → `DAY_OFF`; hari ini 10:00 pilih 09:00 → `PAST_TIME`.

**F1 — Peringkat (PRD §8.1):** B 88 → A 85 → D 68 (anomali) → E 35; C (11 km) di luar radius. **F4 — Trust (PRD §8.4):** sehat 62,75/batal 20%; batal tinggi 63/batal 40% anomali; skor rendah 25 anomali; 1 transaksi 80; tanpa ulasan = 0.

---

## 6. Definition of Done Proyek (PRD §14 + Plan Fase 1–6)

- [ ] Seluruh test vector Bagian 8 lulus sebagai unit test (`./gradlew test`)
- [ ] Alur Screen 1–7 berjalan end-to-end dengan data cloud pada dua akun/perangkat
- [ ] Tidak ada operasi jaringan/perhitungan berat di main thread; layar tahan rotasi
- [ ] RLS aktif; klien tidak dapat mengubah `bookings` di luar RPC; tidak ada rahasia di repository
- [ ] Semua teks UI berbahasa Indonesia; setiap layar punya loading/empty/error state
- [ ] README menjelaskan setup Supabase (migrasi, akun demo, `local.properties`)
- [ ] `docs/DECISIONS.md` mencatat setiap keputusan di luar spesifikasi
- [ ] `./gradlew assembleRelease` menghasilkan APK tanpa warning lint kritis
- [ ] Tangkapan layar tiap skenario §4 dan video demo 1080p tersedia untuk Bab IV laporan

---

## 7. Catatan Deviasi & Batasan yang Perlu Diuji

1. **Fitur di luar PRD awal (PRD §3.2 menyatakan out of scope, tetapi sudah diimplementasikan dan didokumentasikan di `DECISIONS.md §11–13`):** pembayaran QRIS + dompet/penarikan mitra, chat pesanan, notifikasi, tema terang/gelap, fitur darurat, dan halaman Pengaturan. Semua tetap wajib diuji (bagian H, L, M, N).
2. **Bottom navigation berbeda dengan PRD §10.4:** tab Pelanggan adalah Beranda / Pesanan / **Chat** / Profil (bukan "Jadwal"). Jadwal pelanggan adalah bagian dari alur order; kalender mitra diakses dari Profil/Beranda Mitra.
3. **QRIS bersifat simulasi:** grafis QR deterministik untuk pengujian, bukan kode pembayaran nyata. Gunakan tombol "Simulasi Pembayaran QRIS Berhasil".
4. **Mode cloud vs offline:** tanpa kredensial, aplikasi memakai `FakeBantuinRepository` dan seluruh alur tetap bisa diuji deterministik, tetapi **tidak ada sinkronisasi dua perangkat**. Uji RT-1 wajib di mode cloud.
5. **Keterbatasan realtime:** kegagalan langganan WebSocket hanya dicatat di log; tidak ada auto-reconnect eksplisit di repository. Skenario O.8 perlu dicek manual.
6. **Countdown 15 menit dan lock 10 menit:** untuk menghemat waktu, uji tampilan dan perilaku dengan menunggu penuh minimal sekali; sisanya cukup verifikasi pesan error saat boundary (mis. terima pesanan setelah kedaluwarsa).
