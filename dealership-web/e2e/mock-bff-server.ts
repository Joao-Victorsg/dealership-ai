// e2e/mock-bff-server.ts
// Standalone Node.js HTTP server that simulates the dealership BFF for E2E tests.
// Started by e2e/global-setup.ts before the Next.js dev server; stopped by global-teardown.ts.
//
// Auth state is signalled by the X-E2E-SESSION cookie forwarded by Next.js middleware:
//   absent / "none"      → unauthenticated (401 for auth probe)
//   "customer"           → authenticated customer with purchase history
//   "customer-empty"     → authenticated customer with no purchases
//   "needs-registration" → authenticated user without client profile yet
//   "admin"              → admin user (200 for profile + admin inventory)

import * as http from "http";
import * as url from "url";

export const MOCK_BFF_PORT = 4000;

// ── Shared fixture data ─────────────────────────────────────────────────────

const MOCK_META = {
  timestamp: "2025-01-01T00:00:00Z",
  requestId: "mock-e2e-req-id",
};

const MOCK_PAGE_META = {
  ...MOCK_META,
  page: 0,
  pageSize: 20,
  totalPages: 1,
};

const CARS: Record<string, Record<string, unknown>> = {
  "car-001": {
    id: "car-001",
    manufacturer: "Toyota",
    model: "Corolla",
    manufacturingYear: 2023,
    externalColor: "Branco Pérola",
    internalColor: "Preto",
    vin: "9BW000000000000001",
    status: "AVAILABLE",
    category: "SEDAN",
    type: "GASOLINE",
    isNew: false,
    kilometers: 35000,
    propulsionType: "FRONT_WHEEL_DRIVE",
    listedValue: 120000,
    imageKey: null,
    optionalItems: ["Ar-condicionado", "Câmera de ré"],
    registrationDate: "2024-06-01T10:00:00Z",
  },
  "car-002": {
    id: "car-002",
    manufacturer: "Honda",
    model: "HR-V",
    manufacturingYear: 2024,
    externalColor: "Cinza Grafite",
    internalColor: "Preto",
    vin: "9BW000000000000002",
    status: "AVAILABLE",
    category: "SUV",
    type: "GASOLINE",
    isNew: true,
    kilometers: 0,
    propulsionType: "FRONT_WHEEL_DRIVE",
    listedValue: 185000,
    imageKey: null,
    optionalItems: [],
    registrationDate: "2024-07-01T10:00:00Z",
  },
  "car-sold": {
    id: "car-sold",
    manufacturer: "Honda",
    model: "Civic",
    manufacturingYear: 2022,
    externalColor: "Preto Metálico",
    internalColor: "Cinza",
    vin: "9BW000000000000003",
    status: "SOLD",
    category: "SEDAN",
    type: "GASOLINE",
    isNew: false,
    kilometers: 48000,
    propulsionType: "FRONT_WHEEL_DRIVE",
    listedValue: 95000,
    imageKey: null,
    optionalItems: [],
    registrationDate: "2024-01-01T10:00:00Z",
  },
  // car-racy: appears available on the detail page but triggers 409 on purchase submission.
  // Used by the CAR_NOT_AVAILABLE race-condition E2E test.
  "car-racy": {
    id: "car-racy",
    manufacturer: "Toyota",
    model: "Corolla",
    manufacturingYear: 2023,
    externalColor: "Branco Pérola",
    internalColor: "Preto",
    vin: "9BW000000000000004",
    status: "AVAILABLE",
    category: "SEDAN",
    type: "GASOLINE",
    isNew: false,
    kilometers: 35000,
    propulsionType: "FRONT_WHEEL_DRIVE",
    listedValue: 120000,
    imageKey: null,
    optionalItems: [],
    registrationDate: "2024-06-01T10:00:00Z",
  },
};

const INVENTORY_LIST = [CARS["car-001"], CARS["car-002"]];

const MOCK_PROFILE = {
  id: "customer-001",
  firstName: "João",
  lastName: "Silva",
  email: "joao@example.com",
  cpf: "123.456.789-09",
  phone: "(11) 91234-5678",
  createdAt: "2024-01-01T00:00:00Z",
  address: {
    cep: "01310-100",
    street: "Av. Paulista",
    number: "1578",
    neighborhood: "Bela Vista",
    city: "São Paulo",
    state: "SP",
  },
};

