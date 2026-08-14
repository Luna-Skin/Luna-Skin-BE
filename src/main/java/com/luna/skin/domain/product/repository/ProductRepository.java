package com.luna.skin.domain.product.repository;

import com.luna.skin.domain.product.entity.Product;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

  @Query(value = "SELECT * FROM product WHERE ingredient = :ingredient ORDER BY RANDOM() LIMIT 1", nativeQuery = true)
  Optional<Product> findRandomByIngredient(@Param("ingredient") String ingredient);
}