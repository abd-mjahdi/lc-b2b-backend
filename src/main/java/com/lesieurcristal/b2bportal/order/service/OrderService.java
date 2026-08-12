package com.lesieurcristal.b2bportal.order.service;

import com.lesieurcristal.b2bportal.entity.app.Product;
import com.lesieurcristal.b2bportal.entity.app.SampleRequest;
import com.lesieurcristal.b2bportal.entity.app.User;
import com.lesieurcristal.b2bportal.entity.app.enums.UserRole;
import com.lesieurcristal.b2bportal.entity.erpmock.Customer;
import com.lesieurcristal.b2bportal.entity.erpmock.Invoice;
import com.lesieurcristal.b2bportal.entity.erpmock.Order;
import com.lesieurcristal.b2bportal.entity.erpmock.OrderStatus;
import com.lesieurcristal.b2bportal.notification.service.PortalNotificationService;
import com.lesieurcristal.b2bportal.order.OrderException;
import com.lesieurcristal.b2bportal.order.connector.ErpOrderConnector;
import com.lesieurcristal.b2bportal.order.dto.CreateOrderRequestDto;
import com.lesieurcristal.b2bportal.order.dto.OrderDetailDto;
import com.lesieurcristal.b2bportal.order.dto.OrderResponseDto;
import com.lesieurcristal.b2bportal.order.dto.OrderStatusResponseDto;
import com.lesieurcristal.b2bportal.order.dto.OrderSubmissionResponseDto;
import com.lesieurcristal.b2bportal.order.dto.UpdateOrderStatusDto;
import com.lesieurcristal.b2bportal.repository.CustomerRepository;
import com.lesieurcristal.b2bportal.repository.OrderRepository;
import com.lesieurcristal.b2bportal.repository.OrderStatusRepository;
import com.lesieurcristal.b2bportal.repository.ProductRepository;
import com.lesieurcristal.b2bportal.repository.UserRepository;
import com.lesieurcristal.b2bportal.sample.service.SampleService;
import com.lesieurcristal.b2bportal.security.AuthenticatedUser;
import com.lesieurcristal.b2bportal.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final ErpOrderConnector erpOrderConnector;
    private final OrderRepository orderRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final SampleService sampleService;
    private final PortalNotificationService portalNotificationService;

    /**
     * Fetch order history for the currently logged-in customer.
     */
    public org.springframework.data.domain.Page<OrderResponseDto> getOrdersForCurrentUser(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String status,
            org.springframework.data.domain.Pageable pageable) {

        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(OrderException::accessDenied);

        String customerNumber = currentUser.getCustomerNumber();
        if (customerNumber == null || customerNumber.isBlank()) {
            throw OrderException.noCustomerAssociated();
        }

        return erpOrderConnector.getOrdersByCustomerNumber(customerNumber, startDate, endDate, status, pageable);
    }

    /**
     * Fetch order history for a specific customer (Admin use).
     */
    public org.springframework.data.domain.Page<OrderResponseDto> getOrdersForCustomer(
            String customerNumber,
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            String status,
            org.springframework.data.domain.Pageable pageable) {

        return erpOrderConnector.getOrdersByCustomerNumber(customerNumber, startDate, endDate, status, pageable);
    }

    /**
     * Fetch live status for a specific order. Enforces data isolation between clients.
     */
    public OrderStatusResponseDto getOrderStatus(String orderNumber) {
        Order order = loadOrderForCurrentUser(orderNumber);
        return erpOrderConnector.getOrderStatus(order.getOrderNumber())
                .orElseThrow(() -> OrderException.notFound(orderNumber));
    }

    /**
     * Détail complet d'une commande : lignes, résumé facture, statut live.
     * Client = ses commandes uniquement ; admin = toutes.
     */
    public OrderDetailDto getOrderDetail(String orderNumber) {
        Order order = loadOrderForCurrentUser(orderNumber);
        return toOrderDetailDto(order);
    }

    /**
     * [Admin] Met à jour {@code erp_mock.order_status} et notifie le(s) utilisateur(s) client.
     */
    @Transactional
    public OrderStatusResponseDto updateOrderStatus(String orderNumber, UpdateOrderStatusDto dto) {
        Order order = orderRepository.findByOrderNumberWithInvoiceAndStatus(orderNumber)
                .orElseThrow(() -> OrderException.notFound(orderNumber));

        OrderStatus status = orderStatusRepository.findByOrderNumber(orderNumber)
                .orElseGet(() -> OrderStatus.builder()
                        .order(order)
                        .orderNumber(orderNumber)
                        .build());

        String oldStatus = status.getCurrentStatus();
        status.setCurrentStatus(dto.status());
        status.setStatusUpdatedAt(OffsetDateTime.now());
        if (dto.expectedDeliveryDate() != null) {
            status.setExpectedDeliveryDate(dto.expectedDeliveryDate());
        }
        if (dto.carrierName() != null) {
            status.setCarrierName(dto.carrierName());
        }
        if (dto.carrierReference() != null) {
            status.setCarrierReference(dto.carrierReference());
        }

        OrderStatus saved = orderStatusRepository.save(status);

        notifyClientOfStatusChange(order, oldStatus, saved.getCurrentStatus());

        return new OrderStatusResponseDto(
                saved.getOrderNumber(),
                saved.getCurrentStatus(),
                saved.getStatusUpdatedAt(),
                saved.getExpectedDeliveryDate(),
                saved.getCarrierName(),
                saved.getCarrierReference()
        );
    }

    private Order loadOrderForCurrentUser(String orderNumber) {
        AuthenticatedUser currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(OrderException::accessDenied);

        Order order = orderRepository.findByOrderNumberWithInvoiceAndStatus(orderNumber)
                .orElseThrow(() -> OrderException.notFound(orderNumber));

        if (currentUser.getRole() == UserRole.CLIENT) {
            String currentUserCustomerNumber = currentUser.getCustomerNumber();
            if (order.getCustomer() == null
                    || !order.getCustomer().getCustomerNumber().equals(currentUserCustomerNumber)) {
                throw OrderException.notFound(orderNumber);
            }
        }
        return order;
    }

    private void notifyClientOfStatusChange(Order order, String oldStatus, String newStatus) {
        if (order.getCustomer() == null) {
            return;
        }
        String customerNumber = order.getCustomer().getCustomerNumber();
        List<User> recipients = userRepository.findByCustomer_CustomerNumber(customerNumber);
        String title = "Mise à jour de votre commande";
        String message = String.format(
                "Votre commande #%s est passée de « %s » à « %s ».",
                order.getOrderNumber(),
                translateOrderStatus(oldStatus),
                translateOrderStatus(newStatus));
        String targetUrl = "/client/orders/" + order.getOrderNumber();

        for (User recipient : recipients) {
            portalNotificationService.createNotificationForUser(
                    recipient,
                    title,
                    message,
                    "ORDER_STATUS_CHANGED",
                    "ORDER",
                    order.getOrderNumber(),
                    targetUrl
            );
        }
    }

    private OrderDetailDto toOrderDetailDto(Order order) {
        String customerNumber = order.getCustomer() != null
                ? order.getCustomer().getCustomerNumber()
                : null;

        List<Order> lines;
        String reference = order.getCustomerOrderReference();
        if (customerNumber != null && reference != null && !reference.isBlank()) {
            lines = orderRepository.findByCustomerNumberAndCustomerOrderReference(customerNumber, reference);
            if (lines.isEmpty()) {
                lines = List.of(order);
            }
        } else {
            lines = List.of(order);
        }

        BigDecimal totalNet = lines.stream()
                .map(Order::getNetAmount)
                .filter(a -> a != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = lines.stream()
                .map(Order::getCurrency)
                .filter(c -> c != null && !c.isBlank())
                .findFirst()
                .orElse(order.getCurrency());

        List<OrderDetailDto.OrderLineDetailDto> lineDtos = lines.stream()
                .map(line -> new OrderDetailDto.OrderLineDetailDto(
                        line.getOrderNumber(),
                        line.getProductCode(),
                        line.getProductLabel(),
                        line.getQuantityOrdered(),
                        line.getQuantityShipped(),
                        line.getSalesUnit(),
                        line.getNetAmount(),
                        line.getCurrency()
                ))
                .toList();

        Invoice invoice = order.getInvoice();
        OrderDetailDto.InvoiceSummaryDto invoiceSummary = null;
        if (invoice != null) {
            invoiceSummary = new OrderDetailDto.InvoiceSummaryDto(
                    invoice.getInvoiceNumber(),
                    invoice.getInvoiceDate(),
                    invoice.getInvoiceStatus(),
                    invoice.getNetAmount(),
                    invoice.getVatAmount(),
                    invoice.getTotalAmount(),
                    invoice.getCurrency(),
                    invoice.getDueDate(),
                    invoice.getPaymentDate()
            );
        }

        OrderStatus status = order.getOrderStatus();
        OrderStatusResponseDto statusDto = null;
        if (status != null) {
            statusDto = new OrderStatusResponseDto(
                    status.getOrderNumber(),
                    status.getCurrentStatus(),
                    status.getStatusUpdatedAt(),
                    status.getExpectedDeliveryDate(),
                    status.getCarrierName(),
                    status.getCarrierReference()
            );
        } else {
            statusDto = erpOrderConnector.getOrderStatus(order.getOrderNumber()).orElse(null);
        }

        return new OrderDetailDto(
                order.getOrderNumber(),
                order.getOrderDate(),
                customerNumber,
                order.getCustomerOrderReference(),
                order.getShipToCity(),
                order.getShipToCountry(),
                order.getRequestedDeliveryDate(),
                order.getPlannedDeliveryDate(),
                order.getGoodsIssueDate(),
                totalNet,
                currency,
                lineDtos,
                invoiceSummary,
                statusDto
        );
    }

    private static String translateOrderStatus(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "confirmed" -> "Confirmée";
            case "in_preparation" -> "En préparation";
            case "shipped" -> "Expédiée";
            case "delivered" -> "Livrée";
            case "cancelled" -> "Annulée";
            default -> status;
        };
    }

    // =========================================================================
    // Soumission d'une nouvelle commande (PRD §2.1 / §4.2 POST /api/orders)
    // =========================================================================

    /**
     * Soumet une nouvelle commande. Crée les lignes de commande dans
     * {@code erp_mock.orders} et lie éventuellement des échantillons.
     *
     * <p>Pour le MVP, on génère un numéro de commande au format
     * "4500NNNNNN" car le connecteur ERP mock ne crée pas vraiment
     * d'ordre SAP. Le commit se fait à la fin (transaction gérée par
     * Spring) pour garantir la cohérence.</p>
     */
    @Transactional
    public OrderSubmissionResponseDto submitOrder(CreateOrderRequestDto dto) {
        AuthenticatedUser current = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new IllegalStateException("Non authentifié"));
        if (current.getCustomerNumber() == null) {
            throw new SecurityException("Seuls les clients peuvent passer commande");
        }

        Customer customer = customerRepository.findById(current.getCustomerNumber())
                .orElseThrow(() -> new IllegalStateException("Client inconnu"));
        User user = userRepository.findById(current.getId()).orElse(null);

        // 1. Calcul du montant total et résolution des produits
        BigDecimal totalNet = BigDecimal.ZERO;
        String currency = "MAD";
        int totalQty = 0;
        List<Order> createdLines = new ArrayList<>();

        for (CreateOrderRequestDto.OrderLine line : dto.orderLines()) {
            Product product = productRepository.findById(line.productCode())
                    .orElseThrow(() -> new IllegalArgumentException("Produit inconnu : " + line.productCode()));

            BigDecimal unitPrice = product.getUnitPrice() != null
                    ? product.getUnitPrice()
                    : BigDecimal.ZERO;
            BigDecimal lineTotal = unitPrice.multiply(line.quantity());
            totalNet = totalNet.add(lineTotal);
            totalQty += line.quantity().intValue();

            String orderNumber = generateOrderNumber();
            Order order = Order.builder()
                    .orderNumber(orderNumber)
                    .orderDate(LocalDate.now())
                    .customer(customer)
                    .customerOrderReference(dto.customerOrderReference())
                    .productCode(product.getCode())
                    .productLabel(product.getName())
                    .quantityOrdered(line.quantity())
                    .quantityShipped(BigDecimal.ZERO)
                    .salesUnit(line.salesUnit() != null ? line.salesUnit() :
                            (product.getSalesUnit() != null ? product.getSalesUnit() : "CAR"))
                    .netAmount(lineTotal)
                    .currency(currency)
                    .shipToCity(dto.shipToCity())
                    .shipToCountry(dto.shipToCountry())
                    .requestedDeliveryDate(dto.requestedDeliveryDate())
                    .plannedDeliveryDate(dto.requestedDeliveryDate())
                    .goodsIssueDate(null)
                    .build();
            Order savedOrder = orderRepository.save(order);
            createdLines.add(savedOrder);

            // Création du statut en direct (PK = orderNumber, FK via @MapsId)
            orderStatusRepository.save(OrderStatus.builder()
                    .order(savedOrder)
                    .orderNumber(orderNumber)
                    .currentStatus("confirmed")
                    .statusUpdatedAt(OffsetDateTime.now())
                    .expectedDeliveryDate(dto.requestedDeliveryDate())
                    .carrierName(null)
                    .carrierReference(null)
                    .build());
        }

        String firstOrderNumber = createdLines.get(0).getOrderNumber();

        // 2. Liaison des échantillons groupés (PRD §2.1 — économie de transport)
        List<String> linkedSampleIds = new ArrayList<>();
        if (dto.sampleProductCodes() != null && !dto.sampleProductCodes().isEmpty()) {
            for (CreateOrderRequestDto.SampleLine sample : dto.sampleProductCodes()) {
                Product product = productRepository.findById(sample.productCode())
                        .orElseThrow(() -> new IllegalArgumentException("Produit échantillon inconnu : " + sample.productCode()));
                if (!Boolean.TRUE.equals(product.getIsSampleable())) {
                    throw new IllegalArgumentException(
                            "Le produit " + product.getCode() + " n'est pas éligible aux échantillons.");
                }
                SampleRequest sr = sampleService.createLinkedSampleRequest(
                        customer, user, product, sample.quantity(), firstOrderNumber);
                linkedSampleIds.add(sr.getId().toString());
            }
        }

        // 3. Notification admin (PRD §2.2 flux aller)
        String message = String.format(
                "Nouvelle commande #%s contenant %d ligne(s) et %d échantillon(s) soumise par %s (%s).",
                firstOrderNumber,
                createdLines.size(),
                linkedSampleIds.size(),
                customer.getCompanyName(),
                customer.getCustomerNumber());

        portalNotificationService.createAdminBroadcast(
                "Nouvelle commande B2B reçue",
                message,
                "ORDER_CREATED",
                "ORDER",
                firstOrderNumber,
                "/admin/orders"
        );

        log.info("Commande {} créée pour {} ({} lignes, {} échantillons)",
                firstOrderNumber, customer.getCustomerNumber(),
                createdLines.size(), linkedSampleIds.size());

        return new OrderSubmissionResponseDto(
                firstOrderNumber,
                dto.customerOrderReference(),
                LocalDate.now(),
                dto.requestedDeliveryDate(),
                totalNet,
                currency,
                dto.shipToCity(),
                dto.shipToCountry(),
                dto.transportMethod(),
                "confirmed",
                totalQty,
                linkedSampleIds,
                OffsetDateTime.now()
        );
    }

    /**
     * Génère un numéro de commande SAP-compatible :
     * préfixe 4500 + 6 chiffres pseudo-aléatoires uniques.
     */
    private String generateOrderNumber() {
        for (int i = 0; i < 10; i++) {
            String candidate = "4500" + String.format("%06d",
                    ThreadLocalRandom.current().nextInt(0, 999_999));
            if (!orderRepository.existsById(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Impossible de générer un numéro de commande unique");
    }
}

