#!/usr/bin/env python3
"""콘텐츠 시세 백필용 CSV 데이터셋 생성 스크립트."""

from __future__ import annotations

import argparse
import csv
import json
import sys
import urllib.parse
import urllib.request
from collections.abc import Iterable
from dataclasses import dataclass
from datetime import date, datetime, timedelta, timezone
from decimal import Decimal, ROUND_HALF_UP
from pathlib import Path


FRANKFURTER_BASE_URL = "https://api.frankfurter.dev/v1"
YAHOO_CHART_BASE_URL = "https://query1.finance.yahoo.com/v8/finance/chart"
DEFAULT_USER_AGENT = "mocktalk-content-market-backfill/1.0"
DECIMAL_PRICE = Decimal("0.00000001")


@dataclass(frozen=True)
class InstrumentDataset:
    """종목별 시세 레코드."""

    instrument_code: str
    observed_at: date
    price_value: Decimal


@dataclass(frozen=True)
class DbImportRow:
    """DB 직접 임포트용 시세 레코드."""

    instrument_code: str
    market_group: str
    provider_name: str
    base_currency: str
    quote_currency: str
    price_value: Decimal
    change_value: Decimal | None
    change_rate: Decimal | None
    observed_at: date


def parse_args() -> argparse.Namespace:
    """명령행 인자를 해석한다."""

    parser = argparse.ArgumentParser(
        description="콘텐츠 시세 백필용 통합/종목별 CSV 데이터셋을 생성합니다."
    )
    parser.add_argument(
        "--start-date",
        default="2016-03-15",
        help="백필 시작일 (YYYY-MM-DD)",
    )
    parser.add_argument(
        "--end-date",
        default=str(date.today()),
        help="백필 종료일 (YYYY-MM-DD)",
    )
    parser.add_argument(
        "--output-dir",
        default=str(
            Path(__file__).resolve().parents[2]
            / "docs"
            / "content-market"
            / "backfill"
            / "datasets"
        ),
        help="CSV 결과를 저장할 디렉터리",
    )
    parser.add_argument(
        "--timeout-seconds",
        type=int,
        default=30,
        help="외부 API 요청 timeout(초)",
    )
    parser.add_argument(
        "--user-agent",
        default=DEFAULT_USER_AGENT,
        help="외부 API 호출용 User-Agent",
    )
    return parser.parse_args()


def main() -> int:
    """메인 진입점."""

    args = parse_args()
    start_date = date.fromisoformat(args.start_date)
    end_date = date.fromisoformat(args.end_date)
    if start_date > end_date:
        raise ValueError("start-date는 end-date보다 늦을 수 없습니다.")

    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    usd_krw = fetch_frankfurter_series(
        base_currency="USD",
        quote_currency="KRW",
        start_date=start_date,
        end_date=end_date,
        timeout_seconds=args.timeout_seconds,
        user_agent=args.user_agent,
    )
    eur_krw = fetch_frankfurter_series(
        base_currency="EUR",
        quote_currency="KRW",
        start_date=start_date,
        end_date=end_date,
        timeout_seconds=args.timeout_seconds,
        user_agent=args.user_agent,
    )
    jpy_krw = fetch_frankfurter_series(
        base_currency="JPY",
        quote_currency="KRW",
        start_date=start_date,
        end_date=end_date,
        timeout_seconds=args.timeout_seconds,
        user_agent=args.user_agent,
    )
    xau_usd = fetch_yahoo_daily_series(
        symbol="GC=F",
        start_date=start_date,
        end_date=end_date,
        timeout_seconds=args.timeout_seconds,
        user_agent=args.user_agent,
    )
    xau_krw = derive_gold_krw_series(xau_usd=xau_usd, usd_krw=usd_krw)

    all_rows = build_unified_rows(
        usd_krw=usd_krw,
        eur_krw=eur_krw,
        jpy_krw=jpy_krw,
        xau_usd=xau_usd,
        xau_krw=xau_krw,
    )

    write_unified_csv(output_dir, start_date, end_date, all_rows)
    db_rows = build_db_import_rows(all_rows)
    write_db_import_csv(output_dir, start_date, end_date, db_rows)
    write_instrument_csv(output_dir, "USD_KRW", usd_krw)
    write_instrument_csv(output_dir, "EUR_KRW", eur_krw)
    write_instrument_csv(output_dir, "JPY_KRW", jpy_krw)
    write_instrument_csv(output_dir, "XAU_USD", xau_usd)
    write_instrument_csv(output_dir, "XAU_KRW", xau_krw)
    write_summary_markdown(
        output_dir=output_dir,
        start_date=start_date,
        end_date=end_date,
        unified_rows=len(all_rows),
        db_rows=len(db_rows),
        usd_krw=usd_krw,
        eur_krw=eur_krw,
        jpy_krw=jpy_krw,
        xau_usd=xau_usd,
        xau_krw=xau_krw,
    )

    print(f"데이터셋 생성 완료: {output_dir}")
    return 0


def fetch_frankfurter_series(
    base_currency: str,
    quote_currency: str,
    start_date: date,
    end_date: date,
    timeout_seconds: int,
    user_agent: str,
) -> dict[date, Decimal]:
    """Frankfurter에서 일간 환율 시계열을 조회한다."""

    url = (
        f"{FRANKFURTER_BASE_URL}/{start_date.isoformat()}..{end_date.isoformat()}"
        f"?base={base_currency}&symbols={quote_currency}"
    )
    payload = fetch_json(url=url, timeout_seconds=timeout_seconds, user_agent=user_agent)
    result: dict[date, Decimal] = {}
    for observed_at, rates in payload.get("rates", {}).items():
        if quote_currency not in rates:
            continue
        result[date.fromisoformat(observed_at)] = normalize_price(rates[quote_currency])
    return result


def fetch_yahoo_daily_series(
    symbol: str,
    start_date: date,
    end_date: date,
    timeout_seconds: int,
    user_agent: str,
) -> dict[date, Decimal]:
    """Yahoo Finance chart API에서 일간 금 선물 시계열을 조회한다."""

    period_start = datetime.combine(start_date, datetime.min.time(), tzinfo=timezone.utc)
    period_end = datetime.combine(end_date + timedelta(days=1), datetime.min.time(), tzinfo=timezone.utc)
    encoded_symbol = urllib.parse.quote(symbol, safe="")
    url = (
        f"{YAHOO_CHART_BASE_URL}/{encoded_symbol}"
        f"?interval=1d&period1={int(period_start.timestamp())}&period2={int(period_end.timestamp())}"
    )
    payload = fetch_json(url=url, timeout_seconds=timeout_seconds, user_agent=user_agent)

    chart = payload.get("chart", {})
    if chart.get("error") is not None:
        raise RuntimeError(f"Yahoo Finance 응답 오류: {chart['error']}")

    results = chart.get("result") or []
    if not results:
        raise RuntimeError("Yahoo Finance 응답에 result가 없습니다.")

    first_result = results[0]
    timestamps = first_result.get("timestamp") or []
    quote_nodes = first_result.get("indicators", {}).get("quote") or []
    if not quote_nodes:
        raise RuntimeError("Yahoo Finance 응답에 quote 데이터가 없습니다.")
    closes = quote_nodes[0].get("close") or []

    result: dict[date, Decimal] = {}
    for timestamp_value, close_value in zip(timestamps, closes):
        if close_value is None:
            continue
        observed_at = datetime.fromtimestamp(timestamp_value, tz=timezone.utc).date()
        result[observed_at] = normalize_price(close_value)
    return result


def derive_gold_krw_series(
    xau_usd: dict[date, Decimal],
    usd_krw: dict[date, Decimal],
) -> dict[date, Decimal]:
    """금 시세(USD)에 가장 가까운 직전 USD/KRW를 곱해 금 시세(KRW)를 만든다."""

    usd_dates = sorted(usd_krw.keys())
    result: dict[date, Decimal] = {}
    current_index = 0

    for observed_at in sorted(xau_usd.keys()):
        while current_index + 1 < len(usd_dates) and usd_dates[current_index + 1] <= observed_at:
            current_index += 1

        if not usd_dates:
            continue

        matched_date = usd_dates[current_index]
        if matched_date > observed_at:
            continue

        result[observed_at] = normalize_price(xau_usd[observed_at] * usd_krw[matched_date])
    return result


def build_unified_rows(
    usd_krw: dict[date, Decimal],
    eur_krw: dict[date, Decimal],
    jpy_krw: dict[date, Decimal],
    xau_usd: dict[date, Decimal],
    xau_krw: dict[date, Decimal],
) -> list[InstrumentDataset]:
    """통합 CSV용 레코드를 날짜 우선 순서로 구성한다."""

    rows: list[InstrumentDataset] = []
    instrument_maps = {
        "USD_KRW": usd_krw,
        "EUR_KRW": eur_krw,
        "JPY_KRW": jpy_krw,
        "XAU_USD": xau_usd,
        "XAU_KRW": xau_krw,
    }

    all_dates = sorted({*usd_krw.keys(), *eur_krw.keys(), *jpy_krw.keys(), *xau_usd.keys(), *xau_krw.keys()})
    ordered_instruments = ["USD_KRW", "EUR_KRW", "JPY_KRW", "XAU_USD", "XAU_KRW"]

    for observed_at in all_dates:
        for instrument_code in ordered_instruments:
            price_value = instrument_maps[instrument_code].get(observed_at)
            if price_value is None:
                continue
            rows.append(InstrumentDataset(instrument_code, observed_at, price_value))
    return rows


def build_db_import_rows(rows: list[InstrumentDataset]) -> list[DbImportRow]:
    """테이블 직접 임포트용 레코드를 구성한다."""

    instrument_meta = {
        "USD_KRW": ("FX", "BACKFILL_FRANKFURTER", "USD", "KRW"),
        "EUR_KRW": ("FX", "BACKFILL_FRANKFURTER", "EUR", "KRW"),
        "JPY_KRW": ("FX", "BACKFILL_FRANKFURTER", "JPY", "KRW"),
        "XAU_USD": ("METAL", "BACKFILL_YAHOO_FINANCE", "XAU", "USD"),
        "XAU_KRW": ("METAL", "BACKFILL_DERIVED", "XAU", "KRW"),
    }
    previous_prices: dict[str, Decimal] = {}
    result: list[DbImportRow] = []

    for row in rows:
        market_group, provider_name, base_currency, quote_currency = instrument_meta[row.instrument_code]
        previous_price = previous_prices.get(row.instrument_code)
        if previous_price is None:
            change_value = None
            change_rate = None
        else:
            change_value = normalize_price(row.price_value - previous_price)
            if previous_price == Decimal("0"):
                change_rate = None
            else:
                change_rate = (
                    ((row.price_value - previous_price) / previous_price) * Decimal("100")
                ).quantize(Decimal("0.000001"), rounding=ROUND_HALF_UP)
        result.append(
            DbImportRow(
                instrument_code=row.instrument_code,
                market_group=market_group,
                provider_name=provider_name,
                base_currency=base_currency,
                quote_currency=quote_currency,
                price_value=row.price_value,
                change_value=change_value,
                change_rate=change_rate,
                observed_at=row.observed_at,
            )
        )
        previous_prices[row.instrument_code] = row.price_value
    return result


def write_unified_csv(
    output_dir: Path,
    start_date: date,
    end_date: date,
    rows: Iterable[InstrumentDataset],
) -> None:
    """통합 CSV를 저장한다."""

    target = output_dir / f"content_market_unified_{start_date.isoformat()}_{end_date.isoformat()}.csv"
    with target.open("w", encoding="utf-8", newline="") as file:
        writer = csv.writer(file)
        writer.writerow(["instrument_code", "observed_at", "price_value"])
        for row in rows:
            writer.writerow([row.instrument_code, row.observed_at.isoformat(), format_decimal(row.price_value)])


