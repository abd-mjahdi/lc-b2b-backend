package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.erpmock.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.invoice WHERE o.customer.customerNumber = :customerNumber ORDER BY o.orderDate DESC, o.orderNumber DESC")
    List<Order> findByCustomerCustomerNumberOrderByOrderDateDesc(@Param("customerNumber") String customerNumber);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.invoice WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithInvoice(@Param("orderNumber") String orderNumber);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.invoice LEFT JOIN FETCH o.orderStatus "
            + "WHERE o.orderNumber = :orderNumber")
    Optional<Order> findByOrderNumberWithInvoiceAndStatus(@Param("orderNumber") String orderNumber);

    @Query("SELECT o FROM Order o LEFT JOIN FETCH o.invoice LEFT JOIN FETCH o.orderStatus "
            + "WHERE o.orderGroupId = :orderGroupId "
            + "ORDER BY o.orderNumber ASC")
    List<Order> findByOrderGroupIdWithInvoiceAndStatus(@Param("orderGroupId") String orderGroupId);

    boolean existsByCustomer_CustomerNumberAndCustomerOrderReference(
            String customerNumber, String customerOrderReference);

    @Query(value = "SELECT nextval('erp_mock.order_number_seq')", nativeQuery = true)
    Long nextOrderNumberSeq();

    @Query(value = "SELECT o FROM Order o LEFT JOIN FETCH o.invoice i LEFT JOIN FETCH o.orderStatus os WHERE " +
            "o.customer.customerNumber = :customerNumber " +
            "AND (cast(:startDate as date) IS NULL OR o.orderDate >= :startDate) " +
            "AND (cast(:endDate as date) IS NULL OR o.orderDate <= :endDate) " +
            "AND (:status IS NULL OR os.currentStatus = :status) " +
            "AND (:invoiceStatus IS NULL OR i.invoiceStatus = :invoiceStatus)",
            countQuery = "SELECT count(o) FROM Order o LEFT JOIN o.orderStatus os LEFT JOIN o.invoice i WHERE " +
            "o.customer.customerNumber = :customerNumber " +
            "AND (cast(:startDate as date) IS NULL OR o.orderDate >= :startDate) " +
            "AND (cast(:endDate as date) IS NULL OR o.orderDate <= :endDate) " +
            "AND (:status IS NULL OR os.currentStatus = :status) " +
            "AND (:invoiceStatus IS NULL OR i.invoiceStatus = :invoiceStatus)")
    org.springframework.data.domain.Page<Order> findFilteredOrders(
            @Param("customerNumber") String customerNumber,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate,
            @Param("status") String status,
            @Param("invoiceStatus") String invoiceStatus,
            org.springframework.data.domain.Pageable pageable);
}
