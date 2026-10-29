import { useEffect, useMemo, useState, type ReactNode } from 'react';
import { demoOverviewUrl, getDemoOverview } from './api';
import type { DemoAccount, DemoOverview, DemoTransaction } from './types';

type IconName = 'overview' | 'accounts' | 'activity' | 'ledger' | 'help' | 'arrow' | 'refresh' | 'external' | 'search' | 'filter' | 'chevron' | 'dots' | 'download' | 'clock' | 'shield' | 'wallet' | 'transfer' | 'layers';
type HistoryPoint = { date: Date; balance: number };
type Filter = 'all' | 'incoming' | 'outgoing';
type Period = '7d' | '30d' | 'all';

const iconContent: Record<IconName, ReactNode> = {
  overview: <><rect x="3.5" y="3.5" width="7" height="7" rx="1.5" /><rect x="13.5" y="3.5" width="7" height="7" rx="1.5" /><rect x="3.5" y="13.5" width="7" height="7" rx="1.5" /><rect x="13.5" y="13.5" width="7" height="7" rx="1.5" /></>,
  accounts: <><rect x="3" y="5" width="18" height="14" rx="3" /><path d="M3 10h18M7 15h4" /></>,
  activity: <><path d="M4 14h3l2-7 4 11 2-7h5" /><path d="M4 4h16" /></>,
  ledger: <><path d="M4 5.5 12 2l8 3.5-8 3.5-8-3.5Z" /><path d="m4 12 8 3.5 8-3.5M4 18l8 3.5 8-3.5" /></>,
  help: <><circle cx="12" cy="12" r="9" /><path d="M9.8 9a2.3 2.3 0 1 1 4.1 1.4c-1 1.1-1.9 1.3-1.9 3M12 17h.01" /></>,
  arrow: <><path d="M5 12h14M13 6l6 6-6 6" /></>,
  refresh: <><path d="M20 7v5h-5M4 17v-5h5" /><path d="M5.5 9a7 7 0 0 1 11.6-2.2L20 12M4 12l2.9 5.2A7 7 0 0 0 18.5 15" /></>,
  external: <><path d="M14 4h6v6M20 4l-9 9" /><path d="M18 13v5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h5" /></>,
  search: <><circle cx="10.8" cy="10.8" r="6.8" /><path d="m16 16 4.5 4.5" /></>,
  filter: <><path d="M4 7h16M7 12h10m-7 5h4" /><circle cx="8" cy="7" r="1.5" /><circle cx="15" cy="12" r="1.5" /></>,
  chevron: <path d="m7 10 5 5 5-5" />,
  dots: <><circle cx="5" cy="12" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" /></>,
  download: <><path d="M12 3v12m0 0 4-4m-4 4-4-4" /><path d="M5 17v4h14v-4" /></>,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
  shield: <><path d="M12 3 20 6v5c0 5-3.4 8.5-8 10-4.6-1.5-8-5-8-10V6l8-3Z" /><path d="m9 12 2 2 4-4" /></>,
  wallet: <><rect x="3" y="5" width="18" height="15" rx="3" /><path d="M3 9h18M16 14h2" /></>,
  transfer: <><path d="M4 8h15l-3-3m4 11H5l3 3" /></>,
  layers: <><path d="m12 3 9 5-9 5-9-5 9-5Z" /><path d="m3 12 9 5 9-5m-18 5 9 5 9-5" /></>,
};

function Icon({ name, size = 18, className = '' }: { name: IconName; size?: number; className?: string }) {
  return <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{iconContent[name]}</svg>;
}

function amountOf(value: number | string): number {
  return typeof value === 'number' ? value : Number(value);
}

const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });
const dateFormat = new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
const timeFormat = new Intl.DateTimeFormat('en-IN', { day: '2-digit', month: 'short', hour: '2-digit', minute: '2-digit' });

function isIncoming(type: DemoTransaction['type']): boolean {
  return type === 'DEPOSIT' || type === 'TRANSFER_IN' || type === 'INTEREST';
}

function buildHistory(account: DemoAccount, transactions: DemoTransaction[]): HistoryPoint[] {
  let balance = 0;
  return [...transactions]
    .filter((transaction) => transaction.accountId === account.id)
    .sort((a, b) => Date.parse(a.timestamp) - Date.parse(b.timestamp))
    .map((transaction) => {
      const amount = amountOf(transaction.amount);
      balance += isIncoming(transaction.type) ? amount : -amount;
      return { date: new Date(transaction.timestamp), balance };
    });
}

