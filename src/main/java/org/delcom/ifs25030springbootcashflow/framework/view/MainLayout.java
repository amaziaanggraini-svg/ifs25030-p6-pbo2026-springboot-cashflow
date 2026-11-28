package org.delcom.ifs25030springbootcashflow.framework.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.HasElement;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.AfterNavigationEvent;
import com.vaadin.flow.router.AfterNavigationObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import org.delcom.ifs25030springbootcashflow.domain.entity.User;
import org.delcom.ifs25030springbootcashflow.domain.repository.IUserRepository;
import org.delcom.ifs25030springbootcashflow.framework.auth.AuthSession;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout utama untuk halaman yang memerlukan login.
 */
public class MainLayout extends AppLayout implements BeforeEnterObserver, AfterNavigationObserver {

    private final IUserRepository userRepository;
    private final List<NavEntry> navEntries = new ArrayList<>();
    private final Image headerAvatarImage = new Image();
    private final Span headerAvatarInitial = new Span();
    private final Div headerAvatar = new Div();
    /** Landmark utama: seluruh konten view dibungkus elemen main. */
    private final Main mainContent = new Main();

    private record NavEntry(Anchor link, String pathPrefix) {
    }

    public MainLayout(IUserRepository userRepository) {
        this.userRepository = userRepository;
        setPrimarySection(Section.DRAWER);
        AuthSession.currentUser(userRepository);
        mainContent.setSizeFull();
        mainContent.getStyle().set(Ui.DISPLAY, Ui.BLOCK).set("box-sizing", "border-box");
        setContent(mainContent);
        createHeader();
        createDrawer();
    }

    @Override
    public void showRouterLayoutContent(HasElement content) {
        mainContent.removeAll();
        if (content != null) {
            mainContent.getElement().appendChild(content.getElement());
        }
    }

    private void createHeader() {
        HorizontalLayout brandTitle = appBrandTitle("1.2rem", "28px");

        configureHeaderAvatar();
        refreshHeaderAvatar(AuthSession.currentUser().orElse(null));

        HorizontalLayout header = new HorizontalLayout(new DrawerToggle(), brandTitle, headerAvatar);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(brandTitle);
        header.setWidthFull();
        header.setPadding(true);
        header.getStyle()
                .set(Ui.BACKGROUND, "#FFFFFF")
                .set("border-bottom", Ui.V_1PX_SOLID_E5E7EB);
        // Landmark banner
        addToNavbar(new Header(header));
    }

    private static HorizontalLayout appBrandTitle(String fontSize, String iconSize) {
        Image logo = new Image("icons/icon.png", "Cashflow");
        logo.setWidth(iconSize);
        logo.setHeight(iconSize);
        logo.getStyle()
                .set(Ui.DISPLAY, Ui.BLOCK)
                .set(Ui.BORDER_RADIUS, "6px")
                .set("object-fit", "cover")
                .set(Ui.FLEX_SHRINK, "0");

        Span title = new Span("Cashflow");
        title.getStyle()
                .set(Ui.FONT_SIZE, fontSize)
                .set(Ui.FONT_WEIGHT, Ui.V_700)
                .set(Ui.MARGIN, "0")
                .set(Ui.COLOR, "#111827")
                .set("line-height", "1.2");

        HorizontalLayout brand = new HorizontalLayout(logo, title);
        brand.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        brand.setSpacing(false);
        brand.setPadding(false);
        brand.getStyle().set(Ui.GAP, "0.65rem");
        return brand;
    }

    private void configureHeaderAvatar() {
        headerAvatarImage.setWidth(Ui.V_40PX);
        headerAvatarImage.setHeight(Ui.V_40PX);
        headerAvatarImage.getStyle()
                .set(Ui.BORDER_RADIUS, Ui.V_50)
                .set("object-fit", "cover")
                .set(Ui.DISPLAY, Ui.BLOCK);

        headerAvatarInitial.getStyle()
                .set(Ui.WIDTH, Ui.V_40PX)
                .set("height", Ui.V_40PX)
                .set(Ui.BORDER_RADIUS, Ui.V_50)
                .set(Ui.DISPLAY, "inline-flex")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set(Ui.BACKGROUND, "#DBEAFE")
                .set(Ui.COLOR, Ui.V_1D4ED8)
                .set(Ui.FONT_WEIGHT, Ui.V_700)
                .set(Ui.FONT_SIZE, Ui.V_0_95REM);

        headerAvatar.removeAll();
        headerAvatar.add(headerAvatarInitial);
        headerAvatar.getStyle()
                .set(Ui.CURSOR, Ui.POINTER)
                .set(Ui.BORDER, "2px solid #93C5FD")
                .set(Ui.BORDER_RADIUS, Ui.V_50)
                .set("overflow", "hidden")
                .set(Ui.WIDTH, Ui.V_40PX)
                .set("height", Ui.V_40PX)
                .set(Ui.FLEX_SHRINK, "0");
        headerAvatar.getElement().setAttribute("title", "Menu akun");

        ContextMenu accountMenu = new ContextMenu(headerAvatar);
        accountMenu.setOpenOnClick(true);

        AuthSession.currentUser().ifPresent(user -> {
            var identityItem = accountMenu.addItem(accountIdentity(user));
            identityItem.setEnabled(false);
            identityItem.getStyle().set(Ui.CURSOR, "default");
            var profileItem = accountMenu.addItem(menuItemContent(VaadinIcon.USER, "Profil", Ui.V_4B5563, Ui.V_1F2937), event ->
                    UI.getCurrent().navigate(ProfileView.class));
            profileItem.getStyle().set(Ui.CURSOR, Ui.POINTER);
            var logoutItem = accountMenu.addItem(menuItemContent(VaadinIcon.SIGN_OUT, "Keluar", "#DC2626", "#DC2626"), event -> {
                AuthSession.logout();
                // Full reload agar UI tidak blank setelah keluar dari MainLayout.
                UI.getCurrent().getPage().setLocation("login");
            });
            logoutItem.getStyle().set(Ui.CURSOR, Ui.POINTER);
        });
    }

