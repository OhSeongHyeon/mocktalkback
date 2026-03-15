package com.mocktalkback.domain.content.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mocktalkback.domain.content.entity.MarketSnapshotEntity;
import com.mocktalkback.domain.content.repository.MarketSnapshotRepository;
import com.mocktalkback.domain.content.type.MarketInstrumentCode;

@ExtendWith(MockitoExtension.class)
class MarketSnapshotCommandServiceTest {

    @Mock
    private MarketSnapshotRepository marketSnapshotRepository;

    // 변동률 재계산은 중간 날짜 백필 이후 뒤쪽 스냅샷도 직전 값 기준으로 다시 계산해야 한다.
    @Test
    void recalculateChanges_recomputes_following_snapshots_after_backfill() {
        // Given: 중간 날짜 데이터가 추가되었지만 마지막 스냅샷의 변동률이 이전 기준으로 남아 있다.
        MarketSnapshotCommandService service = new MarketSnapshotCommandService(marketSnapshotRepository);
        MarketSnapshotEntity firstSnapshot = createSnapshot(
            MarketInstrumentCode.USD_KRW,
            new BigDecimal("100.00000000"),
            null,
            null,
            Instant.parse("2026-03-10T00:00:00Z")
        );
        MarketSnapshotEntity secondSnapshot = createSnapshot(
            MarketInstrumentCode.USD_KRW,
            new BigDecimal("110.00000000"),
            new BigDecimal("10.00000000"),
            new BigDecimal("10.000000"),
            Instant.parse("2026-03-11T00:00:00Z")
        );
        MarketSnapshotEntity thirdSnapshot = createSnapshot(
            MarketInstrumentCode.USD_KRW,
            new BigDecimal("120.00000000"),
            new BigDecimal("20.00000000"),
            new BigDecimal("20.000000"),
            Instant.parse("2026-03-12T00:00:00Z")
        );
        when(marketSnapshotRepository.findByInstrumentCodeOrderByObservedAtAsc(MarketInstrumentCode.USD_KRW))
            .thenReturn(List.of(firstSnapshot, secondSnapshot, thirdSnapshot));

        // When: 종목 전체 변동률을 시간순으로 다시 계산하면
        service.recalculateChanges(MarketInstrumentCode.USD_KRW);

        // Then: 마지막 스냅샷은 직전 날짜 기준 값으로 보정되어야 한다.
        assertThat(firstSnapshot.getChangeValue()).isNull();
        assertThat(firstSnapshot.getChangeRate()).isNull();
        assertThat(secondSnapshot.getChangeValue()).isEqualByComparingTo("10.00000000");
        assertThat(secondSnapshot.getChangeRate()).isEqualByComparingTo("10.000000");
        assertThat(thirdSnapshot.getChangeValue()).isEqualByComparingTo("10.00000000");
        assertThat(thirdSnapshot.getChangeRate()).isEqualByComparingTo("9.090900");
    }

    private MarketSnapshotEntity createSnapshot(
        MarketInstrumentCode instrumentCode,
        BigDecimal priceValue,
        BigDecimal changeValue,
        BigDecimal changeRate,
        Instant observedAt
    ) {
        return MarketSnapshotEntity.create(
            instrumentCode,
            "TEST_PROVIDER",
            priceValue,
            changeValue,
            changeRate,
            observedAt
        );
    }
}
