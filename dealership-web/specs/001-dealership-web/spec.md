# Feature Specification: dealership-web — Full Web Application

**Feature Branch**: `001-dealership-web`  
**Created**: 2026-05-01  
**Status**: Draft  
**Input**: Full web application specification for dealership-ai customer and admin UI

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Browse Inventory & Discover Cars (Priority: P1)

An anonymous visitor opens the application, lands on the home page, and discovers available cars. They browse the full catalog using the inventory listing page with its filters, find a car they are interested in, and view its full detail.

**Why this priority**: The inventory browsing surface is the top of the purchase funnel and the single highest-value capability of the application. Every other feature depends on it. It is fully deliverable without authentication and represents a working, publicly valuable product on its own.

**Independent Test**: Navigate to `/` and verify the hero, category tiles, and recently added cars are rendered. Navigate to `/inventory`, apply category/condition/price filters, observe the results grid and filter chip summary update. Open a car detail page and verify all sections (identity, characteristics, specifications, pricing, sticky buy panel) are present.

**Acceptance Scenarios**:

1. **Given** the visitor is on the home page, **When** they fill in the quick-search form and submit, **Then** they are navigated to `/inventory` with the chosen category, type, and condition pre-applied as URL query parameters.
2. **Given** the visitor is on `/inventory` with no filters, **When** they apply a category filter, **Then** the URL updates, the results grid refreshes with only cars of that category, and a chip badge for the active filter appears above the results.
3. **Given** the visitor is on `/inventory` with filters that match no cars, **When** the results load, **Then** a meaningful empty state with a suggestion to adjust filters is displayed — never a blank area.
4. **Given** the visitor is on a car detail page (`/inventory/{carId}`), **When** the car status is Available, **Then** a "Buy this car" button appears in the sticky panel.
5. **Given** the visitor navigates to `/inventory/{carId}` for a non-existent car ID, **When** the page loads, **Then** a friendly "Car not found" page renders with a link back to inventory.
6. **Given** the visitor is on the home page, **When** they click a category tile, **Then** they are navigated to `/inventory?category=<value>`.

---

### User Story 2 — Customer Registration (Priority: P2)

After creating an account on Keycloak, the user is redirected to the platform to complete their profile with CPF, phone, CEP, and street number. Keycloak handles all credentials (email, password, name) — the web application only collects the fields the BFF requires.

**Why this priority**: Registration is the gateway to the purchase flow. Without it, the highest-revenue journey (purchase confirmation) cannot be exercised end-to-end. It is independently deliverable before any purchase logic is built.

**Independent Test**: Simulate a post-Keycloak redirect to `(auth)/complete-registration` (with a valid SESSION cookie). Fill in CPF (with check-digit validation), phone (Brazilian format), CEP (auto-resolution), and street number. Submit the form and verify the success confirmation screen is displayed.

**Acceptance Scenarios**:

1. **Given** the user has completed Keycloak registration and lands on `(auth)/complete-registration`, **When** they enter a CEP and move focus away from the field, **Then** a loading indicator is shown and, on success, the street, city, and state are displayed as read-only fields below the CEP input.
2. **Given** the user is on `(auth)/complete-registration` and CEP lookup fails, **When** they try to submit, **Then** a non-blocking warning is shown and the form CAN still be submitted without a resolved address.
3. **Given** the user submits the form with an invalid CPF, **When** the field loses focus, **Then** an inline validation error is shown below the CPF field (not on submit).
4. **Given** the user submits the registration form with all valid data (CPF, phone, CEP, street number), **When** the BFF responds with success, **Then** a confirmation screen appears with a link to continue to the application.
5. **Given** the user submits the form and the BFF returns a `VALIDATION_ERROR`, **When** the response is received, **Then** field-level errors are shown inline below the affected fields, announced via `role="alert"`.

---

### User Story 3 — Customer Authentication (Priority: P3)

A registered customer clicks "Sign in". They are redirected to Keycloak (styled with the Aurelio Motors theme), authenticate, and are returned to the application. The application never handles credentials or tokens.

**Why this priority**: Authentication unlocks the purchase flow and the account section. The PKCE flow is a redirect-based flow — the web application's role is minimal (redirect out, receive session cookie back) but must be correct.

