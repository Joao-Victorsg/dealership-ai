# Implementation Plan: dealership-web — Full Web Application

**Branch**: `001-dealership-web` | **Date**: 2026-05-01 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/001-dealership-web/spec.md`

## Summary

dealership-web is the complete customer-facing and admin Next.js 16 web application for the dealership-ai platform. It provides anonymous car inventory browsing, PKCE-based customer authentication via the dealership-bff (no login page on the web app), customer registration, purchase confirmation with race-condition handling (`CAR_NOT_AVAILABLE`), account profile management, purchase history, admin inventory management, and admin sales reporting. All data flows exclusively through the dealership-bff REST API. The architecture defaults to React Server Components for data fetching, TanStack Query for client-side server state, React Hook Form + Zod for all forms, and the Aurelio Motors design system with Tailwind CSS v4 oklch tokens.

Key research findings (see `research.md` for full analysis):
- BFF PKCE auth flow confirmed (002 migration); `/api/v1/auth/login` and `/api/v1/auth/refresh` are REMOVED
- `imageKey` returned as CDN-relative path — requires `NEXT_PUBLIC_CDN_URL` env var
- `PATCH /api/v1/profile` does NOT accept `streetNumber` or `cpf` — profile form omits these fields
- `POST /api/v1/auth/register` is AUTHENTICATED — user must complete Keycloak flow first; fields: cpf, phone, cep, streetNumber only; firstName/lastName from JWT
- CEP resolution via BFF endpoint `GET /api/v1/cep/{cep}` (⚠️ assumed contract — BFF team must implement)
- 10% tax computed client-side from `listedValue` — not a BFF field
- Admin endpoints not yet in BFF contracts — admin pages use assumed contracts

## Technical Context

**Language/Version**: TypeScript 5.x — strict mode (`"strict": true`; no `any`; no `@ts-ignore` without explanatory comment)  
**Primary Dependencies**: Next.js 16.2.x (App Router only; Pages Router prohibited), React 19.x, Tailwind CSS v4 (`@theme inline`; oklch tokens; v3 patterns prohibited), TanStack Query v5, React Hook Form v7, Zod v3, Zustand v4, Vitest, React Testing Library, MSW v2, Playwright, @axe-core/playwright  
**Storage**: N/A — no direct database access; all persistence via dealership-bff REST API  
**Testing**: Vitest + React Testing Library (unit/integration) + MSW (BFF mock); Playwright + @axe-core/playwright (E2E); MSW Node.js adapter for E2E BFF mocking  
**Target Platform**: Browser (minimum 375px viewport) + Node.js via Next.js SSR/SSG; performance baseline: mid-range mobile device on 4G connection  
**Project Type**: Web application — customer-facing (anonymous + authenticated) + admin  
**Performance Goals**: LCP ≤ 2.5 s, CLS ≤ 0.1, INP ≤ 200 ms on mid-range mobile / 4G  
**Constraints**:
- WCAG 2.1 AA minimum; axe-core scan with 0 violations in every Playwright test
- BFF is the sole upstream API — no direct calls to car-api, client-api, or sales-api
- No login page — authentication redirect goes to `<BFF_URL>/oauth2/authorization/keycloak`
- SESSION cookie is HttpOnly, managed by BFF — web app never reads, stores, or refreshes tokens
- Route protection exclusively in Next.js middleware — never in page components
- BFF base URL via `BFF_URL` (server) / `NEXT_PUBLIC_BFF_URL` (client) env vars; CDN base via `NEXT_PUBLIC_CDN_URL`

**Scale/Scope**: 11 pages across 4 route groups — `(marketing)` public, `(auth)` registration-completion, `(customer)` authenticated, `(admin)` admin-only

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| # | Rule (Article) | Verify |
|---|----------------|--------|
| 1 | No direct calls to car-api / client-api / sales-api — only the BFF (Art. I) | All API calls route through `NEXT_PUBLIC_BFF_URL` / `BFF_URL` |
| 2 | All new components start as Server Components; `use client` is justified (Art. VIII) | Each `use client` file has a comment explaining why |
| 3 | No inline `useEffect` + fetch for server state — use TanStack Query (Art. IX) | No `useEffect` that fetches BFF data |
| 4 | No auth tokens / session IDs in localStorage, sessionStorage, or JS context (Art. III & XIV) | Grep for `localStorage` / `sessionStorage` — 0 hits for auth values |
| 5 | Design system compliance: oklch tokens only, no hardcoded hex/rgb colors (Art. V) | CSS audit — no raw color values outside `globals.css` |
| 6 | Images use `next/image` with explicit dimensions; fonts use `next/font` (Art. XII) | No raw `<img>` tags; no CDN font links |
| 7 | TypeScript strict — no `any`, no `@ts-ignore` without comment (Art. XVII) | `tsc --noEmit` passes with no suppressions |
| 8 | Barrel imports prohibited — every import references the specific source file (Art. XII) | No `import { X } from '../../components'` barrel patterns |
| 9 | WCAG 2.1 AA — every interactive element has focus state; color never sole indicator (Art. XIII) | axe-core scan returns 0 violations |
| 10 | Error UI never exposes stack traces, service names, or HTTP codes (Art. XI) | Error components accept only user-safe message strings |
| 11 | Tests present: unit for `lib/` utils, component tests for forms/filters, E2E for critical journeys (Art. XV) | CI passes with required coverage |
| 12 | `dangerouslySetInnerHTML` not used anywhere (Art. XIV) | Grep returns 0 hits |

## Project Structure

### Documentation (this feature)

```text
specs/001-dealership-web/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/
  (marketing)/
    page.tsx                        ← Home (featured vehicles, call-to-action)
    layout.tsx                      ← Marketing layout (header, footer)
    inventory/
      page.tsx                      ← Inventory listing (RSC; searchParams → BFF)
      [carId]/
        page.tsx                    ← Car detail (RSC; generateMetadata for OG)
  (auth)/
    complete-registration/
      page.tsx                      ← Post-Keycloak completion form (CPF, phone, CEP, streetNumber)
      actions.ts                    ← Server Action: POST /api/v1/auth/register (authenticated)
      schema.ts                     ← Zod registration schema
  (customer)/
    layout.tsx                      ← Customer layout (session guard, sidebar nav)
    purchase/
      [carId]/
        page.tsx                    ← Purchase confirmation page (carId pre-fetch)
        actions.ts                  ← Server Action: POST /api/v1/purchases
      success/
        page.tsx                    ← Purchase success (redirect target)
    account/
      page.tsx                      ← Profile management (RSC + client form)
      actions.ts                    ← Server Action: PATCH /api/v1/profile
      schema.ts                     ← Zod profile update schema
      purchases/
        page.tsx                    ← Purchase history (RSC, paginated)
  (admin)/
    layout.tsx                      ← Admin layout (ROLE_ADMIN guard, sidebar)
    admin/
      page.tsx                      ← Inventory management table
      actions.ts                    ← Server Actions: POST/PATCH /api/v1/admin/inventory
      sales/
        page.tsx                    ← Sales reports + KPI cards
      settings/
        page.tsx                    ← Admin settings (future placeholder)
  layout.tsx                        ← Root layout: fonts, ThemeProvider, QueryProvider
  globals.css                       ← Aurelio Motors design system (single source)

middleware.ts                       ← Route protection (auth + role guard)

lib/
  actions/
    cep.ts                          ← CEP lookup Server Action (calls lib/api/cep.ts)
  api/
    inventory.ts                    ← BFF inventory client functions
    auth.ts                         ← BFF auth client functions (register, logout)
    profile.ts                      ← BFF profile client functions
    purchases.ts                    ← BFF purchases client functions
    admin.ts                        ← BFF admin client functions (assumed contracts)
    cep.ts                          ← BFF CEP lookup client function (⚠️ assumed contract)
    types.ts                        ← All BFF TypeScript types (see data-model.md)
    client.ts                       ← fetch wrapper: BFF_URL base, error parsing, revalidation
  utils/
    cpf.ts                          ← isValidCpf, CPF_REGEX, cpfMask
    cep.ts                          ← CEP_REGEX, CEP_REGEX_MASKED, cepMask
    phone.ts                        ← isValidPhone, PHONE_REGEX, phoneMask
  format.ts                         ← formatBRL, formatKm, formatDate
  errors.ts                         ← BffErrorCode → user-safe Portuguese message map
  pricing.ts                        ← TAX_RATE, computeTax, computeTotal

components/
  ui/                               ← Design system primitives (shadcn/ui base)
  inventory/
    CarCard.tsx                     ← Grid card (client — uses Intersection Observer)
    CarGrid.tsx                     ← CSS grid layout wrapper
    InventoryFilters.tsx            ← Filter panel (lazy loaded — dynamic import)
    FilterChips.tsx                 ← Active filter chips with clear buttons
    InventorySkeleton.tsx           ← Loading skeleton for grid
  car/
    CarDetail.tsx                   ← Full detail layout (RSC-compatible)
    BuyPanel.tsx                    ← CTA panel with price breakdown (client)
    CarImageGallery.tsx             ← next/image carousel with priority on [0]
  purchase/
    FinancialSummary.tsx            ← listedValue + tax + total display (tabular)
    ConfirmButton.tsx               ← Submit with loading state and race-condition guard
  account/
    ProfileForm.tsx                 ← RHF form with CEP auto-fill + read-only fields
    AddressPreview.tsx              ← Read-only address display (streetNumber gap noted)
    PurchaseHistoryCard.tsx         ← Purchase card (snapshot-aware, handles missing fields)
  admin/
    InventoryTable.tsx              ← Sortable table with status badges (⚠️ assumed contracts)
    CarFormModal.tsx                ← Create/edit car modal (⚠️ assumed contracts)
    SalesTable.tsx                  ← Sales report table (⚠️ assumed contracts)
    KpiCards.tsx                    ← 4-up KPI summary (⚠️ assumed contracts)
  layout/
    SiteHeader.tsx                  ← Nav, theme toggle, auth CTA (client for theme)
    SiteFooter.tsx                  ← Footer links
    MobileDrawer.tsx                ← Mobile nav drawer (dynamic import)
    SkipLink.tsx                    ← Accessibility skip-to-main link
  shared/
    EmptyState.tsx                  ← Zero results / permission denied displays
    ErrorDisplay.tsx                ← User-safe error card (no stack traces)
    LoadingSkeleton.tsx             ← Reusable skeleton primitive

