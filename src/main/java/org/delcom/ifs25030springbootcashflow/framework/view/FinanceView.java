package org.delcom.ifs25030springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25030springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25030springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25030springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25030springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25030springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25030springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25030springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25030springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25030springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25030springbootcashflow.usecase.FinanceAiUseCase;
import org.delcom.ifs25030springbootcashflow.usecase.FinanceUseCase;

import java.util.Comparator;
import java.util.List;

/**
 * Tampilan utama cashflow milik pengguna yang sedang login:
 * kartu ringkasan, pencarian/filter/urutan, tabel transaksi, dan tombol aksi.
 */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Cashflow")
public class FinanceView extends VerticalLayout {

    private static final String EMPTY_TEXT = "- Data transaksi belum tersedia!";
    private static final String NOT_FOUND_TEXT = "- Transaksi tidak ditemukan!";
    private static final String SEPARATOR = " · ";

    private final FinanceUseCase financeUseCase;
    private final FinancePresenter presenter;
    private final FileStorage fileStorage;
    private final FinanceAiUseCase financeAiUseCase;

    private final Grid<Transaction> grid = new Grid<>(Transaction.class, false);
    private final TextField searchField = new TextField();
    private final Select<TransactionTypeFilter> typeFilter = new Select<>();
    private final Select<SortOption> sortSelect = new Select<>();
    private final Paragraph statusLabel = new Paragraph();
    private final Span emptyState = new Span(EMPTY_TEXT);
    private final Span totalCount = new Span("0");
    private final Span totalIncome = new Span(Ui.RP_0_00);
    private final Span totalExpense = new Span(Ui.RP_0_00);
    private final Span balance = new Span(Ui.RP_0_00);

    public FinanceView(
            FinanceUseCase financeUseCase,
            FinancePresenter presenter,
            FileStorage fileStorage,
            FinanceAiUseCase financeAiUseCase
    ) {
        this.financeUseCase = financeUseCase;
        this.presenter = presenter;
        this.fileStorage = fileStorage;
        this.financeAiUseCase = financeAiUseCase;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set(Ui.BACKGROUND, "#F3F4F6")
                .set(Ui.PADDING, "0.75rem")
                .set("min-height", "0")
                .set("box-sizing", "border-box");

        H1 title = new H1("Cashflow Saya");
        title.getStyle().set("margin", "0").set(Ui.FONT_SIZE, "1.45rem").set("line-height", "1.2");

        statusLabel.getStyle().set("margin", "0").set(Ui.FONT_SIZE, "0.875rem").set(Ui.COLOR, "#4B5563");

        configureGrid();
        configureFilters();

        Div card = PageCard.wrap(title, buildQuickLinks(), buildSummaryCards(), buildToolbar(), statusLabel, grid);
        add(card);
        expand(card);
        card.getStyle().set("min-height", "0");
        refreshAll();
    }

    // ------------------------------------------------------------------ bagian atas

    private static Div buildQuickLinks() {
        Div links = new Div(quickLink("/summary", "Lihat Ringkasan"), quickLink("/profile", "Profil Saya"));
        links.getStyle()
                .set(Ui.DISPLAY, "flex")
                .set("gap", "1rem")
                .set(Ui.FLEX_WRAP, Ui.WRAP)
                .set(Ui.FONT_SIZE, "0.875rem");
        return links;
    }

    private static Anchor quickLink(String href, String text) {
        Anchor anchor = new Anchor(href, text);
        anchor.getElement().setAttribute("router-link", true);
        anchor.getStyle().set(Ui.COLOR, "#1D4ED8").set(Ui.FONT_WEIGHT, "600").set(Ui.CURSOR, Ui.POINTER);
        return anchor;
    }

    private HorizontalLayout buildSummaryCards() {
        HorizontalLayout summary = new HorizontalLayout(
                summaryCard("Total Transaksi", totalCount, "#E0E7FF", "#4338CA", "#3730A3"),
                summaryCard("Total Pemasukan", totalIncome, "#DCFCE7", "#16A34A", "#166534"),
                summaryCard("Total Pengeluaran", totalExpense, "#FEE2E2", "#DC2626", "#991B1B"),
                summaryCard("Saldo", balance, "#DBEAFE", "#1D4ED8", "#1E40AF")
        );
        summary.setWidthFull();
        summary.setSpacing(true);
        summary.getStyle().set("align-items", "stretch").set(Ui.FLEX_WRAP, Ui.WRAP);
        return summary;
    }