**Independent Test**: Click "Sign in" from the home page and verify the redirect goes to the BFF's authorization endpoint. Using a mocked BFF session, verify that post-authentication the user lands at the intended destination with an authenticated session. Verify that clicking "Sign out" invalidates the session.

**Acceptance Scenarios**:

1. **Given** an unauthenticated visitor clicks "Sign in", **When** the click is processed, **Then** the browser is redirected to `<BFF_URL>/oauth2/authorization/keycloak`.
2. **Given** an unauthenticated visitor attempts to access `/account`, **When** the middleware processes the request, **Then** they are redirected to the authentication flow with `/account` preserved as the return destination.
3. **Given** an authenticated user clicks "Sign out", **When** the action is processed, **Then** `POST /api/v1/auth/logout` is called on the BFF, the session is invalidated, and the user is redirected to the home page.
4. **Given** the BFF returns a 401 on an authenticated request (expired session), **When** the response is received, **Then** the user is redirected to the authentication flow preserving their intended destination.
5. **Given** an authenticated non-admin user navigates to `/admin`, **When** the middleware processes the request, **Then** they are redirected to the home page.

---

### User Story 4 — Purchase Confirmation Flow (Priority: P4)

An authenticated customer on a car detail page clicks "Buy this car". They are taken to the purchase confirmation page, review the car details and their buyer profile, see the final price including the 10% platform tax, and confirm the purchase.

**Why this priority**: This is the core revenue transaction of the platform. It must be reliable, safe against double-submission and race conditions, and handle the edge case where another buyer purchases the car between page load and confirmation.

**Independent Test**: With a mocked authenticated session and a mocked BFF, navigate to `/purchase/{carId}`, verify the two-column layout (vehicle + buyer + financial summary), click "Confirm purchase", and verify: button becomes disabled immediately, success redirects to `/purchase/success`, `CAR_NOT_AVAILABLE` shows an inline targeted message (not a generic error).

**Acceptance Scenarios**:

1. **Given** an authenticated customer on `/purchase/{carId}`, **When** the page loads, **Then** car data and the user's profile are fetched in parallel and displayed in the two-column layout with the sticky financial summary.
2. **Given** the customer clicks "Confirm purchase", **When** the click is processed, **Then** the button is immediately disabled and shows a loading state — a second click must produce no additional submission.
3. **Given** the purchase succeeds, **When** the BFF responds with success, **Then** the customer is redirected to `/purchase/success` which shows the car name, final value, and email confirmation message.
4. **Given** the BFF returns `CAR_NOT_AVAILABLE`, **When** the response is received, **Then** an inline message specific to this scenario is shown (not a generic error), with a link to browse similar cars, and the button is re-enabled.
5. **Given** the BFF returns any other error, **When** the response is received, **Then** the error message and the `requestId` reference code are shown with a retry option.
6. **Given** an unauthenticated visitor navigates to `/purchase/{carId}`, **When** the middleware processes the request, **Then** they are redirected to the authentication flow with this page as the return URL.

---

### User Story 5 — Account Profile Management (Priority: P5)

An authenticated customer navigates to their account, views their profile information, and updates editable fields (name, phone, CEP, street number). CPF is shown as read-only.

**Why this priority**: Profile management is a standard post-purchase-registration expectation. It is independently deliverable and does not affect the purchase or browsing flows.

**Independent Test**: With a mocked authenticated session, navigate to `/account`, verify the profile form is pre-populated with the user's current data, update the phone number, save, and verify inline success feedback is shown without a full page reload.

**Acceptance Scenarios**:

1. **Given** an authenticated customer on `/account`, **When** the page loads, **Then** all editable fields are pre-populated with the current profile data from `GET /api/v1/profile`.
2. **Given** the customer updates their postal code, **When** they move focus away from the CEP field, **Then** the CEP resolution flow (loading → resolved / failed) is triggered as in the registration form.
3. **Given** the customer's address could not be resolved at registration time, **When** the profile page loads, **Then** a visible warning banner prompts them to complete their address.
4. **Given** the customer saves valid changes, **When** `PATCH /api/v1/profile` responds with success, **Then** inline success feedback is shown without navigating away from the page.
5. **Given** the customer tries to edit the CPF field, **Then** the field is read-only and a brief note explains it cannot be changed.

