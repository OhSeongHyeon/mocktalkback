package com.mocktalkback.domain.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.mocktalkback.domain.content.type.MarketInstrumentCode;

@ExtendWith(MockitoExtension.class)
class AdminContentMarketServiceTest {

    @Mock
    private MarketSnapshotCollectorService marketSnapshotCollectorService;

    @Mock
    private MarketSnapshotImportService marketSnapshotImportService;

    @Mock
    private MarketSnapshotCommandService marketSnapshotCommandService;

    // 시세 임포트는 저장 후 영향받은 종목 전체의 변동률 재계산을 요청해야 한다.
    @Test
    void importSnapshots_recalculates_changes_for_impacted_instruments() {
        // Given: 서로 다른 두 종목을 포함한 통합 임포트 row가 준비되어 있다.
        AdminContentMarketService service = new AdminContentMarketService(
            marketSnapshotCollectorService,
            marketSnapshotImportService,
            marketSnapshotCommandService,
            Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneOffset.UTC)
        );
        MockMultipartFile file = new MockMultipartFile(
            "file",
            "market.csv",
            "text/csv",
            "instrument_code,observed_at,price_value\nUSD_KRW,2026-03-10,1450.12".getBytes()
        );
        MarketSnapshotImportParsedResult parsedResult = new MarketSnapshotImportParsedResult(
            2,
            List.of(
                new MarketSnapshotImportRow(2, MarketInstrumentCode.USD_KRW, Instant.parse("2026-03-10T00:00:00Z"), new BigDecimal("1450.12000000")),
                new MarketSnapshotImportRow(3, MarketInstrumentCode.XAU_USD, Instant.parse("2026-03-10T00:00:00Z"), new BigDecimal("3000.12000000"))
            ),
            List.of()
        );
        when(marketSnapshotImportService.parse(file, null)).thenReturn(parsedResult);
        when(marketSnapshotCommandService.upsert(eq(MarketInstrumentCode.USD_KRW), any(), any(), any()))
            .thenReturn(new MarketSnapshotWriteResult(MarketInstrumentCode.USD_KRW, Instant.parse("2026-03-10T00:00:00Z"), MarketSnapshotWriteStatus.CREATED));
        when(marketSnapshotCommandService.upsert(eq(MarketInstrumentCode.XAU_USD), any(), any(), any()))
            .thenReturn(new MarketSnapshotWriteResult(MarketInstrumentCode.XAU_USD, Instant.parse("2026-03-10T00:00:00Z"), MarketSnapshotWriteStatus.CREATED));

        // When: 운영자가 시세 파일을 임포트하면
        var response = service.importSnapshots(file, null);

        // Then: 저장 후 영향 종목 전체 재계산이 실행되어야 한다.
        verify(marketSnapshotCommandService).recalculateChanges(Set.of(MarketInstrumentCode.USD_KRW, MarketInstrumentCode.XAU_USD));
        assertThat(response.createdCount()).isEqualTo(2);
        assertThat(response.failedCount()).isZero();
    }
}
