import { Component, inject, OnInit, OnDestroy, signal, ElementRef, ViewChild } from '@angular/core';
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

@Component({
  selector: 'app-account-overview',
  standalone: true,
  imports: [CommonModule, FormsModule, CurrencyFormatPipe],
  templateUrl: './account-overview.component.html',
  styleUrl: './account-overview.component.scss',
})
export class AccountOverviewComponent implements OnInit, OnDestroy {
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
  private readonly pageSize = 20;
  private observer: IntersectionObserver | null = null;
  private chart: Chart | null = null;
  private accountId = 0;

  currencies: Currency[] = ['EUR', 'USD', 'SEK', 'GBP', 'VND'];

  creditAmount = 0;
  creditFromCurrency: Currency = 'EUR';
  creditDescription = '';

  debitAmount = 0;
  debitDescription = '';

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

  private loadTransactions(): void {
    this.accountService.getTransactions(this.accountId, this.currentPage, this.pageSize).subscribe({
      next: (page) => {
        this.transactions.update((prev) => [...prev, ...page.content]);
        this.isLastPage.set(page.last);
        this.loading.set(false);
        setTimeout(() => {
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
        this.updateChart();
      },
      error: () => {
        this.loadingMore.set(false);
      },
    });
  }

  credit(): void {
    if (this.creditAmount <= 0) return;

    this.operationLoading.set(true);
    this.operationError.set(null);
    this.operationSuccess.set(null);

    const request: CreditRequest = {
      amount: this.creditAmount,
      fromCurrency: this.creditFromCurrency,
      ...(this.creditDescription.trim() && { description: this.creditDescription.trim() }),
    };

    this.accountService.credit(this.accountId, request).subscribe({
      next: (tx) => {
        this.operationLoading.set(false);
        this.operationSuccess.set('Credit applied successfully.');
        setTimeout(() => this.operationSuccess.set(null), 3000);

        this.accountService.getAccount(this.accountId).subscribe({
          next: (account) => this.account.set(account),
        });

        this.transactions.update((prev) => [tx, ...prev]);
        this.updateChart();

        this.creditAmount = 0;
        this.creditFromCurrency = 'EUR';
        this.creditDescription = '';
      },
      error: (err) => {
        this.operationLoading.set(false);
        this.operationError.set(err.error?.error ?? 'Credit failed. Please try again.');
      },
    });
  }

  debit(): void {
    if (this.debitAmount <= 0) return;

    this.operationLoading.set(true);
    this.operationError.set(null);
    this.operationSuccess.set(null);

    const request: DebitRequest = {
      amount: this.debitAmount,
      currency: this.account()!.currency,
      ...(this.debitDescription.trim() && { description: this.debitDescription.trim() }),
    };

    this.accountService.debit(this.accountId, request).subscribe({
      next: (tx) => {
        this.operationLoading.set(false);
        this.operationSuccess.set('Debit applied successfully.');
        setTimeout(() => this.operationSuccess.set(null), 3000);

        this.accountService.getAccount(this.accountId).subscribe({
          next: (account) => this.account.set(account),
        });

        this.transactions.update((prev) => [tx, ...prev]);
        this.updateChart();

        this.debitAmount = 0;
        this.debitDescription = '';
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
        new Date(t.createdAt).toLocaleDateString('en-GB', {
          month: 'short',
          day: 'numeric',
        }),
      ),
      data: sorted.map((t) => t.balanceAfter),
    };
  }

  private buildChart(): void {
    if (!this.chartCanvas?.nativeElement || this.transactions().length === 0) return;
    if (this.chart) {
      this.chart.destroy();
      this.chart = null;
    }
    const { labels, data } = this.getChartData();
    this.chart = new Chart(this.chartCanvas.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'Balance',
            data,
            borderColor: '#4a2c8f',
            backgroundColor: 'rgba(74, 44, 143, 0.1)',
            fill: true,
            tension: 0.3,
            pointRadius: 3,
            pointHoverRadius: 6,
          },
        ],
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          tooltip: { mode: 'index', intersect: false },
          legend: { display: false },
        },
        scales: {
          x: { grid: { display: false } },
          y: { beginAtZero: false },
        },
      },
    });
  }

  private updateChart(): void {
    if (!this.chart) {
      this.buildChart();
      return;
    }
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

  goToTransaction(id: number): void {
    this.router.navigate(['/transaction', id]);
  }

  goBack(): void {
    this.router.navigate(['/']);
  }
}