---

### User Story 6 — Purchase History (Priority: P6)

An authenticated customer navigates to "My purchases" and views a paginated list of their past purchases, each showing the complete car snapshot captured at the time of sale.

**Why this priority**: Purchase history is a key post-purchase service. Independently deliverable from the purchase flow itself.

**Independent Test**: With a mocked session and mocked BFF responses, navigate to `/account/purchases`, verify purchases are listed with car snapshots (manufacturer, model, year, colors, options, final value, date), verify the empty state for users with no purchases.

**Acceptance Scenarios**:

1. **Given** the customer has purchases, **When** `/account/purchases` loads, **Then** each purchase card shows the car snapshot (manufacturer, model, year, category, type, colors, optional items, final value with 10% tax, date of purchase).
2. **Given** the customer has no purchases, **When** the page loads, **Then** a meaningful empty state with a link to browse inventory is displayed.
3. **Given** the purchase list spans multiple pages, **When** the customer navigates between pages, **Then** pagination controls update correctly without full page reload.

---

### User Story 7 — Admin Inventory Management (Priority: P7)

An admin user manages the live car inventory: viewing all cars regardless of status, adding new cars, editing existing entries, and toggling availability status.

**Why this priority**: Admin inventory management is critical for business operations. Without it, there is no mechanism to keep the catalog current. It is independently deliverable from the customer-facing flows.

**Independent Test**: With an admin-role session mocked, navigate to `/admin`, verify the inventory table with all status filters, open the "Add car" form, fill in all fields, submit, and verify the table refreshes. Click "Mark unavailable" on a car, verify the confirmation dialog, confirm, and verify the table updates.

**Acceptance Scenarios**:

1. **Given** an admin user on `/admin`, **When** the page loads, **Then** the inventory table shows all cars (Available, Sold, Unavailable) with the status filter tabs above.
2. **Given** the admin clicks "Add car", **When** the form opens, **Then** all required fields (manufacturer, model, year, type, category, condition, price, kilometers, colors, optional items, VIN) are presented with labels.
3. **Given** the admin submits the add-car form with valid data, **When** the BFF responds with success, **Then** the inventory table refreshes and the new car appears.
4. **Given** the admin clicks a destructive status change (Mark unavailable/available), **When** the action is initiated, **Then** a confirmation dialog appears before any change is made.
5. **Given** a non-admin authenticated user navigates to `/admin`, **When** the middleware processes the request, **Then** they are redirected to the home page (not shown a 403 error).

---

### User Story 8 — Admin Sales Reports (Priority: P8)

An admin user navigates to the Sales Reports tab, views KPI summary cards, and examines a sortable, paginated sales table filtered by date range.

**Why this priority**: Operational visibility into revenue and sales performance. Independently deliverable from inventory management.

**Independent Test**: With an admin session and mocked BFF sales data, navigate to `/admin/sales`, verify the KPI cards (total revenue, cars sold, average value, current month count), apply a date range filter, and verify the sales table updates.

**Acceptance Scenarios**:

1. **Given** an admin on the Sales Reports tab, **When** the page loads, **Then** the KPI cards show total revenue, total cars sold, average sale value, and current month count.
2. **Given** the admin sets a date range filter, **When** the filter is applied, **Then** the URL updates with from/to parameters and the table refreshes.
3. **Given** the table has multiple pages, **When** the admin navigates between pages, **Then** the current page, total count, and pagination controls update correctly.

---

### Edge Cases

