#!/usr/bin/env node

const timeoutMs = 12_000;
const [, , ...args] = process.argv;

function option(name) {
  const index = args.indexOf(name);
  if (index < 0) return undefined;
  const value = args[index + 1];
  if (!value || value.startsWith('--')) throw new Error(`Missing value for ${name}.`);
  return value;
}

function parseBaseUrl(value, label) {
  if (!value) throw new Error(`Missing required ${label} URL.`);
  const url = new URL(value);
  if (url.protocol !== 'https:' && !['localhost', '127.0.0.1'].includes(url.hostname)) {
    throw new Error(`${label} must use HTTPS outside localhost.`);
  }
  return url.toString().replace(/\/$/, '');
}

async function expectOk(url, label, options = {}) {
  const response = await fetch(url, { signal: AbortSignal.timeout(timeoutMs), ...options });
  if (!response.ok) throw new Error(`${label} returned HTTP ${response.status}.`);
  return response;
}

async function checkDashboard(baseUrl) {
  const response = await expectOk(baseUrl, 'Dashboard');
  const contentType = response.headers.get('content-type') ?? '';
  const html = await response.text();
  if (!contentType.includes('text/html') || !html.includes('<div id="root">')) {
    throw new Error('Dashboard did not return the expected React page.');
  }
  console.log('PASS dashboard page is reachable');
}

async function checkLiveness(apiUrl) {
  const response = await expectOk(`${apiUrl}/actuator/health/liveness`, 'Liveness probe');
  const result = await response.json();
  if (result.status !== 'UP') throw new Error('Liveness probe did not report UP.');
  console.log('PASS API liveness is UP');
}

async function checkOverview(apiUrl) {
  const response = await expectOk(`${apiUrl}/api/demo/overview`, 'Demo overview');
  const overview = await response.json();
  if (!overview.notice?.toLowerCase().includes('synthetic')) {
    throw new Error('Demo overview is missing its synthetic-data notice.');
  }
  if (!Array.isArray(overview.accounts) || overview.accounts.length !== 2) {
    throw new Error('Demo overview did not return the two expected sample accounts.');
  }
  if (!Array.isArray(overview.transactions)) {
    throw new Error('Demo overview did not return a transactions list.');
  }
  console.log(`PASS demo overview returned ${overview.accounts.length} sample accounts and ${overview.transactions.length} transactions`);
}

async function checkLiveFeed(apiUrl) {
  const response = await expectOk(`${apiUrl}/api/demo/live`, 'Live feed', {
    headers: { Accept: 'text/event-stream' },
  });
  if (!response.headers.get('content-type')?.includes('text/event-stream')) {
    throw new Error('Live feed did not return a server-sent event stream.');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';
  try {
    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      buffer += decoder.decode(value, { stream: true });
      const frames = buffer.split(/\r?\n\r?\n/);
      buffer = frames.pop() ?? '';
      for (const frame of frames) {
        const eventName = frame.match(/^event:\s*(.+)$/m)?.[1];
        const data = frame.match(/^data:\s*(.+)$/m)?.[1];
        if (eventName !== 'ledger-event' || !data) continue;
        const event = JSON.parse(data);
        if (!event.id || !event.label || !Array.isArray(event.transactions) || event.transactions.length === 0) {
          throw new Error('Live feed event is missing expected sample data.');
        }
        console.log(`PASS live feed emitted “${event.label}”`);
        return;
      }
    }
    throw new Error('Live feed closed before emitting an event.');
  } finally {
    await reader.cancel().catch(() => {});
  }
}

try {
  if (args.includes('--help')) {
    console.log('Usage: node scripts/smoke-test-demo.mjs --api https://API_HOST [--dashboard https://DASHBOARD_HOST]');
    process.exit(0);
  }

  const apiUrl = parseBaseUrl(option('--api'), 'API');
  const dashboardUrl = option('--dashboard') && parseBaseUrl(option('--dashboard'), 'Dashboard');
  if (dashboardUrl) await checkDashboard(dashboardUrl);
  await checkLiveness(apiUrl);
  await checkOverview(apiUrl);
  await checkLiveFeed(apiUrl);
  console.log('Demo deployment smoke check passed.');
} catch (error) {
  console.error(`FAIL ${error.message}`);
  process.exitCode = 1;
}
