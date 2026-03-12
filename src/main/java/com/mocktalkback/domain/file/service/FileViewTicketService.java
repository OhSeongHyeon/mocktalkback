package com.mocktalkback.domain.file.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.mocktalkback.domain.file.dto.FileViewTicketResponse;
import com.mocktalkback.domain.file.entity.FileEntity;
import com.mocktalkback.domain.file.repository.FileRepository;
import com.mocktalkback.global.auth.ticket.TicketIdGenerator;
import com.mocktalkback.infra.storage.ObjectStorageProperties;

@Service
public class FileViewTicketService {

    private static final String TICKET_PREFIX = "fv_";
    private static final long DEFAULT_PROTECTED_VIEW_EXPIRE_SECONDS = 120L;
    private static final long MAX_TICKET_TTL_SECONDS = 604800L;

    private final FileRepository fileRepository;
    private final FileAccessDecisionService fileAccessDecisionService;
    private final FileViewTicketStore fileViewTicketStore;
    private final ObjectStorageProperties objectStorageProperties;
    private final TicketIdGenerator ticketIdGenerator;

    public FileViewTicketService(
        FileRepository fileRepository,
        FileAccessDecisionService fileAccessDecisionService,
        FileViewTicketStore fileViewTicketStore,
        ObjectStorageProperties objectStorageProperties,
        TicketIdGenerator ticketIdGenerator
    ) {
        this.fileRepository = fileRepository;
        this.fileAccessDecisionService = fileAccessDecisionService;
        this.fileViewTicketStore = fileViewTicketStore;
        this.objectStorageProperties = objectStorageProperties;
        this.ticketIdGenerator = ticketIdGenerator;
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

    public Duration validate(Long fileId, String ticket) {
        if (ticket == null || ticket.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다.");
        }

        Optional<FileViewTicketStore.FileViewTicketState> ticketState = fileViewTicketStore.find(ticket);
        if (ticketState.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다.");
        }

        FileViewTicketStore.FileViewTicketState state = ticketState.get();
        if (!state.fileId().equals(fileId) || state.remainingTtl().isZero() || state.remainingTtl().isNegative()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일이 존재하지 않습니다.");
        }
        return state.remainingTtl();
    }

    private Duration resolveTicketTtl() {
        long protectedViewExpireSeconds = objectStorageProperties.getProtectedViewExpireSeconds();
        if (protectedViewExpireSeconds <= 0L) {
            protectedViewExpireSeconds = DEFAULT_PROTECTED_VIEW_EXPIRE_SECONDS;
        }
        if (protectedViewExpireSeconds > MAX_TICKET_TTL_SECONDS) {
            protectedViewExpireSeconds = MAX_TICKET_TTL_SECONDS;
        }
        return Duration.ofSeconds(protectedViewExpireSeconds);
    }

    private String buildTicket() {
        return ticketIdGenerator.generate(TICKET_PREFIX);
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
