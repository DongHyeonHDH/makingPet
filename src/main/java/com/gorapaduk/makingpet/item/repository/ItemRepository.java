package com.gorapaduk.makingpet.item.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gorapaduk.makingpet.item.domain.Item;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemRepository extends JpaRepository<Item, UUID>, ItemRepositoryCustom {

    Optional<Item> findByCode(String code);

    boolean existsByCode(String code);

    List<Item> findByIdIn(Collection<UUID> ids);
}
