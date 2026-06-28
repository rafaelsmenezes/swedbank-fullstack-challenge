import { Component, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { switchMap } from 'rxjs/operators';
import { TransactionService } from '../../core/services/transaction.service';
import { Transaction } from '../../core/models/transaction.model';
import { CurrencyFormatPipe } from '../../shared/pipes/currency-format.pipe';
import { Currency } from '../../core/models/account.model';
import { AccountService } from '../../core/services/account.service';
import { Account } from '../../core/models/account.model';
import jsPDF from 'jspdf';

const FLAG_MAP: Record<Currency, string> = {
  EUR: '🇪🇺', USD: '🇺🇸', SEK: '🇸🇪', GBP: '🇬🇧', VND: '🇻🇳',
};

@Component({
  selector: 'app-transaction-overview',
  standalone: true,
  imports: [CommonModule, CurrencyFormatPipe],
  template: `
    <div class="tx-page">
      <div class="back-bar">
        <button class="back-link" (click)="goBack()">← Back to Account</button>
      </div>

      @if (loading()) {
        <div class="spinner-wrapper">
          <div class="spinner"></div>
          <p>Loading…</p>
        </div>
      }

      @if (error()) {
        <div class="error-banner">
          <span>{{ error() }}</span>
        </div>
      }

      @if (!loading() && !error() && transaction()) {
        <!-- Hero -->
        <div class="hero" [class.hero-credit]="transaction()!.type === 'CREDIT'" [class.hero-debit]="transaction()!.type === 'DEBIT'">
          <div class="hero-type">
            {{ transaction()!.type === 'CREDIT' ? '↑ CREDIT' : '↓ DEBIT' }}
          </div>
          <div class="hero-amount">
            {{ transaction()!.type === 'CREDIT' ? '+' : '-' }}{{ transaction()!.amount | currencyFormat: getCurrency() }}
          </div>
          <div class="hero-meta">
            {{ transaction()!.description || 'Transaction' }} · {{ transaction()!.createdAt | date: 'mediumDate' }}
          </div>
        </div>

        <!-- Details -->
        <section class="details-card card">
          <h2 class="section-title">Transaction Details</h2>

          <dl class="detail-table">
            <div class="detail-row">
              <dt>Transaction ID</dt>
              <dd>#{{ transaction()!.id }}</dd>
            </div>
            <div class="detail-row">
              <dt>Account</dt>
              <dd>#{{ transaction()!.accountId }}</dd>
            </div>
            <div class="detail-row">
              <dt>Type</dt>
              <dd>
                <span class="type-badge" [class.credit]="transaction()!.type === 'CREDIT'" [class.debit]="transaction()!.type === 'DEBIT'">
                  {{ transaction()!.type }}
                </span>
              </dd>
            </div>
            <div class="detail-row">
              <dt>Amount</dt>
              <dd>{{ transaction()!.amount | currencyFormat: getCurrency() }}</dd>
            </div>
            <div class="detail-row">
              <dt>Balance After</dt>
              <dd>{{ transaction()!.balanceAfter | currencyFormat: getCurrency() }}</dd>
            </div>
            <div class="detail-row">
              <dt>Description</dt>
              <dd>{{ transaction()!.description || '—' }}</dd>
            </div>
            <div class="detail-row">
              <dt>Date</dt>
              <dd>{{ transaction()!.createdAt | date: 'medium' }}</dd>
            </div>
          </dl>
        </section>

        <!-- Export -->
        <div class="export-row">
          <button
            class="btn btn-orange btn-full"
            (click)="exportPdf()"
            [disabled]="exporting()"
          >
            ⬇ {{ exporting() ? 'Generating…' : 'Export PDF' }}
          </button>
        </div>
      }
    </div>
  `,
  styleUrl: './transaction-overview.component.scss',
})
export class TransactionOverviewComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private transactionService = inject(TransactionService);
  private accountService = inject(AccountService);

  transaction = signal<Transaction | null>(null);
  account = signal<Account | null>(null);
  loading = signal(true);
  error = signal<string | null>(null);
  exporting = signal(false);

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));

    this.transactionService.getTransaction(id).pipe(
      switchMap(tx => {
        this.transaction.set(tx);
        return this.accountService.getAccount(tx.accountId);
      })
    ).subscribe({
      next: (account) => {
        this.account.set(account);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Failed to load transaction.');
        this.loading.set(false);
      }
    });
  }

  goBack(): void {
    const tx = this.transaction();
    if (tx) {
      this.router.navigate(['/account', tx.accountId]);
    } else {
      this.router.navigate(['/']);
    }
  }

  getCurrency(): Currency {
    return this.account()?.currency ?? 'EUR';
  }

  exportPdf(): void {
    const tx = this.transaction();
    const acc = this.account();
    if (!tx) return;

    this.exporting.set(true);

    const doc = new jsPDF({ unit: 'mm', format: 'a4' });
    const currency = (acc?.currency ?? 'EUR') as Currency;
    const pageW = doc.internal.pageSize.getWidth();
    const margin = 20;
    const colVal = 90;

    doc.setFillColor(74, 44, 143);
    doc.rect(0, 0, pageW, 28, 'F');

    doc.setTextColor(255, 255, 255);
    doc.setFontSize(16);
    doc.setFont('helvetica', 'bold');
    doc.text('Transaction Receipt', margin, 18);

    doc.setFontSize(9);
    doc.setFont('helvetica', 'normal');
    doc.text(`Generated ${new Date().toLocaleString('en-GB')}`, pageW - margin, 18, {
      align: 'right',
    });

    const isCredit = tx.type === 'CREDIT';
    doc.setTextColor(isCredit ? 46 : 198, isCredit ? 125 : 40, isCredit ? 50 : 40);
    doc.setFontSize(28);
    doc.setFont('helvetica', 'bold');
    const sign = isCredit ? '+' : '-';
    doc.text(`${sign}${tx.amount.toFixed(2)} ${currency}`, margin, 50);

    doc.setFontSize(11);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(120, 120, 120);
    doc.text(isCredit ? 'Credit' : 'Debit', margin, 58);

    doc.setDrawColor(220, 220, 220);
    doc.setLineWidth(0.4);
    doc.line(margin, 64, pageW - margin, 64);

    const rows: { label: string; value: string }[] = [
      { label: 'Transaction ID', value: String(tx.id) },
      { label: 'Account ID', value: String(tx.accountId) },
      { label: 'Type', value: tx.type },
      { label: 'Amount', value: `${tx.amount.toFixed(2)} ${currency}` },
      { label: 'Balance After', value: `${tx.balanceAfter.toFixed(2)} ${currency}` },
      { label: 'Description', value: tx.description ?? '—' },
      { label: 'Date', value: new Date(tx.createdAt).toLocaleString('en-GB') },
    ];

    let y = 76;
    const rowH = 12;

    rows.forEach((row, i) => {
      if (i % 2 === 0) {
        doc.setFillColor(248, 246, 255);
        doc.rect(margin - 4, y - 6, pageW - margin * 2 + 8, rowH, 'F');
      }
      doc.setFontSize(10);
      doc.setFont('helvetica', 'bold');
      doc.setTextColor(80, 80, 80);
      doc.text(row.label, margin, y);

      doc.setFont('helvetica', 'normal');
      doc.setTextColor(30, 30, 30);
      doc.text(row.value, colVal, y);

      y += rowH;
    });

    const pageH = doc.internal.pageSize.getHeight();
    doc.setFillColor(74, 44, 143);
    doc.rect(0, pageH - 12, pageW, 12, 'F');
    doc.setFontSize(8);
    doc.setTextColor(255, 255, 255);
    doc.setFont('helvetica', 'normal');
    doc.text('Swedbank Demo — Confidential', pageW / 2, pageH - 4.5, { align: 'center' });

    doc.save(`transaction-${tx.id}.pdf`);
    this.exporting.set(false);
  }
}
