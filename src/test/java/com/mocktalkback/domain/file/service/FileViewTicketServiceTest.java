package com.mocktalkback.domain.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import com.mocktalkback.domain.file.dto.FileViewTicketResponse;
import com.mocktalkback.domain.file.entity.FileClassEntity;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.domain.file.type.FileClassCode;
import com.mocktalkback.domain.file.type.MediaKind;
import com.mocktalkback.infra.storage.ObjectStorageProperties;

class FileViewTicketServiceTest {

    // 보호 파일 ticket 발급은 보호 조회 TTL 기준으로 1회용 ticket을 저장해야 한다.
    @Test
    void issue_saves_ticket_for_protected_file() {
        // given: 보호 파일과 ticket 저장소가 있다.
        FileRepository fileRepository = mock(FileRepository.class);
        FileAccessDecisionService accessDecisionService = mock(FileAccessDecisionService.class);
        FileViewTicketStore fileViewTicketStore = mock(FileViewTicketStore.class);
        ObjectStorageProperties properties = new ObjectStorageProperties();
        properties.setProtectedViewExpireSeconds(120L);
        FileViewTicketService service = new FileViewTicketService(
            fileRepository,
            accessDecisionService,
            fileViewTicketStore,
            properties
        );
        FileEntity file = createFileEntity(31L, FileClassCode.ARTICLE_CONTENT_IMAGE);

        when(fileRepository.findByIdAndDeletedAtIsNull(31L)).thenReturn(Optional.of(file));
        when(accessDecisionService.decide(file)).thenReturn(FileAccessDecision.protectedAccess());

        // when: 보호 파일 보기 ticket을 발급하면
        FileViewTicketResponse response = service.issue(31L, "medium");

        // then: ticket이 저장되고 ticket 포함 viewUrl을 반환한다.
        assertThat(response.protectedFile()).isTrue();
        assertThat(response.expiresInSec()).isEqualTo(30L);
        assertThat(response.viewUrl()).startsWith("/api/files/31/view?");
        assertThat(response.viewUrl()).contains("variant=medium");
        assertThat(response.viewUrl()).contains("ticket=fv_");
        String issuedTicket = response.viewUrl().substring(response.viewUrl().indexOf("ticket=") + "ticket=".length());
        verify(fileViewTicketStore).save(eq(issuedTicket), eq(31L), eq(Duration.ofSeconds(30L)));
    }

    // 공개 파일 ticket 발급은 Redis 저장 없이 기존 보기 URL을 반환해야 한다.
    @Test
    void issue_returns_public_view_url_without_ticket_for_public_file() {
        // given: 공개 전달 모드의 게시판 이미지 파일이 있다.
        FileRepository fileRepository = mock(FileRepository.class);
        FileAccessDecisionService accessDecisionService = mock(FileAccessDecisionService.class);
        FileViewTicketStore fileViewTicketStore = mock(FileViewTicketStore.class);
        ObjectStorageProperties properties = new ObjectStorageProperties();
        FileViewTicketService service = new FileViewTicketService(
            fileRepository,
            accessDecisionService,
            fileViewTicketStore,
            properties
        );
        FileEntity file = createFileEntity(44L, FileClassCode.BOARD_IMAGE);

        when(fileRepository.findByIdAndDeletedAtIsNull(44L)).thenReturn(Optional.of(file));
        when(accessDecisionService.decide(file)).thenReturn(FileAccessDecision.publicAccess());

        // when: 공개 파일 보기 ticket을 발급하면
        FileViewTicketResponse response = service.issue(44L, "thumb");

        // then: ticket 없이 기존 보기 URL을 반환한다.
        assertThat(response.protectedFile()).isFalse();
        assertThat(response.expiresInSec()).isZero();
        assertThat(response.viewUrl()).isEqualTo("/api/files/44/view?variant=thumb");
        verify(fileViewTicketStore, never()).save(org.mockito.ArgumentMatchers.anyString(), eq(44L), org.mockito.ArgumentMatchers.any());
    }

    // ticket 소비 시 파일 식별자가 다르면 조회를 허용하면 안 된다.
    @Test
    void consume_throws_when_ticket_file_id_does_not_match() {
        // given: 다른 파일 식별자가 저장된 ticket이 있다.
        FileRepository fileRepository = mock(FileRepository.class);
        FileAccessDecisionService accessDecisionService = mock(FileAccessDecisionService.class);
        FileViewTicketStore fileViewTicketStore = mock(FileViewTicketStore.class);
        ObjectStorageProperties properties = new ObjectStorageProperties();
        FileViewTicketService service = new FileViewTicketService(
            fileRepository,
            accessDecisionService,
            fileViewTicketStore,
            properties
        );
        when(fileViewTicketStore.consume("valid-ticket")).thenReturn(99L);

        // when & then: 다른 파일의 ticket이면 404 예외가 발생한다.
        assertThatThrownBy(() -> service.consume(31L, "valid-ticket"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("404 NOT_FOUND");
    }

    private FileEntity createFileEntity(Long id, String fileClassCode) {
        FileClassEntity fileClassEntity = FileClassEntity.builder()
            .code(fileClassCode)
            .name("테스트 파일")
            .description("테스트")
            .mediaKind(MediaKind.IMAGE)
            .build();

        FileEntity fileEntity = FileEntity.builder()
            .fileClass(fileClassEntity)
            .fileName("file.png")
            .storageKey("uploads/test/1/file.png")
            .fileSize(1024L)
            .mimeType("image/png")
            .metadataPreserved(false)
            .build();
        ReflectionTestUtils.setField(fileEntity, "id", id);
        return fileEntity;
    }
}