- What happens when a car moves to Sold status between the inventory page load and the user clicking on the car detail? → The car detail page shows the Sold status badge and the sticky panel displays the status message (no buy button).
- What happens when a car is purchased by another buyer between the purchase confirmation page load and the customer clicking "Confirm purchase"? → `CAR_NOT_AVAILABLE` error is caught and shown as an inline, targeted message — not a generic error.
- What happens when CEP lookup returns no results? → A non-blocking warning is shown; the user can submit the form without a resolved address.
- What happens when a user with an expired session (BFF returns 401) is on `/purchase/{carId}`? → They are redirected to the authentication flow with `/purchase/{carId}` preserved as the return URL so they land back at the confirmation page after re-authenticating.
- What happens when the car detail page is loaded for a carId that does not exist? → A `notFound()` boundary renders a friendly "Car not found" page.
- What happens when the inventory grid has zero results? → A meaningful empty state with suggestions to broaden filters is shown — never a blank area.
- What happens when the purchase history is empty? → An empty state with a link to browse inventory is shown.
- What happens when an admin attempts a status change and the BFF returns an error? → An inline error with the `requestId` reference code is displayed inside the confirmation dialog.

---

## Requirements *(mandatory)*

### Functional Requirements

#### Browsing & Discovery

- **FR-001**: Visitors MUST be able to browse the full car inventory at `/inventory` without authentication, with filter, sort, and pagination controls.
- **FR-002**: The inventory listing MUST support filtering by: free-text query, category, type (Electric / Combustion), condition (New / Pre-owned), manufacturer, year range, price range, exterior color, and kilometers range.
- **FR-003**: Active filters MUST be reflected as URL query parameters so that the filtered view is bookmarkable and shareable.
- **FR-004**: Active filters MUST be displayed as dismissible chip badges above the results, with a "Reset all" option.
- **FR-005**: The inventory listing MUST show the result count as "{n} cars".
- **FR-006**: The inventory listing MUST provide the following sort options: Recently added, Price low→high, Price high→low, Year newest, Year oldest. Kilometer range is a filter (kmMin/kmMax URL params), not a sort criterion.
- **FR-007**: Visitors MUST be able to view a full car detail page at `/inventory/{carId}` with: identity (manufacturer, model, year, VIN), characteristics, appearance (exterior and interior color swatches), specifications (kilometers and optional items as a readable tag list), pricing, and status.
- **FR-008**: The home page MUST display: a hero section with quick-access search, a "Browse by category" section, editorial split-cards for New and Pre-owned, and a "Recently added" grid fetched server-side from the BFF.
- **FR-009**: The home page quick-search MUST redirect to `/inventory` with the selected filters pre-applied as URL query parameters.

#### Authentication & Authorization

- **FR-010**: The application MUST NOT contain a login page. Login MUST redirect to `<BFF_URL>/oauth2/authorization/keycloak`.
- **FR-011**: Route protection MUST be implemented exclusively in Next.js middleware — page components MUST NOT contain authentication redirect logic.
- **FR-012**: The middleware MUST preserve the intended destination as a return URL when redirecting unauthenticated users to the authentication flow.
- **FR-013**: Admin routes (`/admin/**`) MUST additionally verify the user role from the session and redirect non-admin users to the home page.
- **FR-014**: Logout MUST call `POST /api/v1/auth/logout` on the BFF. The application MUST NOT attempt to clear authentication state independently.
- **FR-015**: The application MUST NEVER store any authentication artifact (tokens, session identifiers) in localStorage, sessionStorage, or any JavaScript-accessible location.
- **FR-016**: On a BFF 401 response during an authenticated request, the application MUST redirect to the authentication flow preserving the current page as the return URL.

#### Registration

- **FR-017**: The registration completion page (`(auth)/complete-registration`) MUST collect: CPF, phone, CEP, and street number. Name and email are provided by Keycloak and MUST NOT be re-collected by the web form.
- **FR-018**: CPF MUST be validated using the Brazilian check-digit algorithm and masked as `XXX.XXX.XXX-XX` as the user types.
- **FR-019**: CEP MUST match `XXXXX-XXX` format and MUST trigger an automatic address lookup on blur, showing three states: loading, resolved (street/city/state as read-only preview), and failed (non-blocking warning).
- **FR-020**: Phone MUST match Brazilian format including area code `(XX) XXXXX-XXXX`.
- **FR-021**: Registration MUST call `POST /api/v1/auth/register` on the BFF. On success, a confirmation screen with a "Sign in" button MUST be displayed.
- **FR-022**: BFF field-level errors from the `details` list MUST be displayed inline below the relevant field, announced via `role="alert"`.

#### Purchase Flow

