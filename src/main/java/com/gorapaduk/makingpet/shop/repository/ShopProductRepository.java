package com.gorapaduk.makingpet.shop.repository;

import java.util.Optional;
import java.util.UUID;

import com.gorapaduk.makingpet.shop.domain.ShopProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopProductRepository extends JpaRepository<ShopProduct, UUID>, ShopProductRepositoryCustom {

    Optional<ShopProduct> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByItemId(UUID itemId);
}
