package org.delcom.ifs25030springbootcashflow.adapter.presenter;

import org.delcom.ifs25030springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25030springbootcashflow.domain.entity.TransactionType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancePresenterTest {

    private final FinancePresenter presenter = new FinancePresenter();

    private Transaction income() {
        return new Transaction(1, 1, "Gaji", 1_000_000L, TransactionType.INCOME, null,
                LocalDateTime.of(2026, 3, 4, 5, 6));
    }

    @Test
    void formatsMoneyWithThousandSeparatorsAndTwoDecimals() {
        assertEquals("Rp 1,000,000.00", presenter.formatMoney(1_000_000L));
        assertEquals("Rp 0.00", presenter.formatMoney(0L));
        assertEquals("-Rp 25,000.00", presenter.formatMoney(-25_000L));
    }

    @Test
    void formatsTypeAndBalance() {
        assertEquals("Pemasukan", presenter.formatType(TransactionType.INCOME));
        assertEquals("Pengeluaran", presenter.formatType(TransactionType.EXPENSE));
        assertEquals("Saldo: Rp 4,975,000.00", presenter.formatBalance(4_975_000L));
    }

    @Test
    void formatsSingleLineAndDate() {
        assertEquals("[1] Gaji | Rp 1,000,000.00 | [Pemasukan]", presenter.format(income()));
        assertEquals("04/03/2026 05:06", presenter.formatCreatedAt(income()));
    }

    @Test
    void formatsListsWithAndWithoutData() {
        String filled = presenter.showTransactions(List.of(income()));
        assertTrue(filled.startsWith("Daftar Transaksi:\n[1] Gaji"));

        assertTrue(presenter.showTransactions(List.of()).contains("belum tersedia"));
        assertTrue(presenter.showSearchResults(List.of(), "x").contains("tidak ditemukan"));
        assertTrue(presenter.showSearchResults(List.of(income()), "gaji").contains("Hasil Pencarian: \"gaji\""));
        assertTrue(presenter.showSortedTransactions(List.of(income())).startsWith("Daftar Transaksi (Terurut):"));
        assertTrue(presenter.showSortedTransactions(List.of()).contains("belum tersedia"));
    }

    @Test
    void formatsMessages() {
        Transaction expense = new Transaction(2, 1, "Makan", 25_000L, TransactionType.EXPENSE);
        assertEquals("Berhasil menambah pemasukan: [1] Gaji | Rp 1,000,000.00 | [Pemasukan]",
                presenter.showAddSuccess(income()));
        assertTrue(presenter.showAddSuccess(expense).startsWith("Berhasil menambah pengeluaran: "));
        assertEquals("Berhasil menambahkan 3 transaksi dari AI", presenter.showAiAddSuccess(3));
        assertEquals("Berhasil menghapus transaksi.", presenter.showRemoveSuccess());
        assertEquals("[!] Gagal menghapus transaksi dengan ID: 7.", presenter.showRemoveFailed(7));
        assertEquals("Berhasil mengubah transaksi.", presenter.showUpdateSuccess());
        assertEquals("[!] Gagal mengubah transaksi dengan ID: 7.", presenter.showUpdateFailed(7));
        assertEquals("[!] Pilihan tidak dimengerti.", presenter.showInvalidChoice());
        assertEquals("[!] ID tidak valid!", presenter.showInvalidId());
        assertEquals("[!] oops", presenter.showValidationError("oops"));
    }
}
