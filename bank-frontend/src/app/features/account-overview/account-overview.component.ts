import { Component, inject, OnInit, OnDestroy, AfterViewInit, signal, ElementRef, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AccountService } from '../../core/services/account.service';
import { Account, Currency, CreditRequest, DebitRequest } from '../../core/models/account.model';
import { Transaction } from '../../core/models/transaction.model';
import { CurrencyFormatPipe } from '../../shared/pipes/currency-format.pipe';
import {
  Chart,
  LineController,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Tooltip,
  Filler,
  Legend,
} from 'chart.js';

Chart.register(
  LineController,
  LineElement,
  PointElement,
  LinearScale,
  CategoryScale,
  Tooltip,
  Filler,
  Legend,
);

const FLAG_MAP: Record<Currency, string> = {
  EUR: '🇪🇺', USD: '🇺🇸', SEK: '🇸🇪', GBP: '🇬🇧', VND: '🇻🇳',
};

@Component({
  selector: 'app-account-overview',
  standalone: true,
  imports: [CommonModule, FormsModule, CurrencyFormatPipe],
  template: `
    <div class="account-page">
      <!-- Back -->
      <div class="back-bar">
        <button class="back-link" (click)="goBack()">← Back to Accounts</button>
      </div>

      @if (loading() && !account()) {
        <div class="spinner-wrapper">
          <div class="spinner"></div>
          <p>Loading account…</p>
        </div>
      }

      @if (error()) {
        <div class="error-banner">
          <span>{{ error() }}</span>
          <button (click)="retryLoad()">Retry</button>
        </div>
      }

      @if (account()) {
        <!-- Top two-column -->
        <div class="top-grid">
          <!-- Account Info -->
          <div class="account-info-panel">
            <div class="flag-large">{{ getFlag(account()!.currency) }}</div>
            <div class="info-currency">{{ account()!.currency }} Account</div>
            <div class="info-balance">
              {{ account()!.balance | currencyFormat: account()!.currency }}
            </div>
            <div class="info-meta">
              Account #{{ account()!.id }} · Since {{ getSinceYear(account()!.createdAt) }}
            </div>
          </div>

          <!-- Quick Actions -->
          <div class="quick-actions card">
            <!-- Credit -->
            <div class="qa-section">
              <div class="qa-header">
                <span class="dot dot-green"></span>
                <span class="qa-label">Credit</span>
              </div>
              <div class="qa-row">
                <input
                  type="number"
                  class="form-input qa-input"
                  placeholder="Amount"
                  min="0.01"
                  step="0.01"
                  [ngModel]="creditAmount()"
                  (ngModelChange)="creditAmount.set($event)"
                  name="creditAmount"
                />
                <select class="form-select qa-input" [ngModel]="creditFromCurrency()" (ngModelChange)="creditFromCurrency.set($event)" name="creditFromCurrency">
                  @for (c of currencies; track c) {
                    <option [value]="c">{{ c }}</option>
                  }
                </select>
              </div>
              <input
                type="text"
                class="form-input qa-input"
                placeholder="Description (optional)"
                [ngModel]="creditDescription()"
                (ngModelChange)="creditDescription.set($event)"
                name="creditDescription"
              />
              <button
                class="btn btn-green btn-full qa-btn"
                (click)="credit()"
                [disabled]="operationLoading() || creditAmount() <= 0"
              >
                {{ operationLoading() ? 'Processing…' : '+ Credit →' }}
              </button>
            </div>

            <div class="qa-divider"></div>

            <!-- Debit -->
            <div class="qa-section">
              <div class="qa-header">
                <span class="dot dot-red"></span>
                <span class="qa-label">Debit</span>
              </div>
              <div class="qa-row">
                <input
                  type="number"
                  class="form-input qa-input"
                  placeholder="Amount"
                  min="0.01"
                  step="0.01"
                  [ngModel]="debitAmount()"
                  (ngModelChange)="debitAmount.set($event)"
                  name="debitAmount"
                />
              </div>
              <input
                type="text"
                class="form-input qa-input"
                placeholder="Description (optional)"
                [ngModel]="debitDescription()"
                (ngModelChange)="debitDescription.set($event)"
                name="debitDescription"
              />
              <button
                class="btn btn-red btn-full qa-btn"
                (click)="debit()"
                [disabled]="operationLoading() || debitAmount() <= 0"
              >
                {{ operationLoading() ? 'Processing…' : '- Debit →' }}
              </button>
            </div>

            @if (operationSuccess()) {
              <div class="success-banner" style="margin-top:12px;">{{ operationSuccess() }}</div>
            }
            @if (operationError()) {
              <div class="error-banner" style="margin-top:12px;">{{ operationError() }}</div>
            }
          </div>
        </div>

        <!-- Chart -->
        <section class="chart-section card">
          <h2 class="section-title">Balance Timeline</h2>
          <!-- BUG FIX: always render canvas (no @if), use style.display to hide; initial build in ngAfterViewInit (Bug 1) -->
          <div class="chart-wrapper" [style.display]="transactions().length === 0 ? 'none' : 'block'">
            <canvas #chartCanvas></canvas>
          </div>
          @if (transactions().length === 0) {
            <div class="chart-placeholder">No transactions yet to display chart.</div>
          }
        </section>

        <!-- Transaction History -->
        <section class="tx-section card">
          <h2 class="section-title">Transaction History</h2>

          @if (transactions().length === 0 && !loadingMore()) {
            <div class="empty-state">No transactions found.</div>
          }

          <ul class="tx-list">
            @for (tx of transactions(); track tx.id) {
              <li
                class="tx-item"
                (click)="goToTransaction(tx.id)"
                role="button"
                tabindex="0"
                (keydown.enter)="goToTransaction(tx.id)"
              >
                <div
                  class="tx-icon"
                  [class.credit]="tx.type === 'CREDIT'"
                  [class.debit]="tx.type === 'DEBIT'"
                >
                  {{ tx.type === 'CREDIT' ? '↑' : '↓' }}
                </div>
                <div class="tx-info">
                  <div class="tx-desc">{{ tx.description || '—' }}</div>
                  <div class="tx-date">{{ tx.createdAt | date: 'mediumDate' }}</div>
                </div>
                <div
                  class="tx-amount"
                  [class.credit]="tx.type === 'CREDIT'"
                  [class.debit]="tx.type === 'DEBIT'"
                >
                  {{ tx.type === 'CREDIT' ? '+' : '-' }}{{ tx.amount | number: '1.2-2' }}
                  <span class="arrow">→</span>
                </div>
              </li>
            }
          </ul>

          @if (loadingMore()) {
            <div class="loading-more">
              <div class="spinner spinner--sm"></div>
            </div>
          }

          @if (!isLastPage()) {
            <div #sentinel class="sentinel"></div>
          }

          @if (isLastPage() && transactions().length > 0) {
            <div class="end-state">All transactions loaded</div>
          }
        </section>
      }
    </div>
  `,
  styleUrl: './account-overview.component.scss',
})
export class AccountOverviewComponent implements OnInit, OnDestroy, AfterViewInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private accountService = inject(AccountService);

  @ViewChild('chartCanvas') chartCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('sentinel') sentinel!: ElementRef<HTMLDivElement>;

  account = signal<Account | null>(null);
  transactions = signal<Transaction[]>([]);
  loading = signal(true);
  loadingMore = signal(false);
  error = signal<string | null>(null);
  isLastPage = signal(false);

  operationLoading = signal(false);
  operationError = signal<string | null>(null);
  operationSuccess = signal<string | null>(null);

  private currentPage = 0;
  private readonly pageSize = 100; // increased so chart gets full history on initial load (25 tx in seed)
  private observer: IntersectionObserver | null = null;
  private chart: Chart | null = null;
  private accountId = 0;

  currencies: Currency[] = ['EUR', 'USD', 'SEK', 'GBP', 'VND'];

  // BUG FIX: convert form fields to signals (Bug 3)
  creditAmount = signal(0);
  creditFromCurrency = signal<Currency>('EUR');
  creditDescription = signal('');
  debitAmount = signal(0);
  debitDescription = signal('');

  ngOnInit(): void {
    this.accountId = Number(this.route.snapshot.paramMap.get('id'));
    this.accountService.getAccount(this.accountId).subscribe({
      next: (account) => {
        this.account.set(account);
        this.loadTransactions();
      },
      error: () => {
        this.error.set('Failed to load account.');
        this.loading.set(false);
      },
    });
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
    this.chart?.destroy();
  }

  ngAfterViewInit(): void {
    // Chart is now built after data arrives (see loadTransactions / credit / debit)
    // because @if (account()) delays the canvas from being in the DOM at ngAfterViewInit time.
  }

  private loadTransactions(): void {
    this.accountService.getTransactions(this.accountId, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.transactions.update((prev) => [...prev, ...page.content]);
        this.isLastPage.set(page.last);
        this.loading.set(false);
        setTimeout(() => {
          // Build (or rebuild) the chart now that data + canvas are ready.
          // Using buildChart ensures creation even on first load (canvas appears only after account() is set).
          this.buildChart();
          if (!this.isLastPage()) {
            this.setupObserver();
          }
        }, 0);
      },
      error: () => {
        this.error.set('Failed to load transactions.');
        this.loading.set(false);
      },
    });
  }

  loadMore(): void {
    if (this.loadingMore() || this.isLastPage()) return;
    this.loadingMore.set(true);
    this.currentPage++;
    this.accountService.getTransactions(this.accountId, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.transactions.update((prev) => [...prev, ...page.content]);
        this.isLastPage.set(page.last);
        this.loadingMore.set(false);
        if (this.isLastPage()) {
          this.observer?.disconnect();
        }
        this.buildChart();
      },
      error: () => {
        this.loadingMore.set(false);
      },
    });
  }

  credit(): void {
    if (this.creditAmount() <= 0) return;

    this.operationLoading.set(true);
    this.operationError.set(null);
    this.operationSuccess.set(null);

    const request: CreditRequest = {
      amount: this.creditAmount(),
      fromCurrency: this.creditFromCurrency(),
      ...(this.creditDescription().trim() && { description: this.creditDescription().trim() }),
    };

    this.accountService.credit(this.accountId, request).subscribe({
      next: (tx) => {
        this.operationLoading.set(false);
        this.operationSuccess.set('Credit applied successfully.');
        setTimeout(() => this.operationSuccess.set(null), 3000);

        // BUG FIX: manually update only the balance using account.update instead of getAccount call (Bug 2)
        this.account.update(acc => acc ? {...acc, balance: tx.balanceAfter} : acc);

        this.transactions.update((prev) => [tx, ...prev]);
        // Rebuild chart with the new transaction included (sorted inside)
        this.buildChart();

        // BUG FIX: reset using .set() on signals (Bug 3)
        this.creditAmount.set(0);
        this.creditFromCurrency.set('EUR');
        this.creditDescription.set('');
      },
      error: (err) => {
        this.operationLoading.set(false);
        this.operationError.set(err.error?.error ?? 'Credit failed. Please try again.');
      },
    });
  }

  debit(): void {
    if (this.debitAmount() <= 0 || !this.account()) return;

    this.operationLoading.set(true);
    this.operationError.set(null);
    this.operationSuccess.set(null);

    const request: DebitRequest = {
      amount: this.debitAmount(),
      currency: this.account()!.currency,
      ...(this.debitDescription().trim() && { description: this.debitDescription().trim() }),
    };

    this.accountService.debit(this.accountId, request).subscribe({
      next: (tx) => {
        this.operationLoading.set(false);
        this.operationSuccess.set('Debit applied successfully.');
        setTimeout(() => this.operationSuccess.set(null), 3000);

        // BUG FIX: manually update only the balance using account.update instead of getAccount call (Bug 2)
        this.account.update(acc => acc ? {...acc, balance: tx.balanceAfter} : acc);

        this.transactions.update((prev) => [tx, ...prev]);
        // Rebuild chart with the new transaction included (sorted inside)
        this.buildChart();

        // BUG FIX: reset using .set() on signals (Bug 3)
        this.debitAmount.set(0);
        this.debitDescription.set('');
      },
      error: (err) => {
        this.operationLoading.set(false);
        this.operationError.set(err.error?.error ?? 'Debit failed. Please try again.');
      },
    });
  }

  private getChartData(): { labels: string[]; data: number[] } {
    const sorted = [...this.transactions()].sort(
      (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
    );
    return {
      labels: sorted.map((t) =>
        new Date(t.createdAt).toLocaleDateString('en-US', {
          month: 'short',
          day: 'numeric',
        }),
      ),
      data: sorted.map((t) => t.balanceAfter),
    };
  }

  private buildChart(): void {
    // Build chart from current transactions data.
    // Called after data loads / updates (not only in ngAfterViewInit) because
    // the canvas is rendered inside @if (account()) and may not exist yet at view init.
    if (!this.chartCanvas?.nativeElement) {
      return;
    }
    const { labels, data } = this.getChartData();
    if (labels.length === 0) {
      if (this.chart) {
        this.chart.destroy();
        this.chart = null;
      }
      return;
    }
    if (this.chart) {
      this.chart.destroy();
      this.chart = null;
    }
    this.chart = new Chart(this.chartCanvas.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'Balance',
            data,
            borderColor: '#EF7B10',
            backgroundColor: 'rgba(239, 123, 16, 0.1)',
            fill: true,
            tension: 0.3,
            pointRadius: 3,
            pointHoverRadius: 5,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          tooltip: {
            mode: 'index',
            intersect: false,
            backgroundColor: '#512B2B',
          },
          legend: { display: false },
        },
        scales: {
          x: {
            grid: { color: '#E8E8E8', lineWidth: 1 },
          },
          y: {
            beginAtZero: false,
            grid: { color: '#E8E8E8', lineWidth: 1 },
          },
        },
      },
    });
    // Ensure Chart.js sizes the canvas correctly after it becomes visible
    setTimeout(() => this.chart?.resize(), 0);
  }

  private updateChart(): void {
    // BUG FIX: only update, build happens only in ngAfterViewInit (Bug 1)
    if (!this.chart) return;
    const { labels, data } = this.getChartData();
    this.chart.data.labels = labels;
    this.chart.data.datasets[0].data = data;
    this.chart.update();
  }

  private setupObserver(): void {
    if (!this.sentinel?.nativeElement) return;
    this.observer = new IntersectionObserver(
      (entries) => {
        if (entries[0].isIntersecting) {
          this.loadMore();
        }
      },
      { threshold: 0.1 },
    );
    this.observer.observe(this.sentinel.nativeElement);
  }

  retryLoad(): void {
    this.error.set(null);
    this.loading.set(true);
    this.accountService.getAccount(this.accountId).subscribe({
      next: (account) => {
        this.account.set(account);
        this.transactions.set([]);
        this.currentPage = 0;
        this.loadTransactions();
      },
      error: () => {
        this.error.set('Failed to load account.');
        this.loading.set(false);
      },
    });
  }

  goToTransaction(id: number): void {
    this.router.navigate(['/transaction', id]);
  }

  goBack(): void {
    this.router.navigate(['/']);
  }

  getFlag(currency: Currency): string {
    return FLAG_MAP[currency] ?? '🏦';
  }

  getSinceYear(dateStr: string): string {
    const d = new Date(dateStr);
    return d.getFullYear().toString();
  }
}
