package com.mocktalkback.domain.content.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mocktalkback.domain.content.entity.MarketSnapshotEntity;
import com.mocktalkback.domain.content.type.MarketInstrumentCode;

public interface MarketSnapshotRepository extends JpaRepository<MarketSnapshotEntity, Long> {

    boolean existsByInstrumentCodeAndObservedAtAndProviderName(
        MarketInstrumentCode instrumentCode,
        Instant observedAt,
        String providerName
    );

    Optional<MarketSnapshotEntity> findFirstByInstrumentCodeOrderByObservedAtDesc(MarketInstrumentCode instrumentCode);

    List<MarketSnapshotEntity> findByInstrumentCodeAndObservedAtGreaterThanEqualOrderByObservedAtAsc(
        MarketInstrumentCode instrumentCode,
        Instant observedAt
    );
}
