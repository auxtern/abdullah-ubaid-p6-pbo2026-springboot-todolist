package org.delcom.ifs18005springboottodolist.domain.entity;

import java.util.Comparator;

/**
 * Kriteria pengurutan todo.
 * Setiap opsi membawa comparator-nya sendiri sehingga logika pengurutan
 * terpusat di domain, bukan tersebar sebagai angka ajaib di view/use case.
 */
public enum SortOption {
    /** Todo terbaru (createdAt / id terbesar) di atas. */
    NEWEST_FIRST(Comparator
            .comparing(Todo::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Todo::getId)
            .reversed()),

    /** Todo terlama di atas. */
    OLDEST_FIRST(Comparator
            .comparing(Todo::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Todo::getId)),

    /** Urutkan judul dari A ke Z (case-insensitive). */
    TITLE_ASC(Comparator.comparing(Todo::getTitle, String.CASE_INSENSITIVE_ORDER)),

    /** Urutkan judul dari Z ke A (case-insensitive). */
    TITLE_DESC(Comparator.comparing(Todo::getTitle, String.CASE_INSENSITIVE_ORDER).reversed()),

    /** Tampilkan todo selesai terlebih dahulu, lalu yang terbaru. */
    FINISHED_FIRST(Comparator.comparing(Todo::isFinished).reversed()
            .thenComparing(Todo::getId, Comparator.reverseOrder())),

    /** Tampilkan todo belum selesai terlebih dahulu, lalu yang terbaru. */
    UNFINISHED_FIRST(Comparator.comparing(Todo::isFinished)
            .thenComparing(Todo::getId, Comparator.reverseOrder()));

    /** Comparator yang digunakan untuk mengurutkan daftar todo. */
    private final Comparator<Todo> comparator;

    SortOption(Comparator<Todo> comparator) {
        this.comparator = comparator;
    }

    /** Mengembalikan comparator yang sesuai dengan opsi ini. */
    public Comparator<Todo> comparator() {
        return comparator;
    }

    /** Label singkat untuk ditampilkan di UI. */
    public String label() {
        return switch (this) {
            case NEWEST_FIRST -> "Terbaru";
            case OLDEST_FIRST -> "Terlama";
            case TITLE_ASC -> "Judul A-Z";
            case TITLE_DESC -> "Judul Z-A";
            case FINISHED_FIRST -> "Selesai dulu";
            case UNFINISHED_FIRST -> "Belum selesai dulu";
        };
    }
}
