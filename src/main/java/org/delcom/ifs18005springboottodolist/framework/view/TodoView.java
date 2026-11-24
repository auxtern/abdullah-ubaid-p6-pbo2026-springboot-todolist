package org.delcom.ifs18005springboottodolist.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import org.delcom.ifs18005springboottodolist.adapter.presenter.TodoPresenter;
import org.delcom.ifs18005springboottodolist.adapter.storage.FileStorage;
import org.delcom.ifs18005springboottodolist.domain.entity.AiGeneratedTodo;
import org.delcom.ifs18005springboottodolist.domain.entity.SortOption;
import org.delcom.ifs18005springboottodolist.domain.entity.Todo;
import org.delcom.ifs18005springboottodolist.domain.entity.TodoStatusFilter;
import org.delcom.ifs18005springboottodolist.domain.entity.User;
import org.delcom.ifs18005springboottodolist.domain.exception.DomainException;
import org.delcom.ifs18005springboottodolist.domain.exception.EntityNotFoundException;
import org.delcom.ifs18005springboottodolist.domain.exception.ValidationException;
import org.delcom.ifs18005springboottodolist.framework.auth.AuthSession;
import org.delcom.ifs18005springboottodolist.usecase.TodoAiUseCase;
import org.delcom.ifs18005springboottodolist.usecase.TodoUseCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Tampilan web Vaadin aplikasi todo milik pengguna yang sedang login.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Todo List")
public class TodoView extends VerticalLayout {

    private final TodoUseCase todoUseCase;
    private final TodoPresenter presenter;
    private final FileStorage fileStorage;
    private final TodoAiUseCase todoAiUseCase;

    private final Grid<Todo> grid = new Grid<>(Todo.class, false);
    private final TextField searchField = new TextField();
    private final Select<TodoStatusFilter> statusFilter = new Select<>();
    private final Select<SortOption> sortSelect = new Select<>();
    private final Paragraph statusLabel = new Paragraph();
    private final Span emptyState = new Span("- Data todo belum tersedia!");
    private final Span totalCount = new Span("0");
    private final Span finishedCount = new Span("0");
    private final Span unfinishedCount = new Span("0");

    public TodoView(TodoUseCase todoUseCase, TodoPresenter presenter, FileStorage fileStorage, TodoAiUseCase todoAiUseCase) {
        this.todoUseCase = todoUseCase;
        this.presenter = presenter;
        this.fileStorage = fileStorage;
        this.todoAiUseCase = todoAiUseCase;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "#F3F4F6")
                .set("padding", "0.75rem")
                .set("min-height", "0")
                .set("box-sizing", "border-box");

