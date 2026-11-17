package com.gorapaduk.makingpet.community.controller;

import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.SuccessCode;
import com.gorapaduk.makingpet.common.dto.ApiResponse;
import com.gorapaduk.makingpet.common.dto.PageResponse;
import com.gorapaduk.makingpet.community.dto.NoticeDtos;
import com.gorapaduk.makingpet.community.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 기획/04_api/10_명세서_Community_Admin.md C-1 · C-2 (🔓) */
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ApiResponse<PageResponse<NoticeDtos.NoticeSummary>> list(@RequestParam(defaultValue = "0") int page,
                                                                   @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(noticeService.list(page, size), SuccessCode.READ);
    }

    @GetMapping("/{noticeId}")
    public ApiResponse<NoticeDtos.NoticeDetail> detail(@PathVariable UUID noticeId) {
        return ApiResponse.ok(noticeService.detail(noticeId), SuccessCode.READ);
    }
}
