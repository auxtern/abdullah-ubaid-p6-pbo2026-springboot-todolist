package org.delcom.ifs18005springboottodolist.adapter.ai;

import org.delcom.ifs18005springboottodolist.domain.entity.AiGeneratedTodo;
import org.delcom.ifs18005springboottodolist.domain.exception.DomainException;
import org.delcom.ifs18005springboottodolist.domain.exception.ValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InMemoryTodoAiServiceTest {

    @Test
    void generatesDefaultTodos() throws Exception {
        InMemoryTodoAiService service = new InMemoryTodoAiService();
        List<AiGeneratedTodo> todos = service.generateTodos("Ujian");
        assertEquals(2, todos.size());
    }

    @Test
    void usesCannedResponsesWhenProvided() throws Exception {
        InMemoryTodoAiService service = new InMemoryTodoAiService(List.of(
                new AiGeneratedTodo("Custom 1", "Desc 1")
        ));
        service.addCannedResponse("Custom 2", "Desc 2");

        List<AiGeneratedTodo> todos = service.generateTodos("Any prompt");
        assertEquals(2, todos.size());
        assertEquals("Custom 1", todos.get(0).title());
        assertEquals("Custom 2", todos.get(1).title());
    }

    @Test
    void handlesNullCannedListConstructor() throws Exception {
        InMemoryTodoAiService service = new InMemoryTodoAiService(null);
        List<AiGeneratedTodo> todos = service.generateTodos("Test");
        assertEquals(2, todos.size());
    }

    @Test
    void rejectsBlankPrompt() {
        InMemoryTodoAiService service = new InMemoryTodoAiService();
        assertThrows(ValidationException.class, () -> service.generateTodos(null));
        assertThrows(ValidationException.class, () -> service.generateTodos("   "));
    }

    @Test
    void simulatesFailure() {
        InMemoryTodoAiService service = new InMemoryTodoAiService();
        service.setSimulateFailure(true);
        assertThrows(DomainException.class, () -> service.generateTodos("Test"));
    }
}
