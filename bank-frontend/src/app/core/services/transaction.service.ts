import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Transaction } from '../models/transaction.model';

@Injectable({ providedIn: 'root' })
export class TransactionService {
  private http = inject(HttpClient);
  private base = '/api/v1';

  getTransaction(id: number): Observable<Transaction> {
    return this.http.get<Transaction>(`${this.base}/transactions/${id}`);
  }
}
