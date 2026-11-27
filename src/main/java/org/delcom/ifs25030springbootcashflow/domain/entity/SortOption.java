package org.delcom.ifs25030springbootcashflow.domain.entity;

import java.util.Comparator;

/**
 * Kriteria pengurutan transaksi.
 * Setiap opsi membawa comparator-nya sendiri sehingga logika pengurutan
 * terpusat di domain, bukan tersebar sebagai angka ajaib di view/use case.
 */
public enum SortOption {
    /** Transaksi terbaru (createdAt / id terbesar) di atas. */
    NEWEST_FIRST(Comparator
            .comparing(Transaction::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Transaction::getId)
            .reversed()),

    /** Transaksi terlama di atas. */
    OLDEST_FIRST(Comparator
            .comparing(Transaction::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Transaction::getId)),

    /** Jumlah terkecil lebih dulu. */
    AMOUNT_ASC(Comparator.comparingLong(Transaction::getAmount)
            .thenComparing(Transaction::getId, Comparator.reverseOrder())),

    /** Jumlah terbesar lebih dulu. */
    AMOUNT_DESC(Comparator.comparingLong(Transaction::getAmount).reversed()
            .thenComparing(Transaction::getId, Comparator.reverseOrder())),

    /** Pemasukan ditampilkan lebih dulu, lalu yang terbaru. */
    INCOME_FIRST(Comparator.comparing(Transaction::getType)
            .thenComparing(Transaction::getId, Comparator.reverseOrder())),

    /** Pengeluaran ditampilkan lebih dulu, lalu yang terbaru. */
    EXPENSE_FIRST(Comparator.comparing(Transaction::getType).reversed()
            .thenComparing(Transaction::getId, Comparator.reverseOrder()));

    /** Comparator yang digunakan untuk mengurutkan daftar transaksi. */
    private final Comparator<Transaction> comparator;

    SortOption(Comparator<Transaction> comparator) {
        this.comparator = comparator;
    }

    /** Mengembalikan comparator yang sesuai dengan opsi ini. */
    public Comparator<Transaction> comparator() {
        return comparator;
    }

    /** Label singkat untuk ditampilkan di UI. */
    public String label() {
        return switch (this) {
            case NEWEST_FIRST -> "Terbaru";
            case OLDEST_FIRST -> "Terlama";
            case AMOUNT_ASC -> "Jumlah terkecil";
            case AMOUNT_DESC -> "Jumlah terbesar";
            case INCOME_FIRST -> "Pemasukan dulu";
            case EXPENSE_FIRST -> "Pengeluaran dulu";
        };
    }
}
