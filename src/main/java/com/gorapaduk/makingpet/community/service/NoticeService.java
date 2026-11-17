package com.gorapaduk.makingpet.community.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.dto.PageResponse;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import com.gorapaduk.makingpet.common.util.KstTime;
import com.gorapaduk.makingpet.common.util.PageRequests;
import com.gorapaduk.makingpet.community.domain.Notice;
import com.gorapaduk.makingpet.community.dto.NoticeDtos;
import com.gorapaduk.makingpet.community.repository.NoticeRepository;
import com.gorapaduk.makingpet.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 공지. C-1 · C-2 + 관리자 AD-10 ~ AD-12 (admin 도메인이 호출). */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    /** 작성자가 없거나(시드) 찾을 수 없을 때 보여 줄 이름 */
    public static final String DEFAULT_AUTHOR = "운영자";

    private static final Sort LIST_SORT = Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("publishedAt"));

    private final NoticeRepository notices;
    private final MemberService memberService;

    /** C-1 */
    public PageResponse<NoticeDtos.NoticeSummary> list(int page, int size) {
        var pageable = PageRequests.of(page, size, LIST_SORT);
        return PageResponse.of(notices.findByDeletedAtIsNullAndPublishedAtLessThanEqual(KstTime.now(), pageable),
                NoticeDtos.NoticeSummary::from);
    }

    /** C-2. 삭제됐거나 아직 게시 전이면 404. */
    public NoticeDtos.NoticeDetail detail(UUID noticeId) {
        Notice notice = getActive(noticeId);
        if (!notice.isPublished(KstTime.now())) throw new BusinessException(ErrorCode.NOTICE_NOT_FOUND);
        return toDetail(notice);
    }

    // ----- Admin -----

    /** AD-10 */
    @Transactional
    public NoticeDtos.NoticeDetail create(UUID authorId, NoticeDtos.CreateRequest req) {
        LocalDateTime publishedAt = req.publishedAt() == null ? KstTime.now() : toKst(req.publishedAt());
        Notice notice = notices.save(Notice.create(req.title().strip(), req.content(), authorId,
                Boolean.TRUE.equals(req.pinned()), publishedAt));
        return toDetail(notice);
    }

    /** AD-11. 관리자는 예약(미게시) 공지도 수정할 수 있다. */
    @Transactional
    public NoticeDtos.NoticeDetail update(UUID noticeId, NoticeDtos.UpdateRequest req) {
        Notice notice = getActive(noticeId);
        if (req.title() != null && req.title().isBlank()) throw new BusinessException(ErrorCode.VALIDATION_FAILED, "title 은 비울 수 없습니다.");
        if (req.content() != null && req.content().isBlank()) throw new BusinessException(ErrorCode.VALIDATION_FAILED, "content 는 비울 수 없습니다.");
        notice.update(req.title() == null ? null : req.title().strip(), req.content(), req.pinned(), toKst(req.publishedAt()));
        return toDetail(notice);
    }

    /** AD-12 soft delete. 이미 삭제된 공지는 404. */
    @Transactional
    public void delete(UUID noticeId) {
        getActive(noticeId).softDelete();
    }

    /** 시드: 공지가 하나도 없을 때만 */
    public boolean isEmpty() {
        return notices.countByDeletedAtIsNull() == 0;
    }

    @Transactional
    public void seed(String title, String content, UUID authorId, boolean pinned) {
        notices.save(Notice.create(title, content, authorId, pinned, KstTime.now()));
    }

    // ----- 내부 -----

    private Notice getActive(UUID noticeId) {
        return notices.findByIdAndDeletedAtIsNull(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
    }

    private NoticeDtos.NoticeDetail toDetail(Notice notice) {
        String nickname = notice.getAuthorId() == null ? DEFAULT_AUTHOR
                : memberService.findNickname(notice.getAuthorId()).orElse(DEFAULT_AUTHOR);
        return NoticeDtos.NoticeDetail.of(notice, nickname);
    }

    private static LocalDateTime toKst(OffsetDateTime t) {
        return t == null ? null : t.atZoneSameInstant(KstTime.ZONE).toLocalDateTime();
    }
}
