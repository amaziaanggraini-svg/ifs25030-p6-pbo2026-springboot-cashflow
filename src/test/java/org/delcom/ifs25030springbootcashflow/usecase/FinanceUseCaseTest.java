package org.delcom.ifs25030springbootcashflow.usecase;

import org.delcom.ifs25030springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25030springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25030springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25030springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25030springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25030springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25030springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceUseCaseTest {

    private static final int USER = 1;
    private static final int OTHER = 2;

    private TransactionRepository repository;
    private FinanceUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new TransactionRepository();
        useCase = new FinanceUseCase(repository);
    }

    @Test
    void addIncomeAndExpenseTrimDescription() throws Exception {
        Transaction income = useCase.addIncome(USER, "  Gaji  ", 5_000_000L);
        Transaction expense = useCase.addExpense(USER, "Makan", 25_000L);

        assertEquals("Gaji", income.getDescription());
        assertEquals(TransactionType.INCOME, income.getType());
        assertEquals(TransactionType.EXPENSE, expense.getType());
        assertEquals(2, useCase.getTransactionsByUser(USER).size());
    }

    @Test
    void addTransactionRejectsInvalidInput() {
        assertThrows(ValidationException.class, () -> useCase.addIncome(USER, null, 10L));
        assertThrows(ValidationException.class, () -> useCase.addIncome(USER, "   ", 10L));
        assertThrows(ValidationException.class, () -> useCase.addIncome(USER, "A", null));
        assertThrows(ValidationException.class, () -> useCase.addIncome(USER, "A", 0L));
        assertThrows(ValidationException.class, () -> useCase.addExpense(USER, "A", -5L));
        assertThrows(ValidationException.class, () -> useCase.addTransaction(USER, "A", 10L, null));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void parseAmountAcceptsIntegersAndThousandSeparators() throws Exception {
        assertEquals(1000000L, useCase.parseAmount("1000000"));
        assertEquals(1000000L, useCase.parseAmount("  1,000,000 "));
        assertEquals(1000000L, useCase.parseAmount("1.000.000"));
        assertEquals(25000L, useCase.parseAmount("25 000"));
        assertEquals(0L, useCase.parseAmount("0"));
    }

    @Test
    void parseAmountRejectsNonNumericInput() {
        assertThrows(ValidationException.class, () -> useCase.parseAmount(null));
        assertThrows(ValidationException.class, () -> useCase.parseAmount("   "));
        assertThrows(ValidationException.class, () -> useCase.parseAmount("abc"));
        assertThrows(ValidationException.class, () -> useCase.parseAmount("12.5"));
        assertThrows(ValidationException.class, () -> useCase.parseAmount("-100"));
        assertThrows(ValidationException.class, () -> useCase.parseAmount("1234567890123456789"));
    }

    @Test
    void listTransactionsFiltersSearchesAndSorts() throws Exception {
        useCase.addIncome(USER, "Gaji bulanan", 5_000L);
        useCase.addExpense(USER, "Makan siang", 100L);
        useCase.addExpense(USER, "Makan malam", 300L);
        useCase.addIncome(OTHER, "Milik orang lain", 1L);

        assertEquals(3, useCase.listTransactions(USER, null, null, null).size());
        assertEquals(3, useCase.getTransactionsByUser(USER).size());

        List<Transaction> expenses = useCase.listTransactions(USER, "", TransactionTypeFilter.EXPENSE, SortOption.AMOUNT_DESC);
        assertEquals(2, expenses.size());
        assertEquals("Makan malam", expenses.get(0).getDescription());

        assertEquals(1, useCase.listTransactions(USER, "  GAJI ", TransactionTypeFilter.ALL, SortOption.NEWEST_FIRST).size());
        assertEquals(2, useCase.searchTransactions(USER, "makan").size());
        assertEquals("Makan malam", useCase.sortTransactions(USER, SortOption.AMOUNT_DESC).get(0).getDescription());
        assertEquals(4, useCase.getAllTransactions().size());
    }

    @Test
    void totalsAndBalanceFollowUserTransactions() throws Exception {
        useCase.addIncome(USER, "Gaji", 5_000_000L);
        useCase.addExpense(USER, "Kos", 25_000L);
        useCase.addIncome(OTHER, "Bukan saya", 999L);

        assertEquals(5_000_000L, useCase.getTotalIncome(USER));
        assertEquals(25_000L, useCase.getTotalExpense(USER));
        assertEquals(4_975_000L, useCase.getBalance(USER));
        assertEquals(999L, useCase.getBalance(OTHER));
    }

    @Test
    void removeTransactionValidatesIdAndExistence() throws Exception {
        Transaction saved = useCase.addIncome(USER, "Hapus", 10L);

        assertThrows(ValidationException.class, () -> useCase.removeTransaction(0));
        assertThrows(ValidationException.class, () -> useCase.removeTransaction(-1));
        assertThrows(EntityNotFoundException.class, () -> useCase.removeTransaction(999));

        useCase.removeTransaction(saved.getId());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void removeTransactionForUserChecksOwnership() throws Exception {
        Transaction saved = useCase.addIncome(USER, "Milik saya", 10L);

        assertThrows(ValidationException.class, () -> useCase.removeTransactionForUser(OTHER, saved.getId()));
        assertThrows(ValidationException.class, () -> useCase.removeTransactionForUser(USER, 0));
        assertThrows(EntityNotFoundException.class, () -> useCase.removeTransactionForUser(USER, 999));

        useCase.removeTransactionForUser(USER, saved.getId());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void updateTransactionForUserAppliesChangesSelectively() throws Exception {
        Transaction saved = useCase.addExpense(USER, "Awal", 10L);

        useCase.updateTransactionForUser(USER, saved.getId(), null, null, null, false);
        assertEquals("Awal", saved.getDescription());
        assertEquals(10L, saved.getAmount());
        assertNull(saved.getProofPath());

        useCase.updateTransactionForUser(USER, saved.getId(), "  Baru ", 20L, "proofs/a.png", false);
        assertEquals("Baru", saved.getDescription());
        assertEquals(20L, saved.getAmount());
        assertEquals("proofs/a.png", saved.getProofPath());

        useCase.updateTransactionForUser(USER, saved.getId(), null, null, "proofs/ignored.png", true);
        assertNull(saved.getProofPath());
    }

    @Test
    void updateTransactionForUserRejectsInvalidInput() throws Exception {
        Transaction saved = useCase.addExpense(USER, "Awal", 10L);

        assertThrows(ValidationException.class,
                () -> useCase.updateTransactionForUser(USER, saved.getId(), "  ", null, null, false));
        assertThrows(ValidationException.class,
                () -> useCase.updateTransactionForUser(USER, saved.getId(), null, 0L, null, false));
        assertThrows(ValidationException.class,
                () -> useCase.updateTransactionForUser(OTHER, saved.getId(), "X", null, null, false));
        assertThrows(EntityNotFoundException.class,
                () -> useCase.updateTransactionForUser(USER, 999, "X", null, null, false));
        assertEquals("Awal", saved.getDescription());
    }
}
