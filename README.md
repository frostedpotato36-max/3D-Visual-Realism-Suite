# 3D Visual Realism Suite

Project praktikum Grafika Komputer Minggu ke-11 berbasis Java OOP, LWJGL 3, JOML, OpenGL 3.3 Core, dan GLSL.

## Menjalankan di Windows
1. Instal JDK 17 atau lebih baru dan Apache Maven.
2. Ekstrak folder project.
3. Buka terminal pada folder yang berisi `pom.xml`.
4. Jalankan `mvn clean compile`.
5. Jalankan `mvn exec:java`.

Pastikan driver kartu grafis mendukung OpenGL 3.3 Core. Koneksi internet diperlukan pada proses Maven pertama kali untuk mengunduh dependensi.

## Kontrol
- Drag mouse kiri: rotasi kamera.
- Scroll mouse: zoom.
- B: Phong / Blinn-Phong.
- F: Smooth / Flat shading.
- Z: aktif/nonaktif Z-Buffer.
- C: aktif/nonaktif Back-Face Culling.
- T: aktif/nonaktif texture.
- M: Nearest / Linear-Mipmap filtering.
- H: aktif/nonaktif shadow mapping.
- ESC: keluar.

## Implementasi
- Mesh sphere UV dan normal serta ground plane.
- Phong dan Blinn-Phong pada fragment shader.
- Flat shading memakai turunan posisi fragment (`dFdx`/`dFdy`); smooth shading memakai interpolasi normal vertex.
- Z-Buffer dan Back-Face Culling dapat diaktifkan/dinonaktifkan.
- Texture checkerboard dibuat secara prosedural, dengan Nearest atau Linear-Mipmap filtering.
- Shadow mapping dua pass menggunakan depth framebuffer 2048x2048 dan PCF 3x3.

Catatan: Tekstur checkerboard dibuat dari data piksel prosedural sehingga tidak membutuhkan file gambar eksternal. Project ini merupakan implementasi demonstrasi praktikum; hasil visual dapat berbeda tergantung driver GPU.
