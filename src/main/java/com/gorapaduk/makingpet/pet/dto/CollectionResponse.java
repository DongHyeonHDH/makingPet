package com.gorapaduk.makingpet.pet.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

/** P-7 도감 */
public record CollectionResponse(int total, int owned, List<Entry> items) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Entry(SpeciesSummary species, boolean owned, UUID petId, LocalDateTime obtainedAt) {}
}
