package org.delcom.ifs25030springbootcashflow.framework.view;

import com.github.mvysny.kaributesting.v10.MockVaadin;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.SortDirection;
import org.delcom.ifs25030springbootcashflow.adapter.ai.InMemoryFinanceAiService;
import org.delcom.ifs25030springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25030springbootcashflow.adapter.repository.TransactionRepository;
import org.delcom.ifs25030springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25030springbootcashflow.domain.entity.Role;
import org.delcom.ifs25030springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25030springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25030springbootcashflow.domain.entity.User;
import org.delcom.ifs25030springbootcashflow.domain.exception.EntityNotFoundException;
import org.delcom.ifs25030springbootcashflow.domain.exception.ValidationException;
import org.delcom.ifs25030springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25030springbootcashflow.usecase.FinanceAiUseCase;
import org.delcom.ifs25030springbootcashflow.usecase.FinanceUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static com.github.mvysny.kaributesting.v10.GridKt._getCellComponent;
import static com.github.mvysny.kaributesting.v10.LocatorJ._click;
import static com.github.mvysny.kaributesting.v10.LocatorJ._get;
import static com.github.mvysny.kaributesting.v10.LocatorJ._setValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

class FinanceViewTest {

    private static final int USER_ID = 1;
    private static final String DESC_INCOME = "Contoh: Gaji bulanan";
    private static final String DESC_EXPENSE = "Contoh: Makan siang";
    private static final String AMOUNT = "Contoh: 1000000";

    @TempDir
    Path tempDir;

    private FinanceUseCase useCase;

    @BeforeEach
    void setUp() {
        TransactionRepository repository = new TransactionRepository();
        useCase = spy(new FinanceUseCase(repository));
        FinanceAiUseCase aiUseCase = new FinanceAiUseCase(new InMemoryFinanceAiService(), repository);
        MockVaadin.setup();
        AuthSession.login(new User(USER_ID, "andi", "hash", "Andi", Role.USER, null));
        UI.getCurrent().add(new FinanceView(useCase, new FinancePresenter(), new FileStorage(tempDir.toString()), aiUseCase));
    }

    @AfterEach
    void tearDown() {
        AuthSession.logout();
        MockVaadin.tearDown();
    }

    @Test
    void addsIncomeAndExpenseAndUpdatesSummary() {
        assertTrue(_get(Paragraph.class).getText().contains("Data transaksi belum tersedia"));
        assertSummary("0", "Rp 0.00", "Rp 0.00", "Rp 0.00");

        addTransactionViaDialog("Pemasukan", DESC_INCOME, "Gaji bulanan", "5.000.000");
        assertEquals(1, useCase.getTransactionsByUser(USER_ID).size());
        assertEquals("Daftar Transaksi · Terbaru", _get(Paragraph.class).getText());

        addTransactionViaDialog("Pengeluaran", DESC_EXPENSE, "Makan siang", "25000");
        assertEquals(2, useCase.getTransactionsByUser(USER_ID).size());
        assertSummary("2", "Rp 5,000,000.00", "Rp 25,000.00", "Rp 4,975,000.00");
    }

    @Test
    void searchesAndFiltersTransactions() {
        seedTwoTransactions();
        TextField searchField = _get(TextField.class, spec -> spec.withPlaceholder("Cari keterangan..."));

        _setValue(searchField, "tidakada");
        clickButton("Cari");
        assertTrue(_get(Paragraph.class).getText().contains("Transaksi tidak ditemukan"));

        _setValue(searchField, "Gaji");
        clickButton("Cari");
        assertEquals("Hasil: \"Gaji\" · Terbaru", _get(Paragraph.class).getText());

        clickButton("Muat Ulang");
        assertEquals(2, grid().getListDataView().getItemCount());

        @SuppressWarnings("unchecked")
        Select<TransactionTypeFilter> typeSelect = _get(Select.class, spec -> spec.withId("type-filter"));
        _setValue(typeSelect, TransactionTypeFilter.INCOME);
        clickButton("Cari");
        assertEquals(1, grid().getListDataView().getItemCount());
        assertEquals("Daftar Transaksi · Pemasukan · Terbaru", _get(Paragraph.class).getText());

        clickButton("Muat Ulang");
        assertEquals(2, grid().getListDataView().getItemCount());
    }

    @Test
    void sortsGridAndDeletesTransaction() {
        seedTwoTransactions();
        Grid<Transaction> grid = grid();

        grid.sort(List.of(new GridSortOrder<>(grid.getColumnByKey("description"), SortDirection.DESCENDING)));
        assertEquals("Makan siang", firstItem(grid).getDescription());
        grid.sort(List.of(new GridSortOrder<>(grid.getColumnByKey("amount"), SortDirection.ASCENDING)));
        assertEquals("Makan siang", firstItem(grid).getDescription());
        for (String key : List.of("id", "type", "createdAt")) {
            grid.sort(List.of(new GridSortOrder<>(grid.getColumnByKey(key), SortDirection.ASCENDING)));
        }

        int row = indexOfDescription(grid, "Gaji bulanan");
        Span badge = (Span) _getCellComponent(grid, row, "type");
        assertEquals("Pemasukan", badge.getText());
        HorizontalLayout actions = (HorizontalLayout) _getCellComponent(grid, row, "tindakan");
        _click(_get(actions, Button.class, spec -> spec.withCaption("Hapus")));
        clickDialogButton(_get(Dialog.class), "Hapus");

        assertEquals(1, useCase.getTransactionsByUser(USER_ID).size());
        assertSummary("1", "Rp 0.00", "Rp 25,000.00", "-Rp 25,000.00");
    }

