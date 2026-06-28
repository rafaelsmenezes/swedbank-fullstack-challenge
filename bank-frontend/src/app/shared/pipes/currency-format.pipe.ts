import { Pipe, PipeTransform } from '@angular/core';
import { Currency } from '../../core/models/account.model';

const CURRENCY_SYMBOLS: Record<Currency, string> = {
  EUR: '€',
  USD: '$',
  SEK: 'kr',
  GBP: '£',
  VND: '₫',
};

@Pipe({ name: 'currencyFormat', standalone: true })
export class CurrencyFormatPipe implements PipeTransform {
  transform(value: number, currency: Currency): string {
    const symbol = CURRENCY_SYMBOLS[currency] ?? currency;
    const formatted = new Intl.NumberFormat('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(value);
    return `${symbol} ${formatted}`;
  }
}