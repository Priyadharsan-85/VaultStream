import { useEffect, useMemo, useState, type FormEvent, type KeyboardEvent, type PointerEvent, type ReactNode } from 'react';
import { demoLiveUrl, demoOverviewUrl, getDemoOverview } from './api';
import type { DemoAccount, DemoLiveEvent, DemoOverview, DemoTransaction } from './types';

type IconName = 'home' | 'accounts' | 'activity' | 'ledger' | 'arrow' | 'refresh' | 'external' | 'search' | 'download' | 'clock' | 'shield' | 'wallet' | 'transfer' | 'check' | 'close';
type HistoryPoint = { date: Date; balance: number };
type Filter = 'all' | 'incoming' | 'outgoing';
type Period = '7d' | '30d' | 'all';
type SandboxChanges = { additions: DemoTransaction[]; edits: Record<string, DemoTransaction>; removed: string[] };

const sandboxStorageKey = 'vaultstream.public-demo.sandbox.v1';
const emptySandbox: SandboxChanges = { additions: [], edits: {}, removed: [] };

function readSandboxChanges(): SandboxChanges {
  if (typeof window === 'undefined') return emptySandbox;
  try {
    const saved = sessionStorage.getItem(sandboxStorageKey);
    if (!saved) return emptySandbox;
    const parsed = JSON.parse(saved) as Partial<SandboxChanges>;
    return {
      additions: Array.isArray(parsed.additions) ? parsed.additions : [],
      edits: parsed.edits && typeof parsed.edits === 'object' ? parsed.edits : {},
      removed: Array.isArray(parsed.removed) ? parsed.removed : [],
    };
  } catch {
    return emptySandbox;
  }
}

function applySandboxChanges(overview: DemoOverview, changes: SandboxChanges, liveEvents: DemoLiveEvent[]): DemoOverview {
  const removed = new Set(changes.removed);
  const seedAndLiveTransactions = [...overview.transactions, ...liveEvents.flatMap((event) => event.transactions)];
  const transactions = [
    ...seedAndLiveTransactions.filter((transaction) => !removed.has(transaction.id)).map((transaction) => changes.edits[transaction.id] ?? transaction),
    ...changes.additions,
  ];
  const accounts = overview.accounts.map((account) => {
    const balance = transactions
      .filter((transaction) => transaction.accountId === account.id)
      .reduce((total, transaction) => total + (isIncoming(transaction.type) ? amountOf(transaction.amount) : -amountOf(transaction.amount)), 0);
    return { ...account, balance };
  });
  return { ...overview, accounts, transactions };
}

function localDateTimeValue(date: Date): string {
  return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 16);
}

const iconContent: Record<IconName, ReactNode> = {
  home: <><path d="m3 10 9-7 9 7" /><path d="M5 9v11h14V9M9 20v-7h6v7" /></>,
  accounts: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M3 10h18M7 15h4" /></>,
  activity: <><path d="M3 12h4l2-7 4 14 3-9 2 2h3" /></>,
  ledger: <><path d="M4 5.5 12 2l8 3.5-8 3.5-8-3.5Z" /><path d="m4 12 8 3.5 8-3.5m-16 6 8 3.5 8-3.5" /></>,
  arrow: <><path d="M4 12h16M13 5l7 7-7 7" /></>,
  refresh: <><path d="M20 7v5h-5M4 17v-5h5" /><path d="M5.5 9a7 7 0 0 1 11.6-2.2L20 12M4 12l2.9 5.2A7 7 0 0 0 18.5 15" /></>,
  external: <><path d="M14 4h6v6m0-6-9 9" /><path d="M18 13v5a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h5" /></>,
  search: <><circle cx="10.8" cy="10.8" r="6.8" /><path d="m16 16 4.5 4.5" /></>,
  download: <><path d="M12 3v12m0 0 4-4m-4 4-4-4" /><path d="M5 17v4h14v-4" /></>,
  clock: <><circle cx="12" cy="12" r="9" /><path d="M12 7v5l3 2" /></>,
  shield: <><path d="M12 3 20 6v5c0 5-3.4 8.5-8 10-4.6-1.5-8-5-8-10V6l8-3Z" /><path d="m9 12 2 2 4-4" /></>,
  wallet: <><rect x="3" y="5" width="18" height="15" rx="2" /><path d="M3 9h18m-5 5h2" /></>,
  transfer: <><path d="M4 8h15l-3-3m4 11H5l3 3" /></>,
  check: <path d="m5 12 4 4L19 6" />,
  close: <path d="m6 6 12 12M18 6 6 18" />,
};

