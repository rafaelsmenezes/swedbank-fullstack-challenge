import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Account, CreditRequest, Currency } from '../../core/models/account.model';
import { AccountService } from '../../core/services/account.service';
import { CurrencyFormatPipe } from '../../shared/pipes/currency-format.pipe';

interface QuickTransferForm {
  toAccountId: number | null;
  amount: number | null;
  fromCurrency: Currency;
  description: string;
}

const FLAG_MAP: Record<Currency, string> = {
  EUR: '🇪🇺',
  USD: '🇺🇸',
  SEK: '🇸🇪',
  GBP: '🇬🇧',
  VND: '🇻🇳',
};

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, FormsModule, CurrencyFormatPipe],
  template: `
    <div class="home-page">
      <div class="home-header">
        <h1 class="greeting">Good morning, Rafael 👋</h1>
        <p class="subtitle">Here's your financial overview</p>
      </div>

      <div class="home-content">
        <!-- Quick Transfer -->
        <section class="quick-transfer card">
          <div class="qt-header">
            <span class="qt-icon">⚡</span>
            <h2 class="qt-title">Quick Transfer</h2>
          </div>

          @if (transferSuccess()) {
            <div class="success-banner">
              <span>{{ transferSuccess() }}</span>
            </div>
          }

          @if (transferError()) {
            <div class="error-banner">
              <span>{{ transferError() }}</span>
              <button type="button" (click)="clearTransferMessages()">Dismiss</button>
            </div>
          }

          <form (ngSubmit)="submitTransfer()" class="qt-form">
            <div class="form-row">
              <div class="form-group">
                <label class="form-label">To account</label>
                <select
                  class="form-select"
                  [ngModel]="transferToAccountId()"
                  (ngModelChange)="transferToAccountId.set($event)"
                  name="toAccountId"
                  required
                >
                  <option [ngValue]="null" disabled>Select account</option>
                  @for (acc of accounts(); track acc.id) {
                    <option [ngValue]="acc.id">
                      {{ acc.currency }} Account — {{ acc.balance | currencyFormat: acc.currency }}
                    </option>
                  }
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">Amount</label>
                <input
                  type="number"
                  class="form-input"
                  [ngModel]="transferAmount()"
                  (ngModelChange)="transferAmount.set($event)"
                  name="amount"
                  placeholder="0.00"
                  min="0.01"
                  step="0.01"
                  required
                />
              </div>
            </div>

            <div class="form-row">
              <div class="form-group">
                <label class="form-label">From currency</label>
                <select
                  class="form-select"
                  [ngModel]="transferFromCurrency()"
                  (ngModelChange)="transferFromCurrency.set($event)"
                  name="fromCurrency"
                >
                  @for (ccy of currencies; track ccy) {
                    <option [value]="ccy">{{ ccy }}</option>
                  }
                </select>
              </div>

              <div class="form-group">
                <label class="form-label">Description (optional)</label>
                <input
                  type="text"
                  class="form-input"
                  [ngModel]="transferDescription()"
                  (ngModelChange)="transferDescription.set($event)"
                  name="description"
                  placeholder="e.g. Rent payment"
                />
              </div>
            </div>

            <button
              type="submit"
              class="btn btn-orange btn-full"
              [disabled]="transferLoading() || !isTransferValid()"
            >
              {{ transferLoading() ? 'Processing…' : 'Transfer →' }}
            </button>
          </form>
        </section>

        <!-- Your Accounts -->
        <section class="accounts-section">
          <h2 class="accounts-title">Your Accounts ({{ accounts().length }})</h2>

          @if (loading()) {
            <div class="skeleton-grid">
              @for (i of [1, 2, 3]; track i) {
                <div class="skeleton-card">
                  <div class="skeleton-badge"></div>
                  <div class="skeleton-line wide"></div>
                  <div class="skeleton-line narrow"></div>
                </div>
              }
            </div>
          }

          @if (error()) {
            <div class="error-banner">
              <span>{{ error() }}</span>
              <button type="button" (click)="loadAccounts()">Retry</button>
            </div>
          }

          @if (!loading() && !error()) {
            @if (accounts().length === 0) {
              <div class="empty-state">No accounts found.</div>
            } @else {
              <div class="accounts-grid">
                @for (account of accounts(); track account.id) {
                  <div
                    class="account-card"
                    (click)="goToAccount(account.id)"
                    role="button"
                    tabindex="0"
                    (keydown.enter)="goToAccount(account.id)"
                  >
                    <div class="account-card__top">
                      <span class="flag">{{ getFlag(account.currency) }}</span>
                      <span class="currency-badge">{{ account.currency }}</span>
                    </div>
                    <div class="account-card__balance">
                      {{ account.balance | currencyFormat: account.currency }}
                    </div>
                    <div class="account-card__label">Balance</div>
                    <div class="account-card__footer">
                      <span class="view-link link-orange">View Account →</span>
                    </div>
                  </div>
                }
              </div>
            }
          }
        </section>
      </div>
    </div>
  `,
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  private accountService = inject(AccountService);
  private router = inject(Router);

  accounts = signal<Account[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

  transferLoading = signal(false);
  transferSuccess = signal<string | null>(null);
  transferError = signal<string | null>(null);

  currencies: Currency[] = ['EUR', 'USD', 'SEK', 'GBP', 'VND'];

  // BUG FIX: convert quick transfer form fields to signals (same as Bug 3 in account overview)
  transferToAccountId = signal<number | null>(null);
  transferAmount = signal(0);
  transferFromCurrency = signal<Currency>('EUR');
  transferDescription = signal('');

  ngOnInit(): void {
    this.loadAccounts();
  }

  loadAccounts(): void {
    this.loading.set(true);
    this.error.set(null);
    this.accountService.getAccounts(1).subscribe({
      next: (data) => {
        this.accounts.set(data);
        this.loading.set(false);
        // Reset form account selection if accounts changed
        if (this.transferToAccountId() && !data.some(a => a.id === this.transferToAccountId())) {
          this.transferToAccountId.set(null);
        }
      },
      error: (err) => {
        this.error.set('Failed to load accounts.');
        this.loading.set(false);
        console.error(err);
      },
    });
  }

  getFlag(currency: Currency): string {
    return FLAG_MAP[currency] ?? '🏦';
  }

  isTransferValid(): boolean {
    return !!this.transferToAccountId() && this.transferAmount() > 0;
  }

  submitTransfer(): void {
    if (!this.isTransferValid() || this.transferLoading()) return;

    const accountId = this.transferToAccountId()!;
    const amount = this.transferAmount();

    this.transferLoading.set(true);
    this.transferError.set(null);
    this.transferSuccess.set(null);

    const request: CreditRequest = {
      amount,
      fromCurrency: this.transferFromCurrency(),
      ...(this.transferDescription().trim() && { description: this.transferDescription().trim() }),
    };

    this.accountService.credit(accountId, request).subscribe({
      next: (tx) => {
        this.transferLoading.set(false);
        const newBalance = tx.balanceAfter;
        const currency = this.getAccountCurrency(accountId) || this.transferFromCurrency();

        this.transferSuccess.set(
          `Transfer successful! New balance: ${this.formatAmount(newBalance, currency)}`
        );

        // BUG FIX: update specific account balance in signal instead of full reload
        this.accounts.update(list => list.map(a => 
          a.id === accountId ? {...a, balance: tx.balanceAfter} : a
        ));

        // BUG FIX: reset using .set() (Bug fix for home quick transfer)
        this.transferAmount.set(0);
        this.transferDescription.set('');
        this.transferToAccountId.set(null);

        // Auto dismiss after 4s
        setTimeout(() => {
          this.transferSuccess.set(null);
        }, 4000);
      },
      error: (err) => {
        this.transferLoading.set(false);
        const msg = err?.error?.error || err?.error?.message || 'Transfer failed. Please try again.';
        this.transferError.set(msg);
      },
    });
  }

  private getAccountCurrency(id: number): Currency | null {
    const acc = this.accounts().find(a => a.id === id);
    return acc ? acc.currency : null;
  }

  private formatAmount(value: number, currency: Currency): string {
    const symbols: Record<Currency, string> = {
      EUR: '€', USD: '$', SEK: 'kr', GBP: '£', VND: '₫',
    };
    const sym = symbols[currency] ?? currency;
    const formatted = new Intl.NumberFormat('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
    return `${sym}${formatted}`;
  }

  clearTransferMessages(): void {
    this.transferError.set(null);
    this.transferSuccess.set(null);
  }

  goToAccount(id: number): void {
    this.router.navigate(['/account', id]);
  }
}