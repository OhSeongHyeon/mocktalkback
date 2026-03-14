function resolveSummaryDirectory() {
  return (__ENV.K6_SUMMARY_DIR || 'perf/k6/results').replace(/\/$/, '');
}

function resolveSummaryStamp() {
  if (__ENV.K6_SUMMARY_STAMP) {
    return sanitizeToken(__ENV.K6_SUMMARY_STAMP);
  }
  return new Date().toISOString().replace(/[:.]/g, '-');
}

function sanitizeToken(value) {
  return String(value).trim().replace(/[^a-zA-Z0-9_-]/g, '-');
}

function resolveMetricValue(metric, key) {
  if (!metric || !metric.values || metric.values[key] === undefined || metric.values[key] === null) {
    return null;
  }
  return metric.values[key];
}

function formatMetricValue(value) {
  if (value === null) {
    return '-';
  }
  if (typeof value !== 'number') {
    return String(value);
  }
  if (Math.abs(value) >= 1000) {
    return value.toFixed(0);
  }
  if (Math.abs(value) >= 100) {
    return value.toFixed(2);
  }
  if (Math.abs(value) >= 1) {
    return value.toFixed(3);
  }
  return value.toFixed(4);
}

function formatTimestamp(isoString) {
  return isoString ? new Date(isoString).toISOString() : '-';
}

function renderMetricLine(metricName, metric) {
  const values = [
    `count=${formatMetricValue(resolveMetricValue(metric, 'count'))}`,
    `rate=${formatMetricValue(resolveMetricValue(metric, 'rate'))}`,
    `avg=${formatMetricValue(resolveMetricValue(metric, 'avg'))}`,
    `p(95)=${formatMetricValue(resolveMetricValue(metric, 'p(95)'))}`,
    `p(99)=${formatMetricValue(resolveMetricValue(metric, 'p(99)'))}`,
    `max=${formatMetricValue(resolveMetricValue(metric, 'max'))}`,
  ];
  return `- ${metricName}: ${values.join(', ')}`;
}

function renderThresholdLines(metrics) {
  const lines = [];
  Object.entries(metrics || {}).forEach(([metricName, metric]) => {
    if (!metric || !metric.thresholds) {
      return;
    }
    Object.entries(metric.thresholds).forEach(([thresholdName, threshold]) => {
      const status = threshold.ok ? 'PASS' : 'FAIL';
      lines.push(`- ${status} ${metricName} :: ${thresholdName}`);
    });
  });
  return lines;
}

function renderConsoleSummary(scriptName, data, baseName) {
  const metrics = data.metrics || {};
  const lines = [
    '',
    `k6 summary saved: ${baseName}.json, ${baseName}.md`,
    `script=${scriptName}`,
    `generated_at=${new Date().toISOString()}`,
    renderMetricLine('http_req_duration', metrics.http_req_duration),
    renderMetricLine('http_req_failed', metrics.http_req_failed),
  ];
  return `${lines.join('\n')}\n`;
}

function renderMarkdownSummary(scriptName, data) {
  const metrics = data.metrics || {};
  const topMetricNames = [
    'http_reqs',
    'http_req_failed',
    'http_req_duration',
    'iterations',
    'vus',
    'vus_max',
  ];
  const metricLines = topMetricNames
    .filter((metricName) => metrics[metricName])
    .map((metricName) => renderMetricLine(metricName, metrics[metricName]));
  const thresholdLines = renderThresholdLines(metrics);

  return [
    `# k6 Summary - ${scriptName}`,
    '',
    '## 실행 정보',
    '',
    `- generatedAt: ${new Date().toISOString()}`,
    `- setupDataKeys: ${Object.keys(data.setup_data || {}).join(', ') || '-'}`,
    '',
    '## 주요 메트릭',
    '',
    ...(metricLines.length > 0 ? metricLines : ['- 측정된 메트릭이 없습니다.']),
    '',
    '## 임계치 결과',
    '',
    ...(thresholdLines.length > 0 ? thresholdLines : ['- 정의된 threshold가 없습니다.']),
    '',
    '## 비고',
    '',
    '- 상세 원본 결과는 같은 이름의 JSON 파일을 확인합니다.',
    '',
  ].join('\n');
}

export function createSummaryHandler(scriptName) {
  const summaryDirectory = resolveSummaryDirectory();
  const stamp = resolveSummaryStamp();
  const tag = __ENV.K6_SUMMARY_TAG ? `-${sanitizeToken(__ENV.K6_SUMMARY_TAG)}` : '';
  const baseName = `${summaryDirectory}/${scriptName}${tag}-${stamp}`;

  return function handleSummary(data) {
    return {
      stdout: renderConsoleSummary(scriptName, data, baseName),
      [`${baseName}.json`]: JSON.stringify(data, null, 2),
      [`${baseName}.md`]: renderMarkdownSummary(scriptName, data),
    };
  };
}
