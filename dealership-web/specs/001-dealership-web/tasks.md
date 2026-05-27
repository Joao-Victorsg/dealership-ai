# Tasks: dealership-web — Full Web Application

**Branch**: `001-dealership-web` | **Date**: 2026-05-01  
**Input**: Design documents from `specs/001-dealership-web/`  
**Prerequisites**: [plan.md](plan.md) · [spec.md](spec.md) · [research.md](research.md) · [data-model.md](data-model.md) · [contracts/bff-api.md](contracts/bff-api.md) · [quickstart.md](quickstart.md)

> **⚠️ Before starting**: Read `plan.md §Open Items` (OI-1 through OI-4) — four blockers must be resolved
> during implementation. OI-1 (middleware auth detection) blocks Phase 3 US3 tasks.

---

## Format: `- [ ] [ID] [P?] [Story?] Description — file path`

- **[P]**: Parallelizable — touches different files, no dependency on incomplete tasks in the same phase
- **[US#]**: Belongs to user story phase only (Setup and Foundational phases have no story label)
- All IDs are sequential in execution order

---

## Phase 1: Setup — Bootstrap & Configuration

**Purpose**: Repository scaffolding, tooling configuration, and mock infrastructure skeleton. No source
code yet. All tasks can be run in parallel after T001.

**Reference**: plan.md §Implementation Phases (Phase A)

- [x] T001 Verify `package.json` — confirm all required packages present; install missing ones: `next@16`, `react@19`, `@tanstack/react-query@5`, `react-hook-form@7`, `zod@3`, `zustand@4`, `vitest`, `@testing-library/react`, `msw@2`, `@playwright/test`, `@axe-core/playwright`, `geist`
- [x] T002 [P] Configure `tsconfig.json` — set `"strict": true`; add path aliases `@/lib/*` → `./lib/*`, `@/components/*` → `./components/*`
- [x] T003 [P] Configure `next.config.ts` — stub `images.remotePatterns` (placeholder domain; see plan OI-4); stub CSP headers (see plan OI-3); confirm `serverActions` enabled — file: `next.config.ts`
- [x] T004 [P] Create `.env.local.example` — document all required env vars: `BFF_URL`, `NEXT_PUBLIC_BFF_URL`, `NEXT_PUBLIC_CDN_URL`, `NEXT_PUBLIC_APP_URL` with example values — file: `.env.local.example`
- [x] T005 [P] Configure Vitest — jsdom environment, MSW browser adapter, path aliases matching tsconfig — file: `vitest.config.ts`
- [x] T006 [P] Configure Playwright — base URL from `NEXT_PUBLIC_APP_URL`, MSW Node.js adapter, axe-core accessibility plugin — file: `playwright.config.ts`
- [x] T007 [P] Create MSW handler skeleton and auth helpers — stub handlers for every BFF endpoint (empty responses, fill in Phase 11); auth session fixture stubs — files: `e2e/mocks/handlers.ts`, `e2e/mocks/auth.ts`

**Checkpoint**: Tooling configured — `pnpm build` and `pnpm test` both run without crashing

---

## Phase 2: Foundational — Library Layer & Application Shell

**Purpose**: All `lib/` code and the application shell (layout, middleware, shared components). No feature
pages yet. The entire library layer is fully unit-testable before any UI exists.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

**Reference**: plan.md §Implementation Phases (Phase B + Phase C); data-model.md; research.md R-09 through R-14

### 2a — Type Definitions & Utilities (all [P] after T008)

- [x] T008 Create `lib/api/types.ts` — copy all types verbatim from data-model.md §1–§8: `BffResponse<T>`, `BffPageMeta`, `BffErrorResponse`, `BffErrorCode`, `Car`, `CarStatus`, `CarCategory`, `CarType`, `InventoryQueryParams`, `CarCardProps`, `RegisterRequest`, `CustomerProfile`, `UpdateProfileRequest`, `Purchase`, `CreatePurchaseRequest`, `SalesKpiSummary`, `CepLookupResponse`, `CepLookupResult`, `InventoryFilters`, `DEFAULT_FILTERS` — file: `lib/api/types.ts`
- [x] T009 [P] Create `lib/api/client.ts` — typed `bffFetch()` wrapper: reads `BFF_URL` / `NEXT_PUBLIC_BFF_URL` from env; parses `BffErrorResponse`; throws typed `BffError` with `code` and `requestId`; handles 401 by throwing `AUTHENTICATION_REQUIRED` — file: `lib/api/client.ts`
- [x] T010 [P] Create `lib/errors.ts` — `getBFFErrorMessage(code: BffErrorCode): string` with all 10 error codes mapped to Portuguese user-safe strings per research.md R-13; unknown code fallback — file: `lib/errors.ts`
- [x] T011 [P] Create `lib/pricing.ts` — `TAX_RATE = 0.10`, `computeTax(listedValue: number): number`, `computeTotal(listedValue: number): number` per research.md R-06 and data-model.md §5 — file: `lib/pricing.ts`
- [x] T012 [P] Create `lib/format.ts` — `formatBRL(value: number): string` (pt-BR `R$ X.XXX,XX`), `formatKm(value: number): string` (thousands separator + "km"), `formatDate(iso: string): string` ("DD de MMM. de YYYY" pt-BR) per FR-041 — file: `lib/format.ts`
- [x] T013 [P] Create `lib/utils/cpf.ts` — `CPF_REGEX` (masked), `isValidCpf(cpf: string): boolean` (check-digit algorithm), `cpfMask(value: string): string` — module-scope constants per research.md R-12 — file: `lib/utils/cpf.ts`
- [x] T014 [P] Create `lib/utils/cep.ts` — `CEP_REGEX` (raw 8 digits), `CEP_REGEX_MASKED` (XXXXX-XXX), `cepMask(value: string): string` — module-scope constants per research.md R-12 — file: `lib/utils/cep.ts`
- [x] T015 [P] Create `lib/utils/phone.ts` — `PHONE_REGEX` (raw 10-11 digits), `isValidPhone(phone: string): boolean`, `phoneMask(value: string): string` — module-scope constants per research.md R-12 — file: `lib/utils/phone.ts`

### 2b — BFF API Client Functions (all [P], depend on T008 + T009)

- [x] T016 [P] Create `lib/api/inventory.ts` — `getInventory(params: InventoryQueryParams): Promise<CarListResponse>` and `getCarById(id: string): Promise<CarDetailResponse>` using `bffFetch()` per contracts/bff-api.md §Inventory — file: `lib/api/inventory.ts`
- [x] T017 [P] Create `lib/api/auth.ts` — `registerUser(body: RegisterRequest): Promise<void>` and `logoutUser(): Promise<void>` using `bffFetch()` per contracts/bff-api.md §Auth; note: `POST /api/v1/auth/register` is AUTHENTICATED — requires SESSION cookie (Keycloak flow must complete first); note: `/api/v1/auth/login` and `/api/v1/auth/refresh` are REMOVED (research.md R-02) — file: `lib/api/auth.ts`
- [x] T018 [P] Create `lib/api/profile.ts` — `getProfile(): Promise<CustomerProfile>` and `updateProfile(body: UpdateProfileRequest): Promise<CustomerProfile>` using `bffFetch()` per contracts/bff-api.md §Profile; do NOT include `cpf` or `streetNumber` in update body (research.md R-04) — file: `lib/api/profile.ts`
- [x] T019 [P] Create `lib/api/purchases.ts` — `confirmPurchase(carId: string): Promise<Purchase>` and `getPurchases(page?: number): Promise<PurchaseListResponse>` using `bffFetch()` per contracts/bff-api.md §Purchase — file: `lib/api/purchases.ts`
- [x] T020 [P] Create `lib/api/admin.ts` — `getAdminInventory()`, `createCar(body: CreateCarRequest)`, `updateCar(id, body: UpdateCarRequest)`, `getSalesReport(params: SalesQueryParams)` using `bffFetch()` per contracts/bff-api.md §Admin (⚠️ assumed contracts — data-model.md §6) — file: `lib/api/admin.ts`
- [x] T021 [P] Create `lib/api/cep.ts` — `lookupCep(cep: string): Promise<CepLookupResult>` BFF client function; calls `GET ${BFF_URL}/api/v1/cep/{cep}` via `bffFetch()`; maps 200 success response to `{ ok: true, street, neighborhood, city, state }`; maps 404/network errors to `{ ok: false, error: string }` per contracts/bff-api.md §CEP Lookup (⚠️ assumed contract — BFF team must implement endpoint) — file: `lib/api/cep.ts`

### 2c — Design System & Application Shell (sequential after 2a/2b)

- [x] T022 Port Aurelio Motors design system to `app/globals.css` — copy from `docs/front-dealership-example/src/styles.css`; add `@source "../app"`, `@source "../components"`, `@source "../lib"` Tailwind v4 directives; confirm `@custom-variant dark (&:is(.dark *))` and `@theme inline` are present per research.md R-09 — file: `app/globals.css`
- [x] T023 [P] Create `components/shared/ErrorDisplay.tsx` — accepts `message: string` and `requestId?: string` props only; renders user-safe error card with support reference; never accepts raw `Error` objects (FR-042, SC-008) — file: `components/shared/ErrorDisplay.tsx`
- [x] T024 [P] Create `components/shared/EmptyState.tsx` — accepts `title: string`, `description: string`, `action?: ReactNode`; used for zero-results and empty purchase history — file: `components/shared/EmptyState.tsx`
- [x] T025 [P] Create `components/shared/LoadingSkeleton.tsx` — reusable animated skeleton primitive (pulse animation via design system tokens) — file: `components/shared/LoadingSkeleton.tsx`
- [x] T026 [P] Create `components/layout/SkipLink.tsx` — renders first focusable element on page: `<a href="#main-content">Ir para conteúdo</a>` per FR-039 — file: `components/layout/SkipLink.tsx`
- [x] T027 [P] Create `components/layout/SiteFooter.tsx` — footer links, copyright — file: `components/layout/SiteFooter.tsx`
- [x] T028 Create `components/layout/SiteHeader.tsx` — `use client` (theme toggle requires browser API); site nav; "Criar conta" CTA linking to `${process.env.NEXT_PUBLIC_BFF_URL}/oauth2/authorization/keycloak-register`; "Entrar" CTA linking to `${process.env.NEXT_PUBLIC_BFF_URL}/oauth2/authorization/keycloak`; theme toggle button; mobile menu button triggering `useInventoryStore.isFilterPanelOpen` toggle; logout button (calls `logoutUser()` Server Action when session active) per research.md R-02, R-10 — file: `components/layout/SiteHeader.tsx`
- [x] T029 Create `components/layout/MobileDrawer.tsx` — `next/dynamic` lazy import; mobile navigation drawer; Zustand `useInventoryStore` open/close — file: `components/layout/MobileDrawer.tsx`
- [x] T030 Create `app/layout.tsx` — root layout: `next/font/google` for Bricolage Grotesque; `next/font/local` for Geist + Geist Mono from `geist` package; CSS vars on `<html>`; dark mode blocking inline `<script>` reading `localStorage.theme` + applying `.dark` (see plan OI-3 for SHA-256 step); `<QueryClientProvider>`; `<SkipLink>` — file: `app/layout.tsx`
- [x] T031 Create `middleware.ts` — protect `(customer)/*` routes (auth probe, see plan OI-1); protect `(admin)/*` routes (auth + admin role probe, see plan OI-2); preserve return URL as `?redirect=<path>` when redirecting unauthenticated users (FR-011, FR-012, FR-013) — file: `middleware.ts`
- [x] T032 [P] Create `app/(marketing)/layout.tsx` — wraps `<SiteHeader>` + `<SiteFooter>`; no auth requirement — file: `app/(marketing)/layout.tsx`
- [x] T033 [P] Create `app/(customer)/layout.tsx` — session-assumed layout (middleware guarantees auth); sidebar navigation with links to /account, /account/purchases — file: `app/(customer)/layout.tsx`
- [x] T034 [P] Create `app/(admin)/layout.tsx` — admin-role-assumed layout (middleware guarantees ROLE_ADMIN); admin sidebar with links to /admin, /admin/sales, /admin/settings — file: `app/(admin)/layout.tsx`

**Checkpoint**: `pnpm build` succeeds with shell. `pnpm test` passes unit tests for `lib/`. Middleware redirects work. No feature pages yet.

---

## Phase 2d: Unit & Mutation Testing — Library Layer

**Purpose**: Establish 90%+ unit test coverage and 90%+ mutation score for all `lib/` modules with logic. Run after Phase 2a/2b (library layer complete) but before any feature pages are implemented.

**Tooling**: Vitest + React Testing Library + MSW v2 (for HTTP mocks); Stryker `@stryker-mutator/core` + `@stryker-mutator/vitest-runner` for mutation testing.

**Reference**: plan.md §Phase B; data-model.md; constitution Art. XV

- [x] T089  Configure Stryker mutation testing — install `@stryker-mutator/core` and `@stryker-mutator/vitest-runner`; create `stryker.config.mjs` targeting `lib/**/*.ts`; set thresholds: `mutationScore: 90`, `lines: 90`, `branches: 90`; add `pnpm mutate` script to `package.json` — files: `stryker.config.mjs`, `package.json`
- [x] T090  [P] Unit tests for `lib/api/client.ts` — test: happy-path response parsing, typed `BffError` throw on 4xx/5xx, `AUTHENTICATION_REQUIRED` throw on 401, `BFF_URL` env var routing (server), `NEXT_PUBLIC_BFF_URL` env var routing (client); MSW v2 handler mocks for all cases; 90% coverage — file: `tests/unit/lib/api/client.test.ts`
- [x] T091  [P] Unit tests for `lib/errors.ts` — test: all 9 known `BffErrorCode` values return expected Portuguese string, unknown code returns generic fallback string; 100% branch coverage — file: `tests/unit/lib/errors.test.ts`
- [x] T092  [P] Unit tests for `lib/pricing.ts` — test: `computeTax()` returns 10% of input, `computeTotal()` returns 110% of input, `TAX_RATE === 0.10`, edge: 0 value, large integer values; 100% branch coverage — file: `tests/unit/lib/pricing.test.ts`
- [x] T093  [P] Unit tests for `lib/format.ts` — test: `formatBRL()` produces `R$\u00a0X.XXX,XX` pt-BR format for typical/large/zero/decimal values, `formatKm()` produces thousands-separated value with "km" suffix, `formatDate()` produces "DD de MMM. de YYYY" pt-BR; locale boundary tests; 90% coverage — file: `tests/unit/lib/format.test.ts`
- [x] T094  [P] Unit tests for `lib/utils/cpf.ts` — test: `isValidCpf()` returns true for known valid CPF, false for known invalid check digit, false for all-same-digit edge cases, false for wrong length; `cpfMask()` produces `XXX.XXX.XXX-XX` format; 100% branch coverage — file: `tests/unit/lib/utils/cpf.test.ts`
- [x] T095  [P] Unit tests for `lib/utils/cep.ts` — test: `CEP_REGEX` matches 8-digit CEP, rejects shorter/longer; `CEP_REGEX_MASKED` matches `XXXXX-XXX`, rejects unmasked; `cepMask()` inserts hyphen at correct position; 100% branch coverage — file: `tests/unit/lib/utils/cep.test.ts`
- [x] T096  [P] Unit tests for `lib/utils/phone.ts` — test: `isValidPhone()` returns true for 10-digit (landline) and 11-digit (mobile) formats, false for 9-digit or wrong lengths; `phoneMask()` produces `(XX) XXXXX-XXXX` or `(XX) XXXX-XXXX` format; 100% branch coverage — file: `tests/unit/lib/utils/phone.test.ts`
- [x] T097  [P] Unit tests for `lib/api/cep.ts` — test: 200 success response maps to `{ ok: true, street, neighborhood, city, state }`, BFF 404 maps to `{ ok: false, error: string }`, network failure maps to `{ ok: false, error: string }`; MSW v2 mock of `GET /api/v1/cep/:cep`; 90% coverage — file: `tests/unit/lib/api/cep.test.ts`

**Checkpoint**: `pnpm test` passes all unit tests. `pnpm mutate` reports ≥ 90% mutation score for `lib/**`. No `lib/` module has < 90% line/branch coverage.

---

## Phase 3: User Story 1 — Browse Inventory & Discover Cars (Priority: P1) 🎯 MVP

**Goal**: Anonymous visitors can browse the full car catalog with filters, view car detail pages, and discover cars from the home page. Fully deliverable without authentication.

**Independent Test**: Navigate to `/`, verify hero + category tiles + recently added grid. Navigate to `/inventory`, apply category + price filters, verify URL updates + grid refreshes + filter chips appear. Open `/inventory/{carId}`, verify all detail sections and sticky Buy panel.

**Reference**: spec.md US1 (SC1-6, FR-001 through FR-009); research.md R-01, R-11, R-14; data-model.md §2 and §8; contracts/bff-api.md §Inventory

- [x] T035 [P] [US1] Create `components/inventory/InventorySkeleton.tsx` — grid of skeleton `<CarCard>` placeholders matching grid dimensions (research.md R-14, FR-040) — file: `components/inventory/InventorySkeleton.tsx`
- [x] T036 [P] [US1] Create `components/inventory/CarGrid.tsx` — responsive CSS grid wrapper (`grid-cols-1 sm:grid-cols-2 lg:grid-cols-3`); accepts `children` — file: `components/inventory/CarGrid.tsx`
- [x] T037 [P] [US1] Create `components/inventory/FilterChips.tsx` — renders one chip per active filter from `InventoryFilters`; each chip has × clear button; "Reset all" clears `DEFAULT_FILTERS`; chips update URL params (data-model.md §8, FR-004) — file: `components/inventory/FilterChips.tsx`
- [x] T038 [US1] Create `components/inventory/InventoryFilters.tsx` — loaded via `next/dynamic` (bundle split, research.md R-14); uses Zustand `useInventoryStore` for mobile panel open/close; filter form controls for all `InventoryQueryParams` fields; sort options: Recently added (`REGISTRATION_DATE DESC`), Price low→high (`PRICE ASC`), Price high→low (`PRICE DESC`), Year newest (`YEAR DESC`), Year oldest (`YEAR ASC`) — km is a range filter (kmMin/kmMax), NOT a sort option (FR-006); applies filters by updating URL `searchParams` (FR-002, FR-003) — file: `components/inventory/InventoryFilters.tsx`; `lib/stores/inventory.ts` (Zustand store)
- [x] T039 [US1] Create `components/inventory/CarCard.tsx` — `use client`; constructs `imageUrl` as `${process.env.NEXT_PUBLIC_CDN_URL}/${imageKey}` when `imageKey` non-null, else category SVG placeholder (research.md R-01); displays manufacturer, model, year, category, condition badge, price via `formatBRL()`, km via `formatKm()`; links to `/inventory/{id}` — file: `components/inventory/CarCard.tsx`
- [x] T040 [US1] Create `app/(marketing)/inventory/page.tsx` — RSC; reads `searchParams` → `InventoryQueryParams`; calls `getInventory()`; wraps grid in `<Suspense fallback={<InventorySkeleton />}`; passes `keepPreviousData: true` equivalent via URL-driven refetch (research.md R-14); renders result count "{n} carros" (FR-005); `<FilterChips>` above grid; `<EmptyState>` when `totalElements === 0` (SC-010, FR-003); add `loading.tsx` + `error.tsx` — files: `app/(marketing)/inventory/page.tsx`, `app/(marketing)/inventory/loading.tsx`, `app/(marketing)/inventory/error.tsx`
- [x] T041 [P] [US1] Create `components/car/CarImageGallery.tsx` — `next/image` with `priority` on index 0; aspect-ratio container (`aspect-video` or explicit ratio) prevents CLS before image loads (research.md R-14, FR-048); falls back to category placeholder when no image — file: `components/car/CarImageGallery.tsx`
- [x] T042 [P] [US1] Create `components/car/CarDetail.tsx` — RSC-compatible; renders all `Car` fields from data-model.md §2: identity (manufacturer, model, year, VIN), characteristics section, appearance (color swatches), specifications (km + optional items tag list), pricing section; receives pre-fetched `Car` as prop — file: `components/car/CarDetail.tsx`
- [x] T043 [US1] Create `components/car/BuyPanel.tsx` — `use client`; sticky panel; status-aware: `AVAILABLE` → "Comprar este carro" link to `/purchase/{carId}`; `SOLD` → "Vendido" badge (no button); `UNAVAILABLE` → "Indisponível" badge; reads `CarStatus` from data-model.md §10 state transitions — file: `components/car/BuyPanel.tsx`
- [x] T044 [US1] Create `app/(marketing)/inventory/[carId]/page.tsx` — RSC; calls `getCarById(carId)`; calls `notFound()` on 404 (spec US1 SC5); `generateMetadata` with car name + description (FR-038); renders `<CarDetail>` + `<BuyPanel>` + `<CarImageGallery>`; add `loading.tsx` + `error.tsx` — files: `app/(marketing)/inventory/[carId]/page.tsx`, `app/(marketing)/inventory/[carId]/loading.tsx`, `app/(marketing)/inventory/[carId]/error.tsx`
- [x] T045 [US1] Create `app/(marketing)/page.tsx` — RSC; hero section with quick-search form (submits to `/inventory?category=X&type=X&condition=X`); "Browse by category" tiles each linking to `/inventory?category=X` (spec US1 SC6); editorial split-cards (New + Pre-owned); "Recently added" grid using `getInventory({ size: 4, sortBy: 'REGISTRATION_DATE', sortDirection: 'DESC' })`; `generateMetadata` (FR-038, FR-008, FR-009); add `loading.tsx` + `error.tsx` — files: `app/(marketing)/page.tsx`, `app/(marketing)/loading.tsx`, `app/(marketing)/error.tsx`

**Checkpoint**: US1 complete and independently testable. `pnpm e2e -- --grep inventory` passes. `pnpm e2e -- --grep car-detail` passes.

---

## Phase 4: User Story 2 — Customer Registration Completion (Priority: P2)

**Goal**: After completing Keycloak account creation, the authenticated user fills in CPF, phone, CEP (with address auto-fill), and street number. On success, they are redirected to the application. The web app does NOT collect credentials — Keycloak handles email, password, and name.

**Independent Test**: Simulate post-Keycloak redirect to `/(auth)/complete-registration` with a valid SESSION cookie. Fill CPF (with check-digit validation), phone (Brazilian format), CEP (auto-resolution), and street number. Submit the form and verify the success redirect.

**Reference**: spec.md US2 (SC1-5, FR-017 through FR-022); research.md R-03, R-05, R-12; data-model.md §3 and §7; contracts/bff-api.md §Auth

- [X] T046 [P] [US2] Create `app/(auth)/complete-registration/schema.ts` — Zod schema: `cpf` (z.string().refine(isValidCpf)), `phone` (z.string().refine(isValidPhone)), `cep` (z.string().regex(CEP_REGEX_MASKED)), `streetNumber` (z.string().min(1, 'Número é obrigatório')); imports validators from `lib/utils/cpf.ts`, `cep.ts`, `phone.ts` (research.md R-12, data-model.md §3) — file: `app/(auth)/complete-registration/schema.ts`
- [X] T047 [P] [US2] Create `app/(auth)/complete-registration/actions.ts` — Server Action `registerAction(formData)`: requires active SESSION (middleware ensures authenticated route); calls `registerUser()` from `lib/api/auth.ts`; maps `VALIDATION_ERROR` → surfaces `details[]` array as field-level errors; maps all other codes via `getBFFErrorMessage()` from `lib/errors.ts`; returns `{ ok: boolean; fieldErrors?: Record<string, string>; message?: string }` — file: `app/(auth)/complete-registration/actions.ts`
- [X] T048 [US2] Create `app/(auth)/complete-registration/page.tsx` — `use client`; middleware ensures user is authenticated (SESSION exists — user has completed Keycloak flow); React Hook Form with `zodResolver(registerSchema)`; CPF input applies `cpfMask` on change; phone input applies `phoneMask` on change; CEP field: on blur strips hyphen, calls `lookupCep()` from `lib/api/cep.ts` (research.md R-05 flow), shows loading indicator → resolved address preview (read-only street/city/state) → failed warning (non-blocking, form still submittable, spec US2 SC2); streetNumber field: plain text input; validation errors display inline below field after interaction (SC-011); VALIDATION_ERROR field errors render below each field with `role="alert"` (FR-022, spec US2 SC5); on success redirect to `/account` or home page; `generateMetadata` (FR-038) — file: `app/(auth)/complete-registration/page.tsx`

**Checkpoint**: US2 complete and independently testable. Simulate Keycloak post-registration redirect to `/(auth)/complete-registration`, fill form, submit — success redirect occurs. CEP lookup, CPF validation, and VALIDATION_ERROR field errors all behave per spec.

---

## Phase 5: User Story 3 — Customer Authentication (Priority: P3)

**Goal**: Sign In redirects to PKCE flow. Middleware protects customer and admin routes with return URL preservation. Sign Out invalidates session. Expired session redirects seamlessly.

**Independent Test**: Click Sign In → verify redirect to BFF auth URL. With mocked session, access `/account` → lands on page. Click Sign Out → session cleared, home page shown. Unauthenticated access to `/account` → redirected with return URL preserved.

**Reference**: spec.md US3 (SC1-5, FR-010 through FR-016); research.md R-02; plan.md OI-1 and OI-2; contracts/bff-api.md §Auth

- [X] T049 [US3] Resolve plan OI-1 — implement and document middleware auth detection strategy in `middleware.ts`: probe `HEAD /api/v1/profile` (or equivalent lightweight BFF call) to determine auth state; on probe 401 redirect to `${BFF_URL}/oauth2/authorization/keycloak?redirect_uri=<current-path>`; cache result within the request cycle; document chosen strategy with code comment in middleware — file: `middleware.ts`
- [X] T050 [US3] Resolve plan OI-2 — implement admin role detection in `middleware.ts`: for `/admin/*` routes, after auth probe passes, call `GET /api/v1/admin/inventory` (returns 403 for non-admin, 200 for admin) as role signal; redirect non-admin to `/` (spec US3 SC5, FR-013); document chosen strategy with code comment — file: `middleware.ts`
- [X] T051 [P] [US3] Wire Sign Out in `components/layout/SiteHeader.tsx` — add Server Action `logoutAction()` calling `logoutUser()` from `lib/api/auth.ts`; on success `redirect('/')` (spec US3 SC3); on 401 (already signed out) also redirect to `/` — files: `components/layout/SiteHeader.tsx`, `app/actions/auth.ts`
- [X] T052 [P] [US3] Handle BFF 401 mid-session in `lib/api/client.ts` — when `bffFetch()` receives 401 from a protected-route request, throw `BffError` with code `AUTHENTICATION_REQUIRED`; calling pages must catch this and redirect; add middleware catch for 401 on navigation (FR-016) — file: `lib/api/client.ts`
- [X] T053 [US3] Create `e2e/auth-redirect.spec.ts` — E2E tests: (1) unauthenticated `/account` → redirected to auth with return URL; (2) unauthenticated `/admin` → redirected to auth; (3) authenticated non-admin `/admin` → redirected to `/`; (4) Sign Out → `POST /api/v1/auth/logout` called → home page; all with axe-core scan — file: `e2e/auth-redirect.spec.ts`

**Checkpoint**: US3 complete. Middleware correctly gates all customer and admin routes. Sign In/Sign Out flow verified via E2E with mocked BFF session.

---

## Phase 6: User Story 4 — Purchase Confirmation Flow (Priority: P4)

**Goal**: Authenticated customer reviews car + profile + financial summary, confirms purchase. Button prevents double-submit. `CAR_NOT_AVAILABLE` race condition shows inline targeted message.

**Independent Test**: With mocked session + mocked BFF, navigate to `/purchase/{carId}`, verify two-column layout, click Confirm → button disables → success redirect. Trigger mocked 409 → inline targeted message appears (not generic error screen).

**Reference**: spec.md US4 (SC1-6, FR-023 through FR-028); research.md R-06, R-07; data-model.md §5; contracts/bff-api.md §Purchase; plan.md SC-006, SC-007, SC-008

- [X] T054 [P] [US4] Create `components/purchase/FinancialSummary.tsx` — receives `listedValue: number`; computes `taxAmount = computeTax(listedValue)` and `total = computeTotal(listedValue)` from `lib/pricing.ts`; renders listed price, tax line, divider, total; all values formatted with `formatBRL()` and `.tabular` CSS class (FR-024, FR-041) — file: `components/purchase/FinancialSummary.tsx`
- [X] T055 [US4] Create `components/purchase/ConfirmButton.tsx` — `use client`; calls `confirmPurchaseAction()` Server Action via `useTransition`; immediately disables on first click + shows loading state (FR-025, SC-006); on `CAR_NOT_AVAILABLE` 409: renders inline targeted message explaining another buyer completed the purchase + link to `/inventory` (FR-026, SC-007, spec US4 SC4); on other errors: renders `<ErrorDisplay message={...} requestId={...} />` with retry option (FR-028, spec US4 SC5); button re-enables after 409 only — file: `components/purchase/ConfirmButton.tsx`
- [X] T056 [US4] Create `app/(customer)/purchase/[carId]/actions.ts` — Server Action `confirmPurchaseAction(carId: string)`: calls `confirmPurchase(carId)` from `lib/api/purchases.ts`; on success: `redirect('/purchase/success')`; on `BffError` code `CAR_NOT_AVAILABLE`: return `{ ok: false, code: 'CAR_NOT_AVAILABLE' }`; on other errors: return `{ ok: false, message: getBFFErrorMessage(code), requestId }` — file: `app/(customer)/purchase/[carId]/actions.ts`
- [X] T057 [US4] Create `app/(customer)/purchase/[carId]/page.tsx` — RSC; parallel-fetch `getCarById(carId)` and `getProfile()` using `Promise.all()`; calls `notFound()` if car 404; renders two-column layout: left (car identity + buyer profile snapshot), right (sticky `<FinancialSummary>` + `<ConfirmButton>`); `generateMetadata` (FR-038); add `loading.tsx` + `error.tsx` — files: `app/(customer)/purchase/[carId]/page.tsx`, `app/(customer)/purchase/[carId]/loading.tsx`, `app/(customer)/purchase/[carId]/error.tsx`
- [X] T058 [US4] Create `app/(customer)/purchase/success/page.tsx` — success indicator; car name + final value (from URL or session state); email confirmation message; CTA: "Ver minhas compras" → `/account/purchases`; CTA: "Continuar navegando" → `/inventory`; `generateMetadata` (FR-027, FR-038) — file: `app/(customer)/purchase/success/page.tsx`

**Checkpoint**: US4 complete and independently testable. `pnpm e2e -- --grep purchase` passes including 409 race-condition and double-click prevention tests.

---

## Phase 7: User Story 5 — Account Profile Management (Priority: P5)

**Goal**: Authenticated customer views and updates their profile (name, phone, CEP). CPF is read-only. CEP auto-fills address. Inline feedback without page reload.

**Independent Test**: With mocked session, navigate to `/account`, verify form pre-populated. Update phone, save, verify inline success. Enter new CEP, verify auto-fill. Verify CPF field is read-only.

**Reference**: spec.md US5 (SC1-5, FR-029 through FR-031); research.md R-04, R-05; data-model.md §4; contracts/bff-api.md §Profile

- [X] T059 [P] [US5] Create `app/(customer)/account/schema.ts` — Zod profile schema: `firstName` (min 1, max 100), `lastName` (min 1, max 100), `phone` (z.string().refine(isValidPhone)), `cep` (z.string().regex(CEP_REGEX_MASKED)); imports from `lib/utils/phone.ts`, `lib/utils/cep.ts`; intentionally excludes `cpf` and `streetNumber` per research.md R-04 — file: `app/(customer)/account/schema.ts`
- [X] T060 [P] [US5] Create `components/account/AddressPreview.tsx` — renders `CustomerAddress` fields as read-only text; displays `address.number` as read-only (not editable); includes `{/* TODO: streetNumber not updatable via PATCH /api/v1/profile — BFF contract gap. See research.md R-04 and plan.md OI */}` comment — file: `components/account/AddressPreview.tsx`
- [X] T061 [US5] Create `components/account/ProfileForm.tsx` — `use client`; React Hook Form with `zodResolver(profileSchema)`; pre-populated from `CustomerProfile` prop; CEP field triggers `lookupCep()` from `lib/api/cep.ts` on blur (research.md R-05 flow); resolved address shown via `<AddressPreview>`; CPF field rendered read-only with note "O CPF não pode ser alterado" (spec US5 SC5); `address.number` (street number) rendered read-only with note "Número não é editável" — `streetNumber` not accepted by `PATCH /api/v1/profile` per BFF contract (research.md R-04); phone applies `phoneMask`; `useTransition` for save action; inline success feedback on `PATCH` success without page reload (FR-031, spec US5 SC4) — file: `components/account/ProfileForm.tsx`
- [X] T062 [US5] Create `app/(customer)/account/actions.ts` — Server Action `updateProfileAction(formData)`: calls `updateProfile()` from `lib/api/profile.ts`; body includes ONLY `firstName`, `lastName`, `phone`, `cep` — never `cpf` or `streetNumber` (research.md R-04); maps errors via `getBFFErrorMessage()`; returns `{ ok: boolean; profile?: CustomerProfile; message?: string; requestId?: string }` — file: `app/(customer)/account/actions.ts`
- [X] T063 [US5] Create `app/(customer)/account/page.tsx` — RSC; calls `getProfile()`; renders `<ProfileForm profile={profile} />`; if `address.number` is empty, renders visible banner prompting address completion (spec US5 SC3); `generateMetadata`; add `loading.tsx` + `error.tsx` — files: `app/(customer)/account/page.tsx`, `app/(customer)/account/loading.tsx`, `app/(customer)/account/error.tsx`

**Checkpoint**: US5 complete and independently testable. Profile form pre-populates, CEP auto-fills, saves successfully with inline feedback.

---

## Phase 8: User Story 6 — Purchase History (Priority: P6)

**Goal**: Authenticated customer views paginated list of past purchases with car snapshots captured at sale time. Empty state shown for users with no purchases.

**Independent Test**: With mocked session and mocked BFF purchases list, navigate to `/account/purchases`. Verify purchase cards show car snapshot data. Test empty state. Test pagination controls.

**Reference**: spec.md US6 (SC1-3, FR-032); research.md R-06, R-07; data-model.md §5; contracts/bff-api.md §Purchase

- [X] T064 [P] [US6] Create `components/account/PurchaseHistoryCard.tsx` — receives `Purchase` prop; displays: manufacturer, model, year, category, externalColor, VIN, purchase date via `formatDate()`, listedValue + `computeTax()` + `computeTotal()` via `formatBRL()` with `.tabular` (FR-041); does NOT render internalColor, type, optionalItems, imageKey (not in BFF snapshot — research.md R-07); includes `{/* TODO: BFF purchase snapshot missing internalColor, type, optionalItems, imageKey. See research.md R-07 */}` comment — file: `components/account/PurchaseHistoryCard.tsx`
- [X] T065 [US6] Create `app/(customer)/account/purchases/page.tsx` — RSC; calls `getPurchases(page)` where `page` from `searchParams`; renders list of `<PurchaseHistoryCard>`; `<EmptyState>` with link to `/inventory` when empty (spec US6 SC2); pagination controls update `?page=N` URL param without full reload (spec US6 SC3); `generateMetadata`; add `loading.tsx` + `error.tsx` — files: `app/(customer)/account/purchases/page.tsx`, `app/(customer)/account/purchases/loading.tsx`, `app/(customer)/account/purchases/error.tsx`

**Checkpoint**: US6 complete and independently testable. `pnpm e2e -- --grep purchase-history` passes.

---

## Phase 9: User Story 7 — Admin Inventory Management (Priority: P7)

**Goal**: Admin users can view all cars (all statuses), add new cars, edit existing entries, and toggle availability with a confirmation dialog. All behind ROLE_ADMIN gate.

**Independent Test**: With admin-role mocked session, navigate to `/admin`. Verify inventory table with status filter tabs. Open Add car form, fill fields, submit. Click "Mark unavailable" → confirmation dialog → confirm → table updates.

**Reference**: spec.md US7 (SC1-5, FR-033 through FR-036); data-model.md §6 and §10; contracts/bff-api.md §Admin (⚠️ assumed contracts)

> **⚠️ All admin tasks use assumed BFF contracts. See plan.md OI-2 and contracts/bff-api.md §Admin.**

- [X] T066 [P] [US7] Create `components/admin/InventoryTable.tsx` — sortable table; status filter tabs (All / Available / Sold / Unavailable) per spec US7 SC1; status badge per `CarStatus`; Edit button per row; status-change action button (Mark Available / Mark Unavailable) triggers confirmation dialog before executing (FR-036); assumed `Car[]` response shape from data-model.md §6 — file: `components/admin/InventoryTable.tsx`
- [X] T067 [P] [US7] Create `components/admin/CarFormModal.tsx` — `next/dynamic` lazy import; create + edit form; all `CreateCarRequest` fields from data-model.md §6: manufacturer, model, year, type, category, isNew, listedValue, km, externalColor, internalColor, optionalItems, VIN; image URL as text input per project assumptions; ⚠️ uses assumed contract shapes — file: `components/admin/CarFormModal.tsx`
- [X] T068 [US7] Create `app/(admin)/admin/actions.ts` — Server Actions: `createCarAction(formData)` calling `createCar()` from `lib/api/admin.ts`; `updateCarAction(id, formData)` calling `updateCar()`; maps errors via `getBFFErrorMessage()`; ⚠️ assumed endpoint shapes — file: `app/(admin)/admin/actions.ts`
- [X] T069 [US7] Create `app/(admin)/admin/page.tsx` — RSC; calls `getAdminInventory()`; renders `<InventoryTable>`; lazy-loads `<CarFormModal>` via `next/dynamic`; `generateMetadata`; add `loading.tsx` + `error.tsx` — files: `app/(admin)/admin/page.tsx`, `app/(admin)/admin/loading.tsx`, `app/(admin)/admin/error.tsx`
- [X] T070 [P] [US7] Create `app/(admin)/admin/settings/page.tsx` — placeholder settings page; renders "Configurações (em breve)"; `generateMetadata` — file: `app/(admin)/admin/settings/page.tsx`

**Checkpoint**: US7 complete with assumed contracts. Admin inventory CRUD functional. Confirmation dialog required for destructive actions.

---

## Phase 10: User Story 8 — Admin Sales Reports (Priority: P8)

**Goal**: Admin users view KPI summary cards and a sortable, paginated sales table with date range filter applied via URL params.

**Independent Test**: With admin-role mocked session and mocked BFF sales data, navigate to `/admin/sales`. Verify 4 KPI cards. Apply date range filter → URL updates → table refreshes. Navigate pages.

**Reference**: spec.md US8 (SC1-3, FR-037); data-model.md §6 `SalesKpiSummary` + `SaleRecord`; contracts/bff-api.md §Admin (⚠️ assumed contracts)

> **⚠️ All admin sales tasks use assumed BFF contracts. See plan.md OI-2 and contracts/bff-api.md §Admin.**

- [X] T071 [P] [US8] Create `components/admin/KpiCards.tsx` — 4-up card grid: total revenue (`formatBRL()`), total cars sold, average sale value (`formatBRL()`), current month count; receives `SalesKpiSummary` prop from data-model.md §6; ⚠️ assumed contract shape — file: `components/admin/KpiCards.tsx`
- [X] T072 [P] [US8] Create `components/admin/SalesTable.tsx` — paginated table; date range `from`/`to` filter updates URL `searchParams`; columns: date (`formatDate()`), buyer name, car, listed value + tax + final value (all `formatBRL()` + `.tabular`); ⚠️ assumed `SaleRecord` shape from data-model.md §6 — file: `components/admin/SalesTable.tsx`
- [X] T073 [US8] Create `app/(admin)/admin/sales/page.tsx` — RSC; reads `from`/`to`/`page` from `searchParams`; calls `getSalesReport()` from `lib/api/admin.ts`; renders `<KpiCards summary={...} />` + `<SalesTable records={...} />`; `generateMetadata`; add `loading.tsx` + `error.tsx` — files: `app/(admin)/admin/sales/page.tsx`, `app/(admin)/admin/sales/loading.tsx`, `app/(admin)/admin/sales/error.tsx`

**Checkpoint**: US8 complete with assumed contracts. KPI cards + sales table functional. Date range filter reflected in URL.

---

## Phase 11: Polish & Cross-Cutting Concerns

**Purpose**: E2E test completion, accessibility audit, performance hardening, CSP finalization,
and Web Vitals instrumentation. Runs after all feature pages are built.

**Reference**: plan.md §Implementation Phases (Phase I); spec.md SC-001 through SC-012; FR-038 through FR-050

- [X] T074 Finalize `e2e/mocks/handlers.ts` — complete all BFF endpoint MSW handlers with realistic fixture data matching data-model.md type shapes; cover: inventory list, car detail, register success + DUPLICATE_IDENTITY, purchase success + 409, profile GET + PATCH, purchases list, admin inventory + sales (⚠️ assumed shapes) — file: `e2e/mocks/handlers.ts`
- [X] T075 [P] Create `e2e/inventory.spec.ts` — tests: browse /inventory, apply category filter (SC-010), price filter, mobile viewport (375px), empty state, result count, axe-core scan — file: `e2e/inventory.spec.ts`
- [X] T076 [P] Create `e2e/car-detail.spec.ts` — tests: load /inventory/{carId}, verify all detail sections, OG meta, Buy CTA shows for AVAILABLE / hidden for SOLD, notFound for invalid ID, axe-core scan — file: `e2e/car-detail.spec.ts`
- [X] T077 [P] Create `e2e/purchase.spec.ts` — tests: happy path purchase → success redirect, 409 CAR_NOT_AVAILABLE inline message (SC-007), double-click prevention (SC-006), auth redirect for unauthenticated, requestId surfaced on error (SC-008), axe-core scan — file: `e2e/purchase.spec.ts`
- [X] T078 [P] Create `e2e/purchase-history.spec.ts` — tests: authenticated list with pagination, empty state with inventory link, axe-core scan on both desktop + mobile — file: `e2e/purchase-history.spec.ts`
- [X] T079 Resolve plan OI-3 — finalize dark mode inline script in `app/layout.tsx`; compute SHA-256 hash of the exact script content; add hash to CSP `script-src` in `next.config.ts` — files: `app/layout.tsx`, `next.config.ts`
- [X] T080 Resolve plan OI-4 — add confirmed CDN domain to `next.config.ts` `images.remotePatterns`; update `.env.local.example` with real CDN URL example — files: `next.config.ts`, `.env.local.example`
- [X] T081 Complete CSP headers in `next.config.ts` — `Content-Security-Policy`: `default-src 'self'`; `script-src 'self' 'sha256-<hash>'`; Keycloak domain in `connect-src`; CDN domain in `img-src`; font sources; `frame-ancestors 'none'` (FR-045) — file: `next.config.ts`
- [X] T082 Add Web Vitals collection — implement `reportWebVitals` export in `app/layout.tsx` or `app/_vitals.tsx`; send LCP, CLS, INP, TTFB, FID to observability endpoint (FR-046, SC-001 through SC-003) — file: `app/_vitals.tsx`
- [X] T083 Add global error boundary — create `app/global-error.tsx`; captures `error.digest` and `requestId` from BFF meta when available; renders `<ErrorDisplay>` without exposing internals (FR-047) — file: `app/global-error.tsx`
- [X] T084 Audit all route segments — verify every `page.tsx` has both `error.tsx` + `loading.tsx` siblings; create any missing ones using `<LoadingSkeleton>` and `<ErrorDisplay>` (FR-040) — all route segment directories
- [X] T085 [P] Audit `generateMetadata` coverage — verify every `page.tsx` exports `generateMetadata` or static `metadata` with `title` and `description` (FR-038); add any missing ones
- [X] T086 Run `pnpm type-check` (`tsc --noEmit`) — resolve all TypeScript strict-mode errors; no `any` types; no `@ts-ignore` without explanatory comment (Constitution Art. XVII)
- [X] T087 [P] Run `pnpm lint` — resolve all ESLint violations; confirm `no-restricted-imports` barrel rule has 0 violations; confirm no raw `<img>` tags (FR-048, FR-049)
- [ ] T088 Run `pnpm e2e` — fix all axe-core violations until scan returns 0 violations on both desktop (1280px) and mobile (375px) viewports for all critical journeys (SC-004, SC-012)

**Checkpoint**: All 88 tasks complete. `tsc --noEmit` passes. `pnpm lint` passes. `pnpm e2e` passes with 0 axe violations. Application ready for deployment.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — start immediately
- **Phase 2 (Foundational)**: Depends on Phase 1 — **BLOCKS all user stories**
- **Phase 2d (Unit Tests)**: Depends on Phase 2 (library layer complete) — should complete before feature pages begin
- **Phase 3–10 (User Stories)**: All depend on Phase 2. Can proceed in priority order or in parallel if staffed
- **Phase 11 (Polish)**: Depends on all user story phases completing

### User Story Dependencies

| Story | Depends On | Notes |
|-------|-----------|-------|
| US1 — Browse Inventory (P1) | Phase 2 only | No auth required; delivers standalone MVP |
| US2 — Registration (P2) | Phase 2 + Phase 2d | Requires `(auth)/complete-registration` route; BFF register is authenticated |
| US3 — Authentication (P3) | Phase 2 + US1 (SiteHeader) | Middleware touches shell components from Phase 2 |
| US4 — Purchase (P4) | US1 (car detail) + US3 (auth gate) | Cannot purchase without a car detail page and auth |
| US5 — Profile (P5) | US3 (auth gate) | Independent of US4 |
| US6 — Purchase History (P6) | US3 (auth gate) | Independent of US4 and US5 |
| US7 — Admin Inventory (P7) | US3 (admin role gate) | Independent of all customer stories |
| US8 — Admin Sales (P8) | US3 (admin role gate) + US7 (admin layout) | Admin layout from US7 |

### Parallel Opportunities Within Phases

```
# Phase 2 — run in parallel across all 2a and 2b tasks:
T009  T010  T011  T012  T013  T014  T015   # all lib/ utilities
T016  T017  T018  T019  T020  T021          # all lib/api/ clients

# Phase 2d — unit tests (all independent, run in parallel):
T089                                       # Stryker setup (sequential first)
T090  T091  T092  T093  T094  T095  T096  T097  # all lib/ unit test files

# Phase 3 — US1 parallel starts:
T035  T036  T037   # inventory components (no inter-dependency)
T041  T042         # car/ components (no inter-dependency)

# Phase 11 — E2E specs are all independent:
T075  T076  T077  T078   # all E2E spec files
T079  T080  T081  T082   # all config finalization tasks
```

---

## Implementation Strategy

### MVP Scope (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (critical — blocks everything)
3. Complete Phase 3: User Story 1 (T035–T045)
4. **STOP and VALIDATE**: `pnpm build` + `pnpm e2e -- --grep "inventory|car-detail"` passes
5. Deploy/demo: anonymous users can browse and discover cars

### Incremental Delivery After MVP

- **Sprint 2**: US2 (Registration) → US3 (Auth) → authenticated shell
- **Sprint 3**: US4 (Purchase) — core revenue transaction
- **Sprint 4**: US5 + US6 (Account + History) — post-purchase customer experience
- **Sprint 5**: US7 + US8 (Admin) — operational tooling (pending OI-2 resolution)
- **Sprint 6**: Phase 11 (Polish + hardening)

---

## Summary

| Metric | Count |
|--------|-------|
| Total tasks | 88 |
| Setup tasks | 7 |
| Foundational tasks | 27 |
| US1 tasks | 11 |
| US2 tasks | 3 |
| US3 tasks | 5 |
| US4 tasks | 5 |
| US5 tasks | 5 |
| US6 tasks | 2 |
| US7 tasks | 5 |
| US8 tasks | 3 |
| Polish tasks | 15 |
| Parallelizable [P] tasks | 47 |
| Open items requiring resolution | 4 (OI-1 through OI-4 in plan.md) |


