package org.delcom.ifs18005springboottodolist.domain.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TodoStatusFilterTest {

    @Test
    void matchesByStatus() {
        Todo open = new Todo(1, 1, "Open", "", null, false);
        Todo done = new Todo(2, 1, "Done", "", null, true);

        assertTrue(TodoStatusFilter.ALL.matches(open));
        assertTrue(TodoStatusFilter.ALL.matches(done));
        assertTrue(TodoStatusFilter.UNFINISHED.matches(open));
        assertFalse(TodoStatusFilter.UNFINISHED.matches(done));
        assertTrue(TodoStatusFilter.FINISHED.matches(done));
        assertFalse(TodoStatusFilter.FINISHED.matches(open));
    }

    @Test
    void labelsAreReadable() {
        assertEquals("Semua status", TodoStatusFilter.ALL.label());
        assertEquals("Belum selesai", TodoStatusFilter.UNFINISHED.label());
        assertEquals("Sudah selesai", TodoStatusFilter.FINISHED.label());
    }
}