- **FR-023**: `/purchase/{carId}` MUST fetch car data and the authenticated user's profile in parallel (server-side) and render a two-column layout (vehicle, buyer, financial summary).
- **FR-024**: The financial summary MUST show: listed value, tax amount (10% platform tax), a divider, and the final total.
- **FR-025**: The "Confirm purchase" button MUST be immediately disabled after the first click and MUST show a loading state — double submission MUST be prevented.
- **FR-026**: On `CAR_NOT_AVAILABLE`, an inline targeted message MUST be shown explaining the situation with a link to browse similar cars — it MUST NOT be displayed as a generic error.
- **FR-027**: On purchase success, the user MUST be redirected to `/purchase/success` which MUST display: a success indicator, car summary, final value, email confirmation message, and two CTAs ("View my purchases", "Continue browsing").
- **FR-028**: All BFF error responses MUST surface the `meta.requestId` to the user as a support reference code.

#### Account Management

- **FR-029**: `/account` MUST fetch and display the user's current profile from `GET /api/v1/profile` (server-side rendered).
- **FR-030**: Profile editable fields: first name, last name, phone, and CEP. CPF and street number MUST be read-only — CPF is immutable and street number is not accepted by `PATCH /api/v1/profile` (see research.md R-04).
- **FR-031**: Profile updates MUST call `PATCH /api/v1/profile`. Feedback MUST be shown inline without a full page reload.
- **FR-032**: `/account/purchases` MUST fetch paginated purchase history from `GET /api/v1/purchases` and display car snapshots captured at the time of sale (not current car state).

#### Admin

- **FR-033**: `/admin` MUST be accessible only to users whose session contains `role === "admin"`.
- **FR-034**: The admin inventory table MUST show all cars regardless of status, filterable by status tabs (All, Available, Sold, Unavailable).
- **FR-035**: Admins MUST be able to add cars via `POST /api/v1/admin/inventory` and edit them via `PATCH /api/v1/admin/inventory/{carId}`.
- **FR-036**: Destructive status changes MUST require confirmation via a dialog before the action is executed.
- **FR-037**: Admin sales reports MUST display: KPI cards (total revenue, cars sold, average value, current month count), a sortable paginated sales table, and a date range filter applied via URL query parameters.

#### Cross-cutting

- **FR-038**: Every page MUST have a `<title>` and `<meta name="description">` defined via Next.js metadata APIs.
- **FR-039**: Every page MUST include a skip-link as the first focusable element.
- **FR-040**: Every route segment MUST have an `error.tsx` boundary and a `loading.tsx` skeleton screen.
- **FR-041**: All currency values MUST be formatted as `R$ X.XXX,XX` using tabular-nums. All kilometer values MUST use thousands separators followed by "km". All dates MUST use `DD de MMM. de YYYY` in pt-BR locale. These formatters MUST be centralized in `lib/format.ts`.
- **FR-042**: All BFF error code → user message mappings MUST be centralized in `lib/errors.ts` — never inlined in components.
- **FR-043**: The BFF base URL MUST be configured via `BFF_URL` (server-side) and `NEXT_PUBLIC_BFF_URL` (client-side) environment variables — never hardcoded.
- **FR-044**: `dangerouslySetInnerHTML` MUST NOT be used anywhere in the application.
- **FR-045**: A Content Security Policy MUST be configured in `next.config.ts` with the Keycloak domain explicitly allowed.
- **FR-046**: Web Vitals (LCP, CLS, INP, TTFB, FID) MUST be collected via Next.js `reportWebVitals` and sent to the observability endpoint.
- **FR-047**: A global error boundary MUST capture client-side errors; error payloads MUST include the BFF `requestId` when available.
- **FR-048**: All images MUST use `next/image` with explicit dimensions. Fonts MUST use `next/font`. Heavy components not in the initial viewport MUST use `next/dynamic` with a Suspense boundary.
- **FR-049**: Barrel imports are PROHIBITED — every import MUST reference the specific source file.
- **FR-050**: All validation regular expressions (CPF, CEP, phone) MUST be hoisted to module scope as constants.

### Key Entities