const MOCK_PURCHASE = {
  id: "purchase-001",
  registeredAt: "2025-01-15T14:30:00Z",
  status: "COMPLETED",
  vehicle: {
    id: "car-sold",
    manufacturer: "Honda",
    model: "Civic",
    manufacturingYear: 2022,
    externalColor: "Preto Metálico",
    vin: "9BW000000000000003",
    category: "SEDAN",
    listedValue: 95000,
  },
  client: {
    firstName: "João",
    lastName: "Silva",
    cpf: "123.456.789-09",
  },
};

// ── Helpers ─────────────────────────────────────────────────────────────────

function parseCookies(header: string | undefined): Record<string, string> {
  if (!header) return {};
  return Object.fromEntries(
    header.split(";").flatMap((part) => {
      const idx = part.indexOf("=");
      if (idx === -1) return [];
      return [[part.slice(0, idx).trim(), part.slice(idx + 1).trim()]];
    })
  );
}

function getSession(req: http.IncomingMessage): string {
  const cookies = parseCookies(req.headers.cookie);
  return cookies["X-E2E-SESSION"] ?? "none";
}

function send(res: http.ServerResponse, status: number, body: unknown): void {
  const payload = JSON.stringify(body);
  res.writeHead(status, {
    "Content-Type": "application/json",
    "Content-Length": Buffer.byteLength(payload),
  });
  res.end(payload);
}

function noContent(res: http.ServerResponse, status: number): void {
  res.writeHead(status);
  res.end();
}

function readBody(req: http.IncomingMessage): Promise<string> {
  return new Promise((resolve) => {
    let data = "";
    req.on("data", (chunk: Buffer) => {
      data += chunk.toString();
    });
    req.on("end", () => resolve(data));
  });
}

// ── Route handler ────────────────────────────────────────────────────────────

