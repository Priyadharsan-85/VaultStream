# Production readiness and go-live checklist

## Current decision

**No-go for customer funds or regulated financial use.** VaultStream currently provides a backend ledger prototype. It has no payment or settlement provider, verified identity/onboarding flow, or production operating environment. Repository hardening reduces deployment risk but does not establish financial, legal, or operational readiness.

Use this checklist to track the remaining work. Mark an item complete only after its evidence is recorded in the deployment or operations runbook.

## Repository safeguards already present

- [x] Production Compose profile separates the database network, mounts credentials as container secrets, and runs the app as a non-root user with a read-only filesystem.
- [x] Spring production profile disables schema auto-update and uses Flyway migrations.
- [x] Production Compose uses separate PostgreSQL bootstrap, migration, and runtime roles; only the database container receives the bootstrap superuser credential.
- [x] Production Compose runs Flyway in a separate one-shot container and withholds its database-owner credential from the long-running API container.
- [x] Public registration defaults to disabled in production; manual balance adjustments are administrator-only.
- [x] Manual deposits and withdrawals are disabled by default in the production profile.
- [x] Customer-to-customer transfers are disabled by default in the production profile.
- [x] WebSocket browser origins are explicitly configured, and Actuator endpoints other than basic health require an administrator.
- [x] OpenAPI and Swagger UI endpoints are disabled in the production profile.
- [x] CI builds and verifies the Java project, scans the container image, runs CodeQL, and reviews dependency changes on pull requests.
- [x] Backup script encrypts database dumps with age before writing them to the selected destination.

These checks confirm repository configuration only. They do not confirm the hosted environment or that a restore has succeeded.

## Required before any public deployment

### Hosting and network

- [ ] Select the hosting provider, region, production hostname, and data residency requirements.
- [ ] Provision a supported production host or managed container service; configure DNS and HTTPS with automatic certificate renewal.
- [ ] Put the service behind a reverse proxy or managed ingress with request size and timeout limits. Keep PostgreSQL private and restrict administrative access to approved operators.
- [ ] Configure host patching, firewall rules, SSH/MFA access, container image update policy, and a documented rollback procedure.
- [ ] Use immutable image tags tied to a commit or release. Do not use a mutable tag as the release identifier.

### Secrets and administrator access

- [ ] Create unique production database, JWT, and administrator credentials in the provider's secret manager or protected host files.
- [ ] For an already initialized database volume, apply and review the runtime/migration role grants before upgrading; the Compose initialization script only runs on a new empty volume.
- [ ] Ensure `.env`, secret files, database dumps, and JWT signing material are excluded from source control and ordinary logs.
- [ ] Establish and rehearse administrator credential rotation and JWT signing-key rotation. Restrict who can read or change production secrets.
- [ ] Confirm the production browser origin list contains only the deployed client origins.

### Database recovery

- [ ] Generate and protect an age private identity outside the application host; configure its public recipient for the backup job.
- [ ] Schedule `scripts/backup-postgres.sh` and copy each completed encrypted dump to access-controlled off-host storage.
- [ ] Define and enforce backup retention, encryption-key custody, recovery point objective (RPO), and recovery time objective (RTO).
- [ ] Restore a recent dump into an isolated PostgreSQL instance. Verify Flyway state, ledger balances, and application reads; record the restore date, duration, and result.
- [ ] Set up database capacity, disk-space, connection, and backup-failure alerts.

### Release controls and service operations

- [ ] Run `mvn --batch-mode --no-transfer-progress verify`, build the production image, and resolve all failing CI, CodeQL, and high/critical image findings.
- [ ] Protect `main` with required CI checks, review requirements, and restricted bypass permissions.
- [ ] Review and merge dependency updates individually after compatibility and migration review; confirm no unresolved critical/high security alerts remain.
- [ ] Configure central, access-controlled logs and actionable alerts for availability, authentication abuse, database health, latency, and resource exhaustion.
- [ ] Document incident response, security contact, on-call ownership, data-retention/deletion, privacy notices, and customer support procedures.
- [ ] Perform an external security review and load/recovery exercises against a production-like environment before launch.

## Required before customer onboarding or real funds

These need product, provider, legal, and operational decisions; repository configuration alone cannot complete them.

- [ ] Implement verified customer onboarding, account approval, secure recovery, and any required multi-factor authentication. Registration is currently disabled by default and there is no complete customer provisioning flow.
- [ ] Integrate a selected payment/settlement provider. Verify webhook signatures, enforce replay protection and idempotency, reconcile provider settlement reports, and define refund, reversal, dispute, and failure handling.
- [ ] For the planned India wallet model, obtain written legal/compliance classification of customer stored balances, transfers, and withdrawals against the [RBI PPI Master Directions](https://www.rbi.org.in/Scripts/NotificationUser.aspx/NotificationUser.aspx?Id=12156) and [payment-system rules](https://www.rbi.org.in/CommonPerson/english/scripts/FAQs.aspx?Id=420). Confirm whether VaultStream needs authorization or must operate through an authorized bank/PPI issuer before enabling customer funds.
- [ ] Remove manual deposits and withdrawals from customer-facing operational flows. Preserve tightly controlled adjustments with reason codes, immutable audit records, and dual approval.
- [ ] Have qualified accounting and security reviewers validate ledger semantics, reconciliation, concurrency behavior, data retention, and auditability against the intended product and jurisdiction.
- [ ] Establish applicable legal, licensing, privacy, consumer-protection, and financial-crime obligations with qualified counsel and compliance specialists before accepting funds.
- [ ] Run end-to-end payment, reconciliation, fraud, outage, duplicate-webhook, and disaster-recovery exercises using provider sandbox accounts before enabling production transactions.

## Deployment choices needed to close the environment-specific items

Record the selected provider and region, hostname/domain, payment/settlement provider, backup storage target, backup schedule and retention, and intended jurisdiction. Until those are chosen and the corresponding checklist evidence is recorded, keep the deployment in a non-customer-facing environment.
