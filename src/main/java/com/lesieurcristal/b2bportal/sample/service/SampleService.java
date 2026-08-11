package com.lesieurcristal.b2bportal.sample.service;

import com.lesieurcristal.b2bportal.entity.app.Product;
import com.lesieurcristal.b2bportal.entity.app.SampleRequest;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.entity.erpmock.Order;
import com.lesieurcristal.b2bportal.notification.dto.NotificationDto;
import com.lesieurcristal.b2bportal.notification.service.PortalNotificationService;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.repository.OrderRepository;
import com.lesieurcristal.b2bportal.repository.ProductRepository;
import com.lesieurcristal.b2bportal.repository.SampleRequestRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.sample.dto.CreateSampleRequestDto;
import com.lesieurcristal.b2bportal.sample.dto.SampleRequestResponseDto;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Service métier des demandes d'échantillons (PRD §2.1, §4.2).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SampleService {

    private final SampleRequestRepository sampleRequestRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PortalNotificationService portalNotificationService;

    // =========================================================================
    // Création
    // =========================================================================

    /**
     * Crée une demande d'échantillon isolée (non rattachée à une commande).
     * Notifie les admins avec un contexte enrichi.
     */
    @Transactional
    public SampleRequestResponseDto createSampleRequest(CreateSampleRequestDto dto) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        if (current.getCustomerNumber() == null) {
            throw new SecurityException("Seuls les clients peuvent demander un échantillon");
        }

        Product product = productRepository.findById(dto.productCode())
                .orElseThrow(() -> new EntityNotFoundException("Produit inconnu : " + dto.productCode()));

        if (!Boolean.TRUE.equals(product.getIsSampleable())) {
            throw new IllegalArgumentException("Ce produit n'est pas éligible aux échantillons.");
        }
        if (product.getMaxSampleQuantity() != null
                && dto.quantity().compareTo(product.getMaxSampleQuantity()) > 0) {
            throw new IllegalArgumentException(
                    "Quantité supérieure au maximum autorisé ("
                            + product.getMaxSampleQuantity() + ").");
        }

        Customer customer = customerRepository.findById(current.getCustomerNumber())
                .orElseThrow(() -> new EntityNotFoundException("Client inconnu"));

        User user = userRepository.findById(current.getId()).orElse(null);

        SampleRequest saved = sampleRequestRepository.save(SampleRequest.builder()
                .customer(customer)
                .user(user)
                .product(product)
                .quantity(dto.quantity())
                .contactName(dto.contactName())
                .contactAddress(dto.contactAddress())
                .status("new")
                .build());

        // Notification admin : contexte client enrichi (PRD §2.2)
        portalNotificationService.createAdminBroadcast(
                "Nouvelle demande d'échantillon",
                String.format("Demande d'échantillon soumise par %s (%s). Produit : %s. Contact : %s.",
                        customer.getCompanyName(),
                        customer.getCustomerNumber(),
                        product.getName(),
                        dto.contactName()),
                "SAMPLE_REQUESTED",
                "SAMPLE_REQUEST",
                saved.getId().toString(),
                "/admin/samples"
        );

        return SampleRequestResponseDto.from(saved);
    }

    /**
     * Crée un échantillon rattaché à une commande existante (PRD §2.1 — économie
     * de transport). Utilisé par {@link com.lesieurcristal.b2bportal.order.service.OrderService}
     * lors de la soumission d'une commande avec {@code sampleProductCodes}.
     */
    @Transactional
    public SampleRequest createLinkedSampleRequest(
            Customer customer,
            User user,
            Product product,
            BigDecimal quantity,
            String orderNumber) {

        SampleRequest saved = sampleRequestRepository.save(SampleRequest.builder()
                .customer(customer)
                .user(user)
                .product(product)
                .quantity(quantity)
                .contactName(user != null
                        ? user.getFirstName() + " " + user.getLastName()
                        : customer.getCompanyName())
                .contactAddress(customer.getPostalAddress() != null
                        ? customer.getPostalAddress() + ", " + customer.getCity()
                        : customer.getCity())
                .linkedOrderNumber(orderNumber)
                .status("new")
                .build());
        log.info("Sample lié à la commande {} pour produit {}", orderNumber, product.getCode());
        return saved;
    }

    // =========================================================================
    // Lecture
    // =========================================================================

    @Transactional(readOnly = true)
    public List<SampleRequestResponseDto> getSamplesForCurrentUser() {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        // Isolation : un client ne voit que ses échantillons
        return sampleRequestRepository
                .findByCustomer_CustomerNumberOrderByRequestedAtDesc(current.getCustomerNumber())
                .stream()
                .map(SampleRequestResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SampleRequestResponseDto> getAllSamples() {
        // Admin only - l'accès est déjà filtré par SecurityConfig
        return sampleRequestRepository.findAll().stream()
                .map(SampleRequestResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SampleRequestResponseDto getById(Long id) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        SampleRequest sr = sampleRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        // Isolation
        if (current.getRole() != null && current.getRole().name().equals("CLIENT")) {
            if (!sr.getCustomer().getCustomerNumber().equals(current.getCustomerNumber())) {
                throw new SecurityException("Accès refusé");
            }
        }
        return SampleRequestResponseDto.from(sr);
    }

    // =========================================================================
    // Mutations admin
    // =========================================================================

    /**
     * Met à jour le statut (ex : processing, fulfilled, rejected) et
     * notifie le client ayant fait la demande.
     */
    @Transactional
    public SampleRequestResponseDto updateStatus(Long id, String newStatus) {
        SampleRequest sr = sampleRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        String oldStatus = sr.getStatus();
        sr.setStatus(newStatus);
        SampleRequest saved = sampleRequestRepository.save(sr);

        // Notification retour au client (PRD §2.2 flux retour)
        NotificationDto dto = portalNotificationService.createNotificationForUser(
                sr.getUser(),
                "Mise à jour de votre demande d'échantillon",
                String.format("Votre demande d'échantillon est passée de « %s » à « %s ».",
                        translateStatus(oldStatus), translateStatus(newStatus)),
                "SAMPLE_STATUS_CHANGED",
                "SAMPLE_REQUEST",
                String.valueOf(saved.getId()),
                "/client/samples"
        );

        return SampleRequestResponseDto.from(saved);
    }

    /**
     * Lie une demande d'échantillon au numéro de commande ERP finale
     * ({@code resulting_order_number}). Permet la mesure de conversion.
     */
    @Transactional
    public SampleRequestResponseDto linkToOrder(Long id, String orderNumber) {
        SampleRequest sr = sampleRequestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));
        Order order = orderRepository.findById(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Commande inconnue : " + orderNumber));
        sr.setResultingOrder(order);
        SampleRequest saved = sampleRequestRepository.save(sr);

        // Notification client : sa conversion a été bouclée
        if (sr.getUser() != null) {
            portalNotificationService.createNotificationForUser(
                    sr.getUser(),
                    "Commande issue de votre échantillon",
                    String.format("Votre commande %s a été liée à votre demande d'échantillon.", orderNumber),
                    "SAMPLE_CONVERTED_TO_ORDER",
                    "SAMPLE_REQUEST",
                    String.valueOf(saved.getId()),
                    "/client/orders"
            );
        }
        return SampleRequestResponseDto.from(saved);
    }

    private static String translateStatus(String s) {
        if (s == null) return "";
        return switch (s) {
            case "new" -> "Nouveau";
            case "processing" -> "En préparation";
            case "fulfilled" -> "Expédié";
            case "rejected" -> "Rejeté";
            default -> s;
        };
    }
}