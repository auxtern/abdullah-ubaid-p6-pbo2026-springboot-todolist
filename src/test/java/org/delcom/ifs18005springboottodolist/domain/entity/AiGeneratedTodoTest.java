package org.delcom.ifs18005springboottodolist.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiGeneratedTodoTest {

    @Test
    void createsValidAiGeneratedTodo() {
        AiGeneratedTodo todo = new AiGeneratedTodo("  Belajar AI  ", "  Membaca dokumentasi  ");
        assertEquals("Belajar AI", todo.title());
        assertEquals("Membaca dokumentasi", todo.description());
    }

    @Test
    void handlesNullDescription() {
        AiGeneratedTodo todo = new AiGeneratedTodo("Tugas", null);
        assertEquals("Tugas", todo.title());
        assertEquals("", todo.description());
    }

    @Test
    void rejectsEmptyOrNullTitle() {
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTodo(null, "desc"));
        assertThrows(IllegalArgumentException.class, () -> new AiGeneratedTodo("   ", "desc"));
    }
}
