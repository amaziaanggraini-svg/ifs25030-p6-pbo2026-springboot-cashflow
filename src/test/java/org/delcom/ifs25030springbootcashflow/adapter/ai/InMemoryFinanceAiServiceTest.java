package org.delcom.ifs25030springbootcashflow.adapter.ai;

import org.delcom.ifs25030springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25030springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25030springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25030springbootcashflow.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryFinanceAiServiceTest {

    @Test
    void generatesDefaultTransactions() throws Exception {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService();
        List<AiGeneratedTransaction> result = service.generateTransactions("Bulan ini");
        assertEquals(2, result.size());
        assertEquals(TransactionType.INCOME, result.get(0).type());
        assertEquals(TransactionType.EXPENSE, result.get(1).type());
    }

    @Test
    void usesCannedResponsesWhenProvided() throws Exception {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService(List.of(
                new AiGeneratedTransaction("Custom 1", 100, TransactionType.INCOME)
        ));
        service.addCannedResponse("Custom 2", 50, TransactionType.EXPENSE);

        List<AiGeneratedTransaction> result = service.generateTransactions("Any prompt");
        assertEquals(2, result.size());
        assertEquals("Custom 1", result.get(0).description());
        assertEquals("Custom 2", result.get(1).description());
    }

    @Test
    void handlesNullCannedListConstructor() throws Exception {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService(null);
        assertEquals(2, service.generateTransactions("Test").size());
    }

    @Test
    void rejectsBlankPrompt() {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService();
        assertThrows(ValidationException.class, () -> service.generateTransactions(null));
        assertThrows(ValidationException.class, () -> service.generateTransactions("   "));
    }

    @Test
    void simulatesFailure() {
        InMemoryFinanceAiService service = new InMemoryFinanceAiService();
        service.setSimulateFailure(true);
        assertThrows(DomainException.class, () -> service.generateTransactions("Test"));
    }
}
