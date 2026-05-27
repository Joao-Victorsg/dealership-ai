import { http, HttpResponse } from "msw";

const BFF_URL = process.env.NEXT_PUBLIC_BFF_URL ?? "http://localhost:8080";

/**
 * MSW handler overrides to simulate an authenticated customer session.
 * Use these in tests that require a logged-in user.
 *
 * Usage in Playwright tests:
 *   await page.route(`${BFF_URL}/api/v1/profile`, authenticatedProfileHandler);
 *
 * Or use the helper functions below with the MSW server setup.
 */

export const MOCK_CUSTOMER_PROFILE = {
  id: "customer-001",
  firstName: "João",
  lastName: "Silva",
  email: "joao.silva@example.com",
  cpf: "123.456.789-09",
  phone: "(11) 91234-5678",
  address: {
    cep: "01310-100",
    street: "Avenida Paulista",
    neighborhood: "Bela Vista",
    city: "São Paulo",
    state: "SP",
    number: "1578",
  },
} as const;

export const MOCK_ADMIN_PROFILE = {
  ...MOCK_CUSTOMER_PROFILE,
  id: "admin-001",
  email: "admin@dealership.example.com",
} as const;

/**
 * Returns MSW handlers that simulate an authenticated customer session.
 * Provides a realistic profile response from GET /api/v1/profile.
 */
export function customerSessionHandlers() {
  return [
    http.get(`${BFF_URL}/api/v1/profile`, () => {
      return HttpResponse.json(MOCK_CUSTOMER_PROFILE);
    }),
  ];
}

/**
 * Returns MSW handlers that simulate an authenticated admin session.
 * Admin inventory probe returns 200 (role signal per plan.md OI-2).
 */
export function adminSessionHandlers() {
  return [
    http.get(`${BFF_URL}/api/v1/profile`, () => {
      return HttpResponse.json(MOCK_ADMIN_PROFILE);
    }),
    http.get(`${BFF_URL}/api/v1/admin/inventory`, () => {
      return HttpResponse.json({ data: [], meta: { page: 0, size: 20, totalElements: 0, totalPages: 0 } });
    }),
  ];
}

/**
 * Returns MSW handlers that simulate a 401 unauthenticated state.
 */
export function unauthenticatedHandlers() {
  return [
    http.get(`${BFF_URL}/api/v1/profile`, () => {
      return new HttpResponse(null, { status: 401 });
    }),
  ];
}