def write_db_import_csv(
    output_dir: Path,
    start_date: date,
    end_date: date,
    rows: Iterable[DbImportRow],
) -> None:
    """DB 직접 임포트용 통합 CSV를 저장한다."""

    target = output_dir / f"tb_market_snapshots_import_{start_date.isoformat()}_{end_date.isoformat()}.csv"
    with target.open("w", encoding="utf-8", newline="") as file:
        writer = csv.writer(file)
        writer.writerow(
            [
                "instrument_code",
                "market_group",
                "provider_name",
                "base_currency",
                "quote_currency",
                "price_value",
                "change_value",
                "change_rate",
                "observed_at",
            ]
        )
        for row in rows:
            writer.writerow(
                [
                    row.instrument_code,
                    row.market_group,
                    row.provider_name,
                    row.base_currency,
                    row.quote_currency,
                    format_decimal(row.price_value),
                    "" if row.change_value is None else format_decimal(row.change_value),
                    "" if row.change_rate is None else format(row.change_rate, "f"),
                    row.observed_at.isoformat() + "T00:00:00Z",
                ]
            )


def write_instrument_csv(output_dir: Path, instrument_code: str, series: dict[date, Decimal]) -> None:
    """종목별 CSV를 저장한다."""

    target = output_dir / f"{instrument_code.lower()}.csv"
    with target.open("w", encoding="utf-8", newline="") as file:
        writer = csv.writer(file)
        writer.writerow(["observed_at", "price_value"])
        for observed_at in sorted(series.keys()):
            writer.writerow([observed_at.isoformat(), format_decimal(series[observed_at])])


def write_summary_markdown(
    output_dir: Path,
    start_date: date,
    end_date: date,
    unified_rows: int,
    db_rows: int,
    usd_krw: dict[date, Decimal],
    eur_krw: dict[date, Decimal],
    jpy_krw: dict[date, Decimal],
    xau_usd: dict[date, Decimal],
    xau_krw: dict[date, Decimal],
) -> None:
    """생성 결과 요약 파일을 저장한다."""

    summary = output_dir / "generation-summary.md"
    lines = [
        "# 생성 요약",
        "",
        f"- 생성 범위: `{start_date.isoformat()} ~ {end_date.isoformat()}`",
        "- 통합 파일: `instrument_code, observed_at, price_value`",
        "- 종목별 파일: `observed_at, price_value`",
        "",
        "## 종목별 row 수",
        "",
        f"- 통합 백오피스 CSV row 수: {unified_rows}",
        f"- DB 직접 임포트 CSV row 수: {db_rows}",
        "",
        f"- `USD_KRW`: {len(usd_krw)}",
        f"- `EUR_KRW`: {len(eur_krw)}",
        f"- `JPY_KRW`: {len(jpy_krw)}",
        f"- `XAU_USD`: {len(xau_usd)}",
        f"- `XAU_KRW`: {len(xau_krw)}",
        "",
        "## 파생 규칙",
        "",
        "- `XAU_KRW`는 같은 날짜의 `XAU_USD`에 대해 가장 가까운 직전 `USD_KRW`를 곱해 계산합니다.",
        "",
    ]
    summary.write_text("\n".join(lines), encoding="utf-8")


def fetch_json(url: str, timeout_seconds: int, user_agent: str) -> dict:
    """GET 요청으로 JSON을 조회한다."""

    request = urllib.request.Request(url, headers={"User-Agent": user_agent})
    with urllib.request.urlopen(request, timeout=timeout_seconds) as response:
        return json.load(response)


def normalize_price(value: object) -> Decimal:
    """가격 값을 공통 scale로 정규화한다."""

    return Decimal(str(value)).quantize(DECIMAL_PRICE, rounding=ROUND_HALF_UP)


def format_decimal(value: Decimal) -> str:
    """CSV 저장용 문자열로 변환한다."""

    return format(value, "f")


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exception:  # pragma: no cover
        print(f"[오류] {exception}", file=sys.stderr)
        raise
