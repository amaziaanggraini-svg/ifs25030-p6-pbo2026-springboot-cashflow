package org.delcom.ifs25030springbootcashflow.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiGeneratedTransactionTest {

    @Test
    void trimsDescriptionAndKeepsValues() {
        AiGeneratedTransaction item = new AiGeneratedTransaction("  Gaji  ", 5_000_000L, TransactionType.INCOME);

        assertEquals("Gaji", item.description());
        assertEquals(5_000_000L, item.amount());
        assertEquals(TransactionType.INCOME, item.type());
    }

    @Test
    void rejectsBlankDescription() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiGeneratedTransaction(null, 10, TransactionType.INCOME));
        assertThrows(IllegalArgumentException.class,
                () -> new AiGeneratedTransaction("   ", 10, TransactionType.INCOME));
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThrows(IllegalArgumentException.class,
                () -> new AiGeneratedTransaction("A", 0, TransactionType.INCOME));
        assertThrows(IllegalArgumentException.class,
                () -> new AiGeneratedTransaction("A", -5, TransactionType.EXPENSE));
    }

    @Test
    void rejectsNullType() {
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTransaction("A", 10, null));
    }
}
