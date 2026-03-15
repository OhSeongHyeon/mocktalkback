package com.mocktalkback.domain.content.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final MarketSnapshotCommandService marketSnapshotCommandService;

    @Transactional
    public List<MarketSnapshotWriteResult> collectLatestSnapshots() {
        Map<MarketInstrumentCode, MarketQuote> collectedQuotes = new EnumMap<>(MarketInstrumentCode.class);
        List<MarketSnapshotWriteResult> writeResults = new ArrayList<>();
        for (MarketInstrumentCode instrumentCode : MarketInstrumentCode.rawTargets()) {
            externalMarketQuoteClient.fetchQuote(instrumentCode)
                .ifPresent(quote -> {
                    collectedQuotes.put(instrumentCode, quote);
                    writeResults.add(saveSnapshot(quote));
                });
        }

        deriveGoldKrw(collectedQuotes)
            .ifPresent(writeResults::add);
        return writeResults;
    }

    public boolean hasAnySnapshot() {
        return marketSnapshotRepository.count() > 0L;
    }

    private java.util.Optional<MarketSnapshotWriteResult> deriveGoldKrw(Map<MarketInstrumentCode, MarketQuote> collectedQuotes) {
        MarketQuote goldUsdQuote = collectedQuotes.get(MarketInstrumentCode.XAU_USD);
        MarketQuote usdKrwQuote = collectedQuotes.get(MarketInstrumentCode.USD_KRW);
        if (goldUsdQuote == null || usdKrwQuote == null) {
            return java.util.Optional.empty();
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
        return java.util.Optional.of(saveSnapshot(derivedQuote));
    }

    private MarketSnapshotWriteResult saveSnapshot(MarketQuote marketQuote) {
        MarketSnapshotWriteResult writeResult = marketSnapshotCommandService.upsert(
            marketQuote.instrumentCode(),
            marketQuote.providerName(),
            marketQuote.priceValue(),
            marketQuote.observedAt()
        );
        if (writeResult.status() == MarketSnapshotWriteStatus.CREATED) {
            log.info("시세 스냅샷을 저장했습니다. instrument={}, observedAt={}", marketQuote.instrumentCode(), marketQuote.observedAt());
        } else if (writeResult.status() == MarketSnapshotWriteStatus.UPDATED) {
            log.info("시세 스냅샷을 갱신했습니다. instrument={}, observedAt={}", marketQuote.instrumentCode(), marketQuote.observedAt());
        }
        return writeResult;
    }
}
