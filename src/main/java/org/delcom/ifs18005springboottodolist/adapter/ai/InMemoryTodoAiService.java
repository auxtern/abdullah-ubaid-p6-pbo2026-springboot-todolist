package org.delcom.ifs18005springboottodolist.adapter.ai;

import org.delcom.ifs18005springboottodolist.domain.entity.AiGeneratedTodo;
import org.delcom.ifs18005springboottodolist.domain.exception.DomainException;
import org.delcom.ifs18005springboottodolist.domain.exception.ValidationException;
import org.delcom.ifs18005springboottodolist.domain.repository.ITodoAiService;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementasi in-memory ITodoAiService untuk pengujian unit deterministik tanpa jaringan.
 */
public class InMemoryTodoAiService implements ITodoAiService {

    private final List<AiGeneratedTodo> cannedResponses = new ArrayList<>();
    private boolean simulateFailure = false;

    public InMemoryTodoAiService() {
    }

    public InMemoryTodoAiService(List<AiGeneratedTodo> defaultResponses) {
        if (defaultResponses != null) {
            this.cannedResponses.addAll(defaultResponses);
        }
    }

    public void setSimulateFailure(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    public void addCannedResponse(String title, String description) {
        this.cannedResponses.add(new AiGeneratedTodo(title, description));
    }

    @Override
    public List<AiGeneratedTodo> generateTodos(String prompt) throws DomainException {
        if (prompt == null || prompt.trim().isEmpty()) {
            throw new ValidationException("Prompt instruksi tidak boleh kosong");
        }
        if (simulateFailure) {
            throw new DomainException("Simulasi kegagalan layanan AI");
        }
        if (!cannedResponses.isEmpty()) {
            return new ArrayList<>(cannedResponses);
        }
        return List.of(
                new AiGeneratedTodo("Rencana AI 1: " + prompt.trim(), "Deskripsi tugas pertama yang di-generate dari prompt"),
                new AiGeneratedTodo("Rencana AI 2: " + prompt.trim(), "Deskripsi tugas kedua yang di-generate dari prompt")
        );
    }
}
