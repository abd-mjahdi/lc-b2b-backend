package com.lesieurcristal.b2bportal.claim.service;

import com.lesieurcristal.b2bportal.claim.dto.CreateReclamationRequest;
import com.lesieurcristal.b2bportal.claim.dto.ReclamationResponseDto;
import com.lesieurcristal.b2bportal.entity.app.Reclamation;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.notification.service.PortalNotificationService;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.repository.ReclamationRepository;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReclamationService {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "new", "in_progress", "resolved", "rejected"
    );

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
            "new", Set.of("in_progress", "rejected"),
            "in_progress", Set.of("resolved", "rejected"),
            "resolved", Set.of(),
            "rejected", Set.of()
    );

    private final ReclamationRepository reclamationRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final PortalNotificationService portalNotificationService;
    private final ObjectStorage objectStorage;
    private final UploadValidator uploadValidator;

    @Transactional
    public ReclamationResponseDto create(CreateReclamationRequest dto, MultipartFile file) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        if (current.getCustomerNumber() == null) {
            throw new SecurityException("Seuls les clients peuvent ouvrir une réclamation.");
        }
        Customer customer = customerRepository.findById(current.getCustomerNumber())
                .orElseThrow(() -> new EntityNotFoundException("Client inconnu"));
        User user = userRepository.findById(current.getId()).orElse(null);

        Optional<UploadValidator.ValidatedUpload> upload = uploadValidator.validateIfPresent(file);

        Reclamation saved = reclamationRepository.save(Reclamation.builder()
                .customer(customer)
                .user(user)
                .lotNumber(dto.lotNumber())
                .description(dto.description())
                .status("new")
                .build());

        String uploadedKey = null;
        try {
            if (upload.isPresent()) {
                UploadValidator.ValidatedUpload validated = upload.get();
                uploadedKey = ObjectKeys.claim(
                        customer.getCustomerNumber(), saved.getId(), validated.extension());
                objectStorage.put(uploadedKey, validated.bytes(), validated.contentType());
                saved.setAttachmentPath(uploadedKey);
                saved = reclamationRepository.save(saved);
            }

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
        } catch (RuntimeException e) {
            if (uploadedKey != null) {
                try {
                    objectStorage.delete(uploadedKey);
                } catch (RuntimeException ignored) {
                    log.warn("Nettoyage S3 échoué après échec réclamation {}", saved.getId());
                }
            }
            throw e;
        }
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

    @Transactional(readOnly = true)
    public AttachmentFile downloadForCurrentUser(Long id) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        Reclamation r = reclamationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Réclamation introuvable"));
        if (current.getCustomerNumber() == null
                || !r.getCustomer().getCustomerNumber().equals(current.getCustomerNumber())) {
            throw new SecurityException("Accès refusé");
        }
        return readAttachment(r);
    }

    @Transactional(readOnly = true)
    public AttachmentFile downloadForAdmin(Long id) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        if (current.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Accès refusé");
        }
        Reclamation r = reclamationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Réclamation introuvable"));
        return readAttachment(r);
    }

    @Transactional
    public ReclamationResponseDto updateStatus(Long id, String status) {
        if (status == null || !ALLOWED_STATUSES.contains(status)) {
            throw new IllegalArgumentException(
                    "Statut invalide. Valeurs autorisées : " + ALLOWED_STATUSES);
        }

        Reclamation r = reclamationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Réclamation introuvable"));

        String current = r.getStatus() != null ? r.getStatus() : "new";
        Set<String> allowedNext = ALLOWED_TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowedNext.contains(status)) {
            throw new IllegalStateException(
                    "Transition interdite : « " + current + " » → « " + status + " ».");
        }

        r.setStatus(status);
        Reclamation saved = reclamationRepository.save(r);

        notifyClaimStatus(saved, status);
        return ReclamationResponseDto.from(saved);
    }

    private AttachmentFile readAttachment(Reclamation r) {
        if (!ObjectKeys.isStoredObjectKey(r.getAttachmentPath())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce jointe introuvable");
        }
        byte[] content = objectStorage.get(r.getAttachmentPath())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pièce jointe introuvable"));
        return new AttachmentFile(
                content,
                ObjectKeys.contentTypeOf(r.getAttachmentPath()),
                ObjectKeys.downloadFilename("reclamation", r.getId(), r.getAttachmentPath()));
    }

    private void notifyClaimStatus(Reclamation r, String status) {
        if (r.getUser() != null) {
            portalNotificationService.createNotificationForUser(
                    r.getUser(),
                    "Mise à jour de votre réclamation",
                    String.format("Votre réclamation (lot %s) est désormais : %s.",
                            r.getLotNumber(), translateStatus(status)),
                    "CLAIM_STATUS_CHANGED",
                    "RECLAMATION",
                    String.valueOf(r.getId()),
                    "/dashboard/demandes/reclamation"
            );
            return;
        }
        if (r.getCustomer() == null) {
            return;
        }
        for (User recipient : userRepository.findByCustomer_CustomerNumber(
                r.getCustomer().getCustomerNumber())) {
            portalNotificationService.createNotificationForUser(
                    recipient,
                    "Mise à jour de votre réclamation",
                    String.format("Votre réclamation (lot %s) est désormais : %s.",
                            r.getLotNumber(), translateStatus(status)),
                    "CLAIM_STATUS_CHANGED",
                    "RECLAMATION",
                    String.valueOf(r.getId()),
                    "/dashboard/demandes/reclamation"
            );
        }
    }

    private static String translateStatus(String s) {
        if (s == null) return "";
        return switch (s) {
            case "new" -> "Nouveau";
            case "in_progress" -> "En cours";
            case "resolved" -> "Résolu";
            case "rejected" -> "Rejeté";
            default -> s;
        };
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }

    public record AttachmentFile(byte[] content, String contentType, String filename) {
    }
}
