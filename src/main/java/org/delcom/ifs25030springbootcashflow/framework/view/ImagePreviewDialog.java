package org.delcom.ifs25030springbootcashflow.framework.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * Modal untuk menampilkan gambar dalam ukuran diperbesar dengan fitur zoom interaktif.
 */
final class ImagePreviewDialog {

    private static final double MIN_SCALE = 1.0;
    private static final double MAX_SCALE = 3.0;
    private static final double STEP = 0.4;
    private static final double CLICK_SCALE = 1.8;
    private static final String FULL_WIDTH = "100%";

    private final Image image;
    private final Dialog dialog = new Dialog();
    private double scale = MIN_SCALE;

    private ImagePreviewDialog(String imageUrl, String title) {
        image = new Image(imageUrl, title);
        build(title);
    }

    static void open(String imageUrl, String alt) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return;
        }
        String title = alt == null || alt.isBlank() ? "Pratinjau Gambar" : alt;
        new ImagePreviewDialog(imageUrl, title).dialog.open();
    }

    static Image clickableThumbnail(String imageUrl, String alt, String height) {
        return clickableThumbnail(imageUrl, alt, height, "6px");
    }

    static Image clickableThumbnail(String imageUrl, String alt, String height, String borderRadius) {
        Image thumbnail = new Image(imageUrl, alt);
        thumbnail.setHeight(height);
        thumbnail.getStyle()
                .set("border-radius", borderRadius)
                .set("object-fit", "cover")
                .set("cursor", "pointer")
                .set("display", "block");
        thumbnail.getElement().setAttribute("title", "Klik untuk memperbesar");
        thumbnail.addClickListener(event -> open(imageUrl, alt));
        return thumbnail;
    }

    private void build(String title) {
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);
        dialog.setResizable(false);
        dialog.setMaxWidth("95vw");

        image.getStyle()
                .set(Ui.DISPLAY, Ui.BLOCK)
                .set("max-width", FULL_WIDTH)
                .set("max-height", "68vh")
                .set(Ui.WIDTH, Ui.AUTO)
                .set("height", Ui.AUTO)
                .set("object-fit", "contain")
                .set(Ui.BORDER_RADIUS, "8px")
                .set(Ui.CURSOR, "zoom-in")
                .set("transition", "transform 0.25s ease")
                .set("user-select", "none");
        image.getElement().setAttribute("title", "Klik untuk zoom");
        image.addClickListener(event -> setScale(scale > MIN_SCALE ? MIN_SCALE : CLICK_SCALE));

        Div shell = new Div(header(title), body(), footer());
        shell.getStyle()
                .set(Ui.DISPLAY, Ui.FLEX)
                .set("flex-direction", "column")
                .set(Ui.WIDTH, FULL_WIDTH)
                .set(Ui.OVERFLOW, "hidden")
                .set(Ui.BORDER_RADIUS, "12px")
                .set(Ui.BACKGROUND, Ui.FFFFFF);

        dialog.add(shell);
        DialogShell.removeOverlayPadding(dialog);
    }

    private static Div header(String title) {
        Span headerTitle = new Span(title);
        headerTitle.getStyle()
                .set("margin", "0")
                .set("color", Ui.FFFFFF)
                .set("font-size", "1.125rem")
                .set("font-weight", "600")
                .set("line-height", "1.4");

        Span headerHint = new Span("Klik gambar untuk memperbesar / memperkecil");
        headerHint.getStyle()
                .set("margin-top", "0.2rem")
                .set("color", "#BFDBFE")
                .set("font-size", "0.8rem")
                .set(Ui.DISPLAY, Ui.BLOCK);

        Div header = new Div(headerTitle, headerHint);
        header.getStyle()
                .set(Ui.BACKGROUND, "#2563EB")
                .set(Ui.PADDING, "0.9rem 1.5rem")
                .set(Ui.BOX_SIZING, Ui.BORDER_BOX)
                .set(Ui.WIDTH, FULL_WIDTH);
        return header;
    }

    private Div body() {
        Div wrapper = new Div(image);
        wrapper.getStyle()
                .set(Ui.DISPLAY, Ui.FLEX)
                .set(Ui.JUSTIFY_CONTENT, Ui.CENTER)
                .set(Ui.ALIGN_ITEMS, Ui.CENTER)
                .set(Ui.OVERFLOW, Ui.AUTO)
                .set("max-height", "70vh")
                .set("max-width", "min(88vw, 860px)")
                .set(Ui.WIDTH, FULL_WIDTH)
                .set(Ui.BOX_SIZING, Ui.BORDER_BOX);

        Div body = new Div(wrapper);
        body.getStyle()
                .set(Ui.DISPLAY, Ui.FLEX)
                .set(Ui.JUSTIFY_CONTENT, Ui.CENTER)
                .set(Ui.ALIGN_ITEMS, Ui.CENTER)
                .set(Ui.BACKGROUND, Ui.FFFFFF)
                .set(Ui.PADDING, "1rem 1.25rem")
                .set(Ui.BOX_SIZING, Ui.BORDER_BOX)
                .set(Ui.WIDTH, FULL_WIDTH)
                .set(Ui.OVERFLOW, "hidden");
        return body;
    }

    private Div footer() {
        Button zoomOut = new Button("Perkecil", VaadinIcon.SEARCH_MINUS.create(),
                event -> setScale(Math.max(scale - STEP, MIN_SCALE)));
        Button zoomIn = new Button("Perbesar", VaadinIcon.SEARCH_PLUS.create(),
                event -> setScale(Math.min(scale + STEP, MAX_SCALE)));
        Button close = new Button("Tutup", VaadinIcon.CLOSE.create(), event -> dialog.close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        for (Button button : new Button[] {zoomOut, zoomIn, close}) {
            button.getStyle().set(Ui.CURSOR, "pointer");
        }

        Div footer = new Div(zoomOut, zoomIn, close);
        footer.getStyle()
                .set(Ui.DISPLAY, Ui.FLEX)
                .set(Ui.JUSTIFY_CONTENT, "flex-end")
                .set(Ui.ALIGN_ITEMS, Ui.CENTER)
                .set("gap", "0.6rem")
                .set(Ui.BACKGROUND, "#F3F4F6")
                .set("border-top", "1px solid #E5E7EB")
                .set(Ui.PADDING, "0.85rem 1.5rem 1.1rem")
                .set(Ui.BOX_SIZING, Ui.BORDER_BOX)
                .set(Ui.WIDTH, FULL_WIDTH);
        return footer;
    }

    private void setScale(double newScale) {
        scale = newScale;
        if (scale <= MIN_SCALE) {
            scale = MIN_SCALE;
            image.getStyle().set("transform", "scale(1)").set(Ui.CURSOR, "zoom-in");
        } else {
            image.getStyle()
                    .set("transform", "scale(" + scale + ")")
                    .set("transform-origin", "center center")
                    .set(Ui.CURSOR, "zoom-out");
        }
    }
}
