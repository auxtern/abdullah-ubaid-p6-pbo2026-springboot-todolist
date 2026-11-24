package org.delcom.ifs18005springboottodolist.usecase;

import org.delcom.ifs18005springboottodolist.adapter.ai.InMemoryTodoAiService;
import org.delcom.ifs18005springboottodolist.adapter.repository.TodoRepository;
import org.delcom.ifs18005springboottodolist.domain.entity.AiGeneratedTodo;
import org.delcom.ifs18005springboottodolist.domain.entity.Todo;
import org.delcom.ifs18005springboottodolist.domain.exception.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TodoAiUseCaseTest {

    private InMemoryTodoAiService aiService;
    private TodoRepository todoRepository;
    private TodoAiUseCase useCase;

    @BeforeEach
    void setUp() {
        aiService = new InMemoryTodoAiService();
        todoRepository = new TodoRepository();
        useCase = new TodoAiUseCase(aiService, todoRepository);
    }

    @Test
    void generateTodosReturnsList() throws Exception {
        List<AiGeneratedTodo> result = useCase.generateTodos("Rencana belajar PBO");
        assertFalse(result.isEmpty());
        assertEquals(2, result.size());
    }

    @Test
    void generateTodosRejectsBlankPrompt() {
        assertThrows(ValidationException.class, () -> useCase.generateTodos(null));
        assertThrows(ValidationException.class, () -> useCase.generateTodos("   "));
    }

    @Test
    void saveGeneratedTodosPersistsToRepository() throws Exception {
        List<AiGeneratedTodo> items = List.of(
                new AiGeneratedTodo("Tugas 1", "Deskripsi 1"),
                new AiGeneratedTodo("Tugas 2", "Deskripsi 2")
        );

        List<Todo> saved = useCase.saveGeneratedTodos(1, items);
        assertEquals(2, saved.size());
        assertEquals(2, todoRepository.findByUserId(1).size());
    }

    @Test
    void saveGeneratedTodosRejectsEmptyOrNullItems() {
        assertThrows(ValidationException.class, () -> useCase.saveGeneratedTodos(1, null));
        assertThrows(ValidationException.class, () -> useCase.saveGeneratedTodos(1, Collections.emptyList()));
    }
}
