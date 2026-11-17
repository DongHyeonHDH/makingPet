package com.gorapaduk.makingpet.item.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gorapaduk.makingpet.item.domain.InventoryItem;
import com.gorapaduk.makingpet.item.domain.InventoryItemId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, InventoryItemId> {

    /** 수량을 바꾸는 모든 경로는 이 메서드로 행을 잠근 뒤 처리한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from InventoryItem i where i.id.memberId = :memberId and i.id.itemId = :itemId")
    Optional<InventoryItem> findForUpdate(@Param("memberId") UUID memberId, @Param("itemId") UUID itemId);

    @Query("select i from InventoryItem i where i.id.memberId = :memberId and i.quantity > 0")
    List<InventoryItem> findOwned(@Param("memberId") UUID memberId);

    @Query("select i from InventoryItem i where i.id.memberId = :memberId and i.id.itemId in :itemIds")
    List<InventoryItem> findByMemberAndItems(@Param("memberId") UUID memberId, @Param("itemIds") Collection<UUID> itemIds);
}