    private static Div summaryCard(String label, Span value, String background, String accent, String textColor) {
        Span caption = new Span(label);
        caption.getStyle()
                .set(Ui.DISPLAY, "block")
                .set(Ui.FONT_SIZE, "0.8rem")
                .set(Ui.FONT_WEIGHT, "500")
                .set(Ui.COLOR, textColor);

        value.getStyle()
                .set(Ui.DISPLAY, "block")
                .set("margin-top", "0.15rem")
                .set(Ui.FONT_SIZE, "1.25rem")
                .set(Ui.FONT_WEIGHT, "700")
                .set("line-height", "1.1")
                .set(Ui.COLOR, textColor);

        Div card = new Div(caption, value);
        card.getStyle()
                .set("flex", "1")
                .set("min-width", "11rem")
                .set(Ui.BACKGROUND, background)
                .set("border", "1px solid " + accent)
                .set("border-left", "4px solid " + accent)
                .set("border-radius", "10px")
                .set(Ui.PADDING, "0.55rem 0.85rem")
                .set("box-sizing", "border-box");
        return card;
    }

    private void updateSummary() {
        int userId = currentUserId();
        totalCount.setText(String.valueOf(financeUseCase.getTransactionsByUser(userId).size()));
        totalIncome.setText(presenter.formatMoney(financeUseCase.getTotalIncome(userId)));
        totalExpense.setText(presenter.formatMoney(financeUseCase.getTotalExpense(userId)));
        balance.setText(presenter.formatMoney(financeUseCase.getBalance(userId)));
    }

    private static int currentUserId() {
        return AuthSession.currentUser().orElseThrow().getId();
    }

    // ------------------------------------------------------------------ tabel

    private void configureGrid() {
        grid.addColumn(Transaction::getId)
                .setHeader("ID").setKey("id").setAutoWidth(true).setFlexGrow(0)
                .setSortable(true).setComparator(Transaction::getId);

        grid.addColumn(Transaction::getDescription)
                .setHeader("Keterangan").setKey("description").setFlexGrow(1)
                .setSortable(true)
                .setComparator(Comparator.comparing(Transaction::getDescription, String.CASE_INSENSITIVE_ORDER));

        grid.addColumn(transaction -> presenter.formatMoney(transaction.getAmount()))
                .setHeader("Jumlah").setKey("amount").setAutoWidth(true)
                .setSortable(true).setComparator(Comparator.comparingLong(Transaction::getAmount));

        grid.addComponentColumn(this::createTypeBadge)
                .setHeader("Tipe").setKey("type").setAutoWidth(true).setFlexGrow(0)
                .setSortable(true).setComparator(Comparator.comparing(Transaction::getType));

        grid.addComponentColumn(FinanceView::createProofPreview)
                .setHeader("Bukti").setKey("proof").setAutoWidth(true).setFlexGrow(0).setSortable(false);

        grid.addColumn(presenter::formatCreatedAt)
                .setHeader("Waktu").setKey("createdAt").setAutoWidth(true)
                .setSortable(true).setComparator(Comparator.comparing(Transaction::getCreatedAt));

        grid.addComponentColumn(this::createActionButtons)
                .setHeader("Tindakan").setKey("tindakan").setAutoWidth(true).setFlexGrow(0).setSortable(false);

        grid.setEmptyStateComponent(emptyState);
        grid.setSizeFull();
        GridStyles.apply(grid);
    }

    private Component createTypeBadge(Transaction transaction) {
        Span badge = new Span(presenter.formatType(transaction.getType()));
        boolean income = transaction.isIncome();
        badge.getStyle()
                .set(Ui.DISPLAY, "inline-block")
                .set(Ui.PADDING, "0.1rem 0.55rem")
                .set("border-radius", "999px")
                .set(Ui.FONT_SIZE, "0.8rem")
                .set(Ui.FONT_WEIGHT, "600")
                .set(Ui.BACKGROUND, income ? "#DCFCE7" : "#FEE2E2")
                .set(Ui.COLOR, income ? "#166534" : "#991B1B");
        return badge;
    }

    private static Component createProofPreview(Transaction transaction) {
        String proof = transaction.getProofPath();
        if (proof == null || proof.isBlank()) {
            return new Span("-");
        }
        return ImagePreviewDialog.clickableThumbnail("/uploads/" + proof, "Bukti transaksi", "48px");
    }

    private HorizontalLayout createActionButtons(Transaction transaction) {
        Button deleteButton = new Button("Hapus", VaadinIcon.TRASH.create(),
                event -> DeleteTransactionDialog.open(transaction, () -> deleteTransaction(transaction)));
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        deleteButton.getStyle().set(Ui.CURSOR, Ui.POINTER);
        return new HorizontalLayout(deleteButton);
    }

    // ------------------------------------------------------------------ toolbar

