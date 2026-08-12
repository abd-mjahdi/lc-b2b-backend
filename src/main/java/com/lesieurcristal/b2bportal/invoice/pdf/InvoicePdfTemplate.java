package com.lesieurcristal.b2bportal.invoice.pdf;

import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.entity.erpmock.Order;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/**
 * Template PDF basique d'une facture Lesieur Cristal (OpenPDF).
 * Produit un flux binaire prêt à être stocké dans {@code app.documents}.
 */
@Component
public class InvoicePdfTemplate {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Locale FR_MA = Locale.forLanguageTag("fr-MA");

    private static final Color BRAND_RED = new Color(0xC4, 0x10, 0x2E);
    private static final Color HEADER_BG = new Color(0xF5, 0xF0, 0xEB);
    private static final Color LINE_BG = new Color(0xFA, 0xFA, 0xFA);

    /**
     * Génère le PDF d'une facture à partir des entités ERP mock.
     *
     * @return contenu PDF (bytes), jamais {@code null}
     */
    public byte[] generate(Invoice invoice) {
        Objects.requireNonNull(invoice, "invoice");
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 48, 48, 48, 48);
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BRAND_RED);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, Color.DARK_GRAY);
            Font labelFont = FontFactory.getFont(FontFactory.HELVETICA, 9, Color.GRAY);
            Font valueFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);

            // En-tête émetteur
            Paragraph brand = new Paragraph("LESIEUR CRISTAL", titleFont);
            brand.setSpacingAfter(2);
            document.add(brand);
            document.add(muted("Portail B2B — Facture client", labelFont));
            document.add(spacer(12));

            // Bloc facture / client
            PdfPTable header = new PdfPTable(2);
            header.setWidthPercentage(100);
            header.setWidths(new float[]{1f, 1f});
            header.addCell(infoBlock("Facture", java.util.List.of(
                    "N° " + nullSafe(invoice.getInvoiceNumber()),
                    "Date : " + formatDate(invoice.getInvoiceDate()),
                    "Échéance : " + formatDate(invoice.getDueDate()),
                    "Statut : " + statusLabel(invoice.getInvoiceStatus())
            ), sectionFont, valueFont, HEADER_BG));
            header.addCell(infoBlock("Client", clientLines(invoice.getCustomer()), sectionFont, valueFont, HEADER_BG));
            document.add(header);
            document.add(spacer(16));

            // Ligne commande liée
            Order order = invoice.getOrder();
            document.add(new Paragraph("Détail", sectionFont));
            document.add(spacer(6));

            PdfPTable lines = new PdfPTable(5);
            lines.setWidthPercentage(100);
            lines.setWidths(new float[]{2.2f, 3.2f, 1.2f, 1.4f, 1.6f});
            addHeaderCell(lines, "Code");
            addHeaderCell(lines, "Désignation");
            addHeaderCell(lines, "Qté");
            addHeaderCell(lines, "Unité");
            addHeaderCell(lines, "Montant HT");

            if (order != null) {
                addBodyCell(lines, nullSafe(order.getProductCode()), valueFont);
                addBodyCell(lines, nullSafe(order.getProductLabel()), valueFont);
                addBodyCell(lines, formatQty(order.getQuantityOrdered()), valueFont);
                addBodyCell(lines, nullSafe(order.getSalesUnit()), valueFont);
                addBodyCell(lines, formatMoney(order.getNetAmount(), order.getCurrency()), valueFont);
            } else {
                addBodyCell(lines, "—", valueFont);
                addBodyCell(lines, "Commande non liée", valueFont);
                addBodyCell(lines, "—", valueFont);
                addBodyCell(lines, "—", valueFont);
                addBodyCell(lines, formatMoney(invoice.getNetAmount(), invoice.getCurrency()), valueFont);
            }
            document.add(lines);
            document.add(spacer(14));

            // Totaux
            PdfPTable totals = new PdfPTable(2);
            totals.setWidthPercentage(45);
            totals.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totals.setWidths(new float[]{1.4f, 1.2f});
            addTotalRow(totals, "Net HT", formatMoney(invoice.getNetAmount(), invoice.getCurrency()), labelFont, valueFont);
            addTotalRow(totals, "TVA", formatMoney(invoice.getVatAmount(), invoice.getCurrency()), labelFont, valueFont);
            addTotalRow(totals, "Total TTC", formatMoney(invoice.getTotalAmount(), invoice.getCurrency()), boldFont, boldFont);
            document.add(totals);

            if (invoice.getPaymentDate() != null) {
                document.add(spacer(12));
                document.add(muted("Payée le " + formatDate(invoice.getPaymentDate()), valueFont));
            }

            document.add(spacer(28));
            document.add(muted(
                    "Document généré automatiquement par le Portail B2B Lesieur Cristal. "
                            + "Référence commande : "
                            + (order != null ? order.getOrderNumber() : "n/a"),
                    labelFont));

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Échec de génération PDF pour la facture "
                    + invoice.getInvoiceNumber(), e);
        }
    }

    private static java.util.List<String> clientLines(Customer customer) {
        if (customer == null) {
            return java.util.List.of("Client inconnu");
        }
        java.util.List<String> lines = new java.util.ArrayList<>();
        lines.add(nullSafe(customer.getCompanyName()));
        lines.add("N° client : " + nullSafe(customer.getCustomerNumber()));
        if (customer.getPostalAddress() != null && !customer.getPostalAddress().isBlank()) {
            lines.add(customer.getPostalAddress());
        }
        String cityLine = joinNonBlank(customer.getCity(), customer.getCountry());
        if (!cityLine.isBlank()) {
            lines.add(cityLine);
        }
        if (customer.getVatId() != null && !customer.getVatId().isBlank()) {
            lines.add("ICE / TVA : " + customer.getVatId());
        }
        return lines;
    }

    private static PdfPCell infoBlock(String title, java.util.List<String> lines,
                                      Font titleFont, Font bodyFont, Color bg) {
        Paragraph p = new Paragraph();
        p.add(new Phrase(title + "\n", titleFont));
        for (String line : lines) {
            p.add(new Phrase(line + "\n", bodyFont));
        }
        PdfPCell cell = new PdfPCell(p);
        cell.setBackgroundColor(bg);
        cell.setPadding(10);
        cell.setBorderColor(new Color(0xE5, 0xE0, 0xDB));
        return cell;
    }

    private static void addHeaderCell(PdfPTable table, String text) {
        Font white = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
        PdfPCell cell = new PdfPCell(new Phrase(text, white));
        cell.setBackgroundColor(BRAND_RED);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
    }

    private static void addBodyCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setBackgroundColor(LINE_BG);
        cell.setBorderColor(new Color(0xE5, 0xE0, 0xDB));
        table.addCell(cell);
    }

    private static void addTotalRow(PdfPTable table, String label, String value, Font labelFont, Font valueFont) {
        PdfPCell l = new PdfPCell(new Phrase(label, labelFont));
        l.setBorder(0);
        l.setPadding(4);
        PdfPCell v = new PdfPCell(new Phrase(value, valueFont));
        v.setBorder(0);
        v.setPadding(4);
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(l);
        table.addCell(v);
    }

    private static Paragraph muted(String text, Font font) {
        return new Paragraph(text, font);
    }

    private static Paragraph spacer(float points) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingAfter(points);
        return p;
    }

    private static String formatDate(LocalDate date) {
        return date == null ? "—" : DATE_FMT.format(date);
    }

    private static String formatMoney(BigDecimal amount, String currency) {
        if (amount == null) {
            return "—";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(FR_MA);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        String cur = (currency == null || currency.isBlank()) ? "MAD" : currency;
        return nf.format(amount) + " " + cur;
    }

    private static String formatQty(BigDecimal qty) {
        if (qty == null) {
            return "—";
        }
        return qty.stripTrailingZeros().toPlainString();
    }

    private static String statusLabel(String status) {
        if (status == null) {
            return "—";
        }
        return switch (status) {
            case "paid" -> "Payée";
            case "unpaid" -> "À régler";
            case "partially_paid" -> "Partiellement payée";
            case "disputed" -> "Contestée";
            default -> status;
        };
    }

    private static String nullSafe(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    private static String joinNonBlank(String a, String b) {
        String left = a == null ? "" : a.trim();
        String right = b == null ? "" : b.trim();
        if (left.isEmpty()) {
            return right;
        }
        if (right.isEmpty()) {
            return left;
        }
        return left + ", " + right;
    }
}