    private static VerticalLayout accountIdentity(User user) {
        Span username = new Span(user.getUsername());
        username.getStyle()
                .set(Ui.DISPLAY, Ui.BLOCK)
                .set(Ui.FONT_WEIGHT, Ui.V_700)
                .set(Ui.FONT_SIZE, Ui.V_0_95REM)
                .set(Ui.COLOR, "#111827")
                .set(Ui.MARGIN, "0");

        Span role = new Span(user.getRole().name());
        role.getStyle()
                .set(Ui.DISPLAY, Ui.BLOCK)
                .set("margin-top", "0.15rem")
                .set(Ui.FONT_SIZE, "0.75rem")
                .set(Ui.FONT_WEIGHT, Ui.V_600)
                .set("letter-spacing", "0.02em")
                .set(Ui.COLOR, Ui.V_1D4ED8);

        VerticalLayout identity = new VerticalLayout(username, role);
        identity.setPadding(false);
        identity.setSpacing(false);
        identity.setMargin(false);
        identity.getStyle()
                .set(Ui.GAP, "0")
                .set(Ui.PADDING, "0.15rem 0")
                .set("min-width", "8rem");
        return identity;
    }

    private static HorizontalLayout menuItemContent(VaadinIcon iconType, String label, String iconColor, String textColor) {
        Icon icon = iconType.create();
        icon.setSize("1rem");
        icon.getStyle()
                .set(Ui.COLOR, iconColor)
                .set(Ui.FLEX_SHRINK, "0")
                .set(Ui.MARGIN, "0");

        Span text = new Span(label);
        text.getStyle()
                .set(Ui.COLOR, textColor)
                .set(Ui.FONT_WEIGHT, Ui.V_600)
                .set(Ui.MARGIN, "0");

        HorizontalLayout content = new HorizontalLayout(icon, text);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        content.setSpacing(false);
        content.setPadding(false);
        content.setMargin(false);
        content.setWidth(null);
        content.getStyle()
                .set(Ui.GAP, "0.5rem")
                .set(Ui.MARGIN, "0")
                .set(Ui.PADDING, "0")
                .set("justify-content", "flex-start")
                .set(Ui.DISPLAY, "inline-flex");
        return content;
    }

    private void refreshHeaderAvatar(User user) {
        if (user == null) {
            headerAvatarInitial.setText("?");
            headerAvatar.removeAll();
            headerAvatar.add(headerAvatarInitial);
            return;
        }

        String initial = user.getFullName() == null || user.getFullName().isBlank()
                ? user.getUsername().substring(0, 1).toUpperCase()
                : user.getFullName().substring(0, 1).toUpperCase();
        headerAvatarInitial.setText(initial);

        headerAvatar.removeAll();
        if (user.getPhotoPath() != null && !user.getPhotoPath().isBlank()) {
            headerAvatarImage.setSrc("/uploads/" + user.getPhotoPath() + "?t=" + System.currentTimeMillis());
            headerAvatarImage.setAlt(user.getFullName());
            headerAvatar.add(headerAvatarImage);
        } else {
            headerAvatar.add(headerAvatarInitial);
        }
    }

    private void createDrawer() {
        VerticalLayout drawer = new VerticalLayout();
        drawer.setSizeFull();
        drawer.setPadding(false);
        drawer.setSpacing(false);
        drawer.getStyle()
                .set(Ui.BACKGROUND, "#FFFFFF")
                .set("border-right", Ui.V_1PX_SOLID_E5E7EB);

        Div brand = new Div();
        brand.getStyle()
                .set(Ui.PADDING, "1.25rem 1rem 1rem")
                .set("border-bottom", Ui.V_1PX_SOLID_E5E7EB);
        brand.add(appBrandTitle("1.05rem", "30px"));

        VerticalLayout menu = new VerticalLayout();
        menu.setPadding(true);
        menu.setSpacing(false);
        menu.getStyle().set(Ui.GAP, "0.35rem").set("padding-top", "1rem");

        menu.add(navItem("Cashflow Saya", "", VaadinIcon.WALLET));
        menu.add(navItem("Ringkasan", "summary", VaadinIcon.CHART));
        menu.add(navItem("Profil", "profile", VaadinIcon.USER));

        AuthSession.currentUser()
                .filter(User::isAdmin)
                .ifPresent(user -> menu.add(navItem("Daftar Pengguna", "users", VaadinIcon.USERS)));

        drawer.add(brand, menu);
        drawer.expand(menu);
        // Landmark navigation
        Nav nav = new Nav(drawer);
        nav.getElement().setAttribute("aria-label", "Navigasi utama");
        nav.setSizeFull();
        addToDrawer(nav);
    }

