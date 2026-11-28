package org.delcom.ifs25030springbootcashflow.framework.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import org.delcom.ifs25030springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25030springbootcashflow.domain.entity.User;
import org.delcom.ifs25030springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25030springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25030springbootcashflow.usecase.UserUseCase;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/** Halaman profil: ubah nama, password, dan foto profil. */
@Route(value = "profile", layout = MainLayout.class)
@PageTitle("Profil")
public class ProfileView extends VerticalLayout {

    private static final String PHOTO_SIZE = "120px";

    private final UserUseCase userUseCase;
    private final FileStorage fileStorage;
    private final User user;

    private final Image photoPreview = new Image();
    private final Span photoInitial = new Span();
    private final Div photoFrame = new Div();
    private final Span photoPlaceholder = new Span("Belum ada foto profil. Upload foto di bawah.");

    private final TextField fullName = new TextField("Nama Lengkap");
    private final TextField username = new TextField("Username");
    private final PasswordField password = new PasswordField("Password Baru (opsional)");
    private final ProgressBar loadingBar = new ProgressBar();
    private final Button savePhoto = new Button("Simpan Foto");
    private final Button saveProfile = new Button("Simpan Profil");
    private final AtomicReference<String> pendingPhotoName = new AtomicReference<>();
    private final AtomicReference<byte[]> pendingPhotoData = new AtomicReference<>();

    public ProfileView(UserUseCase userUseCase, FileStorage fileStorage) {
        this.userUseCase = userUseCase;
        this.fileStorage = fileStorage;
        this.user = AuthSession.currentUser().orElseThrow();

        setSizeFull();
        setPadding(true);
        setSpacing(false);
        getStyle().set(Ui.BACKGROUND, "#F3F4F6");

        configureFields();
        configurePhotoFrame();
        refreshPhoto(user);
        configureButtons();

        H1 title = new H1("Profil");
        title.getStyle().set("margin", "0");
        Paragraph subtitle = new Paragraph("Perbarui nama, password, dan foto profil.");
        subtitle.getStyle().set("margin", "0").set(Ui.COLOR, "#4B5563");

        Span photoLabel = new Span("Foto Profil");
        photoLabel.getStyle().set("font-weight", "600").set(Ui.COLOR, "#1F2937");

        HorizontalLayout photoRow = new HorizontalLayout(photoFrame, photoPlaceholder);
        photoRow.setAlignItems(FlexComponent.Alignment.CENTER);
        photoRow.setSpacing(true);
        photoRow.setWidthFull();

        Div card = PageCard.wrap(false, title, subtitle, photoLabel, photoRow, createUpload(), savePhoto,
                fullName, username, password, loadingBar, saveProfile);
        card.getStyle().set("max-width", "560px");
        add(card);
    }

    private void configureFields() {
        fullName.setWidthFull();
        fullName.setValue(user.getFullName());
        username.setWidthFull();
        username.setValue(user.getUsername());
        username.setReadOnly(true);
        password.setWidthFull();

        loadingBar.setIndeterminate(true);
        loadingBar.setVisible(false);
        loadingBar.setWidthFull();
    }

    private void configureButtons() {
        savePhoto.getStyle().set(Ui.CURSOR, "pointer");
        savePhoto.addClickListener(event -> onSavePhoto());

        saveProfile.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveProfile.getStyle().set("margin-top", "0.5rem").set(Ui.CURSOR, "pointer");
        saveProfile.addClickListener(event -> onSaveProfile());
    }

    private Upload createUpload() {
        Upload upload = new Upload(UploadHandler.inMemory((metadata, data) -> {
            pendingPhotoName.set(metadata.fileName());
            pendingPhotoData.set(data);
        }));
        upload.setAcceptedMimeTypes("image/png", "image/jpeg", "image/webp");
        upload.setAcceptedFileExtensions(".png", ".jpg", ".jpeg", ".webp");
        upload.setMaxFiles(1);
        upload.setWidthFull();
        return upload;
    }

