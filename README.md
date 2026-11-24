# ifs18005-p6-pbo2026-springboot-todolist

Aplikasi web Todo List berbasis **Spring Boot 4.1.2**, **Maven**, **Vaadin 25.3.4**, dan **JDBC (PostgreSQL)**, dengan arsitektur **Clean Architecture** (domain, usecase, adapter, framework) serta **integrasi OpenAI** untuk generate todo.

| | |
| --- | --- |
| Folder | `ifs18005-p6-pbo2026-springboot-todolist` |
| Artifact Maven | `ifs18005-p6-pbo2026-springboot-todolist` |
| Package | `org.delcom.ifs18005springboottodolist` |
| Database | `pbo2026_todolist` |

## Prasyarat

- JDK 25
- PostgreSQL
- Maven Wrapper (`mvnw` / `mvnw.cmd`)

## Konfigurasi `.env`

Salin `.env.example` menjadi `.env` (file `.env` diabaikan git):

```powershell
Copy-Item .env.example .env
```

Isi contoh:

```env
APP_PORT=8080

DB_CONNECTION=pgsql
DB_HOST=localhost
DB_PORT=5432
DB_DATABASE=pbo2026_todolist
DB_USERNAME=postgres
DB_PASSWORD=postgres

ROOT_USERNAME=admin
ROOT_PASSWORD=admin

UPLOAD_DIR=uploads

OPENAI_ENDPOINT=https://delcom.org/api/v1
OPENAI_KEY=key
OPENAI_MODEL=nama-model
```

Sesuaikan `DB_*` dengan Postgres lokal Anda, dan isi `OPENAI_*` dengan endpoint/key/model yang valid.

Akun admin di-seed otomatis saat start dari `ROOT_USERNAME` / `ROOT_PASSWORD`. Upload disimpan di `UPLOAD_DIR` dan dilayani di `/uploads/**`.

`DotEnv` hanya mengisi system property jika variabel lingkungan / property dengan nama yang sama **belum** ada. Di server/cloud, nilai `DB_*` dari environment platform yang dipakai.

**Env DB yang didukung (selaras platform deploy):**

| Variabel | Keterangan |
| --- | --- |
| `DB_CONNECTION` | `pgsql` (informasional; driver tetap PostgreSQL) |
| `DB_HOST` | host Postgres/PgBouncer |
| `DB_PORT` | port (contoh cloud: `6432`) |
| `DB_DATABASE` | nama database (utama; di cloud: `db_<website_id>` dengan `-` diganti `_`) |
| `DB_USERNAME` | user DB |
| `DB_PASSWORD` | password DB |

`application.yaml` memakai `${DB_DATABASE}` tanpa default — wajib diisi lewat `.env` / environment.

## Fitur

- Registrasi, login, logout (`AuthSession`: cookie + flag logout)
- Todo per pengguna: judul, deskripsi, cover image opsional, status selesai
- **Generate AI**: buat daftar todo dari prompt teks bebas (tombol *Generate AI*)
- Klik cover → modal zoom (`ImagePreviewDialog`)
- Profil: nama, password, foto
- Role **ADMIN**: daftar pengguna + lihat todo user lain (read-only); admin punya todo sendiri
- Role **USER**: hanya todo sendiri
- UI: `MainLayout` (sidebar + menu avatar Profil/Keluar), kartu login/register, `styles.css` (`cursor: pointer`)

## Struktur singkat

```text
src/main/java/org/delcom/ifs18005springboottodolist/
├── domain/          # entity, exception, repository port, PasswordHasher, ITodoAiService
├── usecase/         # AuthUseCase, TodoUseCase, TodoAiUseCase, UserUseCase
├── adapter/         # Jdbc*/InMemory*, ai (OpenAiTodoService), FileStorage, presenter
├── framework/       # auth (AuthSession), view (Vaadin)
├── config/          # AppConfig (composition root)
├── DotEnv.java
└── App.java
```

Runtime memakai **JDBC + PostgreSQL**. Unit test repository memakai **InMemory**; tes JDBC memakai **H2**.

## Menjalankan

### 1. Buat database

```sql
CREATE DATABASE pbo2026_todolist;
```

Contoh lewat PowerShell (sesuaikan path `psql` dan password):

```powershell
$env:PGPASSWORD="postgres"
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -h localhost -c "CREATE DATABASE pbo2026_todolist;"
```

### 2. Siapkan `.env`

Dari root proyek:

```powershell
cd ifs18005-p6-pbo2026-springboot-todolist
Copy-Item .env.example .env
# edit .env: DB_PASSWORD, OPENAI_KEY, OPENAI_MODEL, dll.
```

### 3. Jalankan development

```powershell
.\mvnw.cmd spring-boot:run
```

Buka [http://localhost:8080](http://localhost:8080) (atau port `APP_PORT`).

Login admin: `ROOT_USERNAME` / `ROOT_PASSWORD` di `.env` (default `admin` / `admin`).

### 4. Build & jalankan JAR (profil production Vaadin)

Jalankan dari **root proyek** agar `.env` terbaca (jika tidak di-override environment):

```powershell
.\mvnw.cmd clean package -Pproduction
java -jar target\ifs18005-p6-pbo2026-springboot-todolist-0.0.1-SNAPSHOT.jar
```

Tanpa `-Pproduction`, menjalankan JAR biasanya gagal (`vaadin-dev-server not found`).

## Uji & coverage

```powershell
.\mvnw.cmd clean verify
```

- Jacoco mengecualikan `framework/view` dan `framework/auth`
- Laporan: `target/site/jacoco/index.html`
- Counter terukur wajib **100%** (INSTRUCTION, BRANCH, LINE, METHOD, CLASS)

## Troubleshooting JAR di server/cloud

Jika log berisi `Gagal menginisialisasi schema database` / `Connection ... refused`, **JAR sudah berhasil di-build**; yang gagal adalah koneksi database.

1. Pastikan PostgreSQL (atau PgBouncer) hidup dan menerima TCP dari host aplikasi.
2. Cocokkan `DB_HOST`, `DB_PORT`, `DB_DATABASE`, `DB_USERNAME`, `DB_PASSWORD` (environment server mengalahkan `.env`).
3. Pastikan database sudah ada / dibuat di platform.
4. Contoh error `Connection to delcom-deployment-pgbouncer:6432 refused` berarti host/port DB tidak reachable — perbaiki layanan DB/PgBouncer atau jaringan, bukan rebuild Vaadin.

Jika tombol **Generate AI** gagal: pastikan `OPENAI_ENDPOINT`, `OPENAI_KEY`, dan `OPENAI_MODEL` di `.env` / environment valid dan server AI dapat dijangkau.
