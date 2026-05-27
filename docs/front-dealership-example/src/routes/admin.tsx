import { createFileRoute, Link, Outlet, redirect, useLocation } from "@tanstack/react-router";
import { Boxes, LineChart, Settings2, ArrowLeft } from "lucide-react";
import { getAuthSnapshot, useAuth } from "@/lib/mock-auth";
import { cn } from "@/lib/utils";

export const Route = createFileRoute("/admin")({
  beforeLoad: ({ location }) => {
    const snap = getAuthSnapshot();
    if (!snap.isAuthenticated) {
      throw redirect({ to: "/login", search: { redirect: location.pathname } });
    }
    if (snap.user?.role !== "admin") {
      throw redirect({ to: "/" });
    }
  },
  head: () => ({
    meta: [
      { title: "Admin — Aurelio Motors" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: AdminLayout,
});

const TABS = [
  { to: "/admin" as const, label: "Inventory", icon: Boxes, exact: true },
  { to: "/admin/sales" as const, label: "Sales reports", icon: LineChart },
  { to: "/admin/settings" as const, label: "Settings", icon: Settings2 },
];

function AdminLayout() {
  const { user } = useAuth();
  const location = useLocation();

  return (
    <div className="mx-auto w-full max-w-[1280px] px-4 py-8 sm:px-6">
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
            Aurelio · Admin
          </p>
          <h1 className="mt-2 font-display">Inventory management.</h1>
          <p className="mt-2 max-w-2xl text-sm text-muted-foreground">
            Welcome back{user?.firstName ? `, ${user.firstName}` : ""}. Manage live
            listings, review sales performance, and configure the storefront.
          </p>
        </div>
        <Link
          to="/"
          className="hidden items-center gap-1.5 text-xs text-muted-foreground hover:text-foreground sm:inline-flex"
        >
          <ArrowLeft className="size-3.5" aria-hidden /> Back to storefront
        </Link>
      </div>

      <nav
        aria-label="Admin sections"
        role="tablist"
        className="mt-8 flex gap-1 overflow-x-auto border-b border-border"
      >
        {TABS.map((tab) => {
          const isActive = tab.exact
            ? location.pathname === tab.to
            : location.pathname.startsWith(tab.to);
          const Icon = tab.icon;
          return (
            <Link
              key={tab.to}
              to={tab.to}
              role="tab"
              aria-selected={isActive}
              className={cn(
                "inline-flex items-center gap-2 border-b-2 px-4 py-3 text-sm font-medium transition-colors",
                isActive
                  ? "border-primary text-foreground"
                  : "border-transparent text-muted-foreground hover:text-foreground",
              )}
            >
              <Icon className="size-4" aria-hidden />
              {tab.label}
            </Link>
          );
        })}
      </nav>

      <div className="mt-8">
        <Outlet />
      </div>
    </div>
  );
}