    private void configureFilters() {
        searchField.setPlaceholder("Cari keterangan...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();
        searchField.setAriaLabel("Cari keterangan transaksi");
        searchField.addKeyPressListener(Key.ENTER, event -> applyFilters());
        searchField.addValueChangeListener(event -> {
            if (event.isFromClient() && event.getValue().isBlank()) {
                applyFilters();
            }
        });

        configureSelect(typeFilter, TransactionTypeFilter.values(), TransactionTypeFilter::label,
                TransactionTypeFilter.ALL, "11rem", "type-filter", "Filter tipe");
        configureSelect(sortSelect, SortOption.values(), SortOption::label,
                SortOption.NEWEST_FIRST, "12rem", "sort-select", "Urutan");
    }

    private <T> void configureSelect(
            Select<T> select,
            T[] items,
            com.vaadin.flow.component.ItemLabelGenerator<T> labels,
            T defaultValue,
            String width,
            String id,
            String ariaLabel
    ) {
        select.setItems(items);
        select.setItemLabelGenerator(labels);
        select.setValue(defaultValue);
        select.setWidth(width);
        select.setId(id);
        select.setAriaLabel(ariaLabel);
        select.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                applyFilters();
            }
        });
    }

    private HorizontalLayout buildToolbar() {
        Button incomeButton = toolbarButton("Pemasukan", VaadinIcon.ARROW_CIRCLE_DOWN_O,
                () -> openForm(TransactionType.INCOME), ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        Button expenseButton = toolbarButton("Pengeluaran", VaadinIcon.ARROW_CIRCLE_UP_O,
                () -> openForm(TransactionType.EXPENSE), ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        Button searchButton = toolbarButton("Cari", VaadinIcon.SEARCH, this::applyFilters);
        Button refreshButton = toolbarButton("Muat Ulang", VaadinIcon.REFRESH, this::refreshAll);
        Button aiButton = toolbarButton("Generate AI", VaadinIcon.MAGIC, this::openAiDialog,
                ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(
                searchField, typeFilter, sortSelect,
                searchButton, refreshButton, aiButton, incomeButton, expenseButton);
        toolbar.setWidthFull();
        toolbar.setAlignItems(Alignment.END);
        toolbar.getStyle().set(Ui.FLEX_WRAP, Ui.WRAP);
        toolbar.setFlexGrow(1, searchField);
        return toolbar;
    }

    private static Button toolbarButton(String text, VaadinIcon icon, Runnable action, ButtonVariant... variants) {
        Button button = new Button(text, icon.create(), event -> action.run());
        button.addThemeVariants(variants);
        button.getStyle().set(Ui.CURSOR, Ui.POINTER);
        return button;
    }

    // ------------------------------------------------------------------ aksi

    private void openForm(TransactionType type) {
        new TransactionFormDialog(financeUseCase, presenter, fileStorage, type, this::refreshAll).open();
    }

    private void openAiDialog() {
        new AiGenerateDialog(financeAiUseCase, presenter, this::refreshAll).open();
    }

    private void deleteTransaction(Transaction transaction) {
        try {
            financeUseCase.removeTransactionForUser(currentUserId(), transaction.getId());
            Notifier.success(presenter.showRemoveSuccess());
            refreshAll();
        } catch (EntityNotFoundException ex) {
            Notifier.error(presenter.showRemoveFailed(ex.getId()));
        } catch (DomainException ex) {
            Notifier.error(presenter.showValidationError(ex.getMessage()));
        }
    }

    // ------------------------------------------------------------------ data

    private void applyFilters() {
        String keyword = searchField.getValue().trim();
        TransactionTypeFilter type = typeFilter.getValue();
        SortOption sort = sortSelect.getValue();

        List<Transaction> results = financeUseCase.listTransactions(currentUserId(), keyword, type, sort);

        boolean filtered = !keyword.isBlank() || type != TransactionTypeFilter.ALL;
        emptyState.setText(filtered ? NOT_FOUND_TEXT : EMPTY_TEXT);
        refreshGrid(results, buildTitle(keyword, type, sort));
    }

    private static String buildTitle(String keyword, TransactionTypeFilter type, SortOption sort) {
        StringBuilder title = new StringBuilder(keyword.isBlank() ? "Daftar Transaksi" : "Hasil: \"" + keyword + "\"");
        if (type != TransactionTypeFilter.ALL) {
            title.append(SEPARATOR).append(type.label());
        }
        return title.append(SEPARATOR).append(sort.label()).toString();
    }

    private void refreshAll() {
        searchField.clear();
        typeFilter.setValue(TransactionTypeFilter.ALL);
        sortSelect.setValue(SortOption.NEWEST_FIRST);
        applyFilters();
    }

    private void refreshGrid(List<Transaction> transactions, String title) {
        grid.setItems(transactions);
        updateSummary();
        String empty = emptyState.getText().replaceFirst("^- ", "");
        statusLabel.setText(transactions.isEmpty() ? title + " — " + empty : title);
    }
}
