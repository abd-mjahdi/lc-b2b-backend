package com.lesieurcristal.b2bportal.dispute.service;

import com.lesieurcristal.b2bportal.dispute.dto.CreateInvoiceDisputeRequest;
import com.lesieurcristal.b2bportal.dispute.dto.InvoiceDisputeResponseDto;
import com.lesieurcristal.b2bportal.entity.app.InvoiceDispute;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.notification.service.PortalNotificationService;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.repository.InvoiceDisputeRepository;
import com.lesieurcristal.b2bportal.repository.InvoiceRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DisputeService {

    private final InvoiceDisputeRepository disputeRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PortalNotificationService portalNotificationService;

    // =========================================================================
    // Création client
    // =========================================================================

    @Transactional
    public InvoiceDisputeResponseDto createDispute(String invoiceNumber,
                                                   CreateInvoiceDisputeRequest dto,
                                                   String filePath) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        if (current.getCustomerNumber() == null) {
            throw new SecurityException("Seuls les clients peuvent contester une facture");
        }

        Invoice invoice = invoiceRepository.findById(invoiceNumber)
                .orElseThrow(() -> new EntityNotFoundException("Facture inconnue : " + invoiceNumber));

        // Isolation : la facture doit appartenir au client connecté
        if (!invoice.getCustomer().getCustomerNumber().equals(current.getCustomerNumber())) {
            throw new SecurityException("Cette facture n'appartient pas à votre compte.");
        }

        Customer customer = customerRepository.findById(current.getCustomerNumber())
                .orElseThrow(() -> new EntityNotFoundException("Client inconnu"));
        User user = userRepository.findById(current.getId()).orElse(null);

        InvoiceDispute saved = disputeRepository.save(InvoiceDispute.builder()
                .invoiceNumber(invoiceNumber)
                .customer(customer)
                .user(user)
                .reason(dto.reason())
                .description(dto.description())
                .filePath(filePath)
                .status(InvoiceDispute.DisputeStatus.PENDING)
                .build());

        // Marque la facture ERP comme contestée
        invoice.setInvoiceStatus("disputed");
        invoiceRepository.save(invoice);

        // Notification admin contextuelle (PRD §2.2)
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
                "/admin/invoices/disputes"
        );

        return InvoiceDisputeResponseDto.from(saved);
    }

    // =========================================================================
    // Lectures
    // =========================================================================

    @Transactional(readOnly = true)
    public List<InvoiceDisputeResponseDto> listForCurrentUser() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        return disputeRepository
                .findByCustomer_CustomerNumberOrderByCreatedAtDesc(current.getCustomerNumber())
                .stream()
                .map(InvoiceDisputeResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceDisputeResponseDto getByIdForCurrentUser(Long id) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        InvoiceDispute d = disputeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contestation introuvable"));
        if (current.getCustomerNumber() != null
                && !d.getCustomer().getCustomerNumber().equals(current.getCustomerNumber())) {
            throw new SecurityException("Accès refusé");
        }
        return InvoiceDisputeResponseDto.from(d);
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

    // =========================================================================
    // Arbitrage admin
    // =========================================================================

    @Transactional
    public InvoiceDisputeResponseDto approveDispute(Long id, String note) {
        InvoiceDispute d = disputeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contestation introuvable"));
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));

        d.setStatus(InvoiceDispute.DisputeStatus.APPROVED);
        d.setResolutionNote(note);
        d.setResolvedBy(userRepository.findById(current.getId()).orElse(null));
        d.setResolvedAt(OffsetDateTime.now());
        InvoiceDispute saved = disputeRepository.save(d);

        // La facture ERP est remise à "unpaid" après arbitrage (ou reste "disputed"
        // selon le contexte). Pour le MVP : on libère le statut.
        invoiceRepository.findById(d.getInvoiceNumber()).ifPresent(inv -> {
            inv.setInvoiceStatus("unpaid");
            invoiceRepository.save(inv);
        });

        if (d.getUser() != null) {
            portalNotificationService.createNotificationForUser(
                    d.getUser(),
                    "Votre contestation a été acceptée",
                    "La contestation de votre facture " + d.getInvoiceNumber()
                            + " a été acceptée. Un avoir sera émis sous peu.",
                    "DISPUTE_RESOLVED",
                    "INVOICE_DISPUTE",
                    String.valueOf(saved.getId()),
                    "/client/invoices"
            );
        }
        return InvoiceDisputeResponseDto.from(saved);
    }

    @Transactional
    public InvoiceDisputeResponseDto rejectDispute(Long id, String note) {
        InvoiceDispute d = disputeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Contestation introuvable"));
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));

        d.setStatus(InvoiceDispute.DisputeStatus.REJECTED);
        d.setResolutionNote(note);
        d.setResolvedBy(userRepository.findById(current.getId()).orElse(null));
        d.setResolvedAt(OffsetDateTime.now());
        InvoiceDispute saved = disputeRepository.save(d);

        // Facture revient à son statut antérieur (paid/unpaid) — on l'infère
        // depuis le payment_date : si payment_date IS NOT NULL => paid, sinon unpaid.
        invoiceRepository.findById(d.getInvoiceNumber()).ifPresent(inv -> {
            inv.setInvoiceStatus(inv.getPaymentDate() != null ? "paid" : "unpaid");
            invoiceRepository.save(inv);
        });

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
                    "/client/invoices"
            );
        }
        return InvoiceDisputeResponseDto.from(saved);
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
}