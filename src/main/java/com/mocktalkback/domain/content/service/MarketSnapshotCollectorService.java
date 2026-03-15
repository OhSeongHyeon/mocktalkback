package com.mocktalkback.domain.content.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mocktalkback.domain.content.entity.MarketSnapshotEntity;
import com.mocktalkback.domain.content.repository.MarketSnapshotRepository;
import com.mocktalkback.domain.content.type.MarketInstrumentCode;
import com.mocktalkback.infra.market.ExternalMarketQuoteClient;
import com.mocktalkback.infra.market.MarketQuote;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class MarketSnapshotCollectorService {

    private final ExternalMarketQuoteClient externalMarketQuoteClient;
    private final MarketSnapshotRepository marketSnapshotRepository;

    @Transactional
    public void collectLatestSnapshots() {
        Map<MarketInstrumentCode, MarketQuote> collectedQuotes = new EnumMap<>(MarketInstrumentCode.class);
        for (MarketInstrumentCode instrumentCode : MarketInstrumentCode.rawTargets()) {
            externalMarketQuoteClient.fetchQuote(instrumentCode)
                .ifPresent(quote -> {
                    collectedQuotes.put(instrumentCode, quote);
                    saveSnapshotIfNeeded(quote);
                });
        }

        deriveGoldKrw(collectedQuotes);
    }

    public boolean hasAnySnapshot() {
        return marketSnapshotRepository.count() > 0L;
    }

    private void deriveGoldKrw(Map<MarketInstrumentCode, MarketQuote> collectedQuotes) {
        MarketQuote goldUsdQuote = collectedQuotes.get(MarketInstrumentCode.XAU_USD);
        MarketQuote usdKrwQuote = collectedQuotes.get(MarketInstrumentCode.USD_KRW);
        if (goldUsdQuote == null || usdKrwQuote == null) {
            return;
        }

        BigDecimal derivedPrice = goldUsdQuote.priceValue().multiply(usdKrwQuote.priceValue())
            .setScale(8, RoundingMode.HALF_UP);
        Instant observedAt = goldUsdQuote.observedAt().isAfter(usdKrwQuote.observedAt())
            ? goldUsdQuote.observedAt()
            : usdKrwQuote.observedAt();
        MarketQuote derivedQuote = new MarketQuote(
            MarketInstrumentCode.XAU_KRW,
            derivedPrice,
            observedAt,
            "DERIVED_YAHOO_FINANCE"
        );
        saveSnapshotIfNeeded(derivedQuote);
    }

    private void saveSnapshotIfNeeded(MarketQuote marketQuote) {
        if (marketSnapshotRepository.existsByInstrumentCodeAndObservedAtAndProviderName(
            marketQuote.instrumentCode(),
            marketQuote.observedAt(),
            marketQuote.providerName()
        )) {
            return;
        }

        Optional<MarketSnapshotEntity> latestSnapshot = marketSnapshotRepository.findFirstByInstrumentCodeOrderByObservedAtDesc(
            marketQuote.instrumentCode()
        );
        BigDecimal changeValue = latestSnapshot
            .map(snapshot -> marketQuote.priceValue().subtract(snapshot.getPriceValue()).setScale(8, RoundingMode.HALF_UP))
            .orElse(null);
        BigDecimal changeRate = latestSnapshot
            .filter(snapshot -> snapshot.getPriceValue().compareTo(BigDecimal.ZERO) > 0)
            .map(snapshot -> marketQuote.priceValue()
                .subtract(snapshot.getPriceValue())
                .divide(snapshot.getPriceValue(), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100L))
                .setScale(6, RoundingMode.HALF_UP))
            .orElse(null);

        MarketSnapshotEntity entity = MarketSnapshotEntity.create(
            marketQuote.instrumentCode(),
            marketQuote.providerName(),
            marketQuote.priceValue(),
            changeValue,
            changeRate,
            marketQuote.observedAt()
        );
        marketSnapshotRepository.save(entity);
        log.info("시세 스냅샷을 저장했습니다. instrument={}, observedAt={}", marketQuote.instrumentCode(), marketQuote.observedAt());
    }
}
