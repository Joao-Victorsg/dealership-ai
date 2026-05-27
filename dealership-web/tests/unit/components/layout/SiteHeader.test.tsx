import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it } from "vitest";
import { SiteHeader } from "@/components/layout/SiteHeader";

describe("SiteHeader", () => {
  beforeEach(() => {
    process.env.NEXT_PUBLIC_BFF_URL = "http://localhost:8083";
  });

  it("renders a customer dropdown with profile and purchases links", async () => {
    const user = userEvent.setup();

    render(
      <SiteHeader
        isAuthenticated={true}
        isAdmin={false}
        canAccessAccount={true}
        user={{
          firstName: "Joao",
          lastName: "Silva",
          email: "joao@example.com",
        }}
      />
    );

    expect(screen.getByRole("button", { name: "Open account menu" })).toHaveTextContent("Joao");
    expect(screen.queryByRole("link", { name: "Sign out" })).not.toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Open account menu" }));

    expect(screen.getByRole("menuitem", { name: "Profile" })).toHaveAttribute("href", "/account");
    expect(screen.getByRole("menuitem", { name: "My purchases" })).toHaveAttribute("href", "/account/purchases");
    expect(screen.getByRole("menuitem", { name: "Sign out" })).toHaveAttribute(
      "href",
      "http://localhost:8083/api/v1/auth/logout"
    );
  });

  it("falls back to account link and standalone sign out when profile is unavailable", () => {
    render(
      <SiteHeader
        isAuthenticated={true}
        isAdmin={false}
        canAccessAccount={true}
      />
    );

    expect(screen.getByRole("link", { name: "Account" })).toHaveAttribute("href", "/account");
    expect(screen.getByRole("link", { name: "Sign out" })).toHaveAttribute(
      "href",
      "http://localhost:8083/api/v1/auth/logout"
    );
  });
});
