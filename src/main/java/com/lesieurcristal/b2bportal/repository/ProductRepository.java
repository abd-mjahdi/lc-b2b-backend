package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByIsActiveTrueAndIsSampleableTrueOrderByCategoryAscNameAsc();

    List<Product> findByCategoryOrderByNameAsc(String category);

    List<Product> findByIsActiveTrueOrderByCategoryAscNameAsc();

    List<Product> findAllByOrderByCategoryAscNameAsc();

    @Query(value = "SELECT COUNT(*) FROM app.quotation_requests WHERE product_code = :code",
            nativeQuery = true)
    long countQuotationRequestsByProductCode(@Param("code") String code);

    @Query(value = "SELECT COUNT(*) FROM app.certificates WHERE product_code = :code",
            nativeQuery = true)
    long countCertificatesByProductCode(@Param("code") String code);
}
