export type TransactionType = 'CREDIT' | 'DEBIT';

export interface Transaction {
  id: number;
  accountId: number;
  type: TransactionType;
  amount: number;
  balanceAfter: number;
  description?: string;
  createdAt: string;
}
