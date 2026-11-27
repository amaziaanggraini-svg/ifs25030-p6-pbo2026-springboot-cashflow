package org.delcom.ifs25030springbootcashflow.framework.view;

import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.delcom.ifs25030springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25030springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25030springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25030springbootcashflow.domain.entity.TransactionTypeFilter;
import org.delcom.ifs25030springbootcashflow.domain.entity.User;
import org.delcom.ifs25030springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25030springbootcashflow.usecase.FinanceUseCase;

import java.util.List;

/**
 * Halaman ringkasan cashflow: total pemasukan, pengeluaran, saldo, rasio, dan transaksi terbesar.
 */
@Route(value = "summary", layout = MainLayout.class)
@PageTitle("Ringkasan")
public class SummaryView extends VerticalLayout {

    private static final int TOP_COUNT = 5;

    public SummaryView(FinanceUseCase financeUseCase, FinancePresenter presenter) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle()
                .set("background", "#F3F4F6")
                .set("padding", "0.75rem")
                .set("box-sizing", "border-box");

        H1 title = new H1("Ringkasan Cashflow");
        title.getStyle().set("margin", "0").set("font-size", "1.45rem").set("line-height", "1.2");

        Anchor back = new Anchor("/", "Kembali ke Cashflow Saya");
        back.getElement().setAttribute("router-link", true);
        back.getStyle().set("color", "#1D4ED8").set("font-weight", "600").set("font-size", "0.875rem");

        User user = AuthSession.currentUser().orElse(null);
        if (user == null) {
            add(PageCard.wrap(title, back, new Paragraph("Silakan login terlebih dahulu.")));
            return;
        }

        long income = financeUseCase.getTotalIncome(user.getId());
        long expense = financeUseCase.getTotalExpense(user.getId());
        long balance = financeUseCase.getBalance(user.getId());

        Div stats = new Div(
                stat("Total Pemasukan", presenter.formatMoney(income), "#DCFCE7", "#166534"),
                stat("Total Pengeluaran", presenter.formatMoney(expense), "#FEE2E2", "#991B1B"),
                stat("Saldo", presenter.formatMoney(balance), "#DBEAFE", "#1E40AF")
        );
        stats.getStyle().set("display", "flex").set("gap", "0.75rem").set("flex-wrap", "wrap");

        String ratio = income == 0 ? "-" : Math.round(expense * 100.0 / income) + "%";
        Paragraph ratioText = new Paragraph("Rasio pengeluaran terhadap pemasukan: " + ratio);
        ratioText.getStyle().set("margin", "0").set("color", "#374151");

        List<Transaction> top = financeUseCase
                .listTransactions(user.getId(), null, TransactionTypeFilter.ALL, SortOption.AMOUNT_DESC)
                .stream()
                .limit(TOP_COUNT)
                .toList();

        Grid<Transaction> grid = new Grid<>(Transaction.class, false);
        grid.addColumn(Transaction::getDescription).setHeader("Keterangan").setFlexGrow(1);
        grid.addColumn(t -> presenter.formatMoney(t.getAmount())).setHeader("Jumlah").setAutoWidth(true);
        grid.addColumn(t -> presenter.formatType(t.getType())).setHeader("Tipe").setAutoWidth(true);
        grid.addColumn(presenter::formatCreatedAt).setHeader("Waktu").setAutoWidth(true);
        grid.setItems(top);
        grid.setEmptyStateComponent(new Span("- Data transaksi belum tersedia!"));
        grid.setAllRowsVisible(true);
        GridStyles.apply(grid);

        add(PageCard.wrap(title, back, stats, ratioText, grid));
    }

    private Div stat(String label, String value, String background, String color) {
        Span caption = new Span(label);
        caption.getStyle().set("display", "block").set("font-size", "0.8rem").set("color", color);
        Span amount = new Span(value);
        amount.getStyle().set("display", "block").set("font-size", "1.25rem").set("font-weight", "700").set("color", color);
        Div card = new Div(caption, amount);
        card.getStyle()
                .set("flex", "1")
                .set("min-width", "11rem")
                .set("background", background)
                .set("border-radius", "10px")
                .set("padding", "0.55rem 0.85rem")
                .set("box-sizing", "border-box");
        return card;
    }
}
