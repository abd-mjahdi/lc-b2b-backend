package com.lesieurcristal.b2bportal.invoice.service;

import com.lesieurcristal.b2bportal.entity.app.Document;
import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.invoice.pdf.InvoicePdfTemplate;
import com.lesieurcristal.b2bportal.repository.DocumentRepository;
import com.lesieurcristal.b2bportal.repository.InvoiceRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.SecurityUtils;
import com.lesieurcristal.b2bportal.storage.ObjectKeys;
import com.lesieurcristal.b2bportal.storage.ObjectStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * Génère / réutilise le PDF facture (PRD — téléchargement).
 * Principe : vérifier l'objet MinIO/S3 d'abord, générer une seule fois.
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
    private final ObjectStorage objectStorage;

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
            if ("ready".equals(doc.getStatus()) && ObjectKeys.isStoredObjectKey(doc.getFilePath())) {
                Optional<byte[]> cached = objectStorage.get(doc.getFilePath());
                if (cached.isPresent()) {
                    return new InvoiceFile(invoiceNumber, cached.get());
                }
                log.info("Document {} référencé mais objet manquant — régénération", invoiceNumber);
            }
        }

        if (ownerCustomerNumber == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Facture introuvable");
        }

        byte[] pdf = invoicePdfTemplate.generate(invoice);
        String objectKey = ObjectKeys.invoice(ownerCustomerNumber, invoiceNumber);
        objectStorage.put(objectKey, pdf, "application/pdf");

        Document saved = persistDocument(invoice, existing.orElse(null), objectKey);
        log.info("PDF facture {} prêt (document id={}, key={})",
                invoiceNumber, saved.getId(), saved.getFilePath());

        return new InvoiceFile(invoiceNumber, pdf);
    }

    private Document persistDocument(Invoice invoice, Document existing, String objectKey) {
        Document doc = existing != null ? existing : Document.builder()
                .customer(invoice.getCustomer())
                .docType(DOC_TYPE_INVOICE)
                .naturalKey(invoice.getInvoiceNumber())
                .build();

        doc.setTitle("Facture " + invoice.getInvoiceNumber());
        doc.setStatus("ready");
        doc.setFilePath(objectKey);
        doc.setRelatedReference(invoice.getOrder() != null ? invoice.getOrder().getOrderNumber() : null);

        try {
            return documentRepository.save(doc);
        } catch (DataIntegrityViolationException race) {
            log.warn("Concurrence sur document invoice {}: réutilisation de l'existant", invoice.getInvoiceNumber());
            Document winner = documentRepository
                    .findByDocTypeAndNaturalKey(DOC_TYPE_INVOICE, invoice.getInvoiceNumber())
                    .orElseThrow(() -> race);
            assertDocumentBelongsToCustomer(winner, invoice.getCustomer().getCustomerNumber());
            if (!"ready".equals(winner.getStatus())
                    || !ObjectKeys.isStoredObjectKey(winner.getFilePath())) {
                winner.setTitle(doc.getTitle());
                winner.setStatus("ready");
                winner.setFilePath(objectKey);
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

    public record InvoiceFile(String invoiceNumber, byte[] content) {
        public String filename() {
            return "facture-" + invoiceNumber + ".pdf";
        }
    }
}
