package org.delcom.ifs25030springbootcashflow.framework.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.progressbar.ProgressBar;
import org.delcom.ifs25030springbootcashflow.domain.entity.Transaction;

/** Dialog konfirmasi penghapusan transaksi. */
final class DeleteTransactionDialog {

    private DeleteTransactionDialog() {
    }

    static void open(Transaction transaction, Runnable onConfirm) {
        Paragraph message = new Paragraph(
                "Yakin ingin menghapus transaksi \"" + transaction.getDescription()
                        + "\" (ID " + transaction.getId() + ")?");
        message.getStyle().set("margin", "0");

        Dialog dialog = new Dialog();
        ProgressBar loadingBar = new ProgressBar();
        loadingBar.setVisible(false);

        Button confirmButton = new Button("Hapus", VaadinIcon.TRASH.create());
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        confirmButton.addClickListener(event -> {
            loadingBar.setVisible(true);
            confirmButton.setEnabled(false);
            onConfirm.run();
            dialog.close();
        });
        Button cancelButton = new Button("Batal", VaadinIcon.CLOSE.create(), event -> dialog.close());

        DialogShell.apply(dialog, "Menghapus Transaksi", loadingBar, message, cancelButton, confirmButton);
        dialog.open();
    }
}
