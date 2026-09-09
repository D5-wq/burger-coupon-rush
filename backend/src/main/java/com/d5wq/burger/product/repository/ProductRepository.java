package com.d5wq.burger.product.repository;

import com.d5wq.burger.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
