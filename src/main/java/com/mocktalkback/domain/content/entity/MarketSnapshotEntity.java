package com.mocktalkback.domain.content.entity;

import java.math.BigDecimal;
import java.time.Instant;

import com.mocktalkback.domain.content.type.MarketGroup;
import com.mocktalkback.domain.content.type.MarketInstrumentCode;
import com.mocktalkback.global.common.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "tb_market_snapshots",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_tb_market_snapshots_instrument_code_observed_at",
            columnNames = {"instrument_code", "observed_at"}
        )
    }
)
public class MarketSnapshotEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "market_snapshot_id", nullable = false)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "instrument_code", nullable = false, length = 32)
    private MarketInstrumentCode instrumentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "market_group", nullable = false, length = 16)
    private MarketGroup marketGroup;

    @Column(name = "provider_name", nullable = false, length = 32)
    private String providerName;

    @Column(name = "base_currency", nullable = false, length = 16)
    private String baseCurrency;

    @Column(name = "quote_currency", nullable = false, length = 16)
    private String quoteCurrency;

    @Column(name = "price_value", nullable = false, precision = 20, scale = 8)
    private BigDecimal priceValue;

    @Column(name = "change_value", precision = 20, scale = 8)
    private BigDecimal changeValue;

    @Column(name = "change_rate", precision = 12, scale = 6)
    private BigDecimal changeRate;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Builder
    private MarketSnapshotEntity(
        MarketInstrumentCode instrumentCode,
        MarketGroup marketGroup,
        String providerName,
        String baseCurrency,
        String quoteCurrency,
        BigDecimal priceValue,
        BigDecimal changeValue,
        BigDecimal changeRate,
        Instant observedAt
    ) {
        this.instrumentCode = instrumentCode;
        this.marketGroup = marketGroup;
        this.providerName = providerName;
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.priceValue = priceValue;
        this.changeValue = changeValue;
        this.changeRate = changeRate;
        this.observedAt = observedAt;
    }

    public static MarketSnapshotEntity create(
        MarketInstrumentCode instrumentCode,
        String providerName,
        BigDecimal priceValue,
        BigDecimal changeValue,
        BigDecimal changeRate,
        Instant observedAt
    ) {
        return MarketSnapshotEntity.builder()
            .instrumentCode(instrumentCode)
            .marketGroup(instrumentCode.getMarketGroup())
            .providerName(providerName)
            .baseCurrency(instrumentCode.getBaseCurrency())
            .quoteCurrency(instrumentCode.getQuoteCurrency())
            .priceValue(priceValue)
            .changeValue(changeValue)
            .changeRate(changeRate)
            .observedAt(observedAt)
            .build();
    }

    public void updateSnapshot(
        String providerName,
        BigDecimal priceValue,
        BigDecimal changeValue,
        BigDecimal changeRate
    ) {
        this.providerName = providerName;
        this.priceValue = priceValue;
        this.changeValue = changeValue;
        this.changeRate = changeRate;
    }
}
