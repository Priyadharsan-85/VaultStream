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

## Verify the demo

After deployment, check:

- The `vaultstream-dashboard` static site URL opens the React dashboard and loads the synthetic sample data.
- `https://YOUR_RENDER_API_HOST/api/demo/overview` returns two synthetic accounts and their sample transactions without requiring a login. This route only returns the fixed demo records and supports `GET` only.
- `https://YOUR_RENDER_API_HOST/actuator/health/liveness` returns a healthy response.
- Render logs show Flyway migrations completed before the API started.
- The database contains only synthetic demo records.
- No customer transfers, deposits, withdrawals, or real payment integrations are enabled.

For a portfolio link, share the dashboard's public URL from Render. The API is hosted separately; its direct sample endpoint is available at `https://YOUR_RENDER_API_HOST/api/demo/overview`. Keep administrator credentials private and do not seed any real personal information.

Render's free service can sleep when idle and has limited compute. This is suitable only for a hobby demo; it provides no production availability guarantee. Keep backups of anything you need because this setup does not configure backups for the Neon free database.
