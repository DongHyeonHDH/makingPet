package com.gorapaduk.makingpet.community.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import com.gorapaduk.makingpet.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공지 (03_erd `notices`). content 는 마크다운 문자열. 삭제는 soft (AD-12).
 * publishedAt 은 KST LocalDateTime. 미래 시각이면 그때까지 목록·상세에 나오지 않는다(예약 게시).
 * authorId 는 시드로 만든 공지처럼 작성자가 없을 수 있다.
 */
@Entity
@Table(name = "notices")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseEntity {

    public static final int TITLE_MAX = 100;
    public static final int CONTENT_MAX = 20_000;

    @Id
    private UUID id;

    @Column(nullable = false, length = TITLE_MAX)
    private String title;

    @Column(nullable = false, length = CONTENT_MAX)
    private String content;

    @Column(name = "author_id")
    private UUID authorId;

    @Column(nullable = false)
    private boolean pinned;

    @Column(nullable = false)
    private LocalDateTime publishedAt;

    public static Notice create(String title, String content, UUID authorId, boolean pinned, LocalDateTime publishedAt) {
        Notice n = new Notice();
        n.id = UUID.randomUUID();
        n.title = title;
        n.content = content;
        n.authorId = authorId;
        n.pinned = pinned;
        n.publishedAt = publishedAt;
        return n;
    }

    /** AD-11. null 은 "보내지 않음". */
    public void update(String title, String content, Boolean pinned, LocalDateTime publishedAt) {
        if (title != null) this.title = title;
        if (content != null) this.content = content;
        if (pinned != null) this.pinned = pinned;
        if (publishedAt != null) this.publishedAt = publishedAt;
    }

    public boolean isPublished(LocalDateTime now) {
        return !isDeleted() && !publishedAt.isAfter(now);
    }
}
