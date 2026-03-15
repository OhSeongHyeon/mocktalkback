package com.mocktalkback.domain.content.service;

import java.util.List;

import com.mocktalkback.domain.content.dto.AdminMarketImportFailureResponse;

record MarketSnapshotImportParsedResult(
    int totalCount,
    List<MarketSnapshotImportRow> rows,
    List<AdminMarketImportFailureResponse> failures
) {
}
