package com.mocktalkback.domain.file.controller;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mocktalkback.domain.file.dto.FileViewTicketResponse;
import com.mocktalkback.domain.file.service.FileViewService;
import com.mocktalkback.domain.file.service.FileViewTicketService;
import com.mocktalkback.global.common.dto.ApiEnvelope;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "FileView", description = "파일 조회 리다이렉트 API")
public class FileViewController {

    private final FileViewService fileViewService;
    private final FileViewTicketService fileViewTicketService;

    @PostMapping("/files/{fileId:\\d+}/view-ticket")
    @Operation(summary = "파일 보기 ticket 발급", description = "보호 파일은 1회용 ticket을 포함한 보기 URL을 발급하고, 공개 파일은 기존 보기 URL을 반환합니다.")
    public ApiEnvelope<FileViewTicketResponse> issueViewTicket(
        @PathVariable("fileId") Long fileId,
        @RequestParam(name = "variant", required = false) String variant
    ) {
        FileViewTicketResponse response = fileViewTicketService.issue(fileId, variant);
        return ApiEnvelope.ok(response);
    }

    @GetMapping("/files/{fileId:\\d+}/view")
    @Operation(summary = "파일 보기", description = "최적화본이 있으면 변환본으로, 없으면 원본으로 리다이렉트합니다.")
    public ResponseEntity<Void> viewFile(
        @PathVariable("fileId") Long fileId,
        @RequestParam(name = "variant", required = false) String variant,
        @RequestParam(name = "ticket", required = false) String ticket
    ) {
        String location = fileViewService.resolveViewLocation(fileId, variant, ticket);
        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(location));
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
