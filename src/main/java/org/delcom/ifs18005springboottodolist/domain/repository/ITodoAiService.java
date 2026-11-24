package org.delcom.ifs18005springboottodolist.domain.repository;

import org.delcom.ifs18005springboottodolist.domain.entity.AiGeneratedTodo;
import org.delcom.ifs18005springboottodolist.domain.exception.DomainException;

import java.util.List;

/**
 * Port layanan AI untuk menghasilkan todo dari instruksi bebas.
 */
public interface ITodoAiService {

    /**
     * Menghasilkan daftar todo dari prompt teks bebas menggunakan model AI.
     *
     * @param prompt instruksi pengguna
     * @return daftar todo hasil generasi
     * @throws DomainException jika terjadi kegagalan komunikasi AI atau parsing
     */
    List<AiGeneratedTodo> generateTodos(String prompt) throws DomainException;
}
