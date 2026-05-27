import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { AccountSidebarNav } from "@/components/account/AccountSidebarNav";

const mockUsePathname = vi.fn();

vi.mock("next/navigation", () => ({
  usePathname: () => mockUsePathname(),
}));

describe("AccountSidebarNav", () => {
  it("renders account tabs and marks profile as active on /account", () => {
    mockUsePathname.mockReturnValue("/account");

    render(<AccountSidebarNav />);

    const profileLink = screen.getByRole("link", { name: "Perfil" });
    const purchasesLink = screen.getByRole("link", { name: "Minhas compras" });

    expect(profileLink).toHaveAttribute("href", "/account");
    expect(profileLink).toHaveAttribute("aria-current", "page");
    expect(purchasesLink).toHaveAttribute("href", "/account/purchases");
    expect(purchasesLink).not.toHaveAttribute("aria-current");
  });

  it("marks purchases tab as active on /account/purchases", () => {
    mockUsePathname.mockReturnValue("/account/purchases");

    render(<AccountSidebarNav />);

    expect(screen.getByRole("link", { name: "Minhas compras" })).toHaveAttribute(
      "aria-current",
      "page"
    );
    expect(screen.getByRole("link", { name: "Perfil" })).not.toHaveAttribute(
      "aria-current"
    );
  });
});
