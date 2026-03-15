package com.mocktalkback.domain.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.mocktalkback.domain.content.dto.MarketSeriesPointResponse;
import com.mocktalkback.domain.content.dto.MarketSeriesResponse;
import com.mocktalkback.domain.content.entity.MarketSnapshotEntity;
import com.mocktalkback.domain.content.repository.MarketSnapshotRepository;
import com.mocktalkback.domain.content.type.MarketInstrumentCode;
import com.mocktalkback.domain.content.type.MarketSeriesPeriod;

@ExtendWith(MockitoExtension.class)
class ContentMarketServiceTest {

    @Mock
    private MarketSnapshotRepository marketSnapshotRepository;

    @Mock
    private ContentMarketSeriesCacheStore contentMarketSeriesCacheStore;

    // 요약 조회는 표시 순서대로 최신 시세 목록을 반환해야 한다.
    @Test
    void findOverview_returns_latest_items_in_display_order() {
        // Given: 두 종목의 최신 스냅샷이 저장되어 있다.
        ContentMarketService service = new ContentMarketService(
            marketSnapshotRepository,
            contentMarketSeriesCacheStore,
            Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneOffset.UTC)
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
        when(marketSnapshotRepository.findFirstByInstrumentCodeAndObservedAtLessThanOrderByObservedAtDesc(
            MarketInstrumentCode.USD_KRW,
            Instant.parse("2026-03-15T03:05:00Z")
        )).thenReturn(Optional.empty());
        when(marketSnapshotRepository.findFirstByInstrumentCodeAndObservedAtLessThanOrderByObservedAtDesc(
            MarketInstrumentCode.XAU_USD,
            Instant.parse("2026-03-15T03:05:00Z")
        )).thenReturn(Optional.empty());

        // When: 요약 목록을 조회하면
        var response = service.findOverview();

        // Then: 표시 순서대로 최신 항목이 반환되어야 한다.
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().get(0).instrumentCode()).isEqualTo(MarketInstrumentCode.USD_KRW);
        assertThat(response.items().get(1).instrumentCode()).isEqualTo(MarketInstrumentCode.XAU_USD);
        assertThat(response.lastObservedAt()).isEqualTo(Instant.parse("2026-03-15T03:05:00Z"));
    }

    // 요약 조회는 최신 스냅샷 직전 값 기준으로 변화량을 다시 계산해야 한다.
    @Test
    void findOverview_recalculates_change_from_previous_snapshot() {
        // Given: 최신값과 직전값이 함께 저장되어 있다.
        ContentMarketService service = new ContentMarketService(
            marketSnapshotRepository,
            contentMarketSeriesCacheStore,
            Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneOffset.UTC)
        );
        MarketSnapshotEntity latestSnapshot = createSnapshot(
            1L,
            MarketInstrumentCode.USD_KRW,
            new BigDecimal("1450.12000000"),
            Instant.parse("2026-03-15T03:05:00Z")
        );
        MarketSnapshotEntity previousSnapshot = createSnapshot(
            2L,
            MarketInstrumentCode.USD_KRW,
            new BigDecimal("1440.12000000"),
            Instant.parse("2026-03-14T03:05:00Z")
        );
        when(marketSnapshotRepository.findFirstByInstrumentCodeOrderByObservedAtDesc(MarketInstrumentCode.USD_KRW)).thenReturn(Optional.of(latestSnapshot));
        when(marketSnapshotRepository.findFirstByInstrumentCodeOrderByObservedAtDesc(MarketInstrumentCode.EUR_KRW)).thenReturn(Optional.empty());
        when(marketSnapshotRepository.findFirstByInstrumentCodeOrderByObservedAtDesc(MarketInstrumentCode.JPY_KRW)).thenReturn(Optional.empty());
        when(marketSnapshotRepository.findFirstByInstrumentCodeOrderByObservedAtDesc(MarketInstrumentCode.XAU_USD)).thenReturn(Optional.empty());
        when(marketSnapshotRepository.findFirstByInstrumentCodeOrderByObservedAtDesc(MarketInstrumentCode.XAU_KRW)).thenReturn(Optional.empty());
        when(marketSnapshotRepository.findFirstByInstrumentCodeAndObservedAtLessThanOrderByObservedAtDesc(
            MarketInstrumentCode.USD_KRW,
            Instant.parse("2026-03-15T03:05:00Z")
        )).thenReturn(Optional.of(previousSnapshot));

        // When: 요약 목록을 조회하면
        var response = service.findOverview();

        // Then: 최신값 기준 변화량이 다시 계산되어야 한다.
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).changeValue()).isEqualByComparingTo("10.00000000");
        assertThat(response.items().get(0).changeRate()).isEqualByComparingTo("0.694400");
    }

    // 시계열 조회는 선택한 기간의 포인트 목록을 시간순으로 반환해야 한다.
    @Test
    void findSeries_returns_points_for_period() {
        // Given: 최근 7일 시세 포인트가 저장되어 있다.
        ContentMarketService service = new ContentMarketService(
            marketSnapshotRepository,
            contentMarketSeriesCacheStore,
            Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneOffset.UTC)
        );
        when(contentMarketSeriesCacheStore.find(MarketInstrumentCode.XAU_USD, MarketSeriesPeriod.WEEK)).thenReturn(Optional.empty());
        List<MarketSnapshotEntity> snapshots = List.of(
            createSnapshot(1L, MarketInstrumentCode.XAU_USD, new BigDecimal("2990.00000000"), Instant.parse("2026-03-10T03:05:00Z")),
            createSnapshot(2L, MarketInstrumentCode.XAU_USD, new BigDecimal("3012.12000000"), Instant.parse("2026-03-15T03:05:00Z"))
        );
        when(marketSnapshotRepository.findByInstrumentCodeAndObservedAtGreaterThanEqualAndObservedAtLessThanOrderByObservedAtAsc(
            MarketInstrumentCode.XAU_USD,
            Instant.parse("2026-03-09T00:00:00Z"),
            Instant.parse("2026-03-16T00:00:00Z")
        )).thenReturn(snapshots);

        // When: 7일 시계열을 조회하면
        var response = service.findSeries(MarketInstrumentCode.XAU_USD, MarketSeriesPeriod.WEEK, null, null);

        // Then: 포인트와 마지막 시각이 반환되어야 한다.
        assertThat(response.points()).hasSize(2);
        assertThat(response.points().get(0).value()).isEqualByComparingTo("2990.00000000");
        assertThat(response.lastObservedAt()).isEqualTo(Instant.parse("2026-03-15T03:05:00Z"));
    }

    // 시계열 조회는 직접 선택 기간 범위를 그대로 사용해야 한다.
    @Test
    void findSeries_returns_points_for_custom_range() {
        // Given: 직접 선택 기간 시세 포인트가 저장되어 있다.
        ContentMarketService service = new ContentMarketService(
            marketSnapshotRepository,
            contentMarketSeriesCacheStore,
            Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneOffset.UTC)
        );
        when(contentMarketSeriesCacheStore.find(MarketInstrumentCode.USD_KRW, MarketSeriesPeriod.CUSTOM)).thenReturn(Optional.empty());
        List<MarketSnapshotEntity> snapshots = List.of(
            createSnapshot(1L, MarketInstrumentCode.USD_KRW, new BigDecimal("1448.10000000"), Instant.parse("2026-03-01T03:05:00Z")),
            createSnapshot(2L, MarketInstrumentCode.USD_KRW, new BigDecimal("1450.12000000"), Instant.parse("2026-03-15T03:05:00Z"))
        );
        when(marketSnapshotRepository.findByInstrumentCodeAndObservedAtGreaterThanEqualAndObservedAtLessThanOrderByObservedAtAsc(
            MarketInstrumentCode.USD_KRW,
            Instant.parse("2026-03-01T00:00:00Z"),
            Instant.parse("2026-03-16T00:00:00Z")
        )).thenReturn(snapshots);

        // When: 직접 선택 기간 시계열을 조회하면
        var response = service.findSeries(
            MarketInstrumentCode.USD_KRW,
            MarketSeriesPeriod.CUSTOM,
            LocalDate.parse("2026-03-01"),
            LocalDate.parse("2026-03-15")
        );

        // Then: 지정한 날짜 범위 포인트와 기간 타입이 반환되어야 한다.
        assertThat(response.period()).isEqualTo(MarketSeriesPeriod.CUSTOM);
        assertThat(response.points()).hasSize(2);
        assertThat(response.points().get(1).value()).isEqualByComparingTo("1450.12000000");
    }

    // 장기 preset 기간 조회는 캐시 hit면 DB를 조회하지 않고 캐시 응답을 반환해야 한다.
    @Test
    void findSeries_returns_cached_response_for_long_preset_period() {
        // Given: 1년 시계열 응답이 Redis 캐시에 저장되어 있다.
        ContentMarketService service = new ContentMarketService(
            marketSnapshotRepository,
            contentMarketSeriesCacheStore,
            Clock.fixed(Instant.parse("2026-03-15T12:00:00Z"), ZoneOffset.UTC)
        );
        MarketSeriesResponse cachedResponse = new MarketSeriesResponse(
            MarketInstrumentCode.USD_KRW,
            "USD/KRW",
            MarketInstrumentCode.USD_KRW.getMarketGroup(),
            MarketInstrumentCode.USD_KRW.getUnitLabel(),
            MarketSeriesPeriod.YEAR,
            Instant.parse("2026-03-15T03:05:00Z"),
            List.of(new MarketSeriesPointResponse(Instant.parse("2026-03-15T03:05:00Z"), new BigDecimal("1450.12000000")))
        );
        when(contentMarketSeriesCacheStore.find(MarketInstrumentCode.USD_KRW, MarketSeriesPeriod.YEAR)).thenReturn(Optional.of(cachedResponse));

        // When: 1년 시계열을 조회하면
        MarketSeriesResponse response = service.findSeries(MarketInstrumentCode.USD_KRW, MarketSeriesPeriod.YEAR, null, null);

        // Then: DB 조회 없이 캐시 응답이 반환되어야 한다.
        assertThat(response).isSameAs(cachedResponse);
        verifyNoInteractions(marketSnapshotRepository);
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
