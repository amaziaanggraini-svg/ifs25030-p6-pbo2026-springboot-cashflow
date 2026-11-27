package org.delcom.ifs25030springbootcashflow.adapter.repository;

import org.delcom.ifs25030springbootcashflow.domain.entity.Role;
import org.delcom.ifs25030springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25030springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25030springbootcashflow.domain.entity.User;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdbcRepositoryTest {

    private JdbcUserRepository userRepository;
    private JdbcTransactionRepository transactionRepository;

    @BeforeEach
    void setUp() {
        DataSource dataSource = createDataSource();
        SchemaInitializer.initialize(dataSource);
        userRepository = new JdbcUserRepository(dataSource);
        transactionRepository = new JdbcTransactionRepository(dataSource);
    }

    private DataSource createDataSource() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:jdbc_repo_" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    @Test
    void userCrudAndFinders() {
        User saved = userRepository.saveNew("andi", "hash", "Andi", Role.USER);
        assertTrue(saved.getId() > 0);

        assertTrue(userRepository.findByUsername("ANDI").isPresent());
        assertEquals(1, userRepository.findAll().size());
        assertTrue(userRepository.findById(saved.getId()).isPresent());
        assertTrue(userRepository.findBy(user -> user.getUsername().equals("andi")).size() == 1);

        saved.changeFullName("Andi Baru");
        saved.changePhotoPath("profiles/a.png");
        userRepository.update(saved);
        assertEquals("Andi Baru", userRepository.findById(saved.getId()).orElseThrow().getFullName());

        User viaSave = userRepository.save(new User(0, "budi", "h", "Budi", Role.USER, null));
        assertTrue(viaSave.getId() > 0);

        assertTrue(userRepository.deleteById(viaSave.getId()));
        assertFalse(userRepository.deleteById(viaSave.getId()));
        assertTrue(userRepository.findByUsername("tidakada").isEmpty());
        assertTrue(userRepository.findById(999).isEmpty());
    }

    @Test
    void transactionCrudAndFinders() {
        User owner = userRepository.saveNew("owner", "hash", "Owner", Role.USER);
        Transaction income = transactionRepository.save(owner.getId(), "Gaji", 5_000_000L, TransactionType.INCOME);
        assertEquals(5_000_000L, income.getAmount());
        assertEquals(TransactionType.INCOME, income.getType());
        Transaction expense = transactionRepository.save(owner.getId(), "Makan", 25_000L, TransactionType.EXPENSE);
        assertNull(expense.getProofPath());

        Transaction viaEntity = transactionRepository.save(
                new Transaction(0, owner.getId(), "Lain", 100L, TransactionType.EXPENSE));
        assertTrue(viaEntity.getId() > 0);

        assertEquals(3, transactionRepository.findAll().size());
        assertEquals(3, transactionRepository.findByUserId(owner.getId()).size());
        assertTrue(transactionRepository.findByUserId(999).isEmpty());
        assertTrue(transactionRepository.findById(income.getId()).isPresent());
        assertTrue(transactionRepository.findById(999).isEmpty());
        assertEquals(1, transactionRepository.findBy(t -> t.getDescription().equals("Gaji")).size());

        income.changeDescription("Gaji Baru");
        income.changeAmount(6_000_000L);
        income.changeProofPath("proofs/x.png");
        transactionRepository.update(income);
        Transaction updated = transactionRepository.findById(income.getId()).orElseThrow();
        assertEquals("Gaji Baru", updated.getDescription());
        assertEquals(6_000_000L, updated.getAmount());
        assertEquals("proofs/x.png", updated.getProofPath());
        assertEquals(TransactionType.INCOME, updated.getType());

        assertTrue(transactionRepository.deleteById(income.getId()));
        assertFalse(transactionRepository.deleteById(income.getId()));
        List<Transaction> remaining = transactionRepository.findByUserId(owner.getId());
        assertEquals(2, remaining.size());
    }
}
