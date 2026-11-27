# DESIGN.md: Sistem Desain & Ikonografi Bantuin

**Design Read:**
`Reading this as: mobile service marketplace home & navigation icons for urban Indonesian households and kos residents (Malang), in a soft two-tone utilitarian craft style, dial ENERGY 2 / RHYTHM 2 / MOTION 1.`

## 1. Palet Inti & Kontras (R-25, R-29)

Seluruh ikon memakai gaya **soft duotone tanpa garis luar (outline-free)** di atas wadah palet hangat aplikasi:

- **Wadah Ikon Seragam:** Light `#FFF3E8` (border lembut `#F1DAC6`) | Dark `#2A1F14` (border `#44301E`) agar selaras dengan identitas terracotta Bantuin dan tidak memecah grid menjadi warna-warni pastel.
- **Inti 1 (Brand Terracotta):** Light `#C43E00` / `#9A3412` | Dark `#FB923C` / `#EA580C` (steker listrik, helm proyek, katup pipa, tuas semprot).
- **Inti 2 (Steel Slate):** Light `#475569` | Dark `#CBD5E1` (pin steker, kunci pas, troli L-frame, flensa pipa; kontras `6.94:1` pada `#FFF3E8`).
- **Inti 3 (Cobalt-Teal Teknis):** Light `#0369A1` / `#0F766E` | Dark `#0284C7` / `#38BDF8` / `#10B981` (unit AC, botol kebersihan, pipa & air; kontras `5.01:1` hingga `5.44:1` pada `#FFF3E8`).
- **Aksen (Amber-Ochre):** Light `#B45309` | Dark `#F59E0B` (kardus logistik; kontras `4.60:1` pada `#FFF3E8`).

Seluruh pasangan warna utama terverifikasi WCAG AA non-teks (>= 3:1, aktual `4.60:1` hingga `6.94:1` di mode terang, serta >= `5.20:1` pada cutout putih) melalui `contrast-check.py`.

## 2. Aturan Visual & Ilustrasi (R-04, R-20, R-22, R-31)

- **Hero Header Profesional:** Bagian hero beranda menampilkan sapaan, subjudul, dan status mitra aktif secara bersih tanpa ilustrasi karakter orang.
- **Pemisahan Bentuk Tanpa Outline:** Alih-alih garis luar hitam, tiap ikon menggunakan bidang isi dua-nada dan cutout terang (`#FFFFFF` / `#FFF3E8`) sehingga tampilan terasa lembut namun tetap tajam terbaca.
- **Tanpa Glyph Generik AI (R-04):** Tidak ada bintang kilau 4-titik dekoratif (`sparkle`) atau efek *glow* tanpa fungsi.
- **Gerak Terarah (R-19, MOTION 1):** Animasi hanya merespons interaksi tekan pengguna (*press feedback*) dengan dukungan `rememberReducedMotion()`. Tidak ada dot status yang berdenyut terus-menerus.
