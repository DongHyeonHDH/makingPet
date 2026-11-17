package com.gorapaduk.makingpet.community.dto;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.gorapaduk.makingpet.community.domain.Notice;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 기획/04_api/10_명세서_Community_Admin.md C-1 · C-2 · AD-10 · AD-11 */
public final class NoticeDtos {

    private NoticeDtos() {}

    /** C-1 */
    public record NoticeSummary(UUID id, String title, boolean pinned, LocalDateTime publishedAt) {
        public static NoticeSummary from(Notice n) {
            return new NoticeSummary(n.getId(), n.getTitle(), n.isPinned(), n.getPublishedAt());
        }
    }

    /** C-2 · AD-10 · AD-11 */
    public record NoticeDetail(UUID id, String title, boolean pinned, LocalDateTime publishedAt, String content, Author author) {
        public static NoticeDetail of(Notice n, String authorNickname) {
            return new NoticeDetail(n.getId(), n.getTitle(), n.isPinned(), n.getPublishedAt(), n.getContent(),
                    new Author(authorNickname));
        }
    }

    public record Author(String nickname) {}

    /** AD-10. publishedAt 이 null 이면 즉시 게시. */
    public record CreateRequest(
            @NotBlank @Size(max = Notice.TITLE_MAX) String title,
            @NotBlank @Size(max = Notice.CONTENT_MAX) String content,
            Boolean pinned,
            OffsetDateTime publishedAt) {}

    /** AD-11. 보낸 필드만 바뀐다. */
    public record UpdateRequest(
            @Size(min = 1, max = Notice.TITLE_MAX) String title,
            @Size(min = 1, max = Notice.CONTENT_MAX) String content,
            Boolean pinned,
            OffsetDateTime publishedAt) {}
}
