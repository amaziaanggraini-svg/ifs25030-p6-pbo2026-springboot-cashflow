package org.delcom.ifs25030springbootcashflow.framework.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import org.delcom.ifs25030springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25030springbootcashflow.domain.entity.AiGeneratedTransaction;
import org.delcom.ifs25030springbootcashflow.domain.entity.User;
import org.delcom.ifs25030springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25030springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25030springbootcashflow.usecase.FinanceAiUseCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/** Dialog Generate AI: catatan bebas -> pratinjau transaksi -> simpan. */
final class AiGenerateDialog {

    private static final int POLL_INTERVAL_MS = 400;

    private final FinanceAiUseCase aiUseCase;
    private final FinancePresenter presenter;
    private final Runnable onSaved;

    private final Dialog dialog = new Dialog();
    private final ProgressBar loadingBar = new ProgressBar();
    private final TextArea promptArea = new TextArea("Catatan Keuangan");
    private final Grid<AiGeneratedTransaction> previewGrid = new Grid<>(AiGeneratedTransaction.class, false);
    private final List<AiGeneratedTransaction> generated = new ArrayList<>();
    private final Button generateButton = new Button("Proses AI", VaadinIcon.MAGIC.create());
    private final Button applyButton = new Button("Tambahkan ke Catatan", VaadinIcon.PLUS_CIRCLE.create());
    private final Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());

    AiGenerateDialog(FinanceAiUseCase aiUseCase, FinancePresenter presenter, Runnable onSaved) {
        this.aiUseCase = aiUseCase;
        this.presenter = presenter;
        this.onSaved = onSaved;
        build();
    }

    void open() {
        dialog.open();
    }

    private void build() {
        loadingBar.setVisible(false);

        Span hint = new Span("Tuliskan catatan keuangan Anda dan biarkan AI menyusunnya menjadi daftar transaksi:");
        hint.getStyle().set("font-size", "0.875rem").set("color", "#4B5563");

        promptArea.setPlaceholder("Contoh: Gajian 5 juta, bayar kos 1,2 juta, makan siang 25 ribu...");
        promptArea.setWidthFull();
        promptArea.setMinHeight("100px");

        previewGrid.addColumn(AiGeneratedTransaction::description).setHeader("Keterangan").setFlexGrow(2);
        previewGrid.addColumn(item -> presenter.formatMoney(item.amount())).setHeader("Jumlah").setFlexGrow(1);
        previewGrid.addColumn(item -> presenter.formatType(item.type())).setHeader("Tipe").setFlexGrow(1);
        previewGrid.setWidthFull();
        previewGrid.setHeight("200px");
        previewGrid.setVisible(false);
        GridStyles.apply(previewGrid);

        applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        applyButton.setEnabled(false);
        applyButton.addClickListener(event -> applyGenerated());

        generateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        generateButton.addClickListener(event -> generate());

        HorizontalLayout actionRow = new HorizontalLayout(generateButton);
        actionRow.setWidthFull();
        actionRow.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        VerticalLayout body = new VerticalLayout(hint, promptArea, actionRow, previewGrid);
        body.setPadding(false);
        body.setSpacing(true);

        DialogShell.apply(dialog, "Generate Transaksi dengan AI", loadingBar, body, cancelButton, applyButton);
        dialog.setWidth("640px");
    }

    private void generate() {
        String prompt = promptArea.getValue() == null ? "" : promptArea.getValue().trim();
        if (prompt.isEmpty()) {
            Notifier.error("Prompt catatan keuangan tidak boleh kosong");
            return;
        }
        UI ui = UI.getCurrent();
        setBusy(true);
        ui.setPollInterval(POLL_INTERVAL_MS);
        CompletableFuture.supplyAsync(() -> requestTransactions(prompt))
                .whenComplete((result, error) -> ui.access(() -> {
                    showResult(result, error);
                    ui.setPollInterval(-1);
                }));
    }

    private List<AiGeneratedTransaction> requestTransactions(String prompt) {
        try {
            return aiUseCase.generateTransactions(prompt);
        } catch (DomainException ex) {
            throw new CompletionException(ex);
        }
    }

    private void showResult(List<AiGeneratedTransaction> result, Throwable error) {
        try {
            if (error != null) {
                Throwable cause = error.getCause() == null ? error : error.getCause();
                Notifier.error("Gagal menghasilkan transaksi AI: " + cause.getMessage());
                return;
            }
            generated.clear();
            generated.addAll(result);
            previewGrid.setItems(generated);
            previewGrid.setVisible(true);
            Notifier.success("AI berhasil menghasilkan " + generated.size() + " transaksi");
        } finally {
            setBusy(false);
        }
    }

    private void setBusy(boolean busy) {
        loadingBar.setVisible(busy);
        generateButton.setEnabled(!busy);
        generateButton.setText(busy ? "Memproses..." : "Proses AI");
        applyButton.setEnabled(!busy && !generated.isEmpty());
        cancelButton.setEnabled(!busy);
        promptArea.setReadOnly(busy);
    }

    private void applyGenerated() {
        setBusy(true);
        try {
            User user = AuthSession.currentUser().orElseThrow();
            aiUseCase.saveGeneratedTransactions(user.getId(), generated);
            Notifier.success(presenter.showAiAddSuccess(generated.size()));
            dialog.close();
            onSaved.run();
        } catch (DomainException ex) {
            Notifier.error(presenter.showValidationError(ex.getMessage()));
            setBusy(false);
        }
    }
}
