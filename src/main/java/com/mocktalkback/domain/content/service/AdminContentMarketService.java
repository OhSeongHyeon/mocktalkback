package com.mocktalkback.domain.content.service;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.mocktalkback.domain.content.dto.AdminMarketImportFailureResponse;
import com.mocktalkback.domain.content.dto.AdminMarketImportResponse;
import com.mocktalkback.domain.content.dto.AdminMarketRefreshItemResponse;
import com.mocktalkback.domain.content.dto.AdminMarketRefreshResponse;
import com.mocktalkback.domain.content.type.MarketInstrumentCode;

@Service
public class AdminContentMarketService {

    private static final String MANUAL_IMPORT_PROVIDER = "MANUAL_IMPORT";

    private final MarketSnapshotCollectorService marketSnapshotCollectorService;
    private final MarketSnapshotImportService marketSnapshotImportService;
    private final MarketSnapshotCommandService marketSnapshotCommandService;
    private final Clock clock;

    @Autowired
    public AdminContentMarketService(
        MarketSnapshotCollectorService marketSnapshotCollectorService,
        MarketSnapshotImportService marketSnapshotImportService,
        MarketSnapshotCommandService marketSnapshotCommandService
    ) {
        this(marketSnapshotCollectorService, marketSnapshotImportService, marketSnapshotCommandService, Clock.systemUTC());
    }

    AdminContentMarketService(
        MarketSnapshotCollectorService marketSnapshotCollectorService,
        MarketSnapshotImportService marketSnapshotImportService,
        MarketSnapshotCommandService marketSnapshotCommandService,
        Clock clock
    ) {
        this.marketSnapshotCollectorService = marketSnapshotCollectorService;
        this.marketSnapshotImportService = marketSnapshotImportService;
        this.marketSnapshotCommandService = marketSnapshotCommandService;
        this.clock = clock;
    }

    @Transactional
    public AdminMarketRefreshResponse refreshNow() {
        List<MarketSnapshotWriteResult> results = marketSnapshotCollectorService.collectLatestSnapshots();
        int createdCount = countByStatus(results, MarketSnapshotWriteStatus.CREATED);
        int updatedCount = countByStatus(results, MarketSnapshotWriteStatus.UPDATED);
        int skippedCount = countByStatus(results, MarketSnapshotWriteStatus.SKIPPED);
        List<AdminMarketRefreshItemResponse> items = results.stream()
            .map(result -> new AdminMarketRefreshItemResponse(
                result.instrumentCode(),
                result.observedAt(),
                result.status().name()
            ))
            .toList();

        return new AdminMarketRefreshResponse(
            clock.instant(),
            results.size(),
            createdCount,
            updatedCount,
            skippedCount,
            items
        );
    }

    @Transactional
    public AdminMarketImportResponse importSnapshots(MultipartFile file, MarketInstrumentCode selectedInstrument) {
        MarketSnapshotImportParsedResult parsedResult = marketSnapshotImportService.parse(file, selectedInstrument);
        List<AdminMarketImportFailureResponse> failures = new ArrayList<>(parsedResult.failures());
        int createdCount = 0;
        int updatedCount = 0;
        int skippedCount = 0;

        for (MarketSnapshotImportRow row : parsedResult.rows()) {
            try {
                MarketSnapshotWriteResult result = marketSnapshotCommandService.upsert(
                    row.instrumentCode(),
                    MANUAL_IMPORT_PROVIDER,
                    row.priceValue(),
                    row.observedAt()
                );
                if (result.status() == MarketSnapshotWriteStatus.CREATED) {
                    createdCount += 1;
                } else if (result.status() == MarketSnapshotWriteStatus.UPDATED) {
                    updatedCount += 1;
                } else {
                    skippedCount += 1;
                }
            } catch (IllegalArgumentException ex) {
                failures.add(new AdminMarketImportFailureResponse(row.rowNumber(), ex.getMessage()));
            }
        }

        return new AdminMarketImportResponse(
            clock.instant(),
            file.getOriginalFilename(),
            selectedInstrument,
            selectedInstrument == null,
            parsedResult.totalCount(),
            createdCount,
            updatedCount,
            skippedCount,
            failures.size(),
            failures
        );
    }

    private int countByStatus(List<MarketSnapshotWriteResult> results, MarketSnapshotWriteStatus status) {
        return (int) results.stream()
            .filter(result -> result.status() == status)
            .count();
    }
}
