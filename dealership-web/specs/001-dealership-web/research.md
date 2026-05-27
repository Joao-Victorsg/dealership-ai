# Research: dealership-web — Full Web Application

**Phase**: Phase 0  
**Date**: 2026-05-01  
**Branch**: `001-dealership-web`  
**Sources**: dealership-bff/specs/001-bff-orchestration/contracts/, dealership-bff/specs/002-pkce-auth-migration/contracts/auth-flow.md, docs/front-dealership-example/src/styles.css, constitution.md

---

## R-01 — BFF Inventory API

**Decision**: Use `GET /api/v1/inventory` and `GET /api/v1/inventory/{id}` as documented. Both are public (no auth).

**Findings**:
- Filter params confirmed: `q`, `category`, `type`, `condition`, `manufacturer`, `yearMin`, `yearMax`, `priceMin`, `priceMax`, `color`, `kmMin`, `kmMax`
- Sort params: `sortBy` (`PRICE` | `YEAR` | `REGISTRATION_DATE`) + `sortDirection` (`ASC` | `DESC`)
- Pagination: zero-based `page` + `size` (default 20, max 100); `totalElements` and `totalPages` in `meta`
- Response includes `imageKey` as a **relative path** (e.g., `"cars/{id}/abc.jpg"`), NOT a full URL
- Car status enum: `AVAILABLE`, `SOLD` (implied by `CAR_NOT_AVAILABLE`), no explicit `UNAVAILABLE` in inventory contract — confirmed as `status` field with string values
- Category enum: `SEDAN`, `SUV`, `HATCHBACK`, `COUPE`, `CONVERTIBLE`, `MINIVAN`, `PICKUP`, `OTHER`
- Type enum: `ELECTRIC`, `HYBRID`, `GASOLINE`, `DIESEL`
- Condition: `isNew` boolean field (not an enum string); UI maps to "New" / "Pre-owned"
- `condition` query param is `NEW` or `USED` string — maps to `isNew=true`/`false` server-side

**Key gap — imageKey vs full URL**:
- The BFF returns `imageKey` (relative S3/CDN key), not a full image URL
- The web app must construct the URL: `${NEXT_PUBLIC_CDN_URL}/${imageKey}`
- A new environment variable `NEXT_PUBLIC_CDN_URL` is required (added to `.env.local.example`)
- `next/image` `src` prop will be `${process.env.NEXT_PUBLIC_CDN_URL}/${car.imageKey}` — when `imageKey` is null/absent, a category-appropriate placeholder SVG is used
- The CDN domain must be added to `next.config.ts` `images.remotePatterns` for `next/image` to accept it

**Rationale**: The BFF contract is authoritative. `imageKey` is the canonical pattern for S3-backed image storage.  
**Alternatives considered**: Pre-signing full URLs on the BFF — not implemented in current contract; approach deferred to future BFF iteration.

---

## R-02 — Authentication Flow (PKCE)

**Decision**: Follow `002-pkce-auth-migration/contracts/auth-flow.md` exclusively. The older `001-bff-orchestration/contracts/auth.md` login/refresh endpoints are REMOVED.

**Findings**:
- `POST /api/v1/auth/login` — **REMOVED** in 002 migration; must NOT be called
- `POST /api/v1/auth/refresh` — **REMOVED** in 002 migration; must NOT be called
- Login: browser navigates to `GET /oauth2/authorization/keycloak` → BFF redirects to Keycloak PKCE flow
- Callback: `GET /login/oauth2/code/keycloak` handled entirely by Spring Security; sets `SESSION` cookie
- Logout: `POST /api/v1/auth/logout` — still exists; invalidates session + OIDC end-session
- `POST /api/v1/auth/register` — **unchanged** in 002; still the registration endpoint
- SESSION cookie: `HttpOnly; Secure; SameSite=Lax; Path=/`
  - **Note**: `SameSite=Lax` (not `Strict`) is required for PKCE OAuth redirect compatibility: when Keycloak redirects back to the BFF callback, the browser must send the state cookie — `Strict` would prevent this cross-site redirect from carrying the cookie. CSRF protection is provided by the PKCE `state` parameter and Next.js Server Actions' built-in CSRF.
  - The constitution's reference to `SameSite=Strict` reflects an earlier design and does not apply to the PKCE flow.

