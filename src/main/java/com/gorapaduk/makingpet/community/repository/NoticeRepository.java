package com.gorapaduk.makingpet.community.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.gorapaduk.makingpet.community.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NoticeRepository extends JpaRepository<Notice, UUID> {

    /** C-1. 정렬은 Pageable(pinned desc, publishedAt desc). */
    Page<Notice> findByDeletedAtIsNullAndPublishedAtLessThanEqual(LocalDateTime now, Pageable pageable);

    Optional<Notice> findByIdAndDeletedAtIsNull(UUID id);

    long countByDeletedAtIsNull();
}
