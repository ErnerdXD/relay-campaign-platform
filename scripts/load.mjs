import { readFileSync, unlinkSync, writeFileSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const rows = Number(process.argv[2] ?? 10_000);
const baseUrl = process.env.BASE_URL ?? 'http://localhost:8081/api';
const token = process.env.TENANT_TOKEN ?? 'demo-northstar-local';
const tenantId = process.env.TENANT_ID ?? 'northstar';
const path = join(tmpdir(), `relay-load-${Date.now()}.csv`);

const lines = ['recipientId,email,phone,timezone'];
for (let index = 0; index < rows; index++) {
  lines.push(`load-${index},load-${index}@example.com,,`);
}
writeFileSync(path, lines.join('\n'));

const form = new FormData();
form.append('tenantId', tenantId);
form.append('name', `Load test ${rows}`);
form.append('channel', 'EMAIL');
form.append('template', 'Hello {{recipientId}}');
form.append('scheduledAt', new Date(Date.now() + 7 * 86_400_000).toISOString());
form.append('csv', new Blob([readFileSync(path)], { type: 'text/csv' }), 'load.csv');

const started = performance.now();
const response = await fetch(`${baseUrl}/campaigns`, {
  method: 'POST',
  headers: { Authorization: `Bearer ${token}` },
  body: form,
});
const seconds = (performance.now() - started) / 1000;
const body = await response.json().catch(() => ({}));

unlinkSync(path);

console.log(
  JSON.stringify(
    {
      rows,
      status: response.status,
      seconds: Number(seconds.toFixed(2)),
      rowsPerSecond: Math.round(rows / seconds),
      body,
    },
    null,
    2,
  ),
);

if (!response.ok) {
  process.exitCode = 1;
}
