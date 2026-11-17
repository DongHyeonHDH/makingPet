package com.gorapaduk.makingpet.pet.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.gorapaduk.makingpet.pet.domain.PetSpecies;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PetSpeciesRepository extends JpaRepository<PetSpecies, UUID> {
    Optional<PetSpecies> findByCode(String code);
    boolean existsByCode(String code);
    List<PetSpecies> findByActiveTrueOrderByRarityAscNameAsc();
    List<PetSpecies> findByIdIn(Collection<UUID> ids);
}
