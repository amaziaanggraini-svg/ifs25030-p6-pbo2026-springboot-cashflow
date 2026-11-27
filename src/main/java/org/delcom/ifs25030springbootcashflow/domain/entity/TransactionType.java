package org.delcom.ifs25030springbootcashflow.domain.entity;

/** Jenis transaksi keuangan. */
public enum TransactionType {
    /** Pemasukan: menambah saldo. */
    INCOME,
    /** Pengeluaran: mengurangi saldo. */
    EXPENSE
}
