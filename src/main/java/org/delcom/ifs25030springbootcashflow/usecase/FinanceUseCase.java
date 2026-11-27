package org.delcom.ifs25030springbootcashflow.usecase;

import org.delcom.ifs25030springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25030springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25030springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25030springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25030springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25030springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25030springbootcashflow.domain.repository.ITransactionRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Use case yang menangani logika bisnis aplikasi cashflow.
 * Tidak melakukan I/O UI — hanya memproses data dan melempar checked exception
 * domain jika operasi tidak valid atau data tidak ditemukan.
 */
public class FinanceUseCase {
    /** Port repository yang di-inject dari luar (Dependency Inversion). */
    private final ITransactionRepository transactionRepository;

    public FinanceUseCase(ITransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    /** Mengambil semua transaksi milik satu pengguna, terbaru di atas. */
    public List<Transaction> getTransactionsByUser(int userId) {
        return listTransactions(userId, null, TransactionTypeFilter.ALL, SortOption.NEWEST_FIRST);
    }

    /**
     * Mengambil transaksi pengguna dengan pencarian keterangan, filter tipe, dan urutan.
     * Keyword kosong/null diabaikan. Filter/sort null memakai default (semua + terbaru).
     */
    public List<Transaction> listTransactions(
            int userId,
            String keyword,
            TransactionTypeFilter typeFilter,
            SortOption sortOption
    ) {
        TransactionTypeFilter filter = typeFilter == null ? TransactionTypeFilter.ALL : typeFilter;
        SortOption sort = sortOption == null ? SortOption.NEWEST_FIRST : sortOption;

        Stream<Transaction> stream = transactionRepository.findByUserId(userId).stream()
                .filter(filter::matches);

        if (keyword != null && !keyword.isBlank()) {
            String lowerKeyword = keyword.trim().toLowerCase();
            stream = stream.filter(t -> t.getDescription().toLowerCase().contains(lowerKeyword));
        }

        return stream.sorted(sort.comparator()).toList();
    }

    /** Mengambil semua transaksi yang tersedia (admin). */
    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAll();
    }

    /** Menambahkan pemasukan baru setelah validasi. */
    public Transaction addIncome(int userId, String description, Long amount) throws ValidationException {
        return addTransaction(userId, description, amount, TransactionType.INCOME);
    }

    /** Menambahkan pengeluaran baru setelah validasi. */
    public Transaction addExpense(int userId, String description, Long amount) throws ValidationException {
        return addTransaction(userId, description, amount, TransactionType.EXPENSE);
    }

    /** Menambahkan transaksi baru setelah validasi keterangan, jumlah, dan tipe. */
    public Transaction addTransaction(int userId, String description, Long amount, TransactionType type)
            throws ValidationException {
        validate(description, amount);
        if (type == null) {
            throw new ValidationException("Tipe transaksi tidak boleh kosong");
        }
        return transactionRepository.save(userId, description.trim(), amount, type);
    }

    /**
     * Mengubah teks jumlah dari form menjadi angka rupiah.
     * Menerima angka bulat; spasi, koma, dan titik pemisah ribuan (1.000.000) diabaikan.
     * Teks non-numerik, desimal, atau kosong ditolak.
     */
    public Long parseAmount(String raw) throws ValidationException {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ValidationException("Jumlah harus berupa angka lebih dari 0");
        }
        String cleaned = raw.trim().replace(",", "").replace(" ", "");
        if (cleaned.matches("\\d{1,3}(\\.\\d{3})+")) {
            cleaned = cleaned.replace(".", "");
        }
        if (!cleaned.matches("\\d{1,18}")) {
            throw new ValidationException("Jumlah harus berupa angka lebih dari 0");
        }
        return Long.parseLong(cleaned);
    }

    /** Menghapus transaksi berdasarkan ID; melempar exception jika ID tidak valid / tidak ditemukan. */
    public void removeTransaction(int id) throws EntityNotFoundException, ValidationException {
        if (id <= 0) {
            throw new ValidationException("ID tidak valid");
        }
        if (!transactionRepository.deleteById(id)) {
            throw new EntityNotFoundException(id);
        }
    }

    /** Menghapus transaksi milik pengguna tertentu. */
    public void removeTransactionForUser(int userId, int id) throws EntityNotFoundException, ValidationException {
        Transaction transaction = requireOwnedTransaction(userId, id);
        removeTransaction(transaction.getId());
    }

    /**
     * Mengubah keterangan, jumlah, dan/atau bukti transaksi milik pengguna.
     * Parameter {@code null} berarti field tersebut tidak diubah
     * (kecuali {@code clearProof=true} untuk menghapus bukti).
     */
    public void updateTransactionForUser(
            int userId,
            int id,
            String description,
            Long amount,
            String proofPath,
            boolean clearProof
    ) throws EntityNotFoundException, ValidationException {
        Transaction transaction = requireOwnedTransaction(userId, id);
        if (description != null) {
            if (description.trim().isEmpty()) {
                throw new ValidationException("Keterangan tidak boleh kosong");
            }
            transaction.changeDescription(description.trim());
        }
        if (amount != null) {
            if (amount <= 0) {
                throw new ValidationException("Jumlah harus berupa angka lebih dari 0");
            }
            transaction.changeAmount(amount);
        }
        if (clearProof) {
            transaction.changeProofPath(null);
        } else if (proofPath != null) {
            transaction.changeProofPath(proofPath);
        }
        transactionRepository.update(transaction);
    }

    /** Mencari transaksi pengguna berdasarkan keterangan (case-insensitive), terbaru di atas. */
    public List<Transaction> searchTransactions(int userId, String keyword) {
        return listTransactions(userId, keyword, TransactionTypeFilter.ALL, SortOption.NEWEST_FIRST);
    }

    /** Mengurutkan transaksi pengguna sesuai kriteria {@link SortOption}. */
    public List<Transaction> sortTransactions(int userId, SortOption option) {
        return listTransactions(userId, null, TransactionTypeFilter.ALL, option);
    }

    /** Total seluruh pemasukan milik pengguna. */
    public long getTotalIncome(int userId) {
        return transactionRepository.findByUserId(userId).stream()
                .filter(Transaction::isIncome)
                .mapToLong(Transaction::getAmount)
                .sum();
    }

    /** Total seluruh pengeluaran milik pengguna. */
    public long getTotalExpense(int userId) {
        return transactionRepository.findByUserId(userId).stream()
                .filter(Transaction::isExpense)
                .mapToLong(Transaction::getAmount)
                .sum();
    }

    /** Saldo = total pemasukan - total pengeluaran milik pengguna. */
    public long getBalance(int userId) {
        return getTotalIncome(userId) - getTotalExpense(userId);
    }

    private Transaction requireOwnedTransaction(int userId, int id)
            throws EntityNotFoundException, ValidationException {
        if (id <= 0) {
            throw new ValidationException("ID tidak valid");
        }
        Optional<Transaction> found = transactionRepository.findById(id);
        if (found.isEmpty()) {
            throw new EntityNotFoundException(id);
        }
        Transaction transaction = found.get();
        if (transaction.getUserId() != userId) {
            throw new ValidationException("Transaksi tidak milik pengguna ini");
        }
        return transaction;
    }

    private void validate(String description, Long amount) throws ValidationException {
        if (description == null || description.trim().isEmpty()) {
            throw new ValidationException("Keterangan tidak boleh kosong");
        }
        if (amount == null || amount <= 0) {
            throw new ValidationException("Jumlah harus berupa angka lebih dari 0");
        }
    }
}
