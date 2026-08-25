package com.lesieurcristal.b2bportal.dispute.service;

import com.lesieurcristal.b2bportal.dispute.dto.CreateInvoiceDisputeRequest;
import com.lesieurcristal.b2bportal.dispute.dto.InvoiceDisputeResponseDto;
import com.lesieurcristal.b2bportal.entity.app.InvoiceDispute;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.notification.service.PortalNotificationService;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.repository.InvoiceDisputeRepository;
import com.lesieurcristal.b2bportal.repository.InvoiceRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.SecurityUtils;
import com.lesieurcristal.b2bportal.storage.ObjectKeys;
import com.lesieurcristal.b2bportal.storage.ObjectStorage;
import com.lesieurcristal.b2bportal.storage.UploadValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeService {

    private static final Set<String> RESTORABLE_STATUSES = Set.of(
            "paid", "unpaid", "partially_paid"
    );

    private final InvoiceDisputeRepository disputeRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PortalNotificationService portalNotificationService;
    private final ObjectStorage objectStorage;
    private final UploadValidator uploadValidator;

    @Transactional
    public InvoiceDisputeResponseDto createDispute(String invoiceNumber,
                                                   CreateInvoiceDisputeRequest dto,
                                                   MultipartFile file) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        if (current.getCustomerNumber() == null) {
            throw new SecurityException("Seuls les clients peuvent contester une facture");
        }

        Optional<UploadValidator.ValidatedUpload> upload = uploadValidator.validateIfPresent(file);

        Invoice invoice = invoiceRepository.findById(invoiceNumber)
                .orElseThrow(() -> new EntityNotFoundException("Facture inconnue : " + invoiceNumber));

        if (!invoice.getCustomer().getCustomerNumber().equals(current.getCustomerNumber())) {
            throw new SecurityException("Cette facture n'appartient pas à votre compte.");
        }

        if ("disputed".equals(invoice.getInvoiceStatus())) {
            throw new IllegalStateException("Cette facture est déjà contestée.");
        }

        Customer customer = customerRepository.findById(current.getCustomerNumber())
                .orElseThrow(() -> new EntityNotFoundException("Client inconnu"));
        User user = userRepository.findById(current.getId()).orElse(null);

        String previousStatus = invoice.getInvoiceStatus() != null
                ? invoice.getInvoiceStatus()
                : "unpaid";

        InvoiceDispute saved = disputeRepository.save(InvoiceDispute.builder()
                .invoiceNumber(invoiceNumber)
                .customer(customer)
                .user(user)
                .reason(dto.reason())
                .description(dto.description())
                .status(InvoiceDispute.DisputeStatus.PENDING)
                .previousInvoiceStatus(previousStatus)
                .build());

        String uploadedKey = null;
        try {
            if (upload.isPresent()) {
                UploadValidator.ValidatedUpload validated = upload.get();
                uploadedKey = ObjectKeys.dispute(
                        customer.getCustomerNumber(), saved.getId(), validated.extension());
                objectStorage.put(uploadedKey, validated.bytes(), validated.contentType());
                saved.setFilePath(uploadedKey);
                saved = disputeRepository.save(saved);
            }

            invoice.setInvoiceStatus("disputed");
            invoiceRepository.save(invoice);

            String reasonLabel = translateReason(dto.reason());
            portalNotificationService.createAdminBroadcast(
                    "Contestation de facture reçue",
                    String.format(
                            "Facture #%s contestée par %s (Client #%s). Motif : %s.",
                            invoiceNumber, customer.getCompanyName(),
                            customer.getCustomerNumber(), reasonLabel),
                    "DISPUTE_OPENED",
                    "INVOICE_DISPUTE",
                    saved.getId().toString(),
                    "/admin/disputes"
            );
            return InvoiceDisputeResponseDto.from(saved);
        } catch (RuntimeException e) {
            deleteQuietly(uploadedKey, "contestation", saved.getId());
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public List<InvoiceDisputeResponseDto> listForCurrentUser() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        if (current.getCustomerNumber() == null || current.getCustomerNumber().isBlank()) {
            return List.of();
        }
        return disputeRepository
                .findByCustomer_CustomerNumberOrderByCreatedAtDesc(current.getCustomerNumber())
                .stream()
                .map(InvoiceDisputeResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceDisputeResponseDto getByIdForCurrentUser(Long id) {
        return InvoiceDisputeResponseDto.from(requireOwnedDispute(id));
    }

    @Transactional(readOnly = true)
    public AttachmentFile downloadForCurrentUser(Long id) {
        InvoiceDispute d = requireOwnedDispute(id);
        return readAttachment(d);
    }

    @Transactional(readOnly = true)
    public AttachmentFile downloadForAdmin(Long id) {
        requireAdmin();
        InvoiceDispute d = disputeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contestation introuvable"));
        return readAttachment(d);
    }

    @Transactional(readOnly = true)
    public List<InvoiceDisputeResponseDto> listPendingForAdmin() {
        return disputeRepository
                .findByStatusOrderByCreatedAtDesc(InvoiceDispute.DisputeStatus.PENDING)
                .stream()
                .map(InvoiceDisputeResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceDisputeResponseDto> listAllForAdmin() {
        return disputeRepository.findAll().stream()
                .map(InvoiceDisputeResponseDto::from)
                .toList();
    }

    @Transactional
    public InvoiceDisputeResponseDto approveDispute(Long id, String note) {
        InvoiceDispute d = requirePendingDispute(id);
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));

        d.setStatus(InvoiceDispute.DisputeStatus.APPROVED);
        d.setResolutionNote(note);
        d.setResolvedBy(userRepository.findById(current.getId()).orElse(null));
        d.setResolvedAt(OffsetDateTime.now());
        InvoiceDispute saved = disputeRepository.save(d);

        restoreInvoiceStatus(d);

        if (d.getUser() != null) {
            portalNotificationService.createNotificationForUser(
                    d.getUser(),
                    "Votre contestation a été acceptée",
                    "Votre contestation de la facture " + d.getInvoiceNumber()
                            + " a été acceptée. Le statut de paiement de la facture a été restauré.",
                    "DISPUTE_RESOLVED",
                    "INVOICE_DISPUTE",
                    String.valueOf(saved.getId()),
                    "/dashboard/factures"
            );
        }
        return InvoiceDisputeResponseDto.from(saved);
    }

    @Transactional
    public InvoiceDisputeResponseDto rejectDispute(Long id, String note) {
        InvoiceDispute d = requirePendingDispute(id);
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));

        d.setStatus(InvoiceDispute.DisputeStatus.REJECTED);
        d.setResolutionNote(note);
        d.setResolvedBy(userRepository.findById(current.getId()).orElse(null));
        d.setResolvedAt(OffsetDateTime.now());
        InvoiceDispute saved = disputeRepository.save(d);

        restoreInvoiceStatus(d);

        if (d.getUser() != null) {
            String msg = "Votre contestation de la facture " + d.getInvoiceNumber()
                    + " a été refusée.";
            if (note != null && !note.isBlank()) {
                msg += " Motif : " + note;
            }
            portalNotificationService.createNotificationForUser(
                    d.getUser(),
                    "Contestation refusée",
                    msg,
                    "DISPUTE_RESOLVED",
                    "INVOICE_DISPUTE",
                    String.valueOf(saved.getId()),
                    "/dashboard/factures"
            );
        }
        return InvoiceDisputeResponseDto.from(saved);
    }

    private InvoiceDispute requireOwnedDispute(Long id) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        InvoiceDispute d = disputeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contestation introuvable"));
        String customerNumber = current.getCustomerNumber();
        if (customerNumber == null || customerNumber.isBlank()
                || d.getCustomer() == null
                || !customerNumber.equals(d.getCustomer().getCustomerNumber())) {
            throw new SecurityException("Accès refusé");
        }
        return d;
    }

    private InvoiceDispute requirePendingDispute(Long id) {
        InvoiceDispute d = disputeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contestation introuvable"));
        if (d.getStatus() != InvoiceDispute.DisputeStatus.PENDING) {
            throw new IllegalStateException(
                    "Seules les contestations en attente (PENDING) peuvent être tranchées. Statut actuel : "
                            + d.getStatus());
        }
        return d;
    }

    private AttachmentFile readAttachment(InvoiceDispute d) {
        if (!ObjectKeys.isStoredObjectKey(d.getFilePath())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce jointe introuvable");
        }
        byte[] content = objectStorage.get(d.getFilePath())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce jointe introuvable"));
        return new AttachmentFile(
                content,
                ObjectKeys.contentTypeOf(d.getFilePath()),
                ObjectKeys.downloadFilename("justificatif", d.getId(), d.getFilePath()));
    }

    private static void requireAdmin() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        if (current.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Accès refusé");
        }
    }

    private void restoreInvoiceStatus(InvoiceDispute d) {
        invoiceRepository.findById(d.getInvoiceNumber()).ifPresent(inv -> {
            String restored = d.getPreviousInvoiceStatus();
            if (restored == null || restored.isBlank() || !RESTORABLE_STATUSES.contains(restored)) {
                restored = inv.getPaymentDate() != null ? "paid" : "unpaid";
            }
            inv.setInvoiceStatus(restored);
            invoiceRepository.save(inv);
            log.info("Facture {} restaurée à « {} » après résolution contestation {}",
                    inv.getInvoiceNumber(), restored, d.getId());
        });
    }

    private void deleteQuietly(String key, String kind, Long id) {
        if (key == null) {
            return;
        }
        try {
            objectStorage.delete(key);
        } catch (RuntimeException ignored) {
            log.warn("Nettoyage S3 échoué après échec {} {}", kind, id);
        }
    }

    private static String translateReason(InvoiceDispute.DisputeReason r) {
        if (r == null) return "";
        return switch (r) {
            case QUANTITY_DISCREPANCY -> "Écart de quantité";
            case PRICE_DISCREPANCY -> "Écart de prix";
            case DAMAGED_GOODS -> "Produit endommagé";
            case OTHER -> "Autre motif";
        };
    }

    public record AttachmentFile(byte[] content, String contentType, String filename) {
    }
}
