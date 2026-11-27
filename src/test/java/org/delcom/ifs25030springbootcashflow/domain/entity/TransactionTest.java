package org.delcom.ifs25030springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionTest {

    @Test
    void shortConstructorSetsDefaults() {
        Transaction transaction = new Transaction(1, 7, "Gaji", 5_000_000L, TransactionType.INCOME);

        assertEquals(1, transaction.getId());
        assertEquals(7, transaction.getUserId());
        assertEquals("Gaji", transaction.getDescription());
        assertEquals(5_000_000L, transaction.getAmount());
        assertEquals(TransactionType.INCOME, transaction.getType());
        assertNull(transaction.getProofPath());
        assertNotNull(transaction.getCreatedAt());
    }

    @Test
    void fullConstructorKeepsAllValues() {
        LocalDateTime time = LocalDateTime.of(2026, 1, 2, 3, 4);
        Transaction transaction = new Transaction(2, 3, "Makan", 25_000L, TransactionType.EXPENSE, "proofs/a.png", time);

        assertEquals("proofs/a.png", transaction.getProofPath());
        assertEquals(time, transaction.getCreatedAt());
    }

    @Test
    void typeHelpersAndSignedAmount() {
        Transaction income = new Transaction(1, 1, "Gaji", 100L, TransactionType.INCOME);
        Transaction expense = new Transaction(2, 1, "Makan", 40L, TransactionType.EXPENSE);

        assertTrue(income.isIncome());
        assertFalse(income.isExpense());
        assertEquals(100L, income.signedAmount());

        assertTrue(expense.isExpense());
        assertFalse(expense.isIncome());
        assertEquals(-40L, expense.signedAmount());
    }

    @Test
    void mutatorsChangeState() {
        Transaction transaction = new Transaction(1, 1, "Lama", 10L, TransactionType.INCOME);

        transaction.changeDescription("Baru");
        transaction.changeAmount(99L);
        transaction.changeProofPath("proofs/x.png");

        assertEquals("Baru", transaction.getDescription());
        assertEquals(99L, transaction.getAmount());
        assertEquals("proofs/x.png", transaction.getProofPath());

        transaction.changeProofPath(null);
        assertNull(transaction.getProofPath());
    }
}