async function handle(
  req: http.IncomingMessage,
  res: http.ServerResponse
): Promise<void> {
  const parsed = url.parse(req.url ?? "/", true);
  const pathname = parsed.pathname ?? "/";
  const method = (req.method ?? "GET").toUpperCase();
  const session = getSession(req);
  const isAuthenticated = session !== "none";
  const isAdminUser = session === "admin";
  const needsRegistration = session === "needs-registration";

  // ── Public: Inventory list ─────────────────────────────────────────────
  if (pathname === "/api/v1/inventory" && method === "GET") {
    const category = parsed.query["category"];
    const filtered =
      category && typeof category === "string"
        ? INVENTORY_LIST.filter((c) => c["category"] === category)
        : INVENTORY_LIST;
    return send(res, 200, {
      data: filtered,
      meta: {
        ...MOCK_PAGE_META,
        totalElements: filtered.length,
        totalPages: filtered.length > 0 ? 1 : 0,
      },
    });
  }

  if (pathname === "/api/v1/inventory/filter-options" && method === "GET") {
    return send(res, 200, {
      data: {
        manufacturers: ["Honda", "Toyota"],
        exteriorColors: ["Branco Pérola", "Cinza Grafite"],
      },
      meta: MOCK_META,
    });
  }

  // ── Public: Car detail ─────────────────────────────────────────────────
  const carDetailMatch = /^\/api\/v1\/inventory\/([^/]+)$/.exec(pathname);
  if (carDetailMatch && method === "GET") {
    const carId = carDetailMatch[1];
    const car = CARS[carId];
    if (!car) {
      return send(res, 404, {
        code: "NOT_FOUND",
        message: "Veículo não encontrado",
        requestId: "mock-404-req",
        timestamp: MOCK_META.timestamp,
      });
    }
    return send(res, 200, { data: car, meta: MOCK_META });
  }

  // ── Auth probe + Profile ───────────────────────────────────────────────
  if (pathname === "/api/v1/profile") {
    if (method === "HEAD") {
      if (!isAuthenticated) return noContent(res, 401);
      return noContent(res, needsRegistration ? 403 : 200);
    }
    if (!isAuthenticated) return noContent(res, 401);
    if (needsRegistration) return noContent(res, 403);
    if (method === "GET") {
      return send(res, 200, { data: MOCK_PROFILE, meta: MOCK_META });
    }
    if (method === "PATCH") {
      const body = await readBody(req);
      const updates = JSON.parse(body) as Record<string, unknown>;
      return send(res, 200, {
        data: { ...MOCK_PROFILE, ...updates },
        meta: MOCK_META,
      });
    }
  }

  // ── Purchases ─────────────────────────────────────────────────────────
  if (pathname === "/api/v1/purchases" && method === "POST") {
    if (!isAuthenticated) return noContent(res, 401);
    const body = await readBody(req);
    const { carId } = JSON.parse(body) as { carId: string };
    // car-racy triggers the 409 CAR_NOT_AVAILABLE race-condition scenario
    if (carId === "car-racy") {
      return send(res, 409, {
        code: "CAR_NOT_AVAILABLE",
        message: "Este veículo não está disponível.",
        requestId: "mock-409-req",
        timestamp: MOCK_META.timestamp,
      });
    }
    const car = CARS[carId];
    return send(res, 201, {
      data: {
        id: "purchase-new",
        registeredAt: new Date().toISOString(),
        status: "COMPLETED",
        vehicle: car
          ? {
              id: car["id"],
              manufacturer: car["manufacturer"],
              model: car["model"],
              manufacturingYear: car["manufacturingYear"],
              externalColor: car["externalColor"],
              vin: car["vin"],
              category: car["category"],
              listedValue: car["listedValue"],
            }
          : null,
        client: {
          firstName: MOCK_PROFILE.firstName,
          lastName: MOCK_PROFILE.lastName,
          cpf: MOCK_PROFILE.cpf,
        },
      },
      meta: MOCK_META,
    });
  }

  if (pathname.startsWith("/api/v1/purchases") && method === "GET") {
    if (!isAuthenticated) return noContent(res, 401);
    // "customer-empty" persona has no purchase history
    const purchases = session === "customer-empty" ? [] : [MOCK_PURCHASE];
    return send(res, 200, {
      data: purchases,
      meta: {
        ...MOCK_PAGE_META,
        totalElements: purchases.length,
        totalPages: purchases.length > 0 ? 1 : 0,
      },
    });
  }

  // ── Admin inventory ────────────────────────────────────────────────────
  if (pathname.startsWith("/api/v1/admin/inventory")) {
    if (!isAuthenticated) return noContent(res, 401);
    if (!isAdminUser) return noContent(res, 403);
    return send(res, 200, {
      data: Object.values(CARS),
      meta: { ...MOCK_PAGE_META, totalElements: Object.keys(CARS).length },
    });
  }

  // ── Admin sales ────────────────────────────────────────────────────────
  if (pathname.startsWith("/api/v1/admin/sales")) {
    if (!isAuthenticated) return noContent(res, 401);
    if (!isAdminUser) return noContent(res, 403);
    return send(res, 200, {
      data: [],
      summary: {
        totalRevenue: 0,
        totalSold: 0,
        averageSaleValue: 0,
        currentMonthCount: 0,
      },
      meta: { ...MOCK_PAGE_META, totalElements: 0 },
    });
  }

  // ── Fallback ───────────────────────────────────────────────────────────
  noContent(res, 404);
}

// ── Server lifecycle ─────────────────────────────────────────────────────────

export function createMockBffServer(): http.Server {
  return http.createServer((req, res) => {
    handle(req, res).catch((err: unknown) => {
      console.error("[mock-bff] Unhandled error:", err);
      res.writeHead(500);
      res.end();
    });
  });
}

export function startMockBffServer(): Promise<http.Server> {
  return new Promise((resolve, reject) => {
    const server = createMockBffServer();
    // Bind to 127.0.0.1 to ensure IPv4 loopback (avoids IPv6 ::1 mismatch on Windows).
    server.listen(MOCK_BFF_PORT, "127.0.0.1", () => {
      console.log(
        `[mock-bff] Listening on http://127.0.0.1:${MOCK_BFF_PORT}`
      );
      resolve(server);
    });
    server.on("error", reject);
  });
}
