package org.delcom.ifs25030springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.progressbar.ProgressBar;

/**
 * Kerangka dialog seragam: header primer (#2563EB), progress bar, isi, dan footer abu-abu
 * dengan tombol rata kanan.
 */
final class DialogShell {

    private static final String FULL_WIDTH = "100%";
    private static final String PAD_X = "1.5rem";

    private DialogShell() {
    }

    static void apply(Dialog dialog, String title, ProgressBar loadingBar, Component body, Button... footerButtons) {
        dialog.setWidth("500px");
        dialog.setMaxWidth("95vw");
        dialog.setResizable(false);
        dialog.addClassName("cashflow-form-dialog");

        loadingBar.setIndeterminate(true);
        loadingBar.getStyle().set("margin", "0").set("height", "4px");

        Div shell = new Div(header(title), loadingBar, bodyBox(body), footer(footerButtons));
        shell.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set(Ui.WIDTH, FULL_WIDTH)
                .set("overflow", "hidden")
                .set("border-radius", "12px")
                .set(Ui.BACKGROUND, Ui.FFFFFF);

        dialog.removeAll();
        dialog.add(shell);
        removeOverlayPadding(dialog);
    }

    private static Div header(String title) {
        Span headerTitle = new Span(title);
        headerTitle.getStyle()
                .set("margin", "0")
                .set("color", Ui.FFFFFF)
                .set("font-size", "1.125rem")
                .set("font-weight", "600")
                .set("line-height", "1.4");

        Div header = new Div(headerTitle);
        header.getStyle()
                .set(Ui.BACKGROUND, "#2563EB")
                .set(Ui.PADDING, "1rem " + PAD_X)
                .set(Ui.BOX_SIZING, Ui.BORDER_BOX)
                .set(Ui.WIDTH, FULL_WIDTH);
        return header;
    }

    private static Div bodyBox(Component content) {
        Div body = new Div(content);
        body.getStyle()
                .set(Ui.BACKGROUND, Ui.FFFFFF)
                .set(Ui.PADDING, "1.25rem " + PAD_X)
                .set("overflow", "hidden")
                .set(Ui.BOX_SIZING, Ui.BORDER_BOX)
                .set(Ui.WIDTH, FULL_WIDTH);
        return body;
    }

    private static Div footer(Button... buttons) {
        for (Button button : buttons) {
            button.getStyle().set("cursor", "pointer");
        }
        Div footer = new Div();
        footer.getStyle()
                .set("display", "flex")
                .set("justify-content", "flex-end")
                .set("align-items", "center")
                .set("gap", "0.75rem")
                .set(Ui.BACKGROUND, "#F3F4F6")
                .set("border-top", "1px solid #E5E7EB")
                .set(Ui.PADDING, "0.85rem " + PAD_X + " 1.1rem")
                .set(Ui.BOX_SIZING, Ui.BORDER_BOX)
                .set(Ui.WIDTH, FULL_WIDTH);
        footer.add(buttons);
        return footer;
    }

    static void removeOverlayPadding(Dialog dialog) {
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
}
