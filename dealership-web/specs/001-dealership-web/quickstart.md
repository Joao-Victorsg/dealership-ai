# Quickstart: dealership-web — Full Web Application

**Date**: 2026-05-01  
**Branch**: `001-dealership-web`

---

## Prerequisites

- **Node.js** 22 LTS or later
- **pnpm** 9 or later (`npm i -g pnpm`)
- **dealership-bff** running locally on port `8080` (or set `BFF_URL` to a dev/staging BFF)
- Keycloak accessible to the BFF (not directly required by the web app)

---

## Initial Setup

```bash
# From the dealership-web/ directory:
pnpm install

# Copy environment template
cp .env.local.example .env.local
```

Edit `.env.local`:

```env
# BFF base URL (server-side — used in Server Components and Server Actions)
BFF_URL=http://localhost:8080

# BFF base URL (client-side — used in Client Components; safe to expose)
NEXT_PUBLIC_BFF_URL=http://localhost:8080

# CDN/S3 base URL for constructing car image URLs from imageKey
# Example: https://cdn.dealership.example.com
# For local dev, can be the same as BFF_URL if BFF proxies images
NEXT_PUBLIC_CDN_URL=http://localhost:8080/static

# Next.js Base URL (for CSP, OAuth redirect allow-listing)
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

---

## Development

```bash
pnpm dev        # starts Next.js dev server on http://localhost:3000
pnpm build      # production build
pnpm start      # start production build
pnpm lint       # ESLint check
pnpm type-check # tsc --noEmit
```

---

## Testing

```bash
# Unit and integration tests (Vitest + React Testing Library + MSW)
pnpm test               # run once
pnpm test:watch         # watch mode
pnpm test:coverage      # with coverage report

# End-to-end tests (Playwright)
pnpm e2e                # headless on all configured browsers
pnpm e2e:ui             # Playwright UI mode (interactive)
pnpm e2e:headed         # headed (visible browser)

# Accessibility (axe-core runs inside every Playwright test automatically)
# Run pnpm e2e and inspect the axe-core assertion results in the report
```

MSW is configured as the BFF mock in all test layers. The mock handlers live in
`e2e/mocks/` (Playwright Node.js adapter) and `src/__mocks__/` (Vitest browser).

---

## Key Environment Variables

| Variable | Required | Description |
|----------|----------|-------------|
| `BFF_URL` | ✅ | BFF base URL — server-side only |
| `NEXT_PUBLIC_BFF_URL` | ✅ | BFF base URL — client-side |
| `NEXT_PUBLIC_CDN_URL` | ✅ | CDN base for car image construction |
| `NEXT_PUBLIC_APP_URL` | ✅ | App base URL (for CSP, OAuth redirects) |

No secrets belong in `NEXT_PUBLIC_*` variables. Never commit `.env.local`.

---

## Project Structure (Key Files)

```text
app/
  (marketing)/
    page.tsx                  ← Home page
    register/page.tsx         ← Registration form
    inventory/page.tsx        ← Inventory listing (with filters)
    inventory/[carId]/page.tsx ← Car detail
  (auth)/
    complete-registration/    ← Future: post-Keycloak profile completion
  (customer)/
    purchase/[carId]/page.tsx ← Purchase confirmation
    purchase/success/page.tsx ← Purchase success
    account/page.tsx          ← Profile management
    account/purchases/page.tsx ← Purchase history
  (admin)/
    admin/page.tsx            ← Inventory management
    admin/sales/page.tsx      ← Sales reports
    admin/settings/page.tsx   ← Admin settings
  layout.tsx                  ← Root layout (fonts, ThemeProvider, QueryClientProvider)
  globals.css                 ← Design system (Aurelio Motors tokens + utilities)