    private Anchor navItem(
            String label,
            String pathPrefix,
            VaadinIcon iconType
    ) {
        Icon icon = iconType.create();
        icon.setSize("1.05rem");
        icon.getStyle().set(Ui.COLOR, Ui.V_4B5563);

        Span text = new Span(label);
        text.getStyle()
                .set(Ui.FONT_SIZE, Ui.V_0_95REM)
                .set(Ui.FONT_WEIGHT, "500")
                .set(Ui.COLOR, Ui.V_1F2937);

        HorizontalLayout content = new HorizontalLayout(icon, text);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
        content.setSpacing(true);
        content.setPadding(false);
        content.getStyle().set(Ui.GAP, "0.65rem");

        // Anchor dengan href absolut + router-link: navigasi client-side dan mudah ditemukan crawler
        Anchor link = new Anchor("/" + pathPrefix);
        link.getElement().setAttribute("router-link", true);
        link.add(content);
        link.getStyle()
                .set(Ui.DISPLAY, Ui.BLOCK)
                .set(Ui.WIDTH, "100%")
                .set("text-decoration", "none")
                .set(Ui.PADDING, "0.7rem 0.85rem")
                .set(Ui.BORDER_RADIUS, "10px")
                .set("box-sizing", "border-box")
                .set(Ui.BORDER, "1px solid transparent")
                .set("transition", "background 0.15s ease")
                .set(Ui.CURSOR, Ui.POINTER);

        link.getElement().addEventListener("mouseover", e -> {
            if (!Ui.TRUE.equals(link.getElement().getAttribute(Ui.DATA_ACTIVE))) {
                link.getStyle().set(Ui.BACKGROUND, "#F3F4F6");
            }
        });
        link.getElement().addEventListener("mouseout", e -> {
            if (!Ui.TRUE.equals(link.getElement().getAttribute(Ui.DATA_ACTIVE))) {
                link.getStyle().set(Ui.BACKGROUND, "transparent");
            }
        });

        navEntries.add(new NavEntry(link, pathPrefix));
        return link;
    }

    private void highlightActive(String path) {
        String normalized = path == null ? "" : path;
        if (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        for (NavEntry entry : navEntries) {
            boolean active;
            if (entry.pathPrefix().isEmpty()) {
                active = normalized.isEmpty();
            } else {
                active = normalized.equals(entry.pathPrefix())
                        || normalized.startsWith(entry.pathPrefix() + "/");
            }
            // Highlight Daftar Pengguna juga saat melihat transaksi user lain.
            if ("users".equals(entry.pathPrefix()) && normalized.startsWith("user-transactions")) {
                active = true;
            }

            Anchor link = entry.link();
            if (active) {
                link.getElement().setAttribute(Ui.DATA_ACTIVE, Ui.TRUE);
                link.getStyle()
                        .set(Ui.BACKGROUND, "#DBEAFE")
                        .set(Ui.BORDER, "1px solid #93C5FD");
                styleNavChildren(link, Ui.V_1D4ED8, "#1E40AF", Ui.V_600);
            } else {
                link.getElement().setAttribute(Ui.DATA_ACTIVE, "false");
                link.getStyle()
                        .set(Ui.BACKGROUND, "transparent")
                        .set(Ui.BORDER, "1px solid transparent");
                styleNavChildren(link, Ui.V_4B5563, Ui.V_1F2937, "500");
            }
        }
    }

    private static void styleNavChildren(Anchor link, String iconColor, String textColor, String weight) {
        link.getChildren()
                .filter(HorizontalLayout.class::isInstance)
                .map(HorizontalLayout.class::cast)
                .findFirst()
                .ifPresent(row -> row.getChildren().forEach(child -> {
                    if (child instanceof Icon icon) {
                        icon.getStyle().set(Ui.COLOR, iconColor);
                    }
                    if (child instanceof Span span) {
                        span.getStyle().set(Ui.COLOR, textColor).set(Ui.FONT_WEIGHT, weight);
                    }
                }));
    }

    @Override
    public void afterNavigation(AfterNavigationEvent event) {
        highlightActive(event.getLocation().getPath());
        refreshHeaderAvatar(AuthSession.currentUser(userRepository).orElse(null));
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (AuthSession.currentUser(userRepository).isEmpty()) {
            event.forwardTo(LoginView.class);
        }
    }
}
