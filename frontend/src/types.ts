export type DemoAccount = {
  id: string;
  accountNumber: string;
  accountHolderName: string;
  balance: number | string;
  status: 'ACTIVE' | 'SUSPENDED' | 'CLOSED' | 'PENDING_VERIFICATION';
  createdAt: string;
  updatedAt: string;
};

export type DemoTransaction = {
  id: string;
  accountId: string;
  amount: number | string;
  type: 'DEPOSIT' | 'WITHDRAWAL' | 'TRANSFER_IN' | 'TRANSFER_OUT' | 'FEE' | 'INTEREST';
  referenceId: string | null;
  description: string | null;
  timestamp: string;
};

export type DemoOverview = {
  notice: string;
  accounts: DemoAccount[];
  transactions: DemoTransaction[];
};

export type DemoLiveEvent = {
  id: string;
  label: string;
  amount: number | string;
  timestamp: string;
  transactions: DemoTransaction[];
};
