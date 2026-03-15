package com.mocktalkback.domain.content.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mocktalkback.domain.content.dto.MarketOverviewItemResponse;
import com.mocktalkback.domain.content.dto.MarketOverviewResponse;
import com.mocktalkback.domain.content.dto.MarketSeriesPointResponse;
import com.mocktalkback.domain.content.dto.MarketSeriesResponse;
import com.mocktalkback.domain.content.entity.MarketSnapshotEntity;
import com.mocktalkback.domain.content.repository.MarketSnapshotRepository;
import com.mocktalkback.domain.content.type.MarketInstrumentCode;
import com.mocktalkback.domain.content.type.MarketSeriesPeriod;

@Service
public class ContentMarketService {

    private final MarketSnapshotRepository marketSnapshotRepository;
    private final Clock clock;

    @Autowired
    public ContentMarketService(MarketSnapshotRepository marketSnapshotRepository) {
        this(marketSnapshotRepository, Clock.systemUTC());
    }

    ContentMarketService(
        MarketSnapshotRepository marketSnapshotRepository,
        Clock clock
    ) {
        this.marketSnapshotRepository = marketSnapshotRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MarketOverviewResponse findOverview() {
        List<MarketOverviewItemResponse> items = new ArrayList<>();
        Instant lastObservedAt = null;

        for (MarketInstrumentCode instrumentCode : MarketInstrumentCode.displayTargets()) {
            Optional<MarketSnapshotEntity> latestSnapshot = marketSnapshotRepository.findFirstByInstrumentCodeOrderByObservedAtDesc(instrumentCode);
            if (latestSnapshot.isEmpty()) {
                continue;
            }

            MarketSnapshotEntity snapshot = latestSnapshot.get();
            items.add(new MarketOverviewItemResponse(
                snapshot.getInstrumentCode(),
                snapshot.getInstrumentCode().getDisplayName(),
                snapshot.getMarketGroup(),
                snapshot.getBaseCurrency(),
                snapshot.getQuoteCurrency(),
                snapshot.getInstrumentCode().getUnitLabel(),
                snapshot.getPriceValue(),
                snapshot.getChangeValue(),
                snapshot.getChangeRate(),
                snapshot.getObservedAt()
            ));
            if (lastObservedAt == null || snapshot.getObservedAt().isAfter(lastObservedAt)) {
                lastObservedAt = snapshot.getObservedAt();
            }
        }

        items.sort(Comparator.comparingInt(item -> MarketInstrumentCode.displayTargets().indexOf(item.instrumentCode())));
        return new MarketOverviewResponse(lastObservedAt, items);
    }

    @Transactional(readOnly = true)
    public MarketSeriesResponse findSeries(MarketInstrumentCode instrumentCode, MarketSeriesPeriod period) {
        Instant start = clock.instant().minus(Duration.ofDays(period.getDays()));
        List<MarketSnapshotEntity> snapshots = marketSnapshotRepository
            .findByInstrumentCodeAndObservedAtGreaterThanEqualOrderByObservedAtAsc(instrumentCode, start);
        List<MarketSeriesPointResponse> points = snapshots.stream()
            .map(snapshot -> new MarketSeriesPointResponse(snapshot.getObservedAt(), snapshot.getPriceValue()))
            .toList();
        Instant lastObservedAt = snapshots.isEmpty() ? null : snapshots.get(snapshots.size() - 1).getObservedAt();

        return new MarketSeriesResponse(
            instrumentCode,
            instrumentCode.getDisplayName(),
            instrumentCode.getMarketGroup(),
            instrumentCode.getUnitLabel(),
            period,
            lastObservedAt,
            points
        );
    }
}