    private void setBusy(Button button, boolean busy) {
        loadingBar.setVisible(busy);
        button.setEnabled(!busy);
    }

    private boolean hasPendingPhoto() {
        String filename = pendingPhotoName.get();
        return filename != null && !filename.isBlank() && pendingPhotoData.get() != null;
    }

    private void onSavePhoto() {
        setBusy(savePhoto, true);
        if (!hasPendingPhoto()) {
            setBusy(savePhoto, false);
            Notifier.error("Pilih file foto terlebih dahulu");
            return;
        }
        try {
            String path = fileStorage.store(
                    "profiles", pendingPhotoName.get(), new ByteArrayInputStream(pendingPhotoData.get()));
            User updated = userUseCase.updatePhoto(user.getId(), path);
            AuthSession.refresh(updated);
            refreshPhoto(updated);
            pendingPhotoName.set(null);
            pendingPhotoData.set(null);
            setBusy(savePhoto, false);
            Notifier.success("Foto profil diperbarui");
            UI.getCurrent().navigate(ProfileView.class);
        } catch (DomainException | IOException ex) {
            setBusy(savePhoto, false);
            Notifier.error(ex.getMessage());
        }
    }

    private void onSaveProfile() {
        setBusy(saveProfile, true);
        try {
            User updated = userUseCase.updateProfile(user.getId(), fullName.getValue(), password.getValue());
            AuthSession.refresh(updated);
            password.clear();
            setBusy(saveProfile, false);
            Notifier.success("Profil diperbarui");
        } catch (DomainException ex) {
            setBusy(saveProfile, false);
            Notifier.error(ex.getMessage());
        }
    }

    private void configurePhotoFrame() {
        photoPreview.setWidth(PHOTO_SIZE);
        photoPreview.setHeight(PHOTO_SIZE);
        photoPreview.getStyle()
                .set(Ui.BORDER_RADIUS, Ui.V_50)
                .set("object-fit", "cover")
                .set("display", "block")
                .set(Ui.CURSOR, "zoom-in");
        photoPreview.getElement().setAttribute("title", "Klik untuk memperbesar");
        photoPreview.addClickListener(
                event -> ImagePreviewDialog.open(photoPreview.getSrc(), photoPreview.getAlt().orElse("Foto profil")));

        photoInitial.getStyle()
                .set("width", PHOTO_SIZE)
                .set("height", PHOTO_SIZE)
                .set(Ui.BORDER_RADIUS, Ui.V_50)
                .set("display", "inline-flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set(Ui.BACKGROUND, "#DBEAFE")
                .set(Ui.COLOR, "#1D4ED8")
                .set("font-weight", "700")
                .set("font-size", "2.5rem");

        photoFrame.getStyle()
                .set("width", PHOTO_SIZE)
                .set("height", PHOTO_SIZE)
                .set(Ui.BORDER_RADIUS, Ui.V_50)
                .set("overflow", "hidden")
                .set("border", "3px solid #93C5FD")
                .set("flex-shrink", "0")
                .set(Ui.BACKGROUND, "#EFF6FF");

        photoPlaceholder.getStyle().set(Ui.COLOR, "#6B7280");
    }

    private void refreshPhoto(User current) {
        String name = current.getFullName() == null || current.getFullName().isBlank()
                ? current.getUsername()
                : current.getFullName();
        photoInitial.setText(name.substring(0, 1).toUpperCase());

        photoFrame.removeAll();
        if (current.getPhotoPath() == null || current.getPhotoPath().isBlank()) {
            photoFrame.add(photoInitial);
            photoPlaceholder.setVisible(true);
            return;
        }
        photoPreview.setSrc("/uploads/" + current.getPhotoPath() + "?t=" + System.currentTimeMillis());
        photoPreview.setAlt(current.getFullName());
        photoFrame.add(photoPreview);
        photoPlaceholder.setVisible(false);
    }
}
