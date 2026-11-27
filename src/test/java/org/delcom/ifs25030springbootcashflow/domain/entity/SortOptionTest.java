package org.delcom.ifs25030springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SortOptionTest {

    private static final LocalDateTime OLDER = LocalDateTime.of(2026, 1, 1, 8, 0);
    private static final LocalDateTime NEWER = LocalDateTime.of(2026, 1, 2, 8, 0);

    private static Transaction tx(int id, String desc, long amount, TransactionType type, LocalDateTime at) {
        return new Transaction(id, 1, desc, amount, type, null, at);
    }

    @Test
    void sortsByNewestAndOldest() {
        List<Transaction> list = new ArrayList<>();
        list.add(tx(1, "Lama", 10, TransactionType.INCOME, OLDER));
        list.add(tx(2, "Baru", 10, TransactionType.INCOME, NEWER));

        list.sort(SortOption.NEWEST_FIRST.comparator());
        assertEquals("Baru", list.get(0).getDescription());

        list.sort(SortOption.OLDEST_FIRST.comparator());
        assertEquals("Lama", list.get(0).getDescription());
    }

    @Test
    void sortsByAmountAscendingAndDescending() {
        List<Transaction> list = new ArrayList<>();
        list.add(tx(1, "Besar", 500, TransactionType.INCOME, OLDER));
        list.add(tx(2, "Kecil", 50, TransactionType.EXPENSE, OLDER));
        list.add(tx(3, "Kecil juga", 50, TransactionType.EXPENSE, NEWER));

        list.sort(SortOption.AMOUNT_ASC.comparator());
        assertEquals(3, list.get(0).getId());
        assertEquals(2, list.get(1).getId());
        assertEquals("Besar", list.get(2).getDescription());

        list.sort(SortOption.AMOUNT_DESC.comparator());
        assertEquals("Besar", list.get(0).getDescription());
        assertEquals(3, list.get(1).getId());
    }

    @Test
    void sortsByTypeFirst() {
        List<Transaction> list = new ArrayList<>();
        list.add(tx(1, "Makan", 10, TransactionType.EXPENSE, OLDER));
        list.add(tx(2, "Gaji", 10, TransactionType.INCOME, OLDER));
        list.add(tx(3, "Bonus", 10, TransactionType.INCOME, NEWER));

        list.sort(SortOption.INCOME_FIRST.comparator());
        assertEquals(3, list.get(0).getId());
        assertEquals(2, list.get(1).getId());
        assertEquals(TransactionType.EXPENSE, list.get(2).getType());

        list.sort(SortOption.EXPENSE_FIRST.comparator());
        assertEquals(TransactionType.EXPENSE, list.get(0).getType());
        assertEquals(3, list.get(1).getId());
    }

    @Test
    void newestFirstHandlesNullCreatedAt() {
        List<Transaction> list = new ArrayList<>();
        list.add(tx(1, "Tanpa waktu", 10, TransactionType.INCOME, null));
        list.add(tx(2, "Ada waktu", 10, TransactionType.INCOME, OLDER));

        list.sort(SortOption.OLDEST_FIRST.comparator());
        assertEquals(2, list.get(0).getId());
    }

    @Test
    void labelsAreReadable() {
        assertEquals("Terbaru", SortOption.NEWEST_FIRST.label());
        assertEquals("Terlama", SortOption.OLDEST_FIRST.label());
        assertEquals("Jumlah terkecil", SortOption.AMOUNT_ASC.label());
        assertEquals("Jumlah terbesar", SortOption.AMOUNT_DESC.label());
        assertEquals("Pemasukan dulu", SortOption.INCOME_FIRST.label());
        assertEquals("Pengeluaran dulu", SortOption.EXPENSE_FIRST.label());
    }
}
