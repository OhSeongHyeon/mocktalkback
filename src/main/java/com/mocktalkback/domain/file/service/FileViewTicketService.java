package com.mocktalkback.domain.file.service;

import java.time.Duration;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.mocktalkback.domain.file.dto.FileViewTicketResponse;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.infra.storage.ObjectStorageProperties;

@Service
public class FileViewTicketService {

    private static final String TICKET_PREFIX = "fv_";
    private static final long DEFAULT_PROTECTED_VIEW_EXPIRE_SECONDS = 120L;
    private static final long DEFAULT_TICKET_TTL_SECONDS = 30L;

    private final FileRepository fileRepository;
    private final FileAccessDecisionService fileAccessDecisionService;
    private final FileViewTicketStore fileViewTicketStore;
    private final ObjectStorageProperties objectStorageProperties;

    public FileViewTicketService(
        FileRepository fileRepository,
        FileAccessDecisionService fileAccessDecisionService,
        FileViewTicketStore fileViewTicketStore,
        ObjectStorageProperties objectStorageProperties
    ) {
        this.fileRepository = fileRepository;
        this.fileAccessDecisionService = fileAccessDecisionService;
        this.fileViewTicketStore = fileViewTicketStore;
        this.objectStorageProperties = objectStorageProperties;
    }

    public FileViewTicketResponse issue(Long fileId, String variantParam) {
        FileEntity file = fileRepository.findByIdAndDeletedAtIsNull(fileId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다."));

        FileAccessDecision accessDecision = fileAccessDecisionService.decide(file);
        if (!accessDecision.allowed()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다.");
        }

        if (accessDecision.deliveryMode() == FileDeliveryMode.PUBLIC) {
            return new FileViewTicketResponse(buildViewUrl(fileId, variantParam, null), 0L, false);
        }

        Duration ticketTtl = resolveTicketTtl();
        String ticket = buildTicket();
        fileViewTicketStore.save(ticket, fileId, ticketTtl);
        return new FileViewTicketResponse(buildViewUrl(fileId, variantParam, ticket), ticketTtl.toSeconds(), true);
    }

    public void consume(Long fileId, String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다.");
        }

        Long storedFileId = fileViewTicketStore.consume(ticket);
        if (storedFileId == null || !storedFileId.equals(fileId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다.");
        }
    }

    private Duration resolveTicketTtl() {
        long protectedViewExpireSeconds = objectStorageProperties.getProtectedViewExpireSeconds();
        if (protectedViewExpireSeconds <= 0L) {
            protectedViewExpireSeconds = DEFAULT_PROTECTED_VIEW_EXPIRE_SECONDS;
        }
        long ticketTtlSeconds = Math.min(DEFAULT_TICKET_TTL_SECONDS, protectedViewExpireSeconds);
        if (ticketTtlSeconds <= 0L) {
            ticketTtlSeconds = DEFAULT_TICKET_TTL_SECONDS;
        }
        return Duration.ofSeconds(ticketTtlSeconds);
    }

    private String buildTicket() {
        return TICKET_PREFIX + UUID.randomUUID().toString().replace("-", "");
    }

    private String buildViewUrl(Long fileId, String variantParam, String ticket) {
        StringBuilder builder = new StringBuilder("/api/files/")
            .append(fileId)
            .append("/view");

        boolean hasQuery = false;
        if (variantParam != null && !variantParam.isBlank()) {
            builder.append("?variant=").append(variantParam);
            hasQuery = true;
        }
        if (ticket != null && !ticket.isBlank()) {
            builder.append(hasQuery ? '&' : '?')
                .append("ticket=")
                .append(ticket);
        }
        return builder.toString();
    }
}
