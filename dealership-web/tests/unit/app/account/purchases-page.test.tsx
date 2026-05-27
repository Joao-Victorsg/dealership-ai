import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import PurchasesPage from "@/app/(customer)/account/purchases/page";
import { getPurchases } from "@/lib/api/purchases";
import type { PurchaseListResponse } from "@/lib/api/types";

vi.mock("@/lib/api/purchases", () => ({
  getPurchases: vi.fn(),
}));

const getPurchasesMock = vi.mocked(getPurchases);

function createPurchasesResponse(
  overrides?: Partial<PurchaseListResponse>
): PurchaseListResponse {
  return {
    data: [],
    meta: {
      page: 0,
      pageSize: 10,
      totalElements: 0,
      totalPages: 1,
      timestamp: "2026-01-01T00:00:00Z",
      requestId: "request-id",
    },
    ...overrides,
  };
}

describe("PurchasesPage", () => {
  it("renders purchase cards when history exists", async () => {
    getPurchasesMock.mockResolvedValue(
      createPurchasesResponse({
        data: [
          {
            id: "purchase-1",
            status: "COMPLETED",
            registeredAt: "2026-01-01T00:00:00Z",
            vehicle: {
              id: "car-1",
              manufacturer: "Audi",
              model: "Q5",
              manufacturingYear: 2025,
              externalColor: "Blue",
              vin: "1HGCM82633A123456",
              category: "SUV",
              listedValue: 320000,
            },
            client: {
              firstName: "Joao",
              lastName: "Silva",
              cpf: "12345678910",
            },
          },
        ],
        meta: {
          page: 0,
          pageSize: 10,
          totalElements: 1,
          totalPages: 1,
          timestamp: "2026-01-01T00:00:00Z",
          requestId: "request-id",
        },
      })
    );

    render(await PurchasesPage({ searchParams: Promise.resolve({}) }));

    expect(screen.getByRole("heading", { name: "Minhas compras" })).toBeInTheDocument();
    expect(screen.getByText("Audi Q5")).toBeInTheDocument();
    expect(screen.getByText("Total pago")).toBeInTheDocument();
    expect(screen.getByText("1 compra registrada")).toBeInTheDocument();
  });

  it("renders empty state when customer has no purchase history", async () => {
    getPurchasesMock.mockResolvedValue(createPurchasesResponse());

    render(await PurchasesPage({ searchParams: Promise.resolve({}) }));

    expect(screen.getByText("Nenhuma compra ainda")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Ver veículos disponíveis" })).toHaveAttribute(
      "href",
      "/inventory"
    );
    expect(screen.getByText("0 compras registradas")).toBeInTheDocument();
  });
});
