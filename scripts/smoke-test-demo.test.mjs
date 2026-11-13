import assert from 'node:assert/strict';
import { execFile } from 'node:child_process';
import { createServer } from 'node:http';
import { promisify } from 'node:util';
import { fileURLToPath } from 'node:url';
import { test } from 'node:test';

const execFileAsync = promisify(execFile);
const repositoryRoot = fileURLToPath(new URL('..', import.meta.url));
const smokeCheckPath = fileURLToPath(new URL('./smoke-test-demo.mjs', import.meta.url));

test('deployment smoke check validates the demo and rejects a missing synthetic-data notice', async (t) => {
  let syntheticNotice = true;
  const server = createServer((request, response) => {
    if (request.url === '/') {
      response.writeHead(200, { 'content-type': 'text/html' });
      response.end('<!doctype html><div id="root"></div>');
      return;
    }
    if (request.url === '/actuator/health/liveness') {
      response.writeHead(200, { 'content-type': 'application/json' });
      response.end(JSON.stringify({ status: 'UP' }));
      return;
    }
    if (request.url === '/api/demo/overview') {
      response.writeHead(200, { 'content-type': 'application/json' });
      response.end(JSON.stringify({
        notice: syntheticNotice ? 'Synthetic portfolio data only.' : 'Sample data.',
        accounts: [{ id: 'one' }, { id: 'two' }],
        transactions: [],
      }));
      return;
    }
    if (request.url === '/api/demo/live') {
      response.writeHead(200, { 'content-type': 'text/event-stream' });
      response.write('id: event-1\nevent: ledger-event\ndata: {"id":"event-1","label":"Simulated transfer","transactions":[{}]}\n\n');
      return;
    }
    response.writeHead(404);
    response.end();
  });

  await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', resolve);
  });
  t.after(() => new Promise((resolve) => server.close(resolve)));

  const { port } = server.address();
  const localUrl = `http://127.0.0.1:${port}`;
  const args = [smokeCheckPath, '--api', localUrl, '--dashboard', localUrl];
  const success = await execFileAsync(process.execPath, args, { cwd: repositoryRoot, timeout: 10_000 });
  assert.match(success.stdout, /PASS dashboard page is reachable/);
  assert.match(success.stdout, /PASS API liveness is UP/);
  assert.match(success.stdout, /PASS demo overview returned 2 sample accounts/);
  assert.match(success.stdout, /PASS live feed emitted/);

  await assert.rejects(
    execFileAsync(process.execPath, [smokeCheckPath, '--api', localUrl, '--dashboard'], {
      cwd: repositoryRoot,
      timeout: 10_000,
    }),
    (error) => error.code === 1 && error.stderr.includes('Missing value for --dashboard'),
  );

  syntheticNotice = false;
  await assert.rejects(
    execFileAsync(process.execPath, args, { cwd: repositoryRoot, timeout: 10_000 }),
    (error) => error.code === 1 && error.stderr.includes('missing its synthetic-data notice'),
  );
});
