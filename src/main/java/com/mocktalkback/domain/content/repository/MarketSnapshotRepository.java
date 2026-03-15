package com.mocktalkback.domain.content.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.content.entity.MarketSnapshotEntity;
import com.mocktalkback.domain.content.type.MarketInstrumentCode;

public interface MarketSnapshotRepository extends JpaRepository<MarketSnapshotEntity, Long> {

    Optional<MarketSnapshotEntity> findFirstByInstrumentCodeAndObservedAt(
        MarketInstrumentCode instrumentCode,
        Instant observedAt
    );

    Optional<MarketSnapshotEntity> findFirstByInstrumentCodeOrderByObservedAtDesc(MarketInstrumentCode instrumentCode);

    Optional<MarketSnapshotEntity> findFirstByInstrumentCodeAndObservedAtLessThanOrderByObservedAtDesc(
        MarketInstrumentCode instrumentCode,
        Instant observedAt
    );

    List<MarketSnapshotEntity> findByInstrumentCodeAndObservedAtGreaterThanEqualAndObservedAtLessThanOrderByObservedAtAsc(
        MarketInstrumentCode instrumentCode,
        Instant startObservedAt,
        Instant endObservedAtExclusive
    );
}