e2e/
  inventory.spec.ts                 ← Browse, filter, paginate, mobile viewport
  car-detail.spec.ts                ← Detail page, OG meta, buy CTA visibility
  purchase.spec.ts                  ← Happy path, CAR_NOT_AVAILABLE, auth redirect
  auth-redirect.spec.ts             ← Unauthenticated access → BFF redirect
  purchase-history.spec.ts          ← Auth'd list, pagination, empty state
  mocks/                            ← MSW Node.js adapter handlers for E2E
    handlers.ts                     ← All BFF endpoint mocks
    auth.ts                         ← Session simulation helpers
```

**Structure Decision**: Single Next.js 16 App Router application. Route groups (`(marketing)`, `(auth)`, `(customer)`, `(admin)`) provide layout nesting without URL prefix overhead. All BFF access is centralized in `lib/api/`. Design system lives in `app/globals.css` as a single source of truth.

## Complexity Tracking

| Item | Why Needed | Simpler Alternative Rejected Because |
|------|------------|-------------------------------------|
| CEP resolution via BFF endpoint | No direct ViaCEP calls from web app — BFF owns external dependencies; `lib/api/cep.ts` calls `GET /api/v1/cep/{cep}` (⚠️ assumed — BFF team must implement) | Direct ViaCEP Server Action would create a hidden external dependency in the web layer and violate the BFF-as-sole-gateway principle |
| Admin pages use assumed contracts | BFF admin spec not yet published; admin is in-scope per spec.md | Deferring admin to a separate feature would require spec revision and stakeholder approval |
| Dark mode blocking inline script in `<head>` | Prevents flash-of-incorrect-theme on first load; localStorage read must happen before paint | Any SSR cookie alternative requires BFF changes; FOTC is unacceptable per design review |

---

## Open Items (Blockers for Specific Tasks)

These gaps will block certain implementation tasks unless resolved first. Each one must
be addressed before the dependent task can start, or implemented with an explicit
placeholder and a TODO comment referencing this section.

| # | Open Item | Blocks | Resolution Path |
|---|-----------|--------|-----------------|
| OI-1 | **Middleware auth detection strategy** — SESSION cookie is `HttpOnly`, so `middleware.ts` cannot read it from JavaScript. It is unknown whether the BFF sets a second readable cookie, a response header, or whether the middleware must make a probe request to confirm auth state. | `middleware.ts`, `(customer)/layout.tsx`, `(admin)/layout.tsx` | Confirm with BFF team whether `/api/v1/profile` can be used as a lightweight auth probe from the edge, or whether BFF can be configured to set a readable `authenticated=1` cookie alongside SESSION. Until resolved, implement middleware using a probe request to `HEAD /api/v1/profile` and cache the result per request cycle. |
| OI-2 | **Admin role claim name** — R-08 confirms admin access requires `ROLE_ADMIN`, but the Keycloak claim name exposed in the session (as seen by Next.js middleware) is unknown. Could be a cookie, a decoded JWT claim, or a BFF-managed claim in a separate readable token. | `(admin)/layout.tsx` admin role guard, `middleware.ts` | Confirm with BFF team what claim or mechanism signals ROLE_ADMIN to the web layer. Placeholder: use same probe pattern as OI-1 (`GET /api/v1/admin/inventory` returns 403 for non-admin, 200/404 for admin) to infer role without reading JWT. |
| OI-3 | **Dark mode inline script SHA-256 hash** — The blocking `<script>` tag in `<head>` that reads `localStorage.theme` and sets `.dark` on `<html>` requires its SHA-256 hash to be listed in the CSP `script-src` directive in `next.config.ts`. The exact script content, and therefore the hash, cannot be computed until the script is finalized. | `app/layout.tsx` (dark mode script), `next.config.ts` (CSP headers) | Write the inline script first; run `echo -n '<script>...</script>' | openssl dgst -sha256 -binary | openssl base64` to produce the hash; add to `Content-Security-Policy` header in `next.config.ts`. |
| OI-4 | **CDN domain for `images.remotePatterns`** — `NEXT_PUBLIC_CDN_URL` is documented as required (R-01) but the actual domain (e.g., `cdn.dealership.example.com` or a local MinIO URL) is not defined in any contract. `next/image` will refuse to serve unconfigured domains. | `next.config.ts`, all `next/image` usages of car images | Confirm CDN/S3 domain with infra team. For local dev, use MinIO or point to BFF static proxy. Add to `images.remotePatterns` in `next.config.ts` and to `.env.local.example`. |

---

## Implementation Phases

Phases define the correct dependency order. Each phase must be complete before the next
begins. Within a phase, tasks in the same phase can run in parallel.

### Phase A — Bootstrap & Configuration
*No source code yet; all repository scaffolding and tooling.*

1. `package.json` — verify all required packages installed; add missing ones
2. `tsconfig.json` — confirm `"strict": true`, `paths` aliases for `@/lib/*`, `@/components/*`
3. `next.config.ts` — add `images.remotePatterns` for CDN domain (see OI-4); add CSP headers (see OI-3); configure `experimental.serverActions`
4. `app/globals.css` — port Aurelio Motors design system from `docs/front-dealership-example/src/styles.css`; add `@source` directives for Tailwind v4 (`@source "../app"`, `@source "../components"`, `@source "../lib"`)
5. `.env.local.example` — document all required env vars (`BFF_URL`, `NEXT_PUBLIC_BFF_URL`, `NEXT_PUBLIC_CDN_URL`, `NEXT_PUBLIC_APP_URL`)
6. Vitest config — `vitest.config.ts` with jsdom, MSW browser adapter, path aliases
7. Playwright config — `playwright.config.ts` with base URL, MSW Node adapter, axe-core plugin
8. `e2e/mocks/handlers.ts` — skeleton MSW handler file for all BFF endpoints (stubs only at this stage)

### Phase B — Foundation Library Layer
*All code in `lib/`. No UI components yet. Fully unit-testable in isolation.*

1. `lib/api/types.ts` — copy types verbatim from `data-model.md` (all 10 sections)
2. `lib/api/client.ts` — BFF fetch wrapper: base URL from env, `BffErrorResponse` parsing, typed throws
3. `lib/errors.ts` — `getBFFErrorMessage()` with all error codes from R-13
4. `lib/pricing.ts` — `TAX_RATE`, `computeTax()`, `computeTotal()` from R-06 / data-model.md §5
5. `lib/format.ts` — `formatBRL()`, `formatKm()`, `formatDate()` (locale: `pt-BR`)
6. `lib/utils/cpf.ts` — `CPF_REGEX`, `isValidCpf()`, `cpfMask()` from R-12
7. `lib/utils/cep.ts` — `CEP_REGEX`, `CEP_REGEX_MASKED`, `cepMask()` from R-12
8. `lib/utils/phone.ts` — `PHONE_REGEX`, `isValidPhone()`, `phoneMask()` from R-12
9. `lib/api/cep.ts` — `lookupCep()` BFF client function calling `GET /api/v1/cep/{cep}` (⚠️ assumed contract — see R-05)
10. `lib/api/inventory.ts` — typed BFF client functions for `GET /api/v1/inventory` and `GET /api/v1/inventory/{id}`
11. `lib/api/auth.ts` — typed BFF client functions for `POST /api/v1/auth/register` (authenticated) and `POST /api/v1/auth/logout`
12. `lib/api/profile.ts` — typed BFF client functions for `GET /api/v1/profile` and `PATCH /api/v1/profile`
13. `lib/api/purchases.ts` — typed BFF client functions for `POST /api/v1/purchases` and `GET /api/v1/purchases`
14. `lib/api/admin.ts` — typed BFF client functions for all assumed admin endpoints (see OI-2)

*Unit tests required: `lib/errors.ts`, `lib/pricing.ts`, `lib/format.ts`, `lib/utils/*`, `lib/api/cep.ts` (MSW mock of BFF CEP endpoint)*

### Phase C — Application Shell
*Root layout, middleware, shared layout components, design system wiring. No feature pages yet.*

1. `app/layout.tsx` — root layout: `next/font` wiring for Geist + Bricolage Grotesque; dark mode inline script (see OI-3); `QueryClientProvider`; `ThemeProvider`; `SkipLink`
2. `middleware.ts` — route protection: `(customer)/*` requires auth; `(admin)/*` requires auth + admin role (see OI-1, OI-2 for strategy)
3. `components/layout/SiteHeader.tsx` — nav, Sign In CTA (redirect to BFF auth URL), theme toggle, mobile menu trigger
4. `components/layout/SiteFooter.tsx`
5. `components/layout/SkipLink.tsx`
6. `components/layout/MobileDrawer.tsx` — `next/dynamic` lazy import
7. `components/shared/ErrorDisplay.tsx` — accepts `message: string`, `requestId?: string`; never accepts raw error objects
8. `components/shared/EmptyState.tsx`
9. `components/shared/LoadingSkeleton.tsx`
10. `app/(marketing)/layout.tsx` — wraps SiteHeader + SiteFooter
11. `app/(customer)/layout.tsx` — session-assumed layout (middleware guarantees auth)
12. `app/(admin)/layout.tsx` — admin-role-assumed layout (middleware guarantees role)

### Phase D — Public Pages (Inventory + Home)
*Spec US1 — Browse Inventory. No auth required.*

1. `components/inventory/InventorySkeleton.tsx`
2. `components/inventory/CarGrid.tsx`
3. `components/inventory/FilterChips.tsx`
4. `components/inventory/InventoryFilters.tsx` — lazy via `next/dynamic`; Zustand `useInventoryStore` for panel open/close
5. `components/inventory/CarCard.tsx` — `use client`; Intersection Observer for lazy image load; `CarCardProps` from data-model.md §2
6. `app/(marketing)/inventory/page.tsx` — RSC; `searchParams` → `InventoryQueryParams`; Suspense with skeleton; `keepPreviousData` pattern per R-14
7. `components/car/CarImageGallery.tsx` — `next/image` with `priority` on index 0; aspect-ratio container for CLS per R-14
8. `components/car/CarDetail.tsx` — RSC-compatible; all Car fields; OG meta via `generateMetadata`
9. `components/car/BuyPanel.tsx` — `use client`; sticky panel; status-aware CTA; links to purchase route
10. `app/(marketing)/inventory/[carId]/page.tsx` — RSC; `notFound()` on 404; `generateMetadata`
11. `app/(marketing)/page.tsx` — home: hero, category tiles (link to `/inventory?category=X`), recently added grid (last 4 from inventory API)

*E2E tests: `inventory.spec.ts`, `car-detail.spec.ts`*

### Phase E — Registration Completion
*Spec US2 — Post-Keycloak profile completion. Requires Phase C (middleware) to protect the `(auth)/complete-registration` route.*

1. `app/(auth)/complete-registration/schema.ts` — Zod schema with CPF check-digit, phone, CEP, and streetNumber validation per R-12; imports from `lib/utils/*`
2. `app/(auth)/complete-registration/actions.ts` — Server Action calling `lib/api/auth.ts` register function (authenticated — requires valid SESSION cookie); maps VALIDATION_ERROR to field errors; maps other codes via `lib/errors.ts`
3. `app/(auth)/complete-registration/page.tsx` — `use client`; middleware ensures user is authenticated (SESSION exists); React Hook Form with `zodResolver`; CEP auto-fill on blur via `lib/api/cep.ts` per R-05 flow; address preview read-only fields; submission success → redirect to home page or `/account`

*Component tests: registration form CEP auto-fill, CPF validation, VALIDATION_ERROR field display*

### Phase F — Purchase Flow
*Spec US4 — Purchase Confirmation. Requires Phase D (car detail) and Phase C (middleware).*

1. `components/purchase/FinancialSummary.tsx` — `listedValue`, computed tax, total from `lib/pricing.ts`; `.tabular` class on prices
2. `components/purchase/ConfirmButton.tsx` — `use client`; disabled after first click (prevents double-submit); pending state; handles `CAR_NOT_AVAILABLE` (409) with targeted inline message per contracts/bff-api.md
3. `app/(customer)/purchase/[carId]/page.tsx` — RSC; fetches car detail; `notFound()` on 404; renders FinancialSummary + ConfirmButton
4. `app/(customer)/purchase/[carId]/actions.ts` — Server Action: `POST /api/v1/purchases`; on success `redirect()` to `/purchase/success`; on 409 returns `CAR_NOT_AVAILABLE` state
5. `app/(customer)/purchase/success/page.tsx` — success confirmation; link to purchase history

*E2E tests: `purchase.spec.ts` (happy path, 409 race, auth redirect)*

### Phase G — Account & Profile
*Spec US5 + US6 — Account Profile and Purchase History.*

1. `components/account/AddressPreview.tsx` — read-only display; TODO comment referencing OI streetNumber gap (R-04)
2. `components/account/ProfileForm.tsx` — `use client`; React Hook Form with profile schema; CEP auto-fill; `address.number` displayed read-only via AddressPreview
3. `app/(customer)/account/schema.ts` — Zod profile schema per data-model.md §4
4. `app/(customer)/account/actions.ts` — Server Action: `PATCH /api/v1/profile`; does NOT send `cpf` or `streetNumber`
5. `app/(customer)/account/page.tsx` — RSC; fetches profile; renders ProfileForm
6. `components/account/PurchaseHistoryCard.tsx` — displays only snapshot fields returned by BFF (R-07); TODO comment for missing fields
7. `app/(customer)/account/purchases/page.tsx` — RSC; paginated; renders PurchaseHistoryCards; empty state

*E2E tests: `purchase-history.spec.ts`*

### Phase H — Admin
*Spec US7 + US8 — Admin Inventory and Sales. Requires OI-2 resolved.*

1. `components/admin/KpiCards.tsx` — 4-up summary cards; assumed contract shapes
2. `components/admin/SalesTable.tsx` — assumed contract shapes; date range filter
3. `components/admin/InventoryTable.tsx` — assumed contract shapes; status badge; sort
4. `components/admin/CarFormModal.tsx` — `next/dynamic`; assumed contract shapes; validates with `CreateCarRequest` types
5. `app/(admin)/admin/page.tsx` — RSC; admin inventory management
6. `app/(admin)/admin/actions.ts` — Server Actions for create/update car
7. `app/(admin)/admin/sales/page.tsx` — RSC; sales reports + KPI cards
8. `app/(admin)/admin/settings/page.tsx` — placeholder

### Phase I — E2E, Accessibility & Performance Hardening
*Cross-cutting concerns. Runs after all pages are built.*

1. Complete `e2e/mocks/handlers.ts` — all BFF endpoint handlers with realistic fixture data
2. `e2e/mocks/auth.ts` — session simulation: authenticated user, admin user, unauthenticated
3. `e2e/auth-redirect.spec.ts` — middleware redirect tests
4. Axe-core validation pass — run `pnpm e2e` and fix all axe violations
5. Lighthouse / Web Vitals audit — verify LCP ≤ 2.5 s on inventory page with throttled network
6. `tsc --noEmit` clean — resolve all type errors
7. `pnpm lint` clean — resolve all ESLint violations including barrel import rule

---

## Cross-Reference Index

When implementing a file, consult the referenced detail documents for the full
specification. This table maps key implementation areas to their authoritative sources.

| Implementation Area | Primary Source | Supporting Sources |
|---------------------|----------------|--------------------|
| `lib/api/types.ts` | [data-model.md](data-model.md) — all 10 sections | [contracts/bff-api.md](contracts/bff-api.md) — response shapes |
| `lib/api/client.ts` — error handling | [data-model.md §1](data-model.md) — `BffErrorResponse`, `BffErrorCode` | [research.md R-13](research.md) — error code list; [contracts/bff-api.md](contracts/bff-api.md) — per-endpoint error codes |
| `lib/errors.ts` | [research.md R-13](research.md) — all error codes + Portuguese strings | — |
| `lib/pricing.ts` | [research.md R-06](research.md) — TAX_RATE decision | [data-model.md §5](data-model.md) — type signatures |
| `lib/utils/cpf.ts`, `cep.ts`, `phone.ts` | [research.md R-12](research.md) — regex constants, masking functions | [data-model.md §3](data-model.md) — schema usage |
| `lib/actions/cep.ts` | [research.md R-05](research.md) — full CEP flow (trigger, request, response, error) | [data-model.md §7](data-model.md) — `ViaCepResponse`, `CepLookupResult` types; [contracts/bff-api.md §CEP](contracts/bff-api.md) |
| `lib/api/inventory.ts` | [contracts/bff-api.md §Inventory](contracts/bff-api.md) — params, response, error codes | [data-model.md §2](data-model.md) — `Car`, `InventoryQueryParams`, `CarListResponse` |
| `lib/api/auth.ts` | [contracts/bff-api.md §Auth](contracts/bff-api.md) — register + logout endpoints | [data-model.md §3](data-model.md) — `RegisterRequest`; [research.md R-02](research.md) — REMOVED endpoints list |
| `lib/api/profile.ts` | [contracts/bff-api.md §Profile](contracts/bff-api.md) — GET + PATCH, accepted fields | [data-model.md §4](data-model.md) — `CustomerProfile`, `UpdateProfileRequest`; [research.md R-04](research.md) — streetNumber gap |
| `lib/api/purchases.ts` | [contracts/bff-api.md §Purchase](contracts/bff-api.md) — POST + GET, 409 handling | [data-model.md §5](data-model.md) — `Purchase`, `CreatePurchaseRequest`; [research.md R-07](research.md) — snapshot completeness |
| `lib/api/admin.ts` | [contracts/bff-api.md §Admin](contracts/bff-api.md) — assumed endpoints | [data-model.md §6](data-model.md) — admin types; [research.md R-08](research.md) — admin scope decision |
| `middleware.ts` | [research.md R-02](research.md) — PKCE auth; [Open Items OI-1, OI-2](#open-items-blockers-for-specific-tasks) | [spec.md US3](spec.md) — auth acceptance scenarios |
| `app/layout.tsx` (dark mode script) | [research.md R-10](research.md) — inline script, localStorage key, CSP hash | [Open Items OI-3](#open-items-blockers-for-specific-tasks) |
| `app/globals.css` | [research.md R-09](research.md) — porting instructions, @source directives, font loading | `docs/front-dealership-example/src/styles.css` (source) |
| `next.config.ts` | [research.md R-01](research.md) — `images.remotePatterns` for CDN; [OI-3, OI-4](#open-items-blockers-for-specific-tasks) — CSP hash, CDN domain | — |
| `app/(marketing)/inventory/page.tsx` | [research.md R-14](research.md) — performance patterns (skeleton, keepPreviousData, Suspense) | [data-model.md §8](data-model.md) — `InventoryFilters`, `DEFAULT_FILTERS`; [research.md R-11](research.md) — TQ query keys |
| `app/(marketing)/inventory/[carId]/page.tsx` | [contracts/bff-api.md §Inventory](contracts/bff-api.md) — 404 handling | [research.md R-14](research.md) — CLS prevention (priority image, aspect-ratio) |
| `components/inventory/InventoryFilters.tsx` | [data-model.md §8](data-model.md) — `InventoryFilters`, `DEFAULT_FILTERS` | [research.md R-11](research.md) — Zustand `useInventoryStore`; [research.md R-14](research.md) — `keepPreviousData`, URL sync |
| `components/car/BuyPanel.tsx` | [spec.md US1 SC4](spec.md) — status-aware CTA | [data-model.md §10](data-model.md) — CarStatus transitions |
| `app/(marketing)/register/page.tsx` | [research.md R-03](research.md) — route placement; [R-05](research.md) — CEP flow | [data-model.md §3](data-model.md) — `RegisterRequest`; [contracts/bff-api.md §Auth](contracts/bff-api.md) — register endpoint |
| `components/purchase/ConfirmButton.tsx` | [contracts/bff-api.md §Purchase](contracts/bff-api.md) — 409 CAR_NOT_AVAILABLE | [spec.md US4](spec.md) — acceptance scenarios; [research.md R-06](research.md) — tax computation |
| `components/account/ProfileForm.tsx` | [research.md R-04](research.md) — streetNumber gap; [R-05](research.md) — CEP auto-fill | [data-model.md §4](data-model.md) — `UpdateProfileRequest`; [contracts/bff-api.md §Profile](contracts/bff-api.md) |
| `components/account/PurchaseHistoryCard.tsx` | [research.md R-07](research.md) — snapshot completeness (missing fields) | [data-model.md §5](data-model.md) — `PurchaseVehicleSnapshot` |
| `components/purchase/FinancialSummary.tsx` | [research.md R-06](research.md) — TAX_RATE, formula | [data-model.md §5](data-model.md) — `computeTax`, `computeTotal` |
| Admin components (`InventoryTable`, `CarFormModal`, `SalesTable`, `KpiCards`) | [contracts/bff-api.md §Admin](contracts/bff-api.md) — ⚠️ assumed shapes | [data-model.md §6](data-model.md) — admin types; [Open Items OI-2](#open-items-blockers-for-specific-tasks) |
| MSW handlers (`e2e/mocks/handlers.ts`) | [contracts/bff-api.md](contracts/bff-api.md) — all endpoint shapes | [data-model.md](data-model.md) — fixture type shapes; [research.md R-07](research.md) — purchase snapshot fields |
