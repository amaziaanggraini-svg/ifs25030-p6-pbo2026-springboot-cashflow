package org.delcom.ifs25030springbootcashflow.domain.repository;

import org.delcom.ifs25030springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25030springbootcashflow.domain.exception.DomainException;

import java.util.List;

/**
 * Port layanan AI untuk menghasilkan transaksi keuangan dari catatan bebas.
 */
public interface IFinanceAiService {

    /**
     * Menghasilkan daftar transaksi dari prompt teks bebas menggunakan model AI.
     *
     * @param prompt catatan keuangan dari pengguna
     * @return daftar transaksi hasil generasi
     * @throws DomainException jika terjadi kegagalan komunikasi AI atau parsing
     */
    List<AiGeneratedTransaction> generateTransactions(String prompt) throws DomainException;
}
