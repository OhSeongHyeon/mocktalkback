package com.mocktalkback.domain.board.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import com.mocktalkback.domain.article.dto.BoardArticleListResponse;
import com.mocktalkback.domain.article.service.ArticleService;
import com.mocktalkback.domain.board.dto.BoardCreateRequest;
import com.mocktalkback.domain.board.dto.BoardDetailResponse;
import com.mocktalkback.domain.board.dto.BoardMemberStatusResponse;
import com.mocktalkback.domain.board.dto.BoardResponse;
import com.mocktalkback.domain.board.dto.BoardSubscribeItemResponse;
import com.mocktalkback.domain.board.dto.BoardSubscribeStatusResponse;
import com.mocktalkback.domain.board.dto.BoardUpdateRequest;
import com.mocktalkback.domain.board.service.BoardService;
import com.mocktalkback.global.common.dto.ApiEnvelope;
import com.mocktalkback.global.common.dto.PageResponse;
import com.mocktalkback.global.common.type.SortOrder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Board", description = "게시판/커뮤니티 API")
public class BoardController {

    private final BoardService boardService;
    private final ArticleService articleService;

    @PostMapping("/boards")
    @Operation(summary = "게시판 생성", description = "게시판을 생성하고 생성자를 OWNER로 등록합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "생성 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<BoardResponse> create(@RequestBody @Valid BoardCreateRequest request) {
        return ApiEnvelope.ok(boardService.create(request));
    }

    @GetMapping("/boards/{id:\\d+}")
    @Operation(summary = "게시판 상세 조회(ID)", description = "게시판 ID로 상세 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "404", description = "게시판 없음")
    })
    public ApiEnvelope<BoardDetailResponse> findById(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(boardService.findById(id));
    }

