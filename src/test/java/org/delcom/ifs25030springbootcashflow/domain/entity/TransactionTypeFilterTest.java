package org.delcom.ifs25030springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTypeFilterTest {

    private final Transaction income = new Transaction(1, 1, "Gaji", 100, TransactionType.INCOME);
    private final Transaction expense = new Transaction(2, 1, "Makan", 10, TransactionType.EXPENSE);

    @Test
    void matchesAccordingToType() {
        assertTrue(TransactionTypeFilter.ALL.matches(income));
        assertTrue(TransactionTypeFilter.ALL.matches(expense));

        assertTrue(TransactionTypeFilter.INCOME.matches(income));
        assertFalse(TransactionTypeFilter.INCOME.matches(expense));

        assertTrue(TransactionTypeFilter.EXPENSE.matches(expense));
        assertFalse(TransactionTypeFilter.EXPENSE.matches(income));
    }

    @Test
    void labelsAreReadable() {
        assertEquals("Semua tipe", TransactionTypeFilter.ALL.label());
        assertEquals("Pemasukan", TransactionTypeFilter.INCOME.label());
        assertEquals("Pengeluaran", TransactionTypeFilter.EXPENSE.label());
    }
}
