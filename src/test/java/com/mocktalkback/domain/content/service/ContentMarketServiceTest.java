package com.mocktalkback.domain.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.mocktalkback.domain.content.entity.MarketSnapshotEntity;
import com.mocktalkback.domain.content.repository.MarketSnapshotRepository;
import com.mocktalkback.domain.content.type.MarketInstrumentCode;
import com.mocktalkback.domain.content.type.MarketSeriesPeriod;

@ExtendWith(MockitoExtension.class)
class ContentMarketServiceTest {

    @Mock
    private MarketSnapshotRepository marketSnapshotRepository;

    // 요약 조회는 표시 순서대로 최신 시세 목록을 반환해야 한다.
    @Test
    void findOverview_returns_latest_items_in_display_order() {
        // Given: 두 종목의 최신 스냅샷이 저장되어 있다.
        ContentMarketService service = new ContentMarketService(
            marketSnapshotRepository,
            Clock.fixed(Instant.parse("2026-03-15T00:00:00Z"), ZoneOffset.UTC)
        );
        MarketSnapshotEntity usdSnapshot = createSnapshot(
            1L,
            MarketInstrumentCode.USD_KRW,
            new BigDecimal("1450.12000000"),
            Instant.parse("2026-03-15T03:05:00Z")
        );
        MarketSnapshotEntity goldSnapshot = createSnapshot(
            2L,
            MarketInstrumentCode.XAU_USD,
            new BigDecimal("3012.12000000"),
            Instant.parse("2026-03-15T03:05:00Z")
        );
        when(marketSnapshotRepository.findFirstByInstrumentCodeOrderByObservedAtDesc(MarketInstrumentCode.USD_KRW)).thenReturn(Optional.of(usdSnapshot));
        when(marketSnapshotRepository.findFirstByInstrumentCodeOrderByObservedAtDesc(MarketInstrumentCode.EUR_KRW)).thenReturn(Optional.empty());
        when(marketSnapshotRepository.findFirstByInstrumentCodeOrderByObservedAtDesc(MarketInstrumentCode.JPY_KRW)).thenReturn(Optional.empty());
        when(marketSnapshotRepository.findFirstByInstrumentCodeOrderByObservedAtDesc(MarketInstrumentCode.XAU_USD)).thenReturn(Optional.of(goldSnapshot));
        when(marketSnapshotRepository.findFirstByInstrumentCodeOrderByObservedAtDesc(MarketInstrumentCode.XAU_KRW)).thenReturn(Optional.empty());

        // When: 요약 목록을 조회하면
        var response = service.findOverview();

        // Then: 표시 순서대로 최신 항목이 반환되어야 한다.
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).instrumentCode()).isEqualTo(MarketInstrumentCode.USD_KRW);
        assertThat(response.items().get(1).instrumentCode()).isEqualTo(MarketInstrumentCode.XAU_USD);
        assertThat(response.lastObservedAt()).isEqualTo(Instant.parse("2026-03-15T03:05:00Z"));
    }

    // 시계열 조회는 선택한 기간의 포인트 목록을 시간순으로 반환해야 한다.
    @Test
    void findSeries_returns_points_for_period() {
        // Given: 최근 7일 시세 포인트가 저장되어 있다.
        ContentMarketService service = new ContentMarketService(
            marketSnapshotRepository,
            Clock.fixed(Instant.parse("2026-03-15T00:00:00Z"), ZoneOffset.UTC)
        );
        List<MarketSnapshotEntity> snapshots = List.of(
            createSnapshot(1L, MarketInstrumentCode.XAU_USD, new BigDecimal("2990.00000000"), Instant.parse("2026-03-10T03:05:00Z")),
            createSnapshot(2L, MarketInstrumentCode.XAU_USD, new BigDecimal("3012.12000000"), Instant.parse("2026-03-15T03:05:00Z"))
        );
        when(marketSnapshotRepository.findByInstrumentCodeAndObservedAtGreaterThanEqualOrderByObservedAtAsc(
            MarketInstrumentCode.XAU_USD,
            Instant.parse("2026-03-08T00:00:00Z")
        )).thenReturn(snapshots);

        // When: 7일 시계열을 조회하면
        var response = service.findSeries(MarketInstrumentCode.XAU_USD, MarketSeriesPeriod.WEEK);

        // Then: 포인트와 마지막 시각이 반환되어야 한다.
        assertThat(response.points()).hasSize(2);
        assertThat(response.points().get(0).value()).isEqualByComparingTo("2990.00000000");
        assertThat(response.lastObservedAt()).isEqualTo(Instant.parse("2026-03-15T03:05:00Z"));
    }

    private MarketSnapshotEntity createSnapshot(
        Long id,
        MarketInstrumentCode instrumentCode,
        BigDecimal priceValue,
        Instant observedAt
    ) {
        MarketSnapshotEntity entity = MarketSnapshotEntity.create(
            instrumentCode,
            "TEST_PROVIDER",
            priceValue,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            observedAt
        );
        ReflectionTestUtils.setField(entity, "id", id);
        ReflectionTestUtils.setField(entity, "createdAt", observedAt);
        return entity;
    }
}