function Icon({ name, size = 18, className = '' }: { name: IconName; size?: number; className?: string }) {
  return <svg className={className} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.65" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">{iconContent[name]}</svg>;
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

function BalanceChart({ points, accountName, period, onPeriodChange }: {
  points: HistoryPoint[];
  accountName: string;
  period: Period;
  onPeriodChange: (period: Period) => void;
}) {
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const min = points.length ? Math.min(...points.map((point) => point.balance)) : 0;
  const max = points.length ? Math.max(...points.map((point) => point.balance)) : 0;
  const padding = Math.max((max - min) * 0.2, max * 0.045, 12);
  const low = Math.max(0, min - padding);
  const high = max + padding;
  const width = 900;
  const height = 245;
  const top = 13;
  const bottom = 220;
  const plotWidth = width - 8;
  const plotHeight = bottom - top;
  const coords = points.map((point, index) => ({
    x: 4 + (points.length < 2 ? plotWidth / 2 : index * plotWidth / (points.length - 1)),
    y: bottom - ((point.balance - low) / (high - low || 1)) * plotHeight,
    point,
  }));
  const linePath = coords.map((point, index) => `${index === 0 ? 'M' : 'L'} ${point.x.toFixed(2)} ${point.y.toFixed(2)}`).join(' ');
  const areaPath = coords.length ? `${linePath} L ${coords.at(-1)?.x ?? width} ${bottom} L ${coords[0]?.x ?? 0} ${bottom} Z` : '';
  const selectedPoint = hoveredIndex !== null ? coords[hoveredIndex] : coords.at(-1);
  const labels = [coords[0], coords[Math.floor((coords.length - 1) / 2)], coords.at(-1)]
    .filter((point, index, list): point is NonNullable<typeof point> => Boolean(point) && list.findIndex((item) => item?.x === point?.x) === index);

  function handlePointerMove(event: PointerEvent<SVGSVGElement>) {
    if (!coords.length) return;
    const rect = event.currentTarget.getBoundingClientRect();
    const fraction = Math.max(0, Math.min(1, (event.clientX - rect.left) / rect.width));
    setHoveredIndex(Math.round(fraction * (coords.length - 1)));
  }

  function handleChartKeys(event: KeyboardEvent<SVGSVGElement>) {
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
    event.preventDefault();
    setHoveredIndex((current) => {
      const next = current ?? coords.length - 1;
      return Math.max(0, Math.min(coords.length - 1, next + (event.key === 'ArrowRight' ? 1 : -1)));
    });
  }

  if (!points.length) return <div className="empty-chart">No synthetic history for this account in this range.</div>;

  return (
    <div className="chart-area">
      <div className="chart-readout" aria-live="polite">
        <span className="readout-date">{selectedPoint ? dateFormat.format(selectedPoint.point.date) : 'Latest entry'}</span>
        <strong>{selectedPoint ? inr.format(selectedPoint.point.balance) : inr.format(max)}</strong>
        <span className="readout-label">{hoveredIndex === null ? 'LATEST BALANCE' : 'HISTORY POINT'}</span>
      </div>
      <div className="balance-chart-layout">
        <div className="chart-y-labels" aria-hidden="true"><span>{inr.format(high)}</span><span>{inr.format((high + low) / 2)}</span><span>{inr.format(low)}</span></div>
        <svg
          className="balance-chart"
          viewBox={`0 0 ${width} ${height}`}
          role="img"
          aria-label={`Interactive synthetic balance history for ${accountName}. Use left and right arrow keys to inspect dates.`}
          preserveAspectRatio="none"
          onPointerMove={handlePointerMove}
          onPointerLeave={() => setHoveredIndex(null)}
          onKeyDown={handleChartKeys}
          tabIndex={0}
        >
          <defs>
            <linearGradient id="chart-paper" x1="0" x2="0" y1="0" y2="1"><stop offset="0%" stopColor="#c45d3d" stopOpacity=".25" /><stop offset="100%" stopColor="#c45d3d" stopOpacity="0" /></linearGradient>
          </defs>
          {[0, 1, 2, 3].map((line) => <line key={line} x1="0" x2={width} y1={top + line * plotHeight / 3} y2={top + line * plotHeight / 3} className="chart-grid" />)}
          {selectedPoint && <line x1={selectedPoint.x} x2={selectedPoint.x} y1={top} y2={bottom} className="chart-crosshair" />}
          <path d={areaPath} fill="url(#chart-paper)" />
          <path d={linePath} className="chart-line" />
          {coords.map((point, index) => <circle key={`${point.point.date.toISOString()}-${index}`} cx={point.x} cy={point.y} r={index === hoveredIndex ? '6' : '3.2'} className={`chart-point ${index === hoveredIndex ? 'point-active' : ''}`}><title>{`${dateFormat.format(point.point.date)} — ${inr.format(point.point.balance)}`}</title></circle>)}
        </svg>
      </div>
      <div className="chart-bottomline"><div className="chart-dates">{labels.map((point, index) => <span key={`${point.point.date.toISOString()}-${index}`}>{dateFormat.format(point.point.date)}</span>)}</div><div className="period-tabs" role="group" aria-label="Balance history period">{(['7d', '30d', 'all'] as const).map((value) => <button key={value} className={period === value ? 'selected' : ''} onClick={() => onPeriodChange(value)}>{value === 'all' ? 'Since opening' : value.toUpperCase()}</button>)}</div></div>
    </div>
  );
}

function TransactionType({ transaction }: { transaction: DemoTransaction }) {
  const incoming = isIncoming(transaction.type);
  const label = transaction.type.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
  return <span className={`transaction-type ${incoming ? 'type-in' : 'type-out'}`}><span />{label}</span>;
}

function SandboxEntryDialog({ account, transaction, onClose, onSave }: {
  account: DemoAccount;
  transaction?: DemoTransaction;
  onClose: () => void;
  onSave: (entry: Omit<DemoTransaction, 'id'>, existingId?: string) => void;
}) {
  const [description, setDescription] = useState(transaction?.description ?? '');
  const [amount, setAmount] = useState(transaction ? String(amountOf(transaction.amount)) : '');
  const [type, setType] = useState<DemoTransaction['type']>(transaction?.type ?? 'DEPOSIT');
  const [timestamp, setTimestamp] = useState(localDateTimeValue(transaction ? new Date(transaction.timestamp) : new Date()));
  const [reference, setReference] = useState(transaction?.referenceId ?? '');

  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSave({
      accountId: account.id,
      amount: Number(amount).toFixed(2),
      type,
      timestamp: new Date(timestamp).toISOString(),
      description: description.trim(),
      referenceId: reference.trim() || `TRY-${Date.now().toString(36).toUpperCase()}`,
    }, transaction?.id);
  }

  return <div className="sandbox-dialog-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) onClose(); }}>
    <section className="sandbox-dialog" role="dialog" aria-modal="true" aria-labelledby="sandbox-dialog-title">
      <div className="sandbox-dialog-head"><div><span className="section-number">VISITOR SANDBOX</span><h2 id="sandbox-dialog-title">{transaction ? 'Edit this entry' : 'Add a test entry'}</h2><p>Fictional only. This change stays in your current browser tab.</p></div><button className="dialog-close" onClick={onClose} aria-label="Close entry form">×</button></div>
      <form onSubmit={submit}>
        <label className="sandbox-field"><span>ACCOUNT</span><input value={account.accountHolderName} readOnly /></label>
        <div className="sandbox-field-row">
          <label className="sandbox-field"><span>ENTRY TYPE</span><select value={type} onChange={(event) => setType(event.target.value as DemoTransaction['type'])}><option value="DEPOSIT">Deposit</option><option value="WITHDRAWAL">Withdrawal</option><option value="TRANSFER_IN">Transfer in</option><option value="TRANSFER_OUT">Transfer out</option><option value="FEE">Fee</option><option value="INTEREST">Interest</option></select></label>
          <label className="sandbox-field"><span>AMOUNT · INR</span><input type="number" min="0.01" max="10000000" step="0.01" value={amount} onChange={(event) => setAmount(event.target.value)} required placeholder="250.00" /></label>
        </div>
        <label className="sandbox-field"><span>DESCRIPTION</span><input value={description} onChange={(event) => setDescription(event.target.value)} required maxLength={80} placeholder="For example, test deposit" /></label>
        <div className="sandbox-field-row">
          <label className="sandbox-field"><span>DATE &amp; TIME</span><input type="datetime-local" value={timestamp} onChange={(event) => setTimestamp(event.target.value)} required /></label>
          <label className="sandbox-field"><span>REFERENCE <i>OPTIONAL</i></span><input value={reference} onChange={(event) => setReference(event.target.value)} maxLength={40} placeholder="Auto-generated if blank" /></label>
        </div>
        <div className="sandbox-dialog-actions"><button type="button" className="dialog-cancel" onClick={onClose}>Cancel</button><button type="submit" className="dialog-save">{transaction ? 'Save test entry' : 'Add test entry'} <Icon name="arrow" size={14} /></button></div>
      </form>
    </section>
  </div>;
}

