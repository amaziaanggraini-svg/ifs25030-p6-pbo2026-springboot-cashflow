package org.delcom.ifs25030springbootcashflow.framework.view;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

/** Penampil notifikasi seragam di pojok kiri bawah. */
final class Notifier {

    private static final int SUCCESS_MS = 3000;
    private static final int ERROR_MS = 4000;

    private Notifier() {
    }

    static void success(String message) {
        Notification.show(message, SUCCESS_MS, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    static void error(String message) {
        Notification.show(message, ERROR_MS, Notification.Position.BOTTOM_START)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