**Keycloak register endpoint**:
- `/oauth2/authorization/keycloak-register` IS the Spring Security registration flow (confirmed in `OAuth2LoginSuccessHandler.java`)
- After successful Keycloak registration, `OAuth2LoginSuccessHandler` detects the `keycloak-register` client registration ID and redirects to `${app.post-registration-redirect-uri}` — configured to point to the web app's `(auth)/complete-registration` page
- `POST /api/v1/auth/register` is an AUTHENTICATED endpoint (`@AuthenticationPrincipal Jwt jwt`): the user must have a valid Keycloak SESSION before calling this endpoint. Fields accepted: `cpf`, `phone`, `cep`, `streetNumber`. `firstName` and `lastName` are extracted from the JWT `given_name`/`family_name` claims by the BFF.
- The web app's `(auth)/complete-registration` route is the correct registration completion entry point. A credentials form on the web app (email/password/name) is constitutionally prohibited and contradicted by BFF source.

**Frontend implications**:
- No JavaScript token handling — ever
- Registration entry: browser navigates to `GET ${BFF_URL}/oauth2/authorization/keycloak-register`; Keycloak handles all credentials
- After Keycloak account creation + PKCE callback, user lands at `(auth)/complete-registration` with a valid SESSION
- That page collects CPF, phone, CEP, and street number → submits to `POST /api/v1/auth/register` (authenticated)
- Logout calls `POST /api/v1/auth/logout` as a Server Action; browser follows the OIDC end-session redirect
- When BFF returns 401, Next.js middleware detects it and redirects to the Keycloak auth flow

**Rationale**: 002 migration is the current BFF state. `OAuth2LoginSuccessHandler.java` and `AuthController.java` source code confirm the PKCE registration flow and the authenticated register endpoint.  
**Alternatives considered**: Implementing a credentials form on the web app — rejected: constitutionally prohibited (Art. III + Art. X) and contradicted by BFF source code.

---

## R-03 — Registration Route Architecture

**Decision**: Registration completion lives in `app/(auth)/complete-registration/` (protected route — requires valid Keycloak SESSION via the `keycloak-register` flow). There is NO `/register` route in `(marketing)`. The web app does not own credentials collection.

**Findings**:
- `POST /api/v1/auth/register` (BFF) is authenticated — confirmed via `@AuthenticationPrincipal Jwt jwt` in `AuthController.java`
- `RegisterRequest` (BFF) accepts ONLY: `cpf`, `phone`, `cep`, `streetNumber`. No email, password, firstName, or lastName.
- `firstName` and `lastName` are extracted from the Keycloak JWT `given_name`/`family_name` claims by the BFF — the web form does NOT collect them
- The `(auth)/complete-registration` route is the only page in the `(auth)/` route group