    @Test
    void rejectsInvalidInputAndAllowsCancel() {
        clickButton("Pemasukan");
        Dialog dialog = _get(Dialog.class);
        TextField description = _get(dialog, TextField.class, spec -> spec.withPlaceholder(DESC_INCOME));
        TextField amount = _get(dialog, TextField.class, spec -> spec.withPlaceholder(AMOUNT));

        _setValue(description, " ");
        _setValue(amount, "100");
        clickDialogButton(dialog, "Simpan");

        _setValue(description, "Valid");
        _setValue(amount, "abc");
        clickDialogButton(dialog, "Simpan");

        _setValue(amount, "0");
        clickDialogButton(dialog, "Simpan");
        assertTrue(useCase.getTransactionsByUser(USER_ID).isEmpty());
        clickDialogButton(dialog, "Batal");

        TextField searchField = _get(TextField.class, spec -> spec.withPlaceholder("Cari keterangan..."));
        _setValue(searchField, "   ");
        clickButton("Cari");
        assertTrue(_get(Paragraph.class).getText().contains("Data transaksi belum tersedia"));
    }

    @Test
    void handlesDeleteFailuresAndCancel() throws Exception {
        useCase.addIncome(USER_ID, "Temp", 100L);
        clickButton("Muat Ulang");
        Transaction selected = useCase.getTransactionsByUser(USER_ID).get(0);

        @SuppressWarnings("unchecked")
        Grid<Transaction> grid = _get(Grid.class);
        HorizontalLayout actions = (HorizontalLayout) _getCellComponent(grid, 0, "tindakan");

        _click(_get(actions, Button.class, spec -> spec.withCaption("Hapus")));
        clickDialogButton(_get(Dialog.class), "Batal");
        assertEquals(1, useCase.getTransactionsByUser(USER_ID).size());

        doThrow(new EntityNotFoundException(selected.getId()))
                .when(useCase).removeTransactionForUser(USER_ID, selected.getId());
        _click(_get(actions, Button.class, spec -> spec.withCaption("Hapus")));
        clickDialogButton(_get(Dialog.class), "Hapus");

        doThrow(new ValidationException("bukan milik"))
                .when(useCase).removeTransactionForUser(anyInt(), anyInt());
        _click(_get(actions, Button.class, spec -> spec.withCaption("Hapus")));
        clickDialogButton(_get(Dialog.class), "Hapus");
        assertEquals(1, useCase.getTransactionsByUser(USER_ID).size());
    }

    @Test
    void aiDialogRejectsEmptyPromptAndCanBeCancelled() {
        clickButton("Generate AI");
        Dialog dialog = _get(Dialog.class);
        assertTrue(_get(dialog, TextArea.class).isVisible());

        clickDialogButton(dialog, "Proses AI");
        assertTrue(useCase.getTransactionsByUser(USER_ID).isEmpty());
        clickDialogButton(dialog, "Batal");
    }

    private void addTransactionViaDialog(String button, String descPlaceholder, String description, String amount) {
        clickButton(button);
        Dialog dialog = _get(Dialog.class);
        _setValue(_get(dialog, TextField.class, spec -> spec.withPlaceholder(descPlaceholder)), description);
        _setValue(_get(dialog, TextField.class, spec -> spec.withPlaceholder(AMOUNT)), amount);
        clickDialogButton(dialog, "Simpan");
    }

    private void seedTwoTransactions() {
        addTransactionViaDialog("Pemasukan", DESC_INCOME, "Gaji bulanan", "5000000");
        addTransactionViaDialog("Pengeluaran", DESC_EXPENSE, "Makan siang", "25000");
    }

    @SuppressWarnings("unchecked")
    private Grid<Transaction> grid() {
        return _get(Grid.class);
    }

    private static Transaction firstItem(Grid<Transaction> grid) {
        return grid.getListDataView().getItems().toList().get(0);
    }

    private int indexOfDescription(Grid<Transaction> grid, String description) {
        List<Transaction> items = grid.getListDataView().getItems().toList();
        for (int i = 0; i < items.size(); i++) {
            if (description.equals(items.get(i).getDescription())) {
                return i;
            }
        }
        throw new AssertionError("Transaksi tidak ditemukan: " + description);
    }

    private void assertSummary(String total, String income, String expense, String balance) {
        FinanceView view = _get(FinanceView.class);
        HorizontalLayout summary = findFirst(view, HorizontalLayout.class);
        List<String> values = summary.getChildren()
                .map(card -> ((Div) card).getChildren()
                        .skip(1)
                        .map(component -> ((Span) component).getText())
                        .findFirst()
                        .orElse(""))
                .toList();
        assertEquals(List.of(total, income, expense, balance), values);
    }

    private <T extends com.vaadin.flow.component.Component> T findFirst(
            com.vaadin.flow.component.Component root,
            Class<T> type
    ) {
        T found = findFirstOrNull(root, type);
        if (found == null) {
            throw new java.util.NoSuchElementException(type.getSimpleName() + " not found");
        }
        return found;
    }

    private <T extends com.vaadin.flow.component.Component> T findFirstOrNull(
            com.vaadin.flow.component.Component root,
            Class<T> type
    ) {
        if (type.isInstance(root)) {
            return type.cast(root);
        }
        return root.getChildren()
                .map(child -> findFirstOrNull(child, type))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private void clickButton(String caption) {
        _click(_get(Button.class, spec -> spec.withCaption(caption)));
    }

    private void clickDialogButton(Dialog dialog, String caption) {
        _click(_get(dialog, Button.class, spec -> spec.withCaption(caption)));
    }
}
