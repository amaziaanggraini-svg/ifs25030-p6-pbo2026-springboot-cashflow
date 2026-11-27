package org.delcom.ifs25030springbootcashflow.domain.entity;

/**
 * Filter tipe transaksi untuk daftar yang ditampilkan di UI.
 */
public enum TransactionTypeFilter {
    ALL,
    INCOME,
    EXPENSE;

    public String label() {
        return switch (this) {
            case ALL -> "Semua tipe";
            case INCOME -> "Pemasukan";
            case EXPENSE -> "Pengeluaran";
        };
    }

    public boolean matches(Transaction transaction) {
        return switch (this) {
            case ALL -> true;
            case INCOME -> transaction.isIncome();
            case EXPENSE -> transaction.isExpense();
        };
    }
}
