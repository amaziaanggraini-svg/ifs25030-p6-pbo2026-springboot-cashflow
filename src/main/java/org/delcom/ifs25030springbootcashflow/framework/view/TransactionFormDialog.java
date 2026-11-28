package org.delcom.ifs25030springbootcashflow.framework.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;
import org.delcom.ifs25030springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25030springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25030springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25030springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25030springbootcashflow.domain.entity.User;
import org.delcom.ifs25030springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25030springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25030springbootcashflow.usecase.FinanceUseCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/** Dialog untuk menambah pemasukan atau pengeluaran (dengan bukti opsional). */
final class TransactionFormDialog {

    private final FinanceUseCase financeUseCase;
    private final FinancePresenter presenter;
    private final FileStorage fileStorage;
    private final TransactionType type;
    private final Runnable onSaved;

    private final Dialog dialog = new Dialog();
    private final ProgressBar loadingBar = new ProgressBar();
    private final TextField descriptionField = new TextField("Keterangan");
    private final TextField amountField = new TextField("Jumlah (Rp)");
    private final Button saveButton = new Button("Simpan", VaadinIcon.CHECK.create());
    private final AtomicReference<String> proofName = new AtomicReference<>();
    private final AtomicReference<byte[]> proofData = new AtomicReference<>();

    TransactionFormDialog(
            FinanceUseCase financeUseCase,
            FinancePresenter presenter,
            FileStorage fileStorage,
            TransactionType type,
            Runnable onSaved
    ) {
        this.financeUseCase = financeUseCase;
        this.presenter = presenter;
        this.fileStorage = fileStorage;
        this.type = type;
        this.onSaved = onSaved;
        build();
    }

    void open() {
        dialog.open();
    }

    private void build() {
        boolean income = type == TransactionType.INCOME;
        descriptionField.setWidthFull();
        descriptionField.setPlaceholder(income ? "Contoh: Gaji bulanan" : "Contoh: Makan siang");
        amountField.setWidthFull();
        amountField.setPlaceholder("Contoh: 1000000");
        amountField.setPrefixComponent(new Span("Rp"));
        loadingBar.setVisible(false);

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> save());
        Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());

        VerticalLayout body = new VerticalLayout(
                descriptionField, amountField, new Span("Bukti transaksi (opsional)"), createUpload());
        body.setPadding(false);
        body.setSpacing(true);

        DialogShell.apply(dialog, income ? "Menambah Pemasukan" : "Menambah Pengeluaran",
                loadingBar, body, cancelButton, saveButton);
    }

    private Upload createUpload() {
        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            proofName.set(metadata.fileName());
            proofData.set(data);
        }));
        upload.setAcceptedMimeTypes("image/png", "image/jpeg", "image/webp");
        upload.setAcceptedFileExtensions(".png", ".jpg", ".jpeg", ".webp");
        upload.setMaxFiles(1);
        return upload;
    }

    private void save() {
        loadingBar.setVisible(true);
        saveButton.setEnabled(false);
        try {
            User user = AuthSession.currentUser().orElseThrow();
            Long amount = financeUseCase.parseAmount(amountField.getValue());
            Transaction created = financeUseCase.addTransaction(
                    user.getId(), descriptionField.getValue(), amount, type);
            attachProof(user.getId(), created.getId());
            Notifier.success(presenter.showAddSuccess(created));
            dialog.close();
            onSaved.run();
        } catch (DomainException | IOException ex) {
            loadingBar.setVisible(false);
            saveButton.setEnabled(true);
            Notifier.error(presenter.showValidationError(ex.getMessage()));
        }
    }

    private void attachProof(int userId, int transactionId) throws IOException, DomainException {
        String filename = proofName.get();
        byte[] data = proofData.get();
        if (filename == null || filename.isBlank() || data == null) {
            return;
        }
        String path = fileStorage.store("proofs", filename, new ByteArrayInputStream(data));
        financeUseCase.updateTransactionForUser(userId, transactionId, null, null, path, false);
    }
}
