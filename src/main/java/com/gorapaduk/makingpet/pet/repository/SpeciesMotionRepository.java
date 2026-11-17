package com.gorapaduk.makingpet.pet.repository;

import java.util.List;
import java.util.UUID;

import com.gorapaduk.makingpet.pet.domain.SpeciesMotion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeciesMotionRepository extends JpaRepository<SpeciesMotion, SpeciesMotion.Key> {
    List<SpeciesMotion> findByIdSpeciesIdOrderByIdAffectionLevelAsc(UUID speciesId);
    void deleteByIdSpeciesId(UUID speciesId);
}
