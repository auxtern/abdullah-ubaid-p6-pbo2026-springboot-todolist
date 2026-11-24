package org.delcom.ifs18005springboottodolist.domain.entity;

/**
 * Representasi todo hasil generasi AI sebelum disimpan ke basis data.
 */
public record AiGeneratedTodo(String title, String description) {

    public AiGeneratedTodo {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Judul todo tidak boleh kosong");
        }
        title = title.trim();
        description = (description == null) ? "" : description.trim();
    }
}
