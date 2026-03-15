package com.mocktalkback.domain.content.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mocktalkback.domain.content.entity.MarketSnapshotEntity;
import com.mocktalkback.domain.content.repository.MarketSnapshotRepository;
import com.mocktalkback.domain.content.type.MarketInstrumentCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MarketSnapshotCommandService {

    private final MarketSnapshotRepository marketSnapshotRepository;

    @Transactional
    public MarketSnapshotWriteResult upsert(
        MarketInstrumentCode instrumentCode,
        String providerName,
        BigDecimal priceValue,
        Instant observedAt
    ) {
        Optional<MarketSnapshotEntity> existingSnapshot = marketSnapshotRepository.findFirstByInstrumentCodeAndObservedAt(
            instrumentCode,
            observedAt
        );
        Optional<MarketSnapshotEntity> previousSnapshot = marketSnapshotRepository
            .findFirstByInstrumentCodeAndObservedAtLessThanOrderByObservedAtDesc(instrumentCode, observedAt);

        BigDecimal changeValue = previousSnapshot
            .map(snapshot -> priceValue.subtract(snapshot.getPriceValue()).setScale(8, RoundingMode.HALF_UP))
            .orElse(null);
        BigDecimal changeRate = previousSnapshot
            .map(snapshot -> calculateChangeRate(priceValue, snapshot.getPriceValue()))
            .orElse(null);

        if (existingSnapshot.isPresent()) {
            MarketSnapshotEntity entity = existingSnapshot.get();
            if (entity.getPriceValue().compareTo(priceValue) == 0
                && equalsNullable(entity.getChangeValue(), changeValue)
                && equalsNullable(entity.getChangeRate(), changeRate)
                && entity.getProviderName().equals(providerName)) {
                return new MarketSnapshotWriteResult(instrumentCode, observedAt, MarketSnapshotWriteStatus.SKIPPED);
            }

            entity.updateSnapshot(providerName, priceValue, changeValue, changeRate);
            return new MarketSnapshotWriteResult(instrumentCode, observedAt, MarketSnapshotWriteStatus.UPDATED);
        }

        MarketSnapshotEntity entity = MarketSnapshotEntity.create(
            instrumentCode,
            providerName,
            priceValue,
            changeValue,
            changeRate,
            observedAt
        );
        marketSnapshotRepository.save(entity);
        return new MarketSnapshotWriteResult(instrumentCode, observedAt, MarketSnapshotWriteStatus.CREATED);
    }

    @Transactional
    public void recalculateChanges(Collection<MarketInstrumentCode> instrumentCodes) {
        for (MarketInstrumentCode instrumentCode : instrumentCodes) {
            recalculateChanges(instrumentCode);
        }
    }

    @Transactional
    public void recalculateChanges(MarketInstrumentCode instrumentCode) {
        List<MarketSnapshotEntity> snapshots = marketSnapshotRepository.findByInstrumentCodeOrderByObservedAtAsc(instrumentCode);
        MarketSnapshotEntity previousSnapshot = null;
        for (MarketSnapshotEntity snapshot : snapshots) {
            BigDecimal changeValue = previousSnapshot == null
                ? null
                : snapshot.getPriceValue()
                    .subtract(previousSnapshot.getPriceValue())
                    .setScale(8, RoundingMode.HALF_UP);
            BigDecimal changeRate = previousSnapshot == null
                ? null
                : calculateChangeRate(snapshot.getPriceValue(), previousSnapshot.getPriceValue());

            if (!equalsNullable(snapshot.getChangeValue(), changeValue)
                || !equalsNullable(snapshot.getChangeRate(), changeRate)) {
                snapshot.updateSnapshot(
                    snapshot.getProviderName(),
                    snapshot.getPriceValue(),
                    changeValue,
                    changeRate
                );
            }
            previousSnapshot = snapshot;
        }
    }

    private BigDecimal calculateChangeRate(BigDecimal currentPrice, BigDecimal previousPrice) {
        if (previousPrice.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return currentPrice
            .subtract(previousPrice)
            .divide(previousPrice, 6, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100L))
            .setScale(6, RoundingMode.HALF_UP);
    }

    private boolean equalsNullable(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return left.compareTo(right) == 0;
    }
}
