package org.delcom.ifs18005springboottodolist.usecase;

import org.delcom.ifs18005springboottodolist.domain.entity.AiGeneratedTodo;
import org.delcom.ifs18005springboottodolist.domain.entity.Todo;
import org.delcom.ifs18005springboottodolist.domain.exception.DomainException;
import org.delcom.ifs18005springboottodolist.domain.exception.ValidationException;
import org.delcom.ifs18005springboottodolist.domain.repository.ITodoAiService;
import org.delcom.ifs18005springboottodolist.domain.repository.ITodoRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * Use case yang menangani logika pembuatan todo menggunakan AI.
 */
public class TodoAiUseCase {

    private final ITodoAiService aiService;
    private final ITodoRepository todoRepository;

    public TodoAiUseCase(ITodoAiService aiService, ITodoRepository todoRepository) {
        this.aiService = aiService;
        this.todoRepository = todoRepository;
    }

    /**
     * Meminta AI menghasilkan daftar todo berdasarkan prompt instruksi bebas.
     *
     * @param prompt instruksi dari pengguna
     * @return daftar todo hasil generasi
     * @throws DomainException jika validasi prompt gagal atau AI bermasalah
     */
    public List<AiGeneratedTodo> generateTodos(String prompt) throws DomainException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new ValidationException("Prompt instruksi tidak boleh kosong");
        }
        return aiService.generateTodos(prompt.trim());
    }

    /**
     * Menyimpan daftar todo hasil generasi AI ke basis data untuk pengguna tertentu.
     *
     * @param userId ID pengguna pemilik
     * @param items daftar todo yang akan disimpan
     * @return daftar entitas todo yang berhasil disimpan
     * @throws ValidationException jika daftar item kosong
     */
    public List<Todo> saveGeneratedTodos(int userId, List<AiGeneratedTodo> items) throws ValidationException {
        if (items == null || items.isEmpty()) {
            throw new ValidationException("Daftar todo hasil generate tidak boleh kosong");
        }
        List<Todo> saved = new ArrayList<>();
        for (AiGeneratedTodo item : items) {
            saved.add(todoRepository.save(userId, item.title(), item.description()));
        }
        return saved;
    }
}