- **Car**: Represents a vehicle in the inventory. Key attributes (user-facing): manufacturer, model, year, category (SUV / Sedan / Sport / Hatch / Pick-up), type (Electric / Combustion), condition (New / Pre-owned), status (Available / Sold / Unavailable), listed price (BRL), kilometer reading, exterior color, interior color, optional items (list), VIN, images, registration date.
- **CarFilter**: Represents the active filter state on the inventory listing. Attributes: free-text query, category, type, condition, manufacturer, year range (min/max), price range (min/max, BRL), exterior color, kilometers range (min/max), sort order, page.
- **Customer Profile**: Represents the authenticated customer's account. Attributes: first name, last name, email, CPF (immutable), phone, CEP, street number, resolved address (street, city, state). Role: CLIENT.
- **Purchase**: Represents a completed transaction. Attributes: purchase date, car snapshot (all car data at time of sale), buyer snapshot (name, CPF, address), listed value (BRL), tax amount (10% of listed value), final value (listed + tax). The car snapshot is immutable — it reflects the car exactly as it was when the purchase was made, regardless of subsequent changes.
- **Admin User**: Represents a user with administrative privileges. Attributes: name, email, role (ADMIN). Cannot be self-registered through the web application.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: The inventory listing page achieves LCP ≤ 2.5 s on a mid-range mobile device on a 4G connection.
- **SC-002**: The inventory listing and car detail pages achieve CLS ≤ 0.1 — no layout shift occurs when images load or CEP resolution completes.
- **SC-003**: All user interactions (filter changes, button clicks, form field focus) respond visually within 200 ms (INP ≤ 200 ms).
- **SC-004**: Every Playwright end-to-end test passes an axe-core accessibility scan with zero violations on both desktop and mobile viewports.
- **SC-005**: Unauthenticated users are always redirected to the authentication flow when accessing any protected route, with the intended destination preserved.
- **SC-006**: The "Confirm purchase" button cannot be clicked more than once — no duplicate purchase submissions reach the BFF under any interaction pattern.
- **SC-007**: A `CAR_NOT_AVAILABLE` race-condition error is shown as a specific, actionable inline message — never as a generic error screen.
- **SC-008**: Every BFF error response surfaces the `requestId` as a visible reference code on the error display.
- **SC-009**: Admin routes are inaccessible to non-admin authenticated users — verified by middleware, not by page-level logic.
- **SC-010**: All filter + sort state is reflected in the URL — the filtered inventory view is bookmarkable and produces the same results when opened directly.
- **SC-011**: All form validation errors appear inline, below the relevant field, only after the user has interacted with it — never on initial render or before interaction.
- **SC-012**: All E2E critical journeys (browse + filter, car detail, purchase confirmation, purchase success, auth redirect, purchase history) pass on both desktop and mobile viewports with no axe-core violations.

---

## Assumptions

- The dealership-bff is the sole API gateway — no additional backend services will be called directly from the web application.
- The BFF exposes an address lookup endpoint for CEP resolution accessible to registered users.
- The BFF exposes admin-specific endpoints (`POST /api/v1/admin/inventory`, `PATCH /api/v1/admin/inventory/{carId}`, `GET /api/v1/admin/sales`) requiring the ADMIN role — these endpoints are defined by the BFF spec and their exact contracts are as documented in the BFF specification.
- Admin users are created and managed externally (e.g., through Keycloak's admin console or the BFF admin API) — the web application has no admin user management UI.
- The Keycloakify theme is maintained as a separate package in the monorepo; it is out of scope for this specification but shares the same design tokens.
- Car images are served from a CDN or object storage URL returned by the BFF in the car's image field — the web application does not handle image upload in the customer-facing UI. The admin "Add / Edit car" form handles image URLs as text input.
- The application supports Portuguese (Brazilian) as the sole locale for this release — no i18n infrastructure is required beyond `pt-BR` date/number formatting.
- Dark mode preference is persisted to `localStorage` and read on first mount — this is the only authentication-unrelated use of `localStorage` permitted.
- The BFF inventory API supports the filter and sort parameters described in the requirements; exact parameter names and pagination structure are as defined by the BFF OpenAPI specification.
- A mid-range mobile device on a 4G connection is the performance baseline per the constitution (LCP ≤ 2.5 s, CLS ≤ 0.1, INP ≤ 200 ms).
