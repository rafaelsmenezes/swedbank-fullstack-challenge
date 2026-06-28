import { Component, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Account } from '../../core/models/account.model';
import { AccountService } from '../../core/services/account.service';
import { CurrencyFormatPipe } from '../../shared/pipes/currency-format.pipe';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, CurrencyFormatPipe],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  private accountService = inject(AccountService);
  private router = inject(Router);

  accounts = signal<Account[]>([]);
  loading = signal(true);
  error = signal<string | null>(null);

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
      },
      error: (err) => {
        this.error.set('Não foi possível carregar as contas.');
        this.loading.set(false);
        console.error(err);
      },
    });
  }

  goToAccount(id: number): void {
    this.router.navigate(['/account', id]);
  }
}