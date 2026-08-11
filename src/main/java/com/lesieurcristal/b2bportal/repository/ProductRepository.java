package com.lesieurcristal.b2bportal.repository;

import com.lesieurcristal.b2bportal.entity.app.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findByIsSampleableTrueOrderByCategoryAscNameAsc();

    List<Product> findByCategoryOrderByNameAsc(String category);

    List<Product> findAllByOrderByCategoryAscNameAsc();
}
