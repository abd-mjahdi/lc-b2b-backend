package com.lesieurcristal.b2bportal.invoice.service;

import com.lesieurcristal.b2bportal.entity.app.Document;
import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.invoice.pdf.InvoicePdfTemplate;
import com.lesieurcristal.b2bportal.repository.DocumentRepository;
import com.lesieurcristal.b2bportal.repository.InvoiceRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Génère / réutilise le PDF facture (PRD — téléchargement).
 * Principe : vérifier {@code app.documents} d'abord, générer une seule fois.
 * Clients : isolés à leur {@code customerNumber}. Admins : toute facture.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceDocumentService {

    public static final String DOC_TYPE_INVOICE = "invoice";

    private final InvoiceRepository invoiceRepository;
    private final DocumentRepository documentRepository;
    private final InvoicePdfTemplate invoicePdfTemplate;

    @Value("${app.documents.storage-dir:./storage/documents}")
    private String storageDir;

    @Transactional
    public InvoiceFile downloadInvoicePdf(String invoiceNumber) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Non authentifié"));

        Invoice invoice = invoiceRepository.findByInvoiceNumberWithOrder(invoiceNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable"));

        boolean admin = current.getRole() == UserRole.ADMIN;
        if (!admin) {
            if (current.getCustomerNumber() == null || current.getCustomerNumber().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Aucun numéro client associé");
            }
            if (invoice.getCustomer() == null
                    || !current.getCustomerNumber().equals(invoice.getCustomer().getCustomerNumber())) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable");
            }
        }

        String ownerCustomerNumber = invoice.getCustomer() != null
                ? invoice.getCustomer().getCustomerNumber()
                : null;

        Optional<Document> existing = documentRepository.findByDocTypeAndNaturalKey(DOC_TYPE_INVOICE, invoiceNumber);
        if (existing.isPresent()) {
            Document doc = existing.get();
            if (!admin) {
                assertDocumentBelongsToCustomer(doc, current.getCustomerNumber());
            } else if (ownerCustomerNumber != null) {
                assertDocumentBelongsToCustomer(doc, ownerCustomerNumber);
            }
            if (doc.getFilePath() != null && "ready".equals(doc.getStatus())) {
                Path path = resolveStoragePath(doc.getFilePath());
                if (Files.isRegularFile(path)) {
                    return new InvoiceFile(invoiceNumber, readBytes(path));
                }
                log.info("Document {} référencé mais fichier manquant — régénération", invoiceNumber);
            }
        }

        byte[] pdf = invoicePdfTemplate.generate(invoice);
        String logicalPath = logicalInvoicePath(invoiceNumber);
        Path physicalPath = resolveStoragePath(logicalPath);
        writeBytes(physicalPath, pdf);

        Document saved = persistDocument(invoice, existing.orElse(null), logicalPath);
        log.info("PDF facture {} prêt (document id={}, path={})",
                invoiceNumber, saved.getId(), saved.getFilePath());

        return new InvoiceFile(invoiceNumber, pdf);
    }

    private Document persistDocument(Invoice invoice, Document existing, String logicalPath) {
        Document doc = existing != null ? existing : Document.builder()
                .customer(invoice.getCustomer())
                .docType(DOC_TYPE_INVOICE)
                .naturalKey(invoice.getInvoiceNumber())
                .build();

        doc.setTitle("Facture " + invoice.getInvoiceNumber());
        doc.setStatus("ready");
        doc.setFilePath(logicalPath);
        doc.setRelatedReference(invoice.getOrder() != null ? invoice.getOrder().getOrderNumber() : null);

        try {
            return documentRepository.save(doc);
        } catch (DataIntegrityViolationException race) {
            log.warn("Concurrence sur document invoice {}: réutilisation de l'existant", invoice.getInvoiceNumber());
            Document winner = documentRepository
                    .findByDocTypeAndNaturalKey(DOC_TYPE_INVOICE, invoice.getInvoiceNumber())
                    .orElseThrow(() -> race);
            assertDocumentBelongsToCustomer(winner, invoice.getCustomer().getCustomerNumber());
            if (winner.getFilePath() == null || !"ready".equals(winner.getStatus())) {
                winner.setTitle(doc.getTitle());
                winner.setStatus("ready");
                winner.setFilePath(logicalPath);
                winner.setRelatedReference(doc.getRelatedReference());
                return documentRepository.save(winner);
            }
            return winner;
        }
    }

    private void assertDocumentBelongsToCustomer(Document doc, String customerNumber) {
        if (doc.getCustomer() == null
                || !customerNumber.equals(doc.getCustomer().getCustomerNumber())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable");
        }
    }

    private Path resolveStoragePath(String filePath) {
        String relative = filePath;
        if (relative.startsWith("/documents/")) {
            relative = relative.substring("/documents/".length());
        } else if (relative.startsWith("documents/")) {
            relative = relative.substring("documents/".length());
        } else if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        Path root = Path.of(storageDir).toAbsolutePath().normalize();
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Chemin document hors du répertoire de stockage");
        }
        return resolved;
    }

    private static String logicalInvoicePath(String invoiceNumber) {
        return "/documents/invoices/" + invoiceNumber + ".pdf";
    }

    private static byte[] readBytes(Path path) {
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de lire le PDF : " + path, e);
        }
    }

    private static void writeBytes(Path path, byte[] bytes) {
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible d'écrire le PDF : " + path, e);
        }
    }

    public record InvoiceFile(String invoiceNumber, byte[] content) {
        public String filename() {
            return "facture-" + invoiceNumber + ".pdf";
        }
    }
}