function exportCsv(transactions: DemoTransaction[], account: DemoAccount) {
  const escape = (value: string) => `"${value.replaceAll('"', '""')}"`;
  const lines = [
    ['Date', 'Account', 'Type', 'Reference', 'Description', 'Amount INR'],
    ...transactions.map((transaction) => [
      transaction.timestamp,
      account.accountNumber,
      transaction.type,
      transaction.referenceId ?? '',
      transaction.description ?? '',
      amountOf(transaction.amount).toFixed(2),
    ]),
  ];
  const file = new Blob([lines.map((line) => line.map(escape).join(',')).join('\r\n')], { type: 'text/csv;charset=utf-8' });
  const url = URL.createObjectURL(file);
  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = `${account.accountNumber.toLowerCase()}-synthetic-ledger.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

function App() {
  const [overview, setOverview] = useState<DemoOverview | null>(null);
  const [sandboxChanges, setSandboxChanges] = useState<SandboxChanges>(readSandboxChanges);
  const [entryEditor, setEntryEditor] = useState<DemoTransaction | null | false>(false);
  const [liveEvents, setLiveEvents] = useState<DemoLiveEvent[]>([]);
  const [liveStatus, setLiveStatus] = useState<'connecting' | 'live' | 'reconnecting' | 'paused'>('connecting');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState('');
  const [selectedAccountId, setSelectedAccountId] = useState('');
  const [period, setPeriod] = useState<Period>('30d');
  const [filter, setFilter] = useState<Filter>('all');
  const [search, setSearch] = useState('');
  const [expandedTransactionId, setExpandedTransactionId] = useState('');
  const [activeNav, setActiveNav] = useState('overview');

  async function loadDemo(signal?: AbortSignal) {
    setError('');
    try {
      const result = await getDemoOverview(signal);
      setOverview(result);
      setSelectedAccountId((current) => result.accounts.some((account) => account.id === current) ? current : result.accounts[0]?.id ?? '');
    } catch (reason) {
      if (reason instanceof DOMException && reason.name === 'AbortError') return;
      setError(reason instanceof Error ? reason.message : 'Could not load the synthetic ledger. Try again in a moment.');
    } finally {
      if (!signal?.aborted) setLoading(false);
    }
  }

  useEffect(() => {
    const controller = new AbortController();
    void loadDemo(controller.signal);
    return () => controller.abort();
  }, []);

  useEffect(() => {
    try {
      sessionStorage.setItem(sandboxStorageKey, JSON.stringify(sandboxChanges));
    } catch {
      // The sandbox still works for this page visit when browser storage is unavailable.
    }
  }, [sandboxChanges]);

  const hasOverview = overview !== null;
  useEffect(() => {
    if (!hasOverview) return;
    const source = new EventSource(demoLiveUrl);
    let receivedEvents = 0;
    setLiveStatus('connecting');

    source.onopen = () => setLiveStatus('live');
    source.onerror = () => setLiveStatus('reconnecting');
    source.addEventListener('ledger-event', (message) => {
      try {
        const event = JSON.parse((message as MessageEvent<string>).data) as DemoLiveEvent;
        if (!event.id || !Array.isArray(event.transactions)) return;
        receivedEvents += 1;
        setLiveEvents((current) => [event, ...current]);
        if (receivedEvents >= 120) {
          source.close();
          setLiveStatus('paused');
        } else {
          setLiveStatus('live');
        }
      } catch {
        // Ignore malformed stream data and let EventSource continue reconnecting.
      }
    });

    return () => source.close();
  }, [hasOverview]);

  const visitorOverview = useMemo(() => overview ? applySandboxChanges(overview, sandboxChanges, liveEvents) : null, [overview, sandboxChanges, liveEvents]);
  const dashboardOverview = visitorOverview;

  function saveSandboxEntry(entry: Omit<DemoTransaction, 'id'>, existingId?: string) {
    const savedEntry: DemoTransaction = { ...entry, id: existingId ?? crypto.randomUUID() };
    setSandboxChanges((current) => {
      if (!existingId) return { ...current, additions: [savedEntry, ...current.additions] };
      if (current.additions.some((item) => item.id === existingId)) {
        return { ...current, additions: current.additions.map((item) => item.id === existingId ? savedEntry : item) };
      }
      return { ...current, edits: { ...current.edits, [existingId]: savedEntry } };
    });
    setEntryEditor(false);
    setExpandedTransactionId('');
  }

  function removeSandboxEntry(id: string) {
    setSandboxChanges((current) => {
      if (current.additions.some((item) => item.id === id)) return { ...current, additions: current.additions.filter((item) => item.id !== id) };
      const edits = { ...current.edits };
      delete edits[id];
      return { ...current, edits, removed: current.removed.includes(id) ? current.removed : [...current.removed, id] };
    });
    setExpandedTransactionId('');
  }

  function resetSandbox() {
    setSandboxChanges(emptySandbox);
    setExpandedTransactionId('');
    setFilter('all');
    setSearch('');
  }

  const selectedAccount = dashboardOverview?.accounts.find((account) => account.id === selectedAccountId) ?? dashboardOverview?.accounts[0];
  const accountTransactions = useMemo(() => dashboardOverview?.transactions
    .filter((transaction) => transaction.accountId === selectedAccount?.id)
    .sort((a, b) => Date.parse(b.timestamp) - Date.parse(a.timestamp)) ?? [], [dashboardOverview, selectedAccount]);
  const periodTransactions = useMemo(() => {
    if (period === 'all') return accountTransactions;
    const threshold = Date.now() - (period === '7d' ? 7 : 30) * 24 * 60 * 60 * 1000;
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
    const allPoints = buildHistory(selectedAccount, dashboardOverview?.transactions ?? []);
    if (period === 'all') return allPoints;
    const threshold = Date.now() - (period === '7d' ? 7 : 30) * 24 * 60 * 60 * 1000;
    const visible = allPoints.filter((point) => point.date.getTime() >= threshold);
    const prior = allPoints.filter((point) => point.date.getTime() < threshold).at(-1);
    return prior && visible.length ? [prior, ...visible] : visible;
  }, [dashboardOverview, selectedAccount, period]);

  const totalBalance = dashboardOverview?.accounts.reduce((total, account) => total + amountOf(account.balance), 0) ?? 0;
  const ledgerBalanced = dashboardOverview?.accounts.every((account) => {
    const endBalance = buildHistory(account, dashboardOverview.transactions).at(-1)?.balance ?? 0;
    return Math.abs(endBalance - amountOf(account.balance)) < 0.001;
  }) ?? false;

  async function handleRefresh() {
    setRefreshing(true);
    await loadDemo();
    setRefreshing(false);
  }

  function jumpTo(id: string) {
    setActiveNav(id);
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  }

  return (
    <div className="studio-shell">
      <aside className="sidebar">
        <a className="brand" href="#overview" aria-label="VaultStream ledger home"><span className="brand-mark"><i /><i /><i /></span><span>VAULT<span>STREAM</span></span></a>
        <div className="sidebar-edition"><span>FIELD NOTES</span><span>NO. 001 / 2026</span></div>
        <div className="workspace-switch"><span className="workspace-stamp">VS</span><span><strong>Open sandbox</strong><small>Public reading room</small></span><span className="workspace-caret">↘</span></div>
        <p className="nav-label">THE LEDGER</p>
        <nav className="side-nav" aria-label="Dashboard sections">
          <button className={`nav-item ${activeNav === 'overview' ? 'active' : ''}`} onClick={() => jumpTo('overview')}><Icon name="home" size={17} /><span>Overview</span><b>01</b></button>
          <button className={`nav-item ${activeNav === 'accounts' ? 'active' : ''}`} onClick={() => jumpTo('accounts')}><Icon name="accounts" size={17} /><span>Accounts</span><b>{String(dashboardOverview?.accounts.length ?? 0).padStart(2, '0')}</b></button>
          <button className={`nav-item ${activeNav === 'activity' ? 'active' : ''}`} onClick={() => jumpTo('activity')}><Icon name="activity" size={17} /><span>Entries</span><b>↗</b></button>
        </nav>
        <div className="sidebar-aside"><span className="aside-rule" /><p>Every movement<br />has a counter-entry.</p><span className="aside-index">DEBIT<br />CREDIT</span></div>
        <div className="sidebar-bottom">
          <div className="sandbox-seal"><span className="seal-icon"><Icon name="shield" size={15} /></span><span><strong>SIMULATION ONLY</strong><small>Nothing here moves money.</small></span></div>
          <a className="github-link" href="https://github.com/Priyadharsan-85/VaultStream" target="_blank" rel="noreferrer"><span>Project source</span><Icon name="external" size={13} /></a>
          <div className="sidebar-footer"><span>VAULTSTREAM / API</span><span>V.01</span></div>
        </div>
      </aside>

      <main className="main-content">
        <header className="topbar">
          <div className="breadcrumbs"><span>VAULTSTREAM</span><span className="crumb-mark">/</span><strong>PUBLIC DEMO</strong></div>
          <div className="topbar-actions"><span className="folio-date">FOLIO 29—10—26</span><span className="demo-badge"><i /> VISITOR TEST SANDBOX</span><button className={`icon-button refresh-button ${refreshing ? 'is-refreshing' : ''}`} onClick={() => void handleRefresh()} aria-label="Refresh sample data" title="Refresh sample data"><Icon name="refresh" size={16} /></button></div>
        </header>

        <div className="page-container">
          <section className="hero" id="overview">
            <div className="hero-copy"><div className="hero-overline"><span>01</span><i /> PUBLIC LEDGER STUDY</div><h1>Money,<br /><em>made legible.</em></h1><p className="hero-subtitle">A small, inspectable ledger. Follow every balance back to the entries that shaped it.</p><div className="hero-meta"><span><Icon name="clock" size={14} /> 30-day sample window</span><span><i /> Try entries in your own tab</span></div></div>
            <div className="hero-ledger" aria-label="Illustration of a balanced double-entry transfer"><div className="ledger-stamp">POSTED<br />DEMO</div><div className="hero-ledger-heading"><span>JOURNAL / ENTRY 011</span><span>INR · 01</span></div><div className="journal-line"><span className="journal-marker debit-marker">DR</span><span className="journal-label"><strong>Source account</strong><small>TRANSFER OUT</small></span><span className="journal-value">₹ 55.00</span></div><div className="journal-line"><span className="journal-marker credit-marker">CR</span><span className="journal-label"><strong>Destination account</strong><small>TRANSFER IN</small></span><span className="journal-value">₹ 55.00</span></div><div className="journal-balance"><span>Journal difference</span><strong>₹ 0.00 <Icon name="check" size={14} /></strong></div><div className="ledger-scribble">equal &amp; opposite</div></div>
            <div className="hero-side-note"><span className="side-note-line" />FIELD STUDY<br />FICTIONAL RECORDS</div>
          </section>

          {dashboardOverview?.notice && <div className="notice-banner"><Icon name="shield" size={16} /><span><strong>Simulation notice</strong> {dashboardOverview.notice}</span></div>}
          {error && <div className="error-banner" role="alert"><span><strong>Demo service is taking a pause.</strong> {error}</span><button onClick={() => void handleRefresh()}>Retry <Icon name="arrow" size={13} /></button></div>}

          <section className="measure-strip" aria-label="Sample ledger summary">
            <article className="measure measure-total"><span className="measure-label">TOTAL SAMPLE BALANCE <i>01</i></span><strong>{loading ? <span className="skeleton skeleton-wide" /> : inr.format(totalBalance)}</strong><small>Across the two demo accounts</small></article>
            <article className="measure"><span className="measure-label">ACCOUNTS <i>02</i></span><strong>{loading ? '—' : String(dashboardOverview?.accounts.length ?? 0).padStart(2, '0')}</strong><small>Closed synthetic set</small></article>
            <article className="measure"><span className="measure-label">POSTINGS <i>03</i></span><strong>{loading ? '—' : String(dashboardOverview?.transactions.length ?? 0).padStart(2, '0')}</strong><small>Paired entries · 30 days</small></article>
            <article className="measure measure-status"><span className="measure-label">BALANCE TEST <i>04</i></span><strong className={loading ? '' : ledgerBalanced ? 'status-good' : 'status-review'}>{loading ? '—' : ledgerBalanced ? 'Passed' : 'Review'}</strong><small><span className={`status-dot ${loading ? 'dot-wait' : ledgerBalanced ? 'dot-good' : 'dot-review'}`} /> Account totals trace to history</small></article>
          </section>

          <section className="live-feed-panel" aria-labelledby="live-feed-title">
            <div className="live-feed-heading"><div><span className="section-number">C / LIVE STREAM</span><h2 id="live-feed-title">Activity, as it happens</h2><p>Generated sample entries update the ledger in real time.</p></div><span className={`live-stream-status stream-${liveStatus}`}><i />{liveStatus === 'live' ? 'STREAM LIVE' : liveStatus === 'reconnecting' ? 'RECONNECTING' : liveStatus === 'paused' ? 'SESSION PAUSED' : 'CONNECTING'}</span></div>
            <div className="live-feed-events" aria-live="polite" aria-relevant="additions">
              {liveEvents.length === 0 ? <div className="live-feed-empty"><span className="live-pulse" />Waiting for the first simulated ledger event<span className="live-feed-dots">···</span></div> : liveEvents.slice(0, 4).map((event) => <article className="live-event" key={event.id}><span className="live-event-symbol">{event.label.includes('transfer') ? '↔' : '↗'}</span><span className="live-event-copy"><strong>{event.label}</strong><small>{timeFormat.format(new Date(event.timestamp))} · {event.transactions.length} {event.transactions.length === 1 ? 'posting' : 'postings'}</small></span><b>{inr.format(amountOf(event.amount))}</b></article>)}
            </div>
            <div className="live-feed-foot"><span>SIMULATED EVENTS</span><span>NO MONEY MOVES · NO SAVED LEDGER WRITES</span><span>{liveEvents.length} / 120</span></div>
          </section>

          <section className="ledger-layout">
            <article className="paper-panel chart-panel" id="accounts">
              <div className="section-head"><div><span className="section-number">A / 01</span><h2>Balance, over time</h2><p>Select a record; inspect its running balance.</p></div><label className="account-selector"><span className="sr-only">Select a sample account</span><select value={selectedAccount?.id ?? ''} onChange={(event) => setSelectedAccountId(event.target.value)} disabled={!dashboardOverview?.accounts.length}><option value="" disabled>Choose account</option>{dashboardOverview?.accounts.map((account) => <option key={account.id} value={account.id}>{account.accountHolderName}</option>)}</select><span aria-hidden="true">⌄</span></label></div>
              <div className="chart-header-readout"><div><span className="readout-caption">{selectedAccount?.accountHolderName ?? 'Selected account'}</span><strong>{loading || !selectedAccount ? <span className="skeleton skeleton-wide" /> : inr.format(amountOf(selectedAccount.balance))}</strong></div><span className="readout-current"><i /> CURRENT BALANCE</span></div>
              {loading ? <div className="chart-loading"><span /><span /><span /></div> : <BalanceChart points={chartPoints} accountName={selectedAccount?.accountHolderName ?? 'demo account'} period={period} onPeriodChange={setPeriod} />}
              <div className="chart-footnote"><span><i /> BALANCE TRACE</span><span>Hover or focus the plot to inspect entries</span></div>
            </article>

            <article className="account-panel" aria-labelledby="accounts-title">
              <div className="account-panel-heading"><div><span className="section-number">A / 02</span><h2 id="accounts-title">The accounts</h2></div><span className="account-count">{loading ? '—' : `${dashboardOverview?.accounts.length ?? 0} RECORDS`}</span></div>
              <p className="account-intro">Two fictional ledgers. Click one to trace its balance.</p>
              <div className="account-stack">{loading ? <><div className="account-skeleton" /><div className="account-skeleton" /></> : dashboardOverview?.accounts.map((account, index) => <AccountCard key={account.id} account={account} index={index} selected={selectedAccount?.id === account.id} onSelect={() => setSelectedAccountId(account.id)} />)}</div>
              <div className="account-panel-foot"><span>NO REAL CUSTOMERS</span><span>EST. 2026</span></div>
            </article>
          </section>

          <section className="paper-panel activity-panel" id="activity">
            <div className="activity-topline"><div><span className="section-number">B / 01</span><h2>Entries &amp; movements</h2><p>Your edits are private to this browser tab and reset when it closes.</p></div><div className="activity-actions"><a className="raw-link" href={demoOverviewUrl} target="_blank" rel="noreferrer">Raw API <Icon name="external" size={13} /></a><button className="sandbox-reset" disabled={sandboxChanges.additions.length === 0 && Object.keys(sandboxChanges.edits).length === 0 && sandboxChanges.removed.length === 0} onClick={resetSandbox}>Reset changes</button><button className="sandbox-add" disabled={!selectedAccount || loading} onClick={() => setEntryEditor(null)}>+ Add test entry</button><button className="export-button" disabled={!selectedAccount || !displayedTransactions.length} onClick={() => selectedAccount && exportCsv(displayedTransactions, selectedAccount)}><Icon name="download" size={14} /> Export CSV</button></div></div>
            <div className="table-toolbar"><div className="filter-tabs" role="group" aria-label="Filter ledger entries">{([['all', 'Everything'], ['incoming', 'Inflow'], ['outgoing', 'Outflow']] as const).map(([value, label]) => <button key={value} className={filter === value ? 'selected' : ''} onClick={() => { setFilter(value); setExpandedTransactionId(''); }}>{label}<small>{value === 'all' ? periodTransactions.length : periodTransactions.filter((transaction) => value === 'incoming' ? isIncoming(transaction.type) : !isIncoming(transaction.type)).length}</small></button>)}</div><label className="search-field"><Icon name="search" size={15} /><span className="sr-only">Search synthetic entries</span><input value={search} onChange={(event) => { setSearch(event.target.value); setExpandedTransactionId(''); }} placeholder="Find a reference" /><kbd>/</kbd></label></div>
            <div className="table-scroll"><table><thead><tr><th>ENTRY</th><th>REFERENCE</th><th>DATE</th><th>CLASSIFICATION</th><th className="amount-cell">AMOUNT</th><th className="expand-column"><span className="sr-only">Open details</span></th></tr></thead><tbody>
              {loading ? <tr><td colSpan={6} className="table-state">Opening the sample ledger…</td></tr> : displayedTransactions.length === 0 ? <tr><td colSpan={6} className="table-state">No entries match this view.</td></tr> : displayedTransactions.map((transaction, index) => {
                const incoming = isIncoming(transaction.type);
                const expanded = expandedTransactionId === transaction.id;
                return <FragmentRow key={transaction.id} transaction={transaction} index={index} incoming={incoming} expanded={expanded} account={selectedAccount} onToggle={() => setExpandedTransactionId(expanded ? '' : transaction.id)} onEdit={() => setEntryEditor(transaction)} onDelete={() => removeSandboxEntry(transaction.id)} />;
              })}
            </tbody></table></div>
            <div className="activity-footer"><span><Icon name="shield" size={13} /> Synthetic ledger · visitor changes stay in this tab</span><span>Showing {displayedTransactions.length} {displayedTransactions.length === 1 ? 'entry' : 'entries'}</span></div>
          </section>

          <footer className="page-footer"><span>VAULTSTREAM <i>×</i> PUBLIC LEDGER STUDY</span><span>Built for learning · Not a financial service</span><a href="https://github.com/Priyadharsan-85/VaultStream" target="_blank" rel="noreferrer">SOURCE CODE <Icon name="external" size={12} /></a></footer>
        </div>
      </main>
      {entryEditor !== false && selectedAccount && <SandboxEntryDialog account={selectedAccount} transaction={entryEditor || undefined} onClose={() => setEntryEditor(false)} onSave={saveSandboxEntry} />}
    </div>
  );
}

function AccountCard({ account, index, selected, onSelect }: { account: DemoAccount; index: number; selected: boolean; onSelect: () => void }) {
  return <button className={`account-card account-card-${index % 2} ${selected ? 'account-card-selected' : ''}`} onClick={onSelect} aria-pressed={selected}>
    <div className="account-card-top"><span>{index === 0 ? 'SOURCE LEDGER' : 'DESTINATION LEDGER'}</span><span className="account-status"><i /> ACTIVE</span></div>
    <div className="account-card-mid"><span className="account-monogram">0{index + 1}</span><span className="account-holder">{account.accountHolderName}</span><Icon name="arrow" size={16} className="account-arrow" /></div>
    <div className="account-card-bottom"><span>•••• {account.accountNumber.slice(-4)}</span><strong>{inr.format(amountOf(account.balance))}</strong></div>
    <span className="account-card-sheen" aria-hidden="true" />
  </button>;
}

function FragmentRow({ transaction, index, incoming, expanded, account, onToggle, onEdit, onDelete }: {
  transaction: DemoTransaction;
  index: number;
  incoming: boolean;
  expanded: boolean;
  account?: DemoAccount;
  onToggle: () => void;
  onEdit: () => void;
  onDelete: () => void;
}) {
  const typeLabel = transaction.type.replaceAll('_', ' ').toLowerCase().replace(/\b\w/g, (letter) => letter.toUpperCase());
  return <>
    <tr className={`activity-row ${expanded ? 'row-expanded' : ''}`}>
      <td><button className="transaction-button" onClick={onToggle} aria-expanded={expanded}><span className={`row-index ${incoming ? 'index-in' : 'index-out'}`}>{String(index + 1).padStart(2, '0')}</span><span><strong>{transaction.description ?? typeLabel}</strong><small>{account?.accountHolderName ?? 'Synthetic account'}</small></span></button></td>
      <td><code className="reference-code">{transaction.referenceId ?? '—'}</code></td>
      <td><span className="date-cell">{timeFormat.format(new Date(transaction.timestamp))}</span></td>
      <td><TransactionType transaction={transaction} /></td>
      <td className={`amount-cell transaction-amount ${incoming ? 'amount-in' : 'amount-out'}`}>{incoming ? '+' : '−'}{inr.format(amountOf(transaction.amount))}</td>
      <td className="expand-cell"><button className={`row-expand ${expanded ? 'expanded' : ''}`} onClick={onToggle} aria-label={expanded ? 'Hide entry details' : 'Show entry details'}><span>+</span></button></td>
    </tr>
    {expanded && <tr className="detail-row"><td colSpan={6}><div className="entry-detail"><div><span>JOURNAL REFERENCE</span><strong>{transaction.referenceId ?? 'No reference'}</strong></div><div><span>POSTED AT</span><strong>{dateFormat.format(new Date(transaction.timestamp))} · {new Date(transaction.timestamp).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}</strong></div><div><span>ENTRY AMOUNT</span><strong>{inr.format(amountOf(transaction.amount))}</strong></div><div className="detail-balance"><span>RECORD TYPE</span><strong>{typeLabel}</strong></div><div className="sandbox-row-actions"><button onClick={onEdit}>Edit test entry</button><button onClick={onDelete}>Remove from this tab</button></div></div></td></tr>}
  </>;
}

export default App;
