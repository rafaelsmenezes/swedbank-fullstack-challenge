import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Account, CreditRequest, DebitRequest } from '../models/account.model';
import { Transaction } from '../models/transaction.model';
import { Page } from '../models/page.model';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private http = inject(HttpClient);
  private base = '/api/v1';

  getAccounts(userId: number): Observable<Account[]> {
    return this.http.get<Account[]>(`${this.base}/accounts`, {
      params: new HttpParams().set('userId', userId),
    });
  }

  getAccount(id: number): Observable<Account> {
    return this.http.get<Account>(`${this.base}/accounts/${id}`);
  }

  getBalance(id: number): Observable<{ balance: number }> {
    return this.http.get<{ balance: number }>(`${this.base}/accounts/${id}/balance`);
  }

  getTransactions(id: number, page: number, size: number): Observable<Page<Transaction>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<Page<Transaction>>(`${this.base}/accounts/${id}/transactions`, { params });
  }

  credit(accountId: number, request: CreditRequest): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.base}/accounts/${accountId}/credit`, request);
  }

  debit(accountId: number, request: DebitRequest): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.base}/accounts/${accountId}/debit`, request);
  }
}
