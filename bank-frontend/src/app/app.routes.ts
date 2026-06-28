import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./features/home/home.component').then((m) => m.HomeComponent),
  },
  {
    path: 'account/:id',
    loadComponent: () =>
      import('./features/account-overview/account-overview.component').then(
        (m) => m.AccountOverviewComponent,
      ),
  },
  {
    path: 'transaction/:id',
    loadComponent: () =>
      import('./features/transaction-overview/transaction-overview.component').then(
        (m) => m.TransactionOverviewComponent,
      ),
  },
  { path: '**', redirectTo: '' },
];
