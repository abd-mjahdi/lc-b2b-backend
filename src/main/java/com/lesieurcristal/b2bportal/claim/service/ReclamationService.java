package com.lesieurcristal.b2bportal.claim.service;

import com.lesieurcristal.b2bportal.claim.dto.CreateReclamationRequest;
import com.lesieurcristal.b2bportal.claim.dto.ReclamationResponseDto;
import com.lesieurcristal.b2bportal.entity.app.Reclamation;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.notification.service.PortalNotificationService;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.repository.ReclamationRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReclamationService {

    private final ReclamationRepository reclamationRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PortalNotificationService portalNotificationService;

    @Transactional
    public ReclamationResponseDto create(CreateReclamationRequest dto) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        if (current.getCustomerNumber() == null) {
            throw new SecurityException("Seuls les clients peuvent ouvrir une réclamation.");
        }
        Customer customer = customerRepository.findById(current.getCustomerNumber())
                .orElseThrow(() -> new EntityNotFoundException("Client inconnu"));
        User user = userRepository.findById(current.getId()).orElse(null);

        Reclamation saved = reclamationRepository.save(Reclamation.builder()
                .customer(customer)
                .user(user)
                .lotNumber(dto.lotNumber())
                .description(dto.description())
                .attachmentPath(dto.attachmentPath())
                .status("new")
                .build());

        portalNotificationService.createAdminBroadcast(
                "Nouvelle réclamation client",
                String.format("Réclamation ouverte par %s (Client #%s). Lot : %s. Détail : %s",
                        customer.getCompanyName(),
                        customer.getCustomerNumber(),
                        dto.lotNumber(),
                        truncate(dto.description(), 120)),
                "CLAIM_OPENED",
                "RECLAMATION",
                saved.getId().toString(),
                "/admin/claims"
        );
        return ReclamationResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ReclamationResponseDto> listForCurrentUser() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        if (current.getCustomerNumber() == null) {
            return List.of();
        }
        return reclamationRepository
                .findByCustomer_CustomerNumberOrderByReceivedAtDesc(current.getCustomerNumber())
                .stream()
                .map(ReclamationResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReclamationResponseDto> listAllForAdmin() {
        return reclamationRepository.findAll().stream()
                .map(ReclamationResponseDto::from)
                .toList();
    }

    @Transactional
    public ReclamationResponseDto updateStatus(Long id, String status) {
        Reclamation r = reclamationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Réclamation introuvable"));
        r.setStatus(status);
        Reclamation saved = reclamationRepository.save(r);

        if (r.getUser() != null) {
            portalNotificationService.createNotificationForUser(
                    r.getUser(),
                    "Mise à jour de votre réclamation",
                    String.format("Votre réclamation (lot %s) est désormais : %s.",
                            r.getLotNumber(), status),
                    "CLAIM_STATUS_CHANGED",
                    "RECLAMATION",
                    String.valueOf(saved.getId()),
                    "/client/claims"
            );
        }
        return ReclamationResponseDto.from(saved);
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