    @GetMapping("/boards/slug/{slug}")
    @Operation(summary = "게시판 상세 조회(슬러그)", description = "게시판 슬러그로 상세 정보를 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "404", description = "게시판 없음")
    })
    public ApiEnvelope<BoardDetailResponse> findBySlug(@PathVariable("slug") String slug) {
        return ApiEnvelope.ok(boardService.findBySlug(slug));
    }

    @GetMapping("/boards")
    @Operation(summary = "게시판 목록 조회", description = "게시판 목록을 페이징으로 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류")
    })
    public ApiEnvelope<PageResponse<BoardResponse>> findAll(
        @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
        @RequestParam(name = "page", defaultValue = "0") int page,
        @Parameter(description = "페이지 크기(최대 50)", example = "10")
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(boardService.findAll(page, size));
    }

    @GetMapping("/boards/subscribes")
    @Operation(summary = "구독한 게시판 목록", description = "로그인 사용자의 구독 목록을 페이징으로 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<PageResponse<BoardSubscribeItemResponse>> findSubscribes(
        @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
        @RequestParam(name = "page", defaultValue = "0") int page,
        @Parameter(description = "페이지 크기(최대 50)", example = "10")
        @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return ApiEnvelope.ok(boardService.findSubscribes(page, size));
    }

    @GetMapping("/boards/{id:\\d+}/articles")
    @Operation(summary = "게시판 게시글 목록", description = "게시판 내 게시글 목록을 페이징으로 조회합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "404", description = "게시판 없음")
    })
    public ApiEnvelope<BoardArticleListResponse> findArticles(
        @PathVariable("id") Long id,
        @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
        @RequestParam(name = "page", defaultValue = "0") int page,
        @Parameter(description = "페이지 크기(최대 50)", example = "10")
        @RequestParam(name = "size", defaultValue = "10") int size,
        @Parameter(description = "정렬(최신순/과거순)", example = "LATEST")
        @RequestParam(name = "order", defaultValue = "LATEST") SortOrder order
    ) {
        return ApiEnvelope.ok(articleService.getBoardArticles(id, page, size, order));
    }

    @PutMapping("/boards/{id:\\d+}")
    @Operation(summary = "게시판 수정", description = "게시판 정보를 수정합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ApiEnvelope<BoardResponse> update(
        @PathVariable("id") Long id,
        @RequestBody @Valid BoardUpdateRequest request
    ) {
        return ApiEnvelope.ok(boardService.update(id, request));
    }

    @DeleteMapping("/boards/{id:\\d+}")
    @Operation(summary = "게시판 삭제", description = "게시판을 소프트 삭제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "삭제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ApiEnvelope<Void> delete(@PathVariable("id") Long id) {
        boardService.delete(id);
        return ApiEnvelope.ok();
    }

    @PostMapping(value = "/boards/{id:\\d+}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "게시판 대표 이미지 업로드", description = "게시판 대표 이미지를 업로드합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "업로드 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "400", description = "요청 값 오류"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음")
    })
    public ApiEnvelope<BoardResponse> uploadImage(
        @PathVariable("id") Long id,
        @RequestPart("boardImage") MultipartFile boardImage,
        @RequestParam(name = "preserveMetadata", defaultValue = "false") boolean preserveMetadata
    ) {
        return ApiEnvelope.ok(boardService.uploadBoardImage(id, boardImage, preserveMetadata));
    }

    @PostMapping("/boards/{id:\\d+}/subscribe")
    @Operation(summary = "게시판 구독", description = "게시판을 구독합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "구독 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "409", description = "이미 구독 중")
    })
    public ApiEnvelope<BoardSubscribeStatusResponse> subscribe(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(boardService.subscribe(id));
    }

    @DeleteMapping("/boards/{id:\\d+}/subscribe")
    @Operation(summary = "게시판 구독 해제", description = "게시판 구독을 해제합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "구독 해제 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<BoardSubscribeStatusResponse> unsubscribe(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(boardService.unsubscribe(id));
    }

    @PostMapping("/boards/{id:\\d+}/members")
    @Operation(summary = "게시판 가입 요청", description = "게시판 가입을 요청합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "가입 요청 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "409", description = "이미 가입된 사용자")
    })
    public ApiEnvelope<BoardMemberStatusResponse> requestJoin(@PathVariable("id") Long id) {
        return ApiEnvelope.ok(boardService.requestJoin(id));
    }

    @PostMapping("/boards/{id:\\d+}/members/{userId:\\d+}/approve")
    @Operation(summary = "게시판 가입 승인", description = "가입 요청을 승인합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "승인 성공", content = @Content(schema = @Schema(implementation = ApiEnvelope.class))),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "대상 없음")
    })
    public ApiEnvelope<BoardMemberStatusResponse> approveJoin(
        @PathVariable("id") Long id,
        @PathVariable("userId") Long userId
    ) {
        return ApiEnvelope.ok(boardService.approveJoin(id, userId));
    }

    @DeleteMapping("/boards/{id:\\d+}/members/me")
    @Operation(summary = "게시판 가입 취소(본인)", description = "본인의 가입 상태를 취소합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "취소 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요")
    })
    public ApiEnvelope<Void> cancelJoin(@PathVariable("id") Long id) {
        boardService.cancelOwnMember(id);
        return ApiEnvelope.ok();
    }

    @DeleteMapping("/boards/{id:\\d+}/members/{userId:\\d+}")
    @Operation(summary = "게시판 멤버 삭제/거절", description = "멤버를 삭제하거나 가입 요청을 거절합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "처리 성공"),
        @ApiResponse(responseCode = "401", description = "인증 필요"),
        @ApiResponse(responseCode = "403", description = "권한 없음"),
        @ApiResponse(responseCode = "404", description = "대상 없음")
    })
    public ApiEnvelope<Void> removeMember(
        @PathVariable("id") Long id,
        @PathVariable("userId") Long userId
    ) {
        boardService.cancelOrRejectMember(id, userId);
        return ApiEnvelope.ok();
    }
}
