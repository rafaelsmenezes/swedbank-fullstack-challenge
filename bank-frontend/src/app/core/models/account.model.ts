export type Currency = 'EUR' | 'USD' | 'SEK' | 'GBP' | 'VND';

export interface Account {
  id: number;
  userId: number;
  currency: Currency;
  balance: number;
  createdAt: string;
}

export interface CreditRequest {
  amount: number;
  fromCurrency: Currency;
  description?: string;
}

export interface DebitRequest {
  amount: number;
  currency: Currency;
  description?: string;
}
