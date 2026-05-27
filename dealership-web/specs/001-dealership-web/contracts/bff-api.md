# Web Contracts: BFF API Surface for dealership-web

**Date**: 2026-05-01  
**Source**: dealership-bff/specs/001-bff-orchestration/contracts/ + 002-pkce-auth-migration/contracts/auth-flow.md  
**Scope**: All BFF endpoints consumed by dealership-web, annotated with web-specific usage notes.

---

## Auth Endpoints

### Login Initiation (browser navigate — not a fetch)
```
GET /oauth2/authorization/keycloak
→ 302 redirect to Keycloak PKCE flow
```
**Web usage**: `window.location.href = `${BFF_URL}/oauth2/authorization/keycloak`` — or a plain `<a>` href.  
**Note**: This is a Spring Security–managed route; no controller code on BFF side.

### PKCE Callback (handled entirely by BFF/Spring — web app does nothing)
```
GET /login/oauth2/code/keycloak?code=...&state=...
→ 302 to post-login redirect; sets SESSION cookie
```
**Web usage**: Never called directly by the web app. Spring Security handles it.

### Registration Initiation — Keycloak-Register Flow (browser navigate — not a fetch)
```
GET /oauth2/authorization/keycloak-register
→ 302 redirect to Keycloak PKCE registration flow
```
**Web usage**: `window.location.href = `${BFF_URL}/oauth2/authorization/keycloak-register`` — or a plain `<a>` href.  
**Note**: This is a Spring Security–managed route using the `keycloak-register` provider registration. After the user creates their Keycloak account, `OAuth2LoginSuccessHandler` sets the SESSION cookie and redirects to the web app's `(auth)/complete-registration` page.

### Registration (AUTHENTICATED — requires SESSION from Keycloak)
```
POST /api/v1/auth/register
Cookie: SESSION=<opaque>        ← Required. User must have authenticated via keycloak-register first.
Content-Type: application/json

{
  "cpf": string,          // 11 digits, valid check digit
  "phone": string,        // 10 or 11 digits
  "cep": string,          // 8 digits, no hyphen
  "streetNumber": string  // non-blank
}
// firstName + lastName are extracted from the Keycloak JWT (given_name / family_name claims)
// email is in Keycloak — NOT accepted by this endpoint

201 Created: { data: null, meta: { requestId, timestamp } }
400 VALIDATION_ERROR: field-level details[]
401 AUTHENTICATION_REQUIRED  ← user not yet authenticated via Keycloak
503 DOWNSTREAM_UNAVAILABLE
```
**Web usage**: Server Action in `app/(auth)/complete-registration/actions.ts`.  
**Fields NOT accepted**: email, password, firstName, lastName — do not include.

### Logout
```
POST /api/v1/auth/logout
Cookie: SESSION=<opaque>

204 No Content: { data: null, meta: { requestId, timestamp } }
401 AUTHENTICATION_REQUIRED
```
**Web usage**: Server Action in `lib/api/auth.ts`; called from site header logout button.  
**Note**: BFF issues OIDC end-session redirect to Keycloak. Web app should follow redirect.

---

## Inventory Endpoints

### List Cars
```
GET /api/v1/inventory
  ?q=<string>
  &category=SEDAN|SUV|HATCHBACK|COUPE|CONVERTIBLE|MINIVAN|PICKUP|OTHER
  &type=ELECTRIC|HYBRID|GASOLINE|DIESEL
  &condition=NEW|USED
  &manufacturer=<string>
  &yearMin=<int> &yearMax=<int>
  &priceMin=<decimal> &priceMax=<decimal>
  &color=<string>
  &kmMin=<decimal> &kmMax=<decimal>
  &sortBy=PRICE|YEAR|REGISTRATION_DATE   (default: REGISTRATION_DATE)
  &sortDirection=ASC|DESC                (default: DESC)
  &page=<int>                            (zero-based, default: 0)
  &size=<int>                            (default: 20, max: 100)

200 OK: { data: Car[], meta: { requestId, timestamp, page, pageSize, totalElements, totalPages } }
400 VALIDATION_ERROR (invalid enum, priceMin > priceMax, etc.)
503 DOWNSTREAM_UNAVAILABLE
```
**Web usage**: RSC `page.tsx` in `app/(marketing)/inventory/` passes searchParams directly.  
**imageKey handling**: `imageUrl = imageKey ? `${process.env.NEXT_PUBLIC_CDN_URL}/${imageKey}` : null`

### Get Car Detail
```
GET /api/v1/inventory/{id}

200 OK: { data: Car, meta: { requestId, timestamp } }
404 NOT_FOUND
503 DOWNSTREAM_UNAVAILABLE
```
**Web usage**: RSC `page.tsx` in `app/(marketing)/inventory/[carId]/`. Uses `notFound()` on 404.

---

## Profile Endpoints (Authenticated — ROLE_CLIENT)

All requests sent with SESSION cookie only — no Authorization header from web app.  
`SessionTokenInjectionFilter` on BFF injects the bearer token transparently.

### Get Profile
```
GET /api/v1/profile

200 OK: { data: CustomerProfile, meta: { requestId, timestamp } }
401 AUTHENTICATION_REQUIRED  → redirect to auth flow
403 FORBIDDEN
404 NOT_FOUND                → profile never created (registration bug scenario)
503 DOWNSTREAM_UNAVAILABLE
```
**Web usage**: RSC `page.tsx` in `app/(customer)/account/`.

