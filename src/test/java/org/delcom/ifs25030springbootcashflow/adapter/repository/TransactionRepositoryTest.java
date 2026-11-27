package org.delcom.ifs25030springbootcashflow.adapter.repository;

import org.delcom.ifs25030springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25030springbootcashflow.domain.entity.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransactionRepositoryTest {

    private TransactionRepository repository;

    @BeforeEach
    void setUp() {
        repository = new TransactionRepository();
    }

    @Test
    void saveAssignsIncrementalIds() {
        Transaction first = repository.save(1, "Gaji", 100L, TransactionType.INCOME);
        Transaction second = repository.save(1, "Makan", 10L, TransactionType.EXPENSE);

        assertEquals(1, first.getId());
        assertEquals(2, second.getId());
        assertEquals(2, repository.findAll().size());
        assertEquals(2, repository.findByUserId(1).size());
        assertTrue(repository.findByUserId(2).isEmpty());
    }

    @Test
    void findByIdAndDelete() {
        Transaction transaction = repository.save(1, "Hapus saya", 5L, TransactionType.EXPENSE);

        Optional<Transaction> found = repository.findById(transaction.getId());
        assertTrue(found.isPresent());
        assertEquals("Hapus saya", found.get().getDescription());

        assertTrue(repository.deleteById(transaction.getId()));
        assertFalse(repository.deleteById(transaction.getId()));
        assertTrue(repository.findById(transaction.getId()).isEmpty());
    }

    @Test
    void findByPredicateAndUpdateNoOp() {
        repository.save(1, "Beli buku", 50L, TransactionType.EXPENSE);
        repository.save(1, "Beli pulsa", 20L, TransactionType.EXPENSE);

        List<Transaction> pulsa = repository.findBy(t -> t.getDescription().contains("pulsa"));
        assertEquals(1, pulsa.size());

        Transaction transaction = pulsa.get(0);
        transaction.changeDescription("Beli pulsa data");
        repository.update(transaction);

        assertEquals("Beli pulsa data", repository.findById(transaction.getId()).orElseThrow().getDescription());
    }

    @Test
    void findAllReturnsDefensiveCopy() {
        repository.save(1, "A", 1L, TransactionType.INCOME);
        List<Transaction> all = repository.findAll();
        all.clear();
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void saveEntityDirectly() {
        Transaction entity = new Transaction(99, 1, "Manual", 1L, TransactionType.INCOME);
        repository.save(entity);
        assertTrue(repository.findById(99).isPresent());
    }
}