middleware.ts                 ← Route protection (auth + role check)
lib/
  api/
    inventory.ts              ← BFF inventory client functions
    auth.ts                   ← BFF auth client functions
    profile.ts                ← BFF profile client functions
    purchases.ts              ← BFF purchases client functions
    admin.ts                  ← BFF admin client functions (assumed contracts)
    types.ts                  ← All BFF TypeScript types
  actions/
    cep.ts                    ← CEP lookup Server Action (ViaCEP)
  utils/
    cpf.ts                    ← CPF validation + masking
    cep.ts                    ← CEP regex constants
    phone.ts                  ← Phone regex constants
  format.ts                   ← Currency (BRL), kilometers, date formatters
  errors.ts                   ← BFF error code → user-safe message map
  pricing.ts                  ← TAX_RATE, computeTax, computeTotal
components/
  ui/                         ← Design system primitives (shadcn/ui base)
  inventory/                  ← CarCard, CarGrid, InventoryFilters, FilterChips
  car/                        ← CarDetail, BuyPanel, CarImageGallery
  purchase/                   ← FinancialSummary, ConfirmButton
  account/                    ← ProfileForm, PurchaseHistoryCard, AddressPreview
  admin/                      ← InventoryTable, CarFormModal, SalesTable, KpiCards
  layout/                     ← SiteHeader, SiteFooter, MobileDrawer, SkipLink
  shared/                     ← EmptyState, ErrorDisplay, LoadingSkeleton
e2e/
  inventory.spec.ts
  car-detail.spec.ts
  purchase.spec.ts
  auth-redirect.spec.ts
  purchase-history.spec.ts
  mocks/                      ← MSW Node.js adapter handlers for E2E
```

---

## Authentication Flow (Dev)

1. Start the BFF locally (`./mvnw spring-boot:run` in `dealership-bff/`)
2. The BFF must be configured with a Keycloak realm — see `dealership-bff` quickstart
3. Navigate to `http://localhost:3000` → click "Sign in" → redirected to BFF → redirected to Keycloak
4. After login, you land back at the app with a `SESSION` cookie
5. To test without a live Keycloak, use the MSW mock session handler in `e2e/mocks/auth.ts`

For E2E tests, the MSW Node.js adapter intercepts all BFF calls. No live BFF or Keycloak is required during test runs.

---

## Design System

The `app/globals.css` file is the canonical copy of the Aurelio Motors design
system (ported from `docs/front-dealership-example/src/styles.css`). All design
tokens are available as Tailwind utility classes via `@theme inline`.

Key tokens in use:

| Class | Token | Description |
|-------|-------|-------------|
| `bg-background` | `--background` | Warm off-white page background |
| `bg-surface` | `--surface` | Sidebar, secondary surface |
| `bg-card` | `--card` | White card containers |
| `text-primary` | `--primary` | Deep oxblood accent |
| `border-border` | `--border` | Standard border |
| `font-display` | `--font-display` | Bricolage Grotesque |
| `font-sans` | `--font-sans` | Geist body |
| `font-mono` | `--font-mono` | Geist Mono (prices, numbers) |
| `tabular` | — | `font-variant-numeric: tabular-nums` for prices/km |
| `grain` | — | Hero noise overlay (pseudo-element) |
| `reveal` + `reveal-1..4` | — | Staggered page-load animations |

Dark mode: Add `.dark` class to `<html>`. Theme toggle persists to `localStorage`.

---

## Known Constraints and Open Items

| Item | Status | Reference |
|------|--------|-----------|
| `streetNumber` not updatable in PATCH /api/v1/profile | BFF contract gap | research.md R-04 |
| Purchase history vehicle snapshot missing internalColor, type, optionalItems, imageKey | BFF contract gap | research.md R-07 |
| Admin endpoints not in BFF contracts — assumed | Pending BFF admin spec | research.md R-08 |
| CEP lookup uses ViaCEP (external) not BFF | No BFF CEP endpoint | research.md R-05 |
| `(auth)/complete-registration` — deferred | Future Keycloak-native registration | research.md R-03 |
| Dark mode inline script — SHA-256 hash required in CSP `script-src` | CSP configuration | research.md R-10 |
| `imageKey` → full URL requires `NEXT_PUBLIC_CDN_URL` env var | Env configuration | research.md R-01 |
