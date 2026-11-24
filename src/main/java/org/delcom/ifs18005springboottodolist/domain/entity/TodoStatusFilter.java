package org.delcom.ifs18005springboottodolist.domain.entity;

/**
 * Filter status selesai todo untuk daftar yang ditampilkan di UI.
 */
public enum TodoStatusFilter {
    ALL,
    UNFINISHED,
    FINISHED;

    public String label() {
        return switch (this) {
            case ALL -> "Semua status";
            case UNFINISHED -> "Belum selesai";
            case FINISHED -> "Sudah selesai";
        };
    }

    public boolean matches(Todo todo) {
        return switch (this) {
            case ALL -> true;
            case UNFINISHED -> !todo.isFinished();
            case FINISHED -> todo.isFinished();
        };
    }
}