### Update Profile
```
PATCH /api/v1/profile
Content-Type: application/json

{
  "firstName"?: string,   // non-blank, max 100
  "lastName"?: string,    // non-blank, max 100
  "phone"?: string,       // 10 or 11 digits
  "cep"?: string          // 8 digits, no hyphen
}
// ⚠️ DO NOT send "cpf" — causes VALIDATION_ERROR in strict mode
// ⚠️ "streetNumber"/"number" is NOT supported — see research.md R-04

200 OK: { data: CustomerProfile, meta: { requestId, timestamp } }
400 VALIDATION_ERROR (cpf present, format invalid, empty body)
401 AUTHENTICATION_REQUIRED
403 FORBIDDEN
503 DOWNSTREAM_UNAVAILABLE
```
**Web usage**: Server Action in `app/(customer)/account/actions.ts`.

---

## Purchase Endpoints (Authenticated — ROLE_CLIENT)

### Confirm Purchase
```
POST /api/v1/purchases
Content-Type: application/json

{ "carId": "<UUID>" }

201 Created: { data: Purchase, meta: { requestId, timestamp } }
400 VALIDATION_ERROR (invalid UUID)
401 AUTHENTICATION_REQUIRED  → redirect to auth
403 FORBIDDEN
404 NOT_FOUND                (car not found)
409 CAR_NOT_AVAILABLE        → inline targeted message (not generic error)
429 RATE_LIMIT_EXCEEDED
503 DOWNSTREAM_UNAVAILABLE
```
**Web usage**: Server Action in `app/(customer)/purchase/[carId]/actions.ts`.  
**Critical**: Disable submit button immediately on first click. 409 = specific UI treatment.

### Get Purchase History
```
GET /api/v1/purchases
  ?from=<ISO-8601>    (optional)
  &to=<ISO-8601>      (optional)
  &page=<int>         (zero-based, default: 0)
  &size=<int>         (default: 20)

200 OK: { data: Purchase[], meta: { requestId, timestamp, page, pageSize, totalElements, totalPages } }
401 AUTHENTICATION_REQUIRED
403 FORBIDDEN
503 DOWNSTREAM_UNAVAILABLE
```
**Web usage**: RSC `page.tsx` in `app/(customer)/account/purchases/`. Never cached.  
**Note**: Vehicle snapshot omits internalColor, type, optionalItems, imageKey — see research.md R-07.

---

## Admin Endpoints (⚠️ Assumed — Pending BFF Spec)

> These contracts are assumed based on spec requirements. They MUST be validated
> against the official BFF admin spec before implementation. See research.md R-08.

### List Admin Inventory
```
GET /api/v1/admin/inventory        [ASSUMED]
  ?status=AVAILABLE|SOLD|UNAVAILABLE (optional — all if absent)
  &page=<int>
  &size=<int>

200 OK: { data: Car[], meta: { ...BffPageMeta } }
```

### Create Car
```
POST /api/v1/admin/inventory       [ASSUMED]
Content-Type: application/json

{ manufacturer, model, manufacturingYear, type, category, isNew,
  listedValue, kilometers, externalColor, internalColor, optionalItems[], vin }

201 Created: { data: Car, meta: ... }
400 VALIDATION_ERROR
```

### Update Car
```
PATCH /api/v1/admin/inventory/{carId}   [ASSUMED]
Content-Type: application/json

{ ...partial Car fields, status? }

200 OK: { data: Car, meta: ... }
400 VALIDATION_ERROR
404 NOT_FOUND
```

### Sales Reports
```
GET /api/v1/admin/sales            [ASSUMED]
  ?from=<ISO-8601>
  &to=<ISO-8601>
  &page=<int>
  &size=<int>

200 OK: {
  data: {
    summary: { totalRevenue, totalCarsSold, averageSaleValue, currentMonthCount },
    records: SaleRecord[]
  },
  meta: { ...BffPageMeta }
}
```

---

## CEP Lookup (⚠️ Assumed — BFF endpoint pending implementation)

> This endpoint does NOT yet exist in the BFF. It must be implemented by the BFF team before
> the CEP auto-fill feature can be used in production. The web app will call this endpoint
> via `lib/api/cep.ts`. Do NOT call ViaCEP directly from the web app.

```
GET /api/v1/cep/{cep}           [ASSUMED]
  (cep = 8 digits, no hyphen)

200 OK (found):
{
  "street": "Avenida Paulista",
  "neighborhood": "Bela Vista",
  "city": "São Paulo",
  "state": "SP"
}

404 NOT_FOUND: { error: { code: "NOT_FOUND", message: "CEP não encontrado" } }
503 DOWNSTREAM_UNAVAILABLE
```

**BFF behaviour (assumed)**: BFF calls ViaCEP internally (`https://viacep.com.br/ws/{cep}/json/`) and maps the response. No auth required (public endpoint).  
**Web usage**: `lib/api/cep.ts` → `lookupCep(cep: string): Promise<CepLookupResult>` — called from Server Actions in the complete-registration and profile update forms.
