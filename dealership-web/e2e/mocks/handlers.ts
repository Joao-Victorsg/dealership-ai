import { http, HttpResponse } from "msw";

/**
 * MSW handlers for BFF endpoint mocking in E2E tests.
 * Realistic fixture data per data-model.md type shapes.
 * Source: tasks.md T074; data-model.md §1-6
 *
 * Endpoints covered:
 *   GET  /api/v1/inventory              — inventory list
 *   GET  /api/v1/inventory/:id          — car detail
 *   POST /api/v1/auth/register          — registration completion
 *   POST /api/v1/auth/logout            — logout
 *   HEAD /api/v1/profile                — auth probe (middleware OI-1)
 *   GET  /api/v1/profile                — customer profile
 *   PATCH /api/v1/profile               — update profile
 *   POST /api/v1/purchases              — confirm purchase
 *   GET  /api/v1/purchases              — purchase history
 *   GET  /api/v1/cep/:cep               — CEP lookup (⚠️ assumed contract)
 *   GET  /api/v1/admin/inventory        — admin inventory (⚠️ assumed contract)
 *   POST /api/v1/admin/inventory        — create car (⚠️ assumed contract)
 *   PATCH /api/v1/admin/inventory/:id   — update car (⚠️ assumed contract)
 *   GET  /api/v1/admin/sales            — sales report (⚠️ assumed contract)
 */

const BFF_URL = process.env.NEXT_PUBLIC_BFF_URL ?? "http://localhost:8080";

// ─── Shared fixtures ─────────────────────────────────────────────────────────

const MOCK_META_BASE = {
  timestamp: "2025-01-01T00:00:00Z",
  requestId: "mock-request-id",
};

const MOCK_PAGE_META = {
  ...MOCK_META_BASE,
  page: 0,
  pageSize: 20,
  totalElements: 2,
  totalPages: 1,
};

const MOCK_CAR_AVAILABLE = {
  id: "car-001",
  manufacturer: "Toyota",
  model: "Corolla",
  manufacturingYear: 2023,
  externalColor: "Branco Pérola",
  internalColor: "Preto",
  vin: "9BW000000000000001",
  status: "AVAILABLE" as const,
  category: "SEDAN" as const,
  type: "GASOLINE" as const,
  isNew: false,
  kilometers: 35000,
  propulsionType: "FRONT_WHEEL_DRIVE",
  listedValue: 120000,
  imageKey: null,
  optionalItems: ["Ar-condicionado", "Câmera de ré"],
  registrationDate: "2024-06-01T10:00:00Z",
};

const MOCK_CAR_SOLD = {
  id: "car-002",
  manufacturer: "Honda",
  model: "Civic",
  manufacturingYear: 2022,
  externalColor: "Cinza Grafite",
  internalColor: "Bege",
  vin: "9BW000000000000002",
  status: "SOLD" as const,
  category: "SEDAN" as const,
  type: "GASOLINE" as const,
  isNew: false,
  kilometers: 50000,
  propulsionType: "FRONT_WHEEL_DRIVE",
  listedValue: 95000,
  imageKey: null,
  optionalItems: [],
  registrationDate: "2024-03-15T08:00:00Z",
};

const MOCK_PROFILE = {
  id: "customer-001",
  firstName: "João",
  lastName: "Silva",
  email: "joao.silva@example.com",
  cpf: "123.456.789-09",
  phone: "11912345678",
  createdAt: "2024-01-01T00:00:00Z",
  address: {
    cep: "01310100",
    street: "Avenida Paulista",
    number: "1578",
    complement: "Apto 42",
    neighborhood: "Bela Vista",
    city: "São Paulo",
    state: "SP",
  },
};

const MOCK_PURCHASE = {
  id: "purchase-001",
  registeredAt: "2025-01-15T14:30:00Z",
  status: "COMPLETED" as const,
  vehicle: {
    id: MOCK_CAR_SOLD.id,
    manufacturer: MOCK_CAR_SOLD.manufacturer,
    model: MOCK_CAR_SOLD.model,
    manufacturingYear: MOCK_CAR_SOLD.manufacturingYear,
    externalColor: MOCK_CAR_SOLD.externalColor,
    vin: MOCK_CAR_SOLD.vin,
    category: MOCK_CAR_SOLD.category,
    listedValue: MOCK_CAR_SOLD.listedValue,
  },
  client: {
    firstName: MOCK_PROFILE.firstName,
    lastName: MOCK_PROFILE.lastName,
    cpf: MOCK_PROFILE.cpf,
  },
};

const MOCK_ADMIN_INVENTORY_META = {
  ...MOCK_META_BASE,
  page: 0,
  pageSize: 20,
  totalElements: 2,
  totalPages: 1,
};

// ─── Handlers ─────────────────────────────────────────────────────────────────

