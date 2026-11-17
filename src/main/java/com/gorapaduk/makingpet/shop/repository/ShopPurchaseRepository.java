package com.gorapaduk.makingpet.shop.repository;

import java.util.UUID;

import com.gorapaduk.makingpet.shop.domain.ShopPurchase;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShopPurchaseRepository extends JpaRepository<ShopPurchase, UUID> {

    long countByMemberId(UUID memberId);
}
