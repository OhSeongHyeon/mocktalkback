package com.mocktalkback.domain.content.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
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
            .filter(snapshot -> snapshot.getPriceValue().compareTo(BigDecimal.ZERO) > 0)
            .map(snapshot -> priceValue
                .subtract(snapshot.getPriceValue())
                .divide(snapshot.getPriceValue(), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100L))
                .setScale(6, RoundingMode.HALF_UP))
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