function IconBadge({ icon, tone }: { icon: IconName; tone: string }) {
  return <span className={`icon-badge ${tone}`}><Icon name={icon} size={20} /></span>;
}

function BalanceChart({ points, accountName }: { points: HistoryPoint[]; accountName: string }) {
  if (points.length === 0) return <div className="empty-chart">No sample history is available for this account yet.</div>;

  const min = Math.min(...points.map((point) => point.balance));
  const max = Math.max(...points.map((point) => point.balance));
  const padding = Math.max((max - min) * 0.18, max * 0.035, 10);
  const low = Math.max(0, min - padding);
  const high = max + padding;
  const width = 920;
  const height = 230;
  const xPad = 4;
  const yPad = 10;
  const chartWidth = width - xPad * 2;
  const chartHeight = height - yPad * 2;
  const coords = points.map((point, index) => ({
    x: xPad + (points.length === 1 ? chartWidth / 2 : index * chartWidth / (points.length - 1)),
    y: yPad + chartHeight - ((point.balance - low) / (high - low || 1)) * chartHeight,
    point,
  }));
  const linePath = coords.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x.toFixed(2)} ${point.y.toFixed(2)}`).join(' ');
  const areaPath = `${linePath} L ${coords.at(-1)?.x ?? width} ${height} L ${coords[0]?.x ?? 0} ${height} Z`;
  const labelPoints = [coords[0], coords[Math.floor((coords.length - 1) / 2)], coords.at(-1)]
    .filter((point, index, list): point is NonNullable<typeof point> => Boolean(point) && list.findIndex((item) => item?.x === point?.x) === index);

  return (
    <div className="chart-wrap">
      <div className="chart-axis-labels" aria-hidden="true"><span>{inr.format(high)}</span><span>{inr.format((high + low) / 2)}</span><span>{inr.format(low)}</span></div>
      <div className="chart-plot">
        <svg className="balance-chart" viewBox={`0 0 ${width} ${height}`} role="img" aria-label={`Synthetic balance history for ${accountName}`} preserveAspectRatio="none">
          <defs><linearGradient id="balance-fill" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor="#63e0c1" stopOpacity=".28" /><stop offset="100%" stopColor="#63e0c1" stopOpacity="0" /></linearGradient></defs>
          {[0, 1, 2, 3].map((line) => <line key={line} x1="0" x2={width} y1={yPad + line * chartHeight / 3} y2={yPad + line * chartHeight / 3} className="chart-grid" />)}
          <path d={areaPath} fill="url(#balance-fill)" />
          <path d={linePath} className="chart-line" />
          {coords.map((point) => <circle key={`${point.point.date.toISOString()}-${point.x}`} cx={point.x} cy={point.y} r="3.6" className="chart-point"><title>{`${dateFormat.format(point.point.date)} · ${inr.format(point.point.balance)}`}</title></circle>)}
        </svg>
        <div className="chart-dates" aria-hidden="true">{labelPoints.map((point, index) => <span key={`${point.point.date.toISOString()}-${index}`}>{dateFormat.format(point.point.date)}</span>)}</div>
      </div>
    </div>
  );
}

function TransactionType({ transaction }: { transaction: DemoTransaction }) {
  const incoming = isIncoming(transaction.type);
  const label = transaction.type.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
  return <span className={`transaction-type ${incoming ? 'type-in' : 'type-out'}`}><span />{label}</span>;
}

function App() {
  const [overview, setOverview] = useState<DemoOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [selectedAccountId, setSelectedAccountId] = useState('');
  const [period, setPeriod] = useState<Period>('30d');
  const [filter, setFilter] = useState<Filter>('all');
  const [search, setSearch] = useState('');
  const [showAll, setShowAll] = useState(false);

  async function loadDemo(signal?: AbortSignal) {
    setError('');
    try {
      const result = await getDemoOverview(signal);
      setOverview(result);
      setSelectedAccountId((current) => result.accounts.some((account) => account.id === current) ? current : result.accounts[0]?.id ?? '');
    } catch (reason) {
      if (reason instanceof DOMException && reason.name === 'AbortError') return;
      setError(reason instanceof Error ? reason.message : 'Could not load demo data. Try again in a moment.');
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }

  useEffect(() => {
    const controller = new AbortController();
    void loadDemo(controller.signal);
    return () => controller.abort();
  }, []);

  const selectedAccount = overview?.accounts.find((account) => account.id === selectedAccountId) ?? overview?.accounts[0];
  const accountTransactions = useMemo(() => overview?.transactions
    .filter((transaction) => transaction.accountId === selectedAccount?.id)
    .sort((a, b) => Date.parse(b.timestamp) - Date.parse(a.timestamp)) ?? [], [overview, selectedAccount]);
  const periodTransactions = useMemo(() => {
    if (period === 'all') return accountTransactions;
    const days = period === '7d' ? 7 : 30;
    const threshold = Date.now() - days * 24 * 60 * 60 * 1000;
    return accountTransactions.filter((transaction) => Date.parse(transaction.timestamp) >= threshold);
  }, [accountTransactions, period]);
  const displayedTransactions = useMemo(() => {
    const query = search.trim().toLowerCase();
    return periodTransactions.filter((transaction) => {
      const matchesFilter = filter === 'all' || (filter === 'incoming' ? isIncoming(transaction.type) : !isIncoming(transaction.type));
      const matchesQuery = !query || `${transaction.description ?? ''} ${transaction.referenceId ?? ''} ${transaction.type}`.toLowerCase().includes(query);
      return matchesFilter && matchesQuery;
    });
  }, [periodTransactions, filter, search]);
  const chartPoints = useMemo(() => {
    if (!selectedAccount) return [];
    const allPoints = buildHistory(selectedAccount, overview?.transactions ?? []);
    if (period === 'all') return allPoints;
    const threshold = Date.now() - (period === '7d' ? 7 : 30) * 24 * 60 * 60 * 1000;
    const visible = allPoints.filter((point) => point.date.getTime() >= threshold);
    const prior = allPoints.filter((point) => point.date.getTime() < threshold).at(-1);
    return prior && visible.length ? [prior, ...visible] : visible;
  }, [overview, selectedAccount, period]);

  const totalBalance = overview?.accounts.reduce((total, account) => total + amountOf(account.balance), 0) ?? 0;
  const ledgerBalanced = overview?.accounts.every((account) => {
    const endBalance = buildHistory(account, overview.transactions).at(-1)?.balance ?? 0;
    return Math.abs(endBalance - amountOf(account.balance)) < 0.001;
  }) ?? false;

  async function handleRefresh() {
    setRefreshing(true);
    await loadDemo();
    setRefreshing(false);
  }

  function scrollTo(id: string) {
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#top" aria-label="VaultStream dashboard home"><span className="brand-icon">V</span><span>vault<span>stream</span></span></a>
        <div className="workspace-switch"><span className="workspace-avatar">VS</span><span className="workspace-copy"><strong>Portfolio demo</strong><small>Sandbox workspace</small></span><Icon name="chevron" size={15} className="workspace-chevron" /></div>
        <p className="nav-label">WORKSPACE</p>
        <nav className="side-nav" aria-label="Dashboard sections">
          <button className="nav-item active" onClick={() => scrollTo('overview')}><Icon name="overview" /><span>Overview</span><span className="nav-indicator" /></button>
          <button className="nav-item" onClick={() => scrollTo('accounts')}><Icon name="accounts" /><span>Accounts</span><span className="nav-count">{overview?.accounts.length ?? '—'}</span></button>
          <button className="nav-item" onClick={() => scrollTo('activity')}><Icon name="activity" /><span>Activity</span></button>
          <button className="nav-item" onClick={() => scrollTo('ledger-health')}><Icon name="ledger" /><span>Ledger health</span></button>
        </nav>
        <div className="sidebar-bottom">
          <div className="sandbox-card"><div className="sandbox-icon"><Icon name="shield" size={17} /></div><div><strong>Safe sandbox</strong><p>Fictional data only.<br />No payments are processed.</p></div><span className="sandbox-dot" /></div>
          <a className="github-link" href="https://github.com/Priyadharsan-85/VaultStream" target="_blank" rel="noreferrer"><span className="github-mark">⌘</span><span>Project on GitHub</span><Icon name="external" size={14} /></a>
          <div className="sidebar-foot"><span>VaultStream API</span><span>v1.0 · Demo</span></div>
        </div>
      </aside>

      <main className="main-content" id="top">
        <header className="topbar">
          <div className="breadcrumbs"><span>Portfolio</span><span className="breadcrumb-slash">/</span><strong>Overview</strong></div>
          <div className="topbar-actions"><span className="demo-badge"><span /> DEMO ENVIRONMENT</span><button className={`icon-button refresh-button ${refreshing ? 'is-refreshing' : ''}`} onClick={() => void handleRefresh()} aria-label="Refresh demo data" title="Refresh demo data"><Icon name="refresh" size={17} /></button><div className="profile-chip"><span className="profile-avatar">D</span><span className="profile-name">Demo visitor</span><Icon name="chevron" size={14} /></div></div>
        </header>

        <div className="page-container" id="overview">
          <section className="page-heading">
            <div><div className="overline"><span className="overline-mark" /> LEDGER OVERVIEW <span className="overline-slash">/</span> SYNTHETIC DATA</div><h1>Good day, <span>explorer.</span></h1><p>See how a balanced ledger comes together, one entry at a time.</p></div>
            <a className="api-link" href={demoOverviewUrl} target="_blank" rel="noreferrer"><Icon name="layers" size={16} /> View API response <Icon name="external" size={14} /></a>
          </section>

          {overview?.notice && <div className="notice-banner"><Icon name="shield" size={17} /><span><strong>Demo data only</strong> {overview.notice}</span></div>}
          {error && <div className="error-banner" role="alert"><span><strong>Could not reach the demo API.</strong> {error}</span><button onClick={() => void handleRefresh()}>Try again</button></div>}

          <section className="metric-grid" aria-label="Demo summary">
            <article className="metric-card balance-metric"><div className="metric-top"><span>Total demo balance</span><IconBadge icon="wallet" tone="tone-teal" /></div><strong className="metric-value">{loading ? <span className="skeleton skeleton-number" /> : inr.format(totalBalance)}</strong><div className="metric-foot"><span className="metric-sparkline">↗</span><span>Across all sample accounts</span><span className="metric-chip">SYNTHETIC</span></div></article>
            <article className="metric-card"><div className="metric-top"><span>Sample accounts</span><IconBadge icon="accounts" tone="tone-violet" /></div><strong className="metric-value">{loading ? <span className="skeleton skeleton-number" /> : overview?.accounts.length ?? '—'}</strong><div className="metric-foot"><span className="metric-dot dot-violet" /> <span>Isolated demo records</span></div></article>
            <article className="metric-card"><div className="metric-top"><span>Sample entries</span><IconBadge icon="transfer" tone="tone-blue" /></div><strong className="metric-value">{loading ? <span className="skeleton skeleton-number" /> : overview?.transactions.length ?? '—'}</strong><div className="metric-foot"><span className="metric-dot dot-blue" /> <span>Paired ledger activity</span></div></article>
            <article className="metric-card" id="ledger-health"><div className="metric-top"><span>Balance check</span><IconBadge icon="shield" tone="tone-green" /></div><strong className={`metric-value health-value ${loading ? '' : ledgerBalanced ? 'healthy' : 'unhealthy'}`}>{loading ? <span className="skeleton skeleton-number" /> : ledgerBalanced ? 'In balance' : 'Review'}</strong><div className="metric-foot"><span className={`metric-dot ${loading ? 'dot-muted' : ledgerBalanced ? 'dot-green' : 'dot-amber'}`} /> <span>Account totals match history</span></div></article>
          </section>

          <section className="content-grid">
            <article className="panel chart-panel">
              <div className="panel-heading chart-heading"><div><div className="panel-kicker">ACCOUNT PERFORMANCE</div><h2>Balance history</h2></div><div className="chart-controls"><label className="account-select-wrap"><span className="sr-only">Choose an account</span><select value={selectedAccount?.id ?? ''} onChange={(event) => setSelectedAccountId(event.target.value)} disabled={!overview?.accounts.length}><option value="" disabled>Select account</option>{overview?.accounts.map((account) => <option key={account.id} value={account.id}>{account.accountHolderName}</option>)}</select><Icon name="chevron" size={14} /></label><div className="period-tabs" role="group" aria-label="Balance history period">{(['7d', '30d', 'all'] as const).map((value) => <button key={value} className={period === value ? 'selected' : ''} onClick={() => setPeriod(value)}>{value === 'all' ? 'All' : value.toUpperCase()}</button>)}</div></div></div>
              <div className="chart-summary"><span className="chart-current">{loading || !selectedAccount ? <span className="skeleton skeleton-number" /> : inr.format(amountOf(selectedAccount.balance))}</span><span className="chart-account-label">Current synthetic balance</span></div>
              {loading ? <div className="chart-loading"><span /><span /><span /></div> : <BalanceChart points={chartPoints} accountName={selectedAccount?.accountHolderName ?? 'demo account'} />}
              <div className="chart-legend"><span className="legend-line" /> Balance <span className="chart-legend-note">Sample history from the demo API</span></div>
            </article>

            <article className="panel accounts-panel" id="accounts">
              <div className="panel-heading"><div><div className="panel-kicker">YOUR DEMO SPACE</div><h2>Accounts</h2></div><span className="count-pill">{loading ? '—' : `${overview?.accounts.length ?? 0} total`}</span></div>
              <div className="account-list">
                {loading ? <><div className="account-skeleton" /><div className="account-skeleton" /></> : overview?.accounts.map((account, index) => <button key={account.id} className={`account-card ${selectedAccount?.id === account.id ? 'account-selected' : ''}`} onClick={() => setSelectedAccountId(account.id)}>
                  <div className={`account-avatar account-avatar-${index % 2}`}>{account.accountHolderName.split(' ').map((part) => part[0]).join('').slice(0, 2)}</div>
                  <div className="account-copy"><strong>{account.accountHolderName}</strong><span>•••• {account.accountNumber.slice(-4)}</span><em><span /> {account.status.toLowerCase().replace('_', ' ')}</em></div>
                  <div className="account-balance"><strong>{inr.format(amountOf(account.balance))}</strong><span>INR</span></div>
                  <Icon name="arrow" size={15} className="account-arrow" />
                </button>)}</div>
              <div className="account-footnote"><Icon name="shield" size={14} /> Fictional balances · Read-only demo</div>
            </article>
          </section>

          <section className="panel activity-panel" id="activity">
            <div className="panel-heading activity-heading"><div><div className="panel-kicker">LEDGER EXPLORER</div><h2>Recent activity</h2><p>Entries for {selectedAccount?.accountHolderName ?? 'the selected account'}.</p></div><div className="activity-tools"><label className="search-field"><Icon name="search" size={15} /><span className="sr-only">Search activity</span><input value={search} onChange={(event) => { setSearch(event.target.value); setShowAll(false); }} placeholder="Search reference or type" /></label><button className="filter-button" onClick={() => setFilter((current) => current === 'all' ? 'incoming' : current === 'incoming' ? 'outgoing' : 'all')} title="Cycle activity filter"><Icon name="filter" size={15} /><span>{filter === 'all' ? 'Filter' : filter === 'incoming' ? 'Money in' : 'Money out'}</span></button></div></div>
            <div className="table-scroll"><table><thead><tr><th>TRANSACTION</th><th>REFERENCE</th><th>DATE &amp; TIME</th><th>TYPE</th><th className="amount-cell">AMOUNT</th></tr></thead><tbody>
              {loading ? <tr><td colSpan={5} className="table-state">Loading synthetic activity…</td></tr> : displayedTransactions.length === 0 ? <tr><td colSpan={5} className="table-state">No matching sample transactions.</td></tr> : displayedTransactions.slice(0, showAll ? undefined : 8).map((transaction) => {
                const incoming = isIncoming(transaction.type);
                return <tr key={transaction.id}><td><div className="transaction-name"><span className={`transaction-icon ${incoming ? 'transaction-icon-in' : 'transaction-icon-out'}`}><Icon name={incoming ? 'arrow' : 'transfer'} size={15} /></span><span><strong>{transaction.description ?? transaction.type.replaceAll('_', ' ')}</strong><small>{transaction.accountId === selectedAccount?.id ? selectedAccount.accountNumber : 'Synthetic record'}</small></span></div></td><td><code className="reference-code">{transaction.referenceId ?? '—'}</code></td><td><span className="date-cell">{timeFormat.format(new Date(transaction.timestamp))}</span></td><td><TransactionType transaction={transaction} /></td><td className={`amount-cell transaction-amount ${incoming ? 'amount-in' : 'amount-out'}`}>{incoming ? '+' : '−'}{inr.format(amountOf(transaction.amount))}</td></tr>;
              })}
            </tbody></table></div>
            {!loading && displayedTransactions.length > 8 && <button className="view-all-button" onClick={() => setShowAll((value) => !value)}>{showAll ? 'Show fewer entries' : `Show all ${displayedTransactions.length} entries`} <Icon name="arrow" size={14} /></button>}
            <div className="table-footer"><span><Icon name="clock" size={14} /> Demo history is fixed synthetic data</span><span>Amounts shown in INR</span></div>
          </section>

          <footer className="page-footer"><span>VaultStream portfolio demo <span className="footer-separator">·</span> Synthetic data only</span><a href={demoOverviewUrl} target="_blank" rel="noreferrer">Read-only API <Icon name="external" size={13} /></a></footer>
        </div>
      </main>
    </div>
  );
}

export default App;