**Registration flow**:
1. User clicks "Criar conta" → browser navigates to `GET ${BFF_URL}/oauth2/authorization/keycloak-register`
2. BFF initiates PKCE flow with Keycloak using the `keycloak-register` provider registration
3. User creates account on Keycloak (Keycloak handles: email, password, first name, last name)
4. BFF PKCE callback → `OAuth2LoginSuccessHandler` detects `keycloak-register` ID → sets SESSION cookie → redirects to `${app.post-registration-redirect-uri}` (the web app's `(auth)/complete-registration` page)
5. Web app renders `(auth)/complete-registration` — user is authenticated (has SESSION) but profile incomplete
6. User fills in: CPF (masked + check-digit validation), phone (Brazilian format), CEP (auto-fill via BFF endpoint — see R-05), street number
7. Form submits Server Action → `POST /api/v1/auth/register` (authenticated via SESSION cookie)
8. On success: redirect to home or `/account`

**Rationale**: Both `OAuth2LoginSuccessHandler.java` and `AuthController.java` in the BFF source confirm the PKCE registration flow. The `AuthController.register()` method requires `@AuthenticationPrincipal Jwt jwt`, proving the user must authenticate via Keycloak before completing profile registration.

---

## R-04 — Profile Update — streetNumber Gap

**Decision**: The Profile update form does NOT include a `streetNumber`/`number` field. The `PATCH /api/v1/profile` endpoint does not accept it.

**Findings**:
- `PATCH /api/v1/profile` accepts ONLY: `firstName`, `lastName`, `phone`, `cep`
- The address `number` (`address.number`) is visible in `GET /api/v1/profile` but NOT updatable through the BFF profile endpoint
- Sending `streetNumber` in the PATCH body would trigger a `VALIDATION_ERROR` (strict JSON mode on BFF)
- The spec's requirement to edit "street number" is therefore not implementable against the current BFF contract

**Resolution**:
- The Profile form edits: firstName, lastName, phone, CEP
- `address.number` is displayed as read-only alongside the resolved address
- A TODO comment in the ProfileForm component must document this limitation and link to the BFF contract gap
- Future BFF update may expose this field; the web form is structured to add it without refactoring

**Rationale**: Implementing a field that the BFF ignores or rejects creates confusion and silent failures.  
**Alternatives considered**: Client-side validation to strip the field before submitting — rejected because the BFF would still reject it in strict mode. Correct solution is to not collect data the BFF cannot store.

---

## R-05 — CEP Resolution

**Decision**: CEP resolution is handled by the BFF. The web app calls `GET /api/v1/cep/{cep}` on the BFF, which internally calls ViaCEP. The web app MUST NOT call ViaCEP directly.

**Findings**:
- The BFF does NOT currently expose a CEP lookup endpoint in the documented contracts
- The planned BFF endpoint is `GET /api/v1/cep/{cep}` (⚠️ assumed contract — see contracts/bff-api.md §CEP Lookup)
- The BFF calls ViaCEP internally and returns the resolved address data
- The web app calls this BFF endpoint via `lib/api/cep.ts` → `lookupCep(cep: string)`
- The call is made server-side from a Server Action (never from a Client Component to the BFF directly)
- The `cep` field sent to the BFF (in register and PATCH profile) must be 8 digits without hyphen

**CEP flow (complete-registration and profile update forms)**:
1. User enters CEP in the form field (formatted as `XXXXX-XXX` via input mask)
2. On `onBlur`, the form hook strips the hyphen and calls `lookupCep()` from `lib/api/cep.ts`
3. `lookupCep()` calls `GET ${BFF_URL}/api/v1/cep/{cep}` server-side
4. On success: street, neighborhood, city, state displayed as read-only preview
5. On failure (CEP not found or network error): non-blocking warning message; form remains submittable
6. When submitting, CEP is sent as 8 digits (hyphen stripped)

**Rationale**: Centralising external API calls in the BFF reduces the web app's external dependencies, eliminates CORS concerns, and allows the BFF to add caching or fallbacks without frontend changes.  
**Alternatives considered**: (a) Calling ViaCEP from the frontend (former approach) — rejected: external dependency, CORS issues, architecture violation per project direction. (b) No live preview — rejected: spec US2 SC1 requires live CEP preview on blur.

---

## R-06 — Purchase Financial Summary

**Decision**: The 10% platform tax and final total are computed by the web app from the BFF's `listedValue`. They are NOT returned as separate fields by the BFF.

**Findings**:
- `POST /api/v1/purchases` response includes `vehicle.listedValue` only — no `taxAmount` or `finalValue` fields
- `GET /api/v1/purchases` (purchase history) also returns only `vehicle.listedValue` per vehicle
- The tax rate (10%) must be treated as a display constant in the web app: `TAX_RATE = 0.10`
- `taxAmount = listedValue * TAX_RATE`
- `finalValue = listedValue + taxAmount`
- These are formatted with `lib/format.ts` currency formatter

**Resolution**: A `lib/pricing.ts` module exports `TAX_RATE`, `computeTax(listedValue)`, and `computeTotal(listedValue)` — centralized to prevent magic numbers and allow future changes.

**Rationale**: The BFF contract is the source of truth. Computing derived values client-side from a known formula is correct. A constant should not require a server round-trip.

---

## R-07 — Purchase History Vehicle Snapshot Completeness

**Decision**: The purchase history page displays only the fields returned by the BFF. A UI note acknowledges the missing fields. Implementation defers to a future BFF contract revision.

**Findings**:
- `GET /api/v1/purchases` vehicle snapshot returns: `id`, `model`, `manufacturer`, `manufacturingYear`, `externalColor`, `vin`, `category`, `listedValue`
- **Missing from snapshot**: `internalColor`, `type` (ELECTRIC/HYBRID/etc.), `optionalItems`, `imageKey`, `isNew` (condition)
- The spec requires showing: type, internal color, optional items, car image in the purchase history

**Resolution**:
- Purchase history cards display: manufacturer, model, year, category, external color, VIN, listed value + computed tax + final total, date
- Missing fields (internal color, type, optional items, image) are omitted from the UI with no placeholder shown for missing structured data
- A TODO comment in PurchaseHistoryCard documents the gap and references the BFF contract

**Rationale**: Displaying empty or "N/A" for most fields of a purchase history card creates a poor UX. The card is still useful without the missing fields, and the BFF contract is the authority.  
**Alternatives considered**: Fetching current car data from `GET /api/v1/inventory/{id}` to supplement the snapshot — rejected. Purchase history must show snapshot data, not current data. Using current data would be semantically incorrect and would break for cars that are no longer in inventory.

---

## R-08 — Admin Endpoints

**Decision**: Admin features (inventory management, sales reports, settings) are included in the route structure but implemented with assumed contracts pending BFF admin spec publication.

**Findings**:
- Neither `001-bff-orchestration` nor `002-pkce-auth-migration` documents admin endpoints
- Assumed endpoints based on spec requirements:
  - `GET /api/v1/admin/inventory` — paginated with status filter
  - `POST /api/v1/admin/inventory` — create car
  - `PATCH /api/v1/admin/inventory/{carId}` — update car
  - `GET /api/v1/admin/sales` — paginated sales with date range filter
- Admin role: `ROLE_ADMIN` (assumed; BFF likely uses Spring Security role prefix convention)
- Admin authentication: same SESSION cookie pattern as client routes; no separate admin token

**Resolution**:
- `lib/api/admin.ts` implements typed client functions with assumed response shapes
- `contracts/bff-admin-assumed.md` documents the assumed contracts explicitly
- Middleware role check: inspect session claim `role === 'admin'` (exact claim name TBD — must be confirmed with BFF team)
- Admin pages show a banner in development: "Admin endpoints pending BFF spec. Using assumed contracts."

**Rationale**: Admin UI is in scope per spec. Deferring the entire admin feature would leave a large scope gap. Assumed contracts with explicit documentation allow parallel development.

---

## R-09 — Design System Integration

**Decision**: Port `docs/front-dealership-example/src/styles.css` directly into `app/globals.css`. All tokens, utilities, and animations are available as-is.

**Findings from styles.css analysis**:
- Tailwind v4 `@theme inline` registration confirmed — all tokens referenced via `var(--*)` in CSS and `text-primary`, `bg-card`, etc. in Tailwind classes
- Complete oklch token set documented for `:root` and `.dark` (see constitution Article V for full list)
- `--ease-out-expo` and `--ease-out-quart` registered as custom ease values in `@theme inline`
- `.grain` utility confirmed: `isolation: isolate` + `::after` pseudo-element with inline SVG fractal noise at `opacity: 0.04`
- `.reveal`, `.reveal-1` through `.reveal-4` confirmed: `opacity: 0`, `translateY(8px)`, 600ms ease-out-expo, staggered 60ms delays (0ms, 60ms, 140ms, 220ms, 300ms)
- `prefers-reduced-motion` media query already in styles.css — zeroes all animation durations and resets `.reveal` to `opacity: 1; transform: none`
- `.tabular` utility: `font-variant-numeric: tabular-nums; font-feature-settings: "tnum"`
- `.hairline` utility: border using `var(--color-border)`
- Font stacks registered: `--font-display: "Bricolage Grotesque"`, `--font-sans: "Geist"`, `--font-mono: "Geist Mono"`

**Font loading**:
- Bricolage Grotesque: `next/font/google` (available on Google Fonts)
- Geist and Geist Mono: `next/font/local` from `geist` npm package (`import { GeistSans, GeistMono } from 'geist/font'`)
- Both applied as CSS variables on `<html>` element via root layout

**Tailwind v4 patterns to note**:
- `@custom-variant dark (&:is(.dark *))` — dark mode variant is class-based (`.dark` on `<html>`)
- `@source "../src"` — equivalent pattern for `app/` in Next.js: `@source "../app"` + `@source "../components"` + `@source "../lib"`
- No v3 `content:` array — v4 uses `@source` directives

**Rationale**: The design system is already finalized and consistent between BFF and web design references. Direct port eliminates drift risk.

---

## R-10 — Dark Mode State Management

**Decision**: Dark mode preference persists to `localStorage` using a `useTheme` hook. The `<html>` element receives the `.dark` class. This is the only permitted non-auth use of `localStorage`.

**Findings**:
- The design system uses `.dark` class selector (via `@custom-variant dark (&:is(.dark *))`)
- On first visit: reads `prefers-color-scheme` media query
- On subsequent visits: reads `localStorage` key `theme` (`"dark"` | `"light"`)
- SSR flash prevention: a blocking inline `<script>` in `<head>` (not `next/script`) reads localStorage and applies `.dark` to `<html>` synchronously before paint
  - This is the standard Next.js / Tailwind dark mode pattern; it requires a tiny inline script which is a deliberate, justified exception to the "no inline scripts" CSP preference — it must be listed as a SHA-256 hash in the CSP `script-src` directive
- The theme control is a Client Component (uses `localStorage`, `window.matchMedia`, event listeners)
- Theme state is NOT in Zustand — it's local to the theme provider/hook and read imperatively

**Rationale**: No auth artifact is in localStorage. The only dark-mode localStorage entry is `theme` (a UX preference). This is explicitly permitted by the constitution.

---

## R-11 — State Management Architecture

**Decision**: TanStack Query for all BFF data; Zustand only for filter panel open/close state shared across header and sidebar; `useState` for all other local UI state.

**Findings**:
- TanStack Query v5 (`@tanstack/react-query`) usage:
  - Query keys: `['inventory', filters]`, `['car', carId]`, `['profile']`, `['purchases', page]`
  - Mutations: purchase confirmation, profile update, registration, logout
  - Queries in Server Components: NOT used — Server Components fetch directly with `async/await`
  - Client Components (inventory filters, purchase page after interaction): use `useQuery` / `useMutation`
  - `QueryClientProvider` in root layout as a Client Component wrapper
- Zustand store: `useInventoryStore` — tracks `isFilterPanelOpen` (boolean), toggled by mobile filter button in site header, consumed by the filter drawer component. Justification: these are in separate subtrees of the component tree.
- No Zustand for auth state — auth is entirely server-side

**Rationale**: Constitution Article IX is precise: TanStack Query for server state, Zustand only when the same state is needed by multiple unrelated components. Filter panel visibility is the canonical example.

---

## R-12 — Form Validation Constants (CPF, CEP, Phone)

**Decision**: All validation regex constants and the CPF check-digit algorithm are defined at module scope in `lib/utils/cpf.ts`, `lib/utils/cep.ts`, `lib/utils/phone.ts`.

**Findings**:
- CPF regex (format only): `/^\d{3}\.\d{3}\.\d{3}-\d{2}$/` (masked) or `/^\d{11}$/` (raw)
- CPF check-digit algorithm: standard Brazilian CPF validation (two remainder checks)
- CEP regex: `/^\d{5}-\d{3}$/` (masked) or `/^\d{8}$/` (raw for BFF submission)
- Phone regex: `/^(\(?\d{2}\)?\s?)(\d{4,5}-?\d{4})$/` (display) or `/^\d{10,11}$/` (raw for BFF submission)
- All regex constants hoisted to module scope — NEVER instantiated inside a component or function (constitution Article XII)
- Masking functions (CPF: `XXX.XXX.XXX-XX`, CEP: `XXXXX-XXX`, Phone: `(XX) XXXXX-XXXX`) also in these modules
- Zod schemas in forms import from these modules rather than duplicating regex

**Rationale**: Constitution Article XII and Article XVII are explicit — regex as module-scope constants, centralized validation.

---

## R-13 — Error Code Mapping

**Decision**: `lib/errors.ts` exports a single `getBFFErrorMessage(code: BFFErrorCode): string` function that maps all documented BFF error codes to user-safe Portuguese messages.

**Confirmed BFF error codes** (from all contracts):
- `CAR_NOT_AVAILABLE` → "Este carro não está mais disponível. Outro comprador finalizou a compra."
- `VALIDATION_ERROR` → "Verifique os campos e tente novamente." (field-level details surfaced separately)
- `AUTHENTICATION_REQUIRED` → triggers redirect to auth flow (not shown as error message)
- `FORBIDDEN` → "Você não tem permissão para realizar esta ação."
- `NOT_FOUND` → "O item solicitado não foi encontrado."
- `RATE_LIMIT_EXCEEDED` → "Muitas tentativas. Aguarde um momento e tente novamente."
- `DOWNSTREAM_UNAVAILABLE` → "Nosso serviço está temporariamente indisponível. Tente novamente em instantes."
- `DUPLICATE_IDENTITY` → "Este e-mail já está cadastrado. Faça login ou use outro e-mail."
- `INTERNAL_ERROR` → "Ocorreu um erro interno. Por favor, tente novamente."
- Unknown codes → "Ocorreu um erro inesperado. Por favor, tente novamente." (fallback)

**Rationale**: Centralized in `lib/errors.ts` per constitution Article XI. No component ever inlines error strings.

---

## R-14 — Performance Patterns

**Decision**: Apply the following patterns to meet LCP ≤ 2.5 s, CLS ≤ 0.1, INP ≤ 200 ms.

**Key decisions**:
1. **Inventory page**: Static shell with `loading.tsx` skeleton; inventory grid fetched as RSC with `Suspense`; filter sidebar lazy-loaded with `next/dynamic` on mobile
2. **Car detail page**: Server-rendered; car image uses `next/image` with `priority` on the main image (above fold); aspect-ratio placeholder reserves space before image loads
3. **CLS prevention**: Every `next/image` has explicit width/height or fill + sized container; hero section reserves its layout even while data loads; CEP resolution result area has fixed height so text reveal doesn't shift layout
4. **INP**: Filter changes update URL params immediately (optimistic); data refetch happens in the background with the previous data shown while loading (`keepPreviousData: true` in TanStack Query)
5. **Bundle splitting**: Filter panel, purchase modal, admin table, car form modal → all `next/dynamic`
6. **Barrel imports**: Prohibited per constitution; enforced with ESLint rule `no-restricted-imports` pattern matching index barrel files

**Rationale**: These are the standard Next.js performance patterns for App Router + React Server Components. Consistent with constitution Article XII.
