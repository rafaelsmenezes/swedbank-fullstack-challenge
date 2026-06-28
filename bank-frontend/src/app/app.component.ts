import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `
    <!-- Top Navigation Bar -->
    <nav class="top-nav">
      <div class="nav-content">
        <div class="nav-left">
          <svg viewBox="0 0 24 24" fill="currentColor" width="28" height="28" class="crown-icon">
            <path d="M2 19h20v2H2v-2zm2-7l3 3 5-8 5 8 3-3v5H4v-5z"/>
          </svg>
          <span class="brand-name">Swedbank</span>
        </div>
        <div class="nav-right">
          <span class="user-email">rafael@bank.com</span>
          <span class="status-dot" aria-hidden="true"></span>
        </div>
      </div>
    </nav>

    <main class="main-content">
      <router-outlet />
    </main>
  `,
  styles: [`
    .top-nav {
      height: 64px;
      background: #512B2B;
      position: sticky;
      top: 0;
      z-index: 100;
      box-shadow: 0 4px 12px rgba(0,0,0,0.10);
    }
    .nav-content {
      max-width: 1280px;
      margin: 0 auto;
      height: 100%;
      padding: 0 24px;
      display: flex;
      align-items: center;
      justify-content: space-between;
    }
    .nav-left {
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .crown-icon {
      color: #FFFFFF;
      flex-shrink: 0;
    }
    .brand-name {
      color: #FFFFFF;
      font-weight: 700;
      font-size: 1.25rem;
      letter-spacing: -0.01em;
    }
    .nav-right {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .user-email {
      color: #FDE8D0;
      font-size: 0.95rem;
      font-weight: 500;
    }
    .status-dot {
      width: 10px;
      height: 10px;
      background: #2E7D32;
      border-radius: 50%;
      display: inline-block;
    }
    .main-content {
      min-height: calc(100vh - 64px);
    }
  `],
})
export class AppComponent {}
