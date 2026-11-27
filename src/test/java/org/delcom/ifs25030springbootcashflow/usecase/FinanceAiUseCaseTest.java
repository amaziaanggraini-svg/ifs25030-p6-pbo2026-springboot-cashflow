package org.delcom.ifs25030springbootcashflow.usecase;

import org.delcom.ifs25030springbootcashflow.adapter.ai.InMemoryFinanceAiService;
import org.delcom.ifs25030springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25030springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25030springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25030springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25030springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinanceAiUseCaseTest {

    private TransactionRepository repository;
    private FinanceAiUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = new TransactionRepository();
        useCase = new FinanceAiUseCase(new InMemoryFinanceAiService(), repository);
    }

    @Test
    void generateTransactionsReturnsList() throws Exception {
        List<AiGeneratedTransaction> result = useCase.generateTransactions("Gajian dan makan");
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
    }

    @Test
    void generateTransactionsRejectsBlankPrompt() {
        assertThrows(ValidationException.class, () -> useCase.generateTransactions(null));
        assertThrows(ValidationException.class, () -> useCase.generateTransactions("   "));
    }

    @Test
    void saveGeneratedTransactionsPersistsToRepository() throws Exception {
        List<AiGeneratedTransaction> items = List.of(
                new AiGeneratedTransaction("Gaji", 5_000_000L, TransactionType.INCOME),
                new AiGeneratedTransaction("Makan", 25_000L, TransactionType.EXPENSE)
        );

        List<Transaction> saved = useCase.saveGeneratedTransactions(1, items);
        assertEquals(2, saved.size());
        assertEquals(2, repository.findByUserId(1).size());
        assertEquals(TransactionType.EXPENSE, saved.get(1).getType());
    }

    @Test
    void saveGeneratedTransactionsRejectsEmptyOrNullItems() {
        assertThrows(ValidationException.class, () -> useCase.saveGeneratedTransactions(1, null));
        assertThrows(ValidationException.class, () -> useCase.saveGeneratedTransactions(1, Collections.emptyList()));
    }
}
