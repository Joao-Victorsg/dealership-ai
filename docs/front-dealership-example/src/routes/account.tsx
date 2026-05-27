import { createFileRoute, Link, Outlet, redirect, useLocation } from "@tanstack/react-router";
import { User2, Receipt } from "lucide-react";
import { cn } from "@/lib/utils";
import { getAuthSnapshot } from "@/lib/mock-auth";

export const Route = createFileRoute("/account")({
  beforeLoad: ({ location }) => {
    if (!getAuthSnapshot().isAuthenticated) {
      throw redirect({ to: "/login", search: { redirect: location.pathname } });
    }
  },
  component: AccountLayout,
});

const TABS = [
  { to: "/account" as const, label: "Profile", icon: User2 },
  { to: "/account/purchases" as const, label: "My purchases", icon: Receipt },
];

function AccountLayout() {
  const location = useLocation();
  return (
    <div className="mx-auto w-full max-w-[1100px] px-4 py-10 sm:px-6">
      <header className="mb-8">
        <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Account</p>
        <h1 className="mt-2 font-display">Your account.</h1>
      </header>

      <div className="grid gap-8 lg:grid-cols-[220px_1fr]">
        <aside>
          <nav aria-label="Account" className="flex flex-row gap-1 overflow-x-auto lg:flex-col">
            {TABS.map(({ to, label, icon: Icon }) => {
              const active = location.pathname === to;
              return (
                <Link
                  key={to}
                  to={to}
                  className={cn(
                    "inline-flex items-center gap-2 rounded-md px-3 py-2 text-sm transition-colors",
                    active ? "bg-primary text-primary-foreground" : "text-foreground/80 hover:bg-muted",
                  )}
                >
                  <Icon className="size-4" aria-hidden /> {label}
                </Link>
              );
            })}
          </nav>
        </aside>
        <div>
          <Outlet />
        </div>
      </div>
    </div>
  );
}
