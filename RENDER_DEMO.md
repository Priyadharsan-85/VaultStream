# Render demo deployment

This setup is for a public **demo with synthetic data only**. It is not approved for customer information, customer funds, or financial production use. The Render profile runs Flyway at application startup using the demo database credentials; do not reuse this arrangement for a real-money deployment.

## Create the database

1. Create a Neon PostgreSQL project in Singapore.
2. Keep the generated database username and password private.
3. Build the JDBC URL in this form, using the host and database name from Neon:

   ```text
   jdbc:postgresql://YOUR_NEON_HOST/YOUR_DATABASE?sslmode=require
   ```

   Enter the URL, username, and password as Render environment variables. Do not commit them to GitHub or paste them into chat.

## Create the Render service

Create a Render **Blueprint** from this repository's `main` branch. Render reads [`render.yaml`](render.yaml) and creates a Docker API service in Singapore plus a separate static React dashboard. The API keeps the money-related feature flags disabled and uses the liveness endpoint for health checks. The Blueprint connects the two public URLs and limits browser API access to the dashboard origin.

Fill the values marked as private in the Render setup form:

| Variable | Value |
|---|---|
| `SPRING_DATASOURCE_URL` | Neon JDBC URL shown above |
| `SPRING_DATASOURCE_USERNAME` | Neon demo database username |
| `SPRING_DATASOURCE_PASSWORD` | Neon demo database password |
| `JWT_SECRET` | A newly generated random value of at least 64 ASCII characters |
| `VAULTSTREAM_ADMIN_USERNAME` | A demo-only administrator name |
| `VAULTSTREAM_ADMIN_PASSWORD` | A unique 12–72 character demo-only password |

The Render platform supplies the API port and public service URL. Keep `VAULTSTREAM_REGISTRATION_ENABLED`, `VAULTSTREAM_LEDGER_MANUAL_ADJUSTMENTS_ENABLED`, and `VAULTSTREAM_CUSTOMER_TRANSFERS_ENABLED` set to `false`.

## Free-tier limits and cost control

The Blueprint selects Render's Free plan for the API; the static dashboard is also free. Render free web services spin down after 15 minutes without inbound traffic and can take about a minute to start again. Render currently includes 750 free web-service instance hours per workspace per month, along with monthly outbound-bandwidth and build-pipeline allowances. See [Render's current free-plan limits](https://render.com/docs/free) before deploying, since quotas can change.

To avoid overage charges, do not add a payment method to Render. If you exceed the free web-service hours or outbound bandwidth, Render may suspend free services for the rest of the month; exhausting build-pipeline minutes prevents new builds until the allowance resets. The Blueprint uses Neon rather than Render Postgres because Render's free Postgres databases expire after 30 days. Neon has separate plan limits, so confirm its current [free-plan terms](https://neon.tech/pricing) and region availability when creating the database. Keep the API and database in Singapore to reduce latency.

## Verify the demo

After deployment, check:

- The `vaultstream-dashboard` static site URL opens the React dashboard and loads the synthetic sample data.
- `https://YOUR_RENDER_API_HOST/api/demo/overview` returns two synthetic accounts and their sample transactions without requiring a login. This route only returns the fixed demo records and supports `GET` only.
- `https://YOUR_RENDER_API_HOST/actuator/health/liveness` returns a healthy response.
- Render logs show Flyway migrations completed before the API started.
- The database contains only synthetic demo records.
- No customer transfers, deposits, withdrawals, or real payment integrations are enabled.

For a portfolio link, share the dashboard's public URL from Render. The API is hosted separately; its direct sample endpoint is available at `https://YOUR_RENDER_API_HOST/api/demo/overview`. Keep administrator credentials private and do not seed any real personal information.

To check the deployed services from the project folder, run the smoke check with the API and dashboard URLs shown in Render:

```powershell
node scripts/smoke-test-demo.mjs `
  --api https://YOUR_RENDER_API_HOST `
  --dashboard https://YOUR_RENDER_DASHBOARD_HOST
```

It checks the dashboard response, API liveness, synthetic overview payload, and that the simulated feed emits an event. The live-feed check waits for its first event, so allow a few seconds for it to finish.

This free setup is suitable only for a hobby demo; it provides no production availability guarantee. Keep backups of anything you need because this setup does not configure backups for the Neon free database.