export const handlers = [
  // Inventory list
  http.get(`${BFF_URL}/api/v1/inventory`, () => {
    return HttpResponse.json({
      data: [MOCK_CAR_AVAILABLE, MOCK_CAR_SOLD],
      meta: MOCK_PAGE_META,
    });
  }),

  // Inventory filter options
  http.get(`${BFF_URL}/api/v1/inventory/filter-options`, () => {
    return HttpResponse.json({
      data: {
        manufacturers: ["Honda", "Toyota"],
        exteriorColors: ["Branco Pérola", "Cinza Grafite"],
      },
      meta: MOCK_META_BASE,
    });
  }),

  // Car detail
  http.get(`${BFF_URL}/api/v1/inventory/:id`, ({ params }) => {
    const { id } = params;
    if (id === MOCK_CAR_AVAILABLE.id) {
      return HttpResponse.json({ data: MOCK_CAR_AVAILABLE, meta: MOCK_META_BASE });
    }
    if (id === MOCK_CAR_SOLD.id) {
      return HttpResponse.json({ data: MOCK_CAR_SOLD, meta: MOCK_META_BASE });
    }
    return HttpResponse.json(
      {
        code: "NOT_FOUND",
        message: "Veículo não encontrado",
        requestId: "mock-req-not-found",
        timestamp: "2025-01-01T00:00:00Z",
      },
      { status: 404 }
    );
  }),

  // Auth register
  http.post(`${BFF_URL}/api/v1/auth/register`, () => {
    return HttpResponse.json({ data: null, meta: MOCK_META_BASE }, { status: 201 });
  }),

  // Auth logout
  http.post(`${BFF_URL}/api/v1/auth/logout`, () => {
    return new HttpResponse(null, { status: 204 });
  }),

  // Profile HEAD (auth probe — OI-1)
  http.head(`${BFF_URL}/api/v1/profile`, () => {
    return new HttpResponse(null, { status: 200 });
  }),

  // Profile GET
  http.get(`${BFF_URL}/api/v1/profile`, () => {
    return HttpResponse.json({ data: MOCK_PROFILE, meta: MOCK_META_BASE });
  }),

  // Profile PATCH
  http.patch(`${BFF_URL}/api/v1/profile`, () => {
    return HttpResponse.json({ data: MOCK_PROFILE, meta: MOCK_META_BASE });
  }),

  // Purchase confirmation
  http.post(`${BFF_URL}/api/v1/purchases`, () => {
    return HttpResponse.json(
      { data: MOCK_PURCHASE, meta: MOCK_META_BASE },
      { status: 201 }
    );
  }),

  // Purchase history
  http.get(`${BFF_URL}/api/v1/purchases`, () => {
    return HttpResponse.json({
      data: [MOCK_PURCHASE],
      meta: MOCK_PAGE_META,
    });
  }),

  // CEP lookup — ⚠️ assumed BFF contract
  http.get(`${BFF_URL}/api/v1/cep/:cep`, ({ params }) => {
    const { cep } = params;
    if (cep === "01310100") {
      return HttpResponse.json({
        data: {
          cep: "01310-100",
          street: "Avenida Paulista",
          neighborhood: "Bela Vista",
          city: "São Paulo",
          state: "SP",
        },
        meta: MOCK_META_BASE,
      });
    }
    return HttpResponse.json(
      {
        code: "NOT_FOUND",
        message: "CEP não encontrado",
        requestId: "mock-cep-not-found",
        timestamp: "2025-01-01T00:00:00Z",
      },
      { status: 404 }
    );
  }),

  // Admin inventory GET — ⚠️ assumed contract; 200 = admin role
  http.get(`${BFF_URL}/api/v1/admin/inventory`, () => {
    return HttpResponse.json({
      data: [MOCK_CAR_AVAILABLE, MOCK_CAR_SOLD],
      meta: MOCK_ADMIN_INVENTORY_META,
    });
  }),

  // Admin create car — ⚠️ assumed contract
  http.post(`${BFF_URL}/api/v1/admin/inventory`, () => {
    return HttpResponse.json(
      { data: MOCK_CAR_AVAILABLE, meta: MOCK_META_BASE },
      { status: 201 }
    );
  }),

  // Admin update car — ⚠️ assumed contract
  http.patch(`${BFF_URL}/api/v1/admin/inventory/:id`, () => {
    return HttpResponse.json({ data: MOCK_CAR_AVAILABLE, meta: MOCK_META_BASE });
  }),

  // Admin sales report — ⚠️ assumed contract
  http.get(`${BFF_URL}/api/v1/admin/sales`, () => {
    return HttpResponse.json({
      data: [
        {
          id: "sale-001",
          registeredAt: "2025-01-15T14:30:00Z",
          buyer: {
            firstName: MOCK_PROFILE.firstName,
            lastName: MOCK_PROFILE.lastName,
            cpf: MOCK_PROFILE.cpf,
          },
          vehicle: MOCK_PURCHASE.vehicle,
          listedValue: MOCK_CAR_SOLD.listedValue,
          taxAmount: MOCK_CAR_SOLD.listedValue * 0.1,
          finalValue: MOCK_CAR_SOLD.listedValue * 1.1,
        },
      ],
      meta: MOCK_PAGE_META,
      kpi: {
        totalRevenue: 104500,
        totalCarsSold: 1,
        averageSaleValue: 104500,
        currentMonthCount: 1,
      },
    });
  }),
];

