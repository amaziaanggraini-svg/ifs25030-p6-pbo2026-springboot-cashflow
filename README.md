# ifs25030-p6-pbo2026-springboot-cashflow

Aplikasi web pencatat keuangan (Cashflow) berbasis **Spring Boot 4.1.2**, **Vaadin 25.3.4**, **JDBC PostgreSQL**, arsitektur **Clean Architecture**, dengan **Generate AI** (OpenAI-compatible).

| Item | Nilai |
|---|---|
| Artifact Maven | `ifs25030-p6-pbo2026-springboot-cashflow` |
| Package | `org.delcom.ifs25030springbootcashflow` |
| Database | `pbo2026_cashflow` |

## Menjalankan
```sql
CREATE DATABASE pbo2026_cashflow;
```
```powershell
copy .env.example .env   # isi DB_*, ROOT_*, OPENAI_*
.\mvnw.cmd spring-boot:run
.\mvnw.cmd clean verify -Pproduction   # test + Jacoco 100%
```

## Rute
| Path | View | Akses |
|---|---|---|
| `/login`, `/register` | LoginView, RegisterView | Publik |
| `/` | FinanceView | USER & ADMIN |
| `/profile` | ProfileView | USER & ADMIN |
| `/users` | UsersView | ADMIN |
| `/user-transactions/{id}` | UserTransactionsView | ADMIN (read-only) |

## Fitur
Register/login/logout, tambah pemasukan/pengeluaran + bukti (zoom), cari/filter tipe/urutan (default terbaru), kartu saldo (`Rp 1,000,000.00`), hapus dengan konfirmasi, Generate AI (prompt → preview → simpan), profil, dan admin melihat transaksi user lain.