        H1 title = new H1("Todo Saya");
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "1.45rem")
                .set("line-height", "1.2");

        statusLabel.getStyle()
                .set("margin", "0")
                .set("font-size", "0.875rem")
                .set("color", "#4B5563");

        configureGrid();
        configureToolbar();

        Div card = PageCard.wrap(title, buildSummaryCards(), buildToolbar(), statusLabel, grid);
        add(card);
        expand(card);
        card.getStyle().set("min-height", "0");
        refreshAll();
    }

    private User requireUser() {
        return AuthSession.currentUser().orElseThrow();
    }

    private HorizontalLayout buildSummaryCards() {
        HorizontalLayout summary = new HorizontalLayout(
                createSummaryCard("Jumlah Todo", totalCount, "#DBEAFE", "#1D4ED8", "#1E40AF"),
                createSummaryCard("Sudah Selesai", finishedCount, "#DCFCE7", "#16A34A", "#166534"),
                createSummaryCard("Belum Selesai", unfinishedCount, "#FEF3C7", "#D97706", "#92400E")
        );
        summary.setWidthFull();
        summary.setSpacing(true);
        summary.getStyle().set("align-items", "stretch");
        return summary;
    }

    private Div createSummaryCard(
            String label,
            Span value,
            String background,
            String accent,
            String valueColor
    ) {
        Span caption = new Span(label);
        caption.getStyle()
                .set("display", "block")
                .set("font-size", "0.8rem")
                .set("font-weight", "500")
                .set("color", accent);

        value.getStyle()
                .set("display", "block")
                .set("margin-top", "0.15rem")
                .set("font-size", "1.35rem")
                .set("font-weight", "700")
                .set("line-height", "1.1")
                .set("color", valueColor);

        Div card = new Div(caption, value);
        card.getStyle()
                .set("flex", "1")
                .set("min-width", "0")
                .set("background", background)
                .set("border", "1px solid " + accent)
                .set("border-left", "4px solid " + accent)
                .set("border-radius", "10px")
                .set("padding", "0.55rem 0.85rem")
                .set("box-sizing", "border-box");
        return card;
    }

    private void updateSummary(List<Todo> todos) {
        long finished = todos.stream().filter(Todo::isFinished).count();
        totalCount.setText(String.valueOf(todos.size()));
        finishedCount.setText(String.valueOf(finished));
        unfinishedCount.setText(String.valueOf(todos.size() - finished));
    }

    private void configureGrid() {
        grid.addComponentColumn(this::createCoverPreview)
                .setHeader("Cover")
                .setKey("cover")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(false);

        grid.addColumn(Todo::getId)
                .setHeader("ID")
                .setKey("id")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(true)
                .setComparator(Todo::getId);

        grid.addColumn(Todo::getTitle)
                .setHeader("Judul")
                .setKey("title")
                .setFlexGrow(1)
                .setSortable(true)
                .setComparator(Comparator.comparing(Todo::getTitle, String.CASE_INSENSITIVE_ORDER));

        grid.addColumn(todo -> {
                    String description = todo.getDescription();
                    if (description == null || description.isBlank()) {
                        return "-";
                    }
                    return description.length() > 40 ? description.substring(0, 40) + "..." : description;
                })
                .setHeader("Deskripsi")
                .setKey("description")
                .setFlexGrow(1)
                .setSortable(false);

        grid.addComponentColumn(this::createFinishedCheckbox)
                .setHeader("Selesai")
                .setKey("selesai")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(true)
                .setComparator(Comparator.comparing(Todo::isFinished));

        grid.addColumn(todo -> presenter.formatCreatedAt(todo))
                .setHeader("Dibuat")
                .setKey("createdAt")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(Todo::getCreatedAt));

        grid.addColumn(todo -> presenter.formatFinishedAt(todo))
                .setHeader("Tanggal Selesai")
                .setKey("finishedAt")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        Todo::getFinishedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())
                ));

        grid.addColumn(todo -> presenter.formatDuration(todo))
                .setHeader("Durasi")
                .setKey("duration")
                .setAutoWidth(true)
                .setSortable(true)
                .setComparator(Comparator.comparing(
                        todo -> todo.duration().orElse(null),
                        Comparator.nullsLast(Comparator.naturalOrder())
                ));

        grid.addComponentColumn(this::createActionButtons)
                .setHeader("Tindakan")
                .setKey("tindakan")
                .setAutoWidth(true)
                .setFlexGrow(0)
                .setSortable(false);

        grid.setEmptyStateComponent(emptyState);
        grid.addItemDoubleClickListener(event -> openEditDialog(event.getItem()));
        grid.setSizeFull();
        GridStyles.apply(grid);
    }

    private Component createCoverPreview(Todo todo) {
        if (todo.getCoverPath() == null || todo.getCoverPath().isBlank()) {
            return new Span("-");
        }
        return ImagePreviewDialog.clickableThumbnail(
                "/uploads/" + todo.getCoverPath(),
                "Cover",
                "48px"
        );
    }

    private Checkbox createFinishedCheckbox(Todo todo) {
        Checkbox checkbox = new Checkbox();
        checkbox.setValue(todo.isFinished());
        checkbox.addValueChangeListener(event -> toggleFinished(todo, event.getValue()));
        return checkbox;
    }

    private HorizontalLayout createActionButtons(Todo todo) {
        Button deleteButton = new Button("Hapus", VaadinIcon.TRASH.create(), event -> openDeleteDialog(todo));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        deleteButton.getStyle().set("cursor", "pointer");
        return new HorizontalLayout(deleteButton);
    }

    private void configureToolbar() {
        searchField.setPlaceholder("Cari judul...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();
        searchField.addKeyPressListener(Key.ENTER, event -> applyFilters());
        searchField.addValueChangeListener(event -> {
            if (event.isFromClient() && (event.getValue() == null || event.getValue().isBlank())) {
                applyFilters();
            }
        });

        statusFilter.setItems(TodoStatusFilter.values());
        statusFilter.setItemLabelGenerator(TodoStatusFilter::label);
        statusFilter.setValue(TodoStatusFilter.ALL);
        statusFilter.setWidth("11rem");
        statusFilter.setAriaLabel("Filter status");
        statusFilter.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                applyFilters();
            }
        });

        sortSelect.setItems(SortOption.values());
        sortSelect.setItemLabelGenerator(SortOption::label);
        sortSelect.setValue(SortOption.NEWEST_FIRST);
        sortSelect.setWidth("12rem");
        sortSelect.setAriaLabel("Urutan");
        sortSelect.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                applyFilters();
            }
        });
    }

    private HorizontalLayout buildToolbar() {
        Button addButton = new Button("Tambah", VaadinIcon.PLUS.create(), event -> openAddDialog());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.getStyle().set("cursor", "pointer");

        Button searchButton = new Button("Cari", VaadinIcon.SEARCH.create(), event -> applyFilters());
        searchButton.getStyle().set("cursor", "pointer");

        Button refreshButton = new Button("Muat Ulang", VaadinIcon.REFRESH.create(), event -> refreshAll());
        refreshButton.getStyle().set("cursor", "pointer");

        Button aiButton = new Button("Generate AI", VaadinIcon.MAGIC.create(), event -> openAiDialog());
        aiButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        aiButton.getStyle().set("cursor", "pointer");

        HorizontalLayout toolbar = new HorizontalLayout(
                searchField,
                statusFilter,
                sortSelect,
                searchButton,
                refreshButton,
                aiButton,
                addButton
        );
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.END);
        toolbar.setFlexGrow(1, searchField);
        return toolbar;
    }

    private void openAiDialog() {
        Dialog dialog = new Dialog();
        ProgressBar loadingBar = new ProgressBar();
        loadingBar.setVisible(false);

        Span hint = new Span("Masukkan prompt / rencana instruksi tugas yang ingin dibuat secara otomatis oleh AI:");
        hint.getStyle().set("font-size", "0.875rem").set("color", "#4B5563");

        TextArea promptArea = new TextArea("Instruksi Prompt");
        promptArea.setPlaceholder("Contoh: Rencana belajar ujian PBO semester ini dalam 3 hari...");
        promptArea.setWidthFull();
        promptArea.setMinHeight("100px");

        Grid<AiGeneratedTodo> previewGrid = new Grid<>(AiGeneratedTodo.class, false);
        previewGrid.addColumn(AiGeneratedTodo::title).setHeader("Judul").setFlexGrow(1);
        previewGrid.addColumn(AiGeneratedTodo::description).setHeader("Deskripsi").setFlexGrow(2);
        previewGrid.setWidthFull();
        previewGrid.setHeight("180px");
        previewGrid.setVisible(false);
        GridStyles.apply(previewGrid);

        List<AiGeneratedTodo> generatedItems = new ArrayList<>();

        Button applyButton = new Button("Tambahkan ke Todo", VaadinIcon.PLUS_CIRCLE.create());
        applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        applyButton.setEnabled(false);

        Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());

        Button generateButton = new Button("Proses AI", VaadinIcon.MAGIC.create());
        generateButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        generateButton.getStyle().set("cursor", "pointer");

        generateButton.addClickListener(event -> {
            String promptText = promptArea.getValue();
            if (promptText == null || promptText.trim().isEmpty()) {
                showError("Prompt instruksi tidak boleh kosong");
                return;
            }
            if (todoAiUseCase == null) {
                showError("Layanan AI belum tersedia");
                return;
            }

            UI ui = UI.getCurrent();
            setAiBusy(true, loadingBar, generateButton, applyButton, cancelButton, promptArea);
            ui.setPollInterval(400);

            String prompt = promptText.trim();
            CompletableFuture.supplyAsync(() -> {
                try {
                    return todoAiUseCase.generateTodos(prompt);
                } catch (DomainException ex) {
                    throw new RuntimeException(ex);
                }
            }).whenComplete((result, error) -> ui.access(() -> {
                try {
                    if (error != null) {
                        Throwable cause = error.getCause() != null ? error.getCause() : error;
                        showError("Gagal menghasilkan konten AI: " + cause.getMessage());
                        return;
                    }
                    generatedItems.clear();
                    generatedItems.addAll(result);
                    previewGrid.setItems(generatedItems);
                    previewGrid.setVisible(true);
                    applyButton.setEnabled(!generatedItems.isEmpty());
                    showSuccess("AI berhasil menghasilkan " + generatedItems.size() + " rencana todo");
                } finally {
                    setAiBusy(false, loadingBar, generateButton, applyButton, cancelButton, promptArea);
                    applyButton.setEnabled(!generatedItems.isEmpty());
                    ui.setPollInterval(-1);
                }
            }));
        });

        applyButton.addClickListener(event -> {
            if (generatedItems.isEmpty()) {
                showError("Tidak ada item todo yang dapat ditambahkan");
                return;
            }
            loadingBar.setVisible(true);
            applyButton.setEnabled(false);
            generateButton.setEnabled(false);
            cancelButton.setEnabled(false);
            try {
                User user = requireUser();
                if (todoAiUseCase != null) {
                    todoAiUseCase.saveGeneratedTodos(user.getId(), generatedItems);
                } else {
                    for (AiGeneratedTodo item : generatedItems) {
                        todoUseCase.addTodo(user.getId(), item.title(), item.description());
                    }
                }
                showSuccess("Berhasil menambahkan " + generatedItems.size() + " todo dari AI");
                dialog.close();
                refreshAll();
            } catch (ValidationException ex) {
                showError("Validasi gagal: " + ex.getMessage());
                loadingBar.setVisible(false);
                applyButton.setEnabled(true);
                generateButton.setEnabled(true);
                cancelButton.setEnabled(true);
            }
        });

        HorizontalLayout promptActionRow = new HorizontalLayout(generateButton);
        promptActionRow.setWidthFull();
        promptActionRow.setJustifyContentMode(JustifyContentMode.END);

        VerticalLayout body = new VerticalLayout(hint, promptArea, promptActionRow, previewGrid);
        body.setPadding(false);
        body.setSpacing(true);

        buildDialog(dialog, "Generate Daftar Todo dengan AI", DialogTone.PRIMARY, loadingBar, body, cancelButton, applyButton);
        dialog.setWidth("600px");
        dialog.open();
    }

    private void openAddDialog() {
        TextField titleField = new TextField("Judul");
        TextArea descriptionField = new TextArea("Deskripsi");
        titleField.setWidthFull();
        descriptionField.setWidthFull();

        AtomicReference<String> pendingCoverName = new AtomicReference<>();
        AtomicReference<byte[]> pendingCoverData = new AtomicReference<>();
        Upload upload = imageUpload(pendingCoverName, pendingCoverData);
        Span uploadHint = new Span("Cover (opsional)");

        Dialog dialog = new Dialog();
        ProgressBar loadingBar = new ProgressBar();
        loadingBar.setVisible(false);

        Button saveButton = new Button("Simpan", VaadinIcon.CHECK.create());
        saveButton.addClickListener(event -> {
            loadingBar.setVisible(true);
            saveButton.setEnabled(false);
            try {
                User user = requireUser();
                Todo created = todoUseCase.addTodo(user.getId(), titleField.getValue(), descriptionField.getValue());
                String filename = pendingCoverName.get();
                byte[] data = pendingCoverData.get();
                if (filename != null && !filename.isBlank() && data != null) {
                    String path = fileStorage.store("covers", filename, new ByteArrayInputStream(data));
                    todoUseCase.updateTodoForUser(user.getId(), created.getId(), null, null, null, path, false);
                }
                showSuccess(presenter.showAddSuccess(created));
                dialog.close();
                refreshAll();
            } catch (ValidationException ex) {
                loadingBar.setVisible(false);
                saveButton.setEnabled(true);
                showError(presenter.showValidationError(ex.getMessage()));
            } catch (EntityNotFoundException | IOException ex) {
                loadingBar.setVisible(false);
                saveButton.setEnabled(true);
                showError(presenter.showValidationError(ex.getMessage()));
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());
        VerticalLayout body = new VerticalLayout(titleField, descriptionField, uploadHint, upload);
        body.setPadding(false);
        body.setSpacing(true);
        buildDialog(dialog, "Menambah Todo", DialogTone.PRIMARY, loadingBar, body, cancelButton, saveButton);
        dialog.open();
    }

    private void openEditDialog(Todo todo) {
        TextField titleField = new TextField("Judul");
        titleField.setValue(todo.getTitle());
        TextArea descriptionField = new TextArea("Deskripsi");
        descriptionField.setValue(todo.getDescription() == null ? "" : todo.getDescription());
        titleField.setWidthFull();
        descriptionField.setWidthFull();

        AtomicReference<String> pendingCoverName = new AtomicReference<>();
        AtomicReference<byte[]> pendingCoverData = new AtomicReference<>();
        Upload upload = imageUpload(pendingCoverName, pendingCoverData);
        Span uploadHint = new Span("Ganti cover (opsional)");

        Dialog dialog = new Dialog();
        ProgressBar loadingBar = new ProgressBar();
        loadingBar.setVisible(false);

        Button saveButton = new Button("Simpan", VaadinIcon.CHECK.create());
        saveButton.addClickListener(event -> {
            loadingBar.setVisible(true);
            saveButton.setEnabled(false);
            try {
                User user = requireUser();
                String coverPath = null;
                boolean clearCover = false;
                String filename = pendingCoverName.get();
                byte[] data = pendingCoverData.get();
                if (filename != null && !filename.isBlank() && data != null) {
                    coverPath = fileStorage.store("covers", filename, new ByteArrayInputStream(data));
                }
                todoUseCase.updateTodoForUser(
                        user.getId(),
                        todo.getId(),
                        titleField.getValue(),
                        descriptionField.getValue(),
                        null,
                        coverPath,
                        clearCover
                );
                showSuccess(presenter.showUpdateSuccess());
                dialog.close();
                refreshAll();
            } catch (EntityNotFoundException ex) {
                loadingBar.setVisible(false);
                saveButton.setEnabled(true);
                showError(presenter.showUpdateFailed(ex.getId()));
            } catch (ValidationException | IOException ex) {
                loadingBar.setVisible(false);
                saveButton.setEnabled(true);
                showError(presenter.showValidationError(ex.getMessage()));
            }
        });
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());
        VerticalLayout body = new VerticalLayout(titleField, descriptionField, uploadHint, upload);
        body.setPadding(false);
        body.setSpacing(true);
        buildDialog(dialog, "Mengubah Todo #" + todo.getId(), DialogTone.PRIMARY, loadingBar, body, cancelButton, saveButton);
        dialog.open();
    }

    private void openDeleteDialog(Todo todo) {
        Paragraph message = new Paragraph(
                "Yakin ingin menghapus todo \"" + todo.getTitle() + "\" (ID " + todo.getId() + ")?"
        );
        message.getStyle().set("margin", "0");

        Dialog dialog = new Dialog();
        ProgressBar loadingBar = new ProgressBar();
        loadingBar.setVisible(false);

        Button confirmButton = new Button("Hapus", VaadinIcon.TRASH.create());
        confirmButton.addClickListener(event -> {
            loadingBar.setVisible(true);
            confirmButton.setEnabled(false);
            deleteTodo(todo);
            dialog.close();
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());
        buildDialog(dialog, "Menghapus Todo", DialogTone.DANGER, loadingBar, message, cancelButton, confirmButton);
        dialog.open();
    }

    private enum DialogTone {
        PRIMARY,
        DANGER
    }

    private void buildDialog(
            Dialog dialog,
            String title,
            DialogTone tone,
            ProgressBar loadingBar,
            Component bodyContent,
            Button... footerButtons
    ) {
        dialog.setWidth("500px");
        dialog.setMaxWidth("95vw");
        dialog.setResizable(false);
        dialog.addClassName("todo-form-dialog");

        boolean danger = tone == DialogTone.DANGER;
        String headerBackground = danger ? "#DC2626" : "#2563EB";

        Span headerTitle = new Span(title);
        headerTitle.getStyle()
                .set("margin", "0")
                .set("color", "#FFFFFF")
                .set("font-size", "1.125rem")
                .set("font-weight", "600")
                .set("line-height", "1.4");

        Div header = new Div(headerTitle);
        header.getStyle()
                .set("background", headerBackground)
                .set("padding", "1rem 1.5rem")
                .set("box-sizing", "border-box")
                .set("width", "100%");

        loadingBar.setIndeterminate(true);
        loadingBar.getStyle().set("margin", "0").set("height", "4px");

        Div body = new Div(bodyContent);
        body.getStyle()
                .set("background", "#FFFFFF")
                .set("padding", "1.25rem 1.5rem")
                .set("overflow", "hidden")
                .set("box-sizing", "border-box")
                .set("width", "100%");

        Div footer = new Div();
        footer.getStyle()
                .set("display", "flex")
                .set("justify-content", "flex-end")
                .set("align-items", "center")
                .set("gap", "0.75rem")
                .set("background", "#F3F4F6")
                .set("border-top", "1px solid #E5E7EB")
                .set("padding", "0.85rem 1.5rem 1.1rem")
                .set("box-sizing", "border-box")
                .set("width", "100%");
        for (Button btn : footerButtons) {
            btn.getStyle().set("cursor", "pointer");
        }
        footer.add(footerButtons);

        Div shell = new Div(header, loadingBar, body, footer);
        shell.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("width", "100%")
                .set("overflow", "hidden")
                .set("border-radius", "12px")
                .set("background", "#FFFFFF");

        dialog.removeAll();
        dialog.add(shell);
        removeOverlayPadding(dialog);
    }

    private void removeOverlayPadding(Dialog dialog) {
        dialog.addOpenedChangeListener(event -> {
            if (event.isOpened()) {
                dialog.getElement().executeJs("""
                        const overlay = this.$.overlay;
                        if (!overlay || !overlay.shadowRoot) {
                          return;
                        }
                        const content = overlay.shadowRoot.querySelector('[part="content"]');
                        if (content) {
                          content.style.padding = '0';
                          content.style.overflow = 'visible';
                          content.style.maxHeight = 'none';
                        }
                        const overlayPart = overlay.shadowRoot.querySelector('[part="overlay"]');
                        if (overlayPart) {
                          overlayPart.style.padding = '0';
                          overlayPart.style.overflow = 'hidden';
                        }
                        """);
            }
        });
    }

    private void toggleFinished(Todo todo, boolean finished) {
        try {
            todoUseCase.updateTodoForUser(requireUser().getId(), todo.getId(), null, null, finished, null, false);
            showSuccess(presenter.showUpdateSuccess());
            applyFilters();
        } catch (EntityNotFoundException ex) {
            showError(presenter.showUpdateFailed(ex.getId()));
            applyFilters();
        } catch (ValidationException ex) {
            showError(presenter.showValidationError(ex.getMessage()));
            applyFilters();
        }
    }

    private void deleteTodo(Todo todo) {
        try {
            todoUseCase.removeTodoForUser(requireUser().getId(), todo.getId());
            showSuccess(presenter.showRemoveSuccess());
            refreshAll();
        } catch (EntityNotFoundException ex) {
            showError(presenter.showRemoveFailed(ex.getId()));
        } catch (ValidationException ex) {
            showError(presenter.showValidationError(ex.getMessage()));
        }
    }

    private void applyFilters() {
        String keyword = searchField.getValue() == null ? "" : searchField.getValue().trim();
        TodoStatusFilter status = statusFilter.getValue() == null ? TodoStatusFilter.ALL : statusFilter.getValue();
        SortOption sort = sortSelect.getValue() == null ? SortOption.NEWEST_FIRST : sortSelect.getValue();

        List<Todo> results = todoUseCase.listTodos(requireUser().getId(), keyword, status, sort);

        String title = "Daftar Todo";
        if (!keyword.isBlank()) {
            title = "Hasil: \"" + keyword + "\"";
        }
        if (status != TodoStatusFilter.ALL) {
            title = title + " · " + status.label();
        }
        title = title + " · " + sort.label();

        if (results.isEmpty()) {
            emptyState.setText(keyword.isBlank() && status == TodoStatusFilter.ALL
                    ? "- Data todo belum tersedia!"
                    : "- Todo tidak ditemukan!");
        } else {
            emptyState.setText("- Data todo belum tersedia!");
        }
        refreshGrid(results, title);
    }

    private void refreshAll() {
        searchField.clear();
        statusFilter.setValue(TodoStatusFilter.ALL);
        sortSelect.setValue(SortOption.NEWEST_FIRST);
        emptyState.setText("- Data todo belum tersedia!");
        applyFilters();
    }

    private void refreshGrid(List<Todo> todos, String title) {
        grid.setItems(todos);
        updateSummary(todoUseCase.getTodosByUser(requireUser().getId()));
        if (todos.isEmpty()) {
            statusLabel.setText(title + " — " + emptyState.getText().replaceFirst("^- ", ""));
        } else {
            statusLabel.setText(title);
        }
    }

    private Upload imageUpload(AtomicReference<String> pendingName, AtomicReference<byte[]> pendingData) {
        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            pendingName.set(metadata.fileName());
            pendingData.set(data);
        }));
        upload.setAcceptedMimeTypes("image/png", "image/jpeg", "image/webp");
        upload.setAcceptedFileExtensions(".png", ".jpg", ".jpeg", ".webp");
        upload.setMaxFiles(1);
        return upload;
    }

    private void setAiBusy(
            boolean busy,
            ProgressBar loadingBar,
            Button generateButton,
            Button applyButton,
            Button cancelButton,
            TextArea promptArea
    ) {
        loadingBar.setVisible(busy);
        generateButton.setEnabled(!busy);
        generateButton.setText(busy ? "Memproses..." : "Proses AI");
        applyButton.setEnabled(false);
        cancelButton.setEnabled(!busy);
        promptArea.setReadOnly(busy);
    }

    private void showSuccess(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void showError(String message) {
        Notification notification = Notification.show(message, 4000, Notification.Position.BOTTOM_START);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
