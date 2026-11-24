package org.delcom.ifs18005springboottodolist.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortOptionTest {

    @Test
    void sortsByTitleAscendingAndDescending() {
        List<Todo> todos = new ArrayList<>();
        todos.add(new Todo(1, 1, "Beta"));
        todos.add(new Todo(2, 1, "Alpha"));

        todos.sort(SortOption.TITLE_ASC.comparator());
        assertEquals("Alpha", todos.get(0).getTitle());

        todos.sort(SortOption.TITLE_DESC.comparator());
        assertEquals("Beta", todos.get(0).getTitle());
    }

    @Test
    void sortsByFinishedStatus() {
        List<Todo> todos = new ArrayList<>();
        todos.add(new Todo(1, 1, "A", "", null, false));
        todos.add(new Todo(2, 1, "B", "", null, true));

        todos.sort(SortOption.FINISHED_FIRST.comparator());
        assertTrue(todos.get(0).isFinished());
        assertFalse(todos.get(1).isFinished());

        todos.sort(SortOption.UNFINISHED_FIRST.comparator());
        assertFalse(todos.get(0).isFinished());
        assertTrue(todos.get(1).isFinished());
    }

    @Test
    void sortsByNewestAndOldest() {
        LocalDateTime older = LocalDateTime.of(2026, 1, 1, 8, 0);
        LocalDateTime newer = LocalDateTime.of(2026, 1, 2, 8, 0);

        List<Todo> todos = new ArrayList<>();
        todos.add(new Todo(1, 1, "Lama", "", null, false, older, null));
        todos.add(new Todo(2, 1, "Baru", "", null, false, newer, null));

        todos.sort(SortOption.NEWEST_FIRST.comparator());
        assertEquals("Baru", todos.get(0).getTitle());

        todos.sort(SortOption.OLDEST_FIRST.comparator());
        assertEquals("Lama", todos.get(0).getTitle());
    }

    @Test
    void labelsAreReadable() {
        assertEquals("Terbaru", SortOption.NEWEST_FIRST.label());
        assertEquals("Terlama", SortOption.OLDEST_FIRST.label());
        assertEquals("Judul A-Z", SortOption.TITLE_ASC.label());
        assertEquals("Judul Z-A", SortOption.TITLE_DESC.label());
        assertEquals("Selesai dulu", SortOption.FINISHED_FIRST.label());
        assertEquals("Belum selesai dulu", SortOption.UNFINISHED_FIRST.label());
    }
}
