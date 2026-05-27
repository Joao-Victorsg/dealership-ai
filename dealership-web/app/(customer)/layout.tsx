// app/(customer)/layout.tsx
// Customer route group layout — wraps authenticated customer pages.
// Middleware ensures only authenticated users can reach these routes.
// Header shows authenticated state; profile link included.

import { SiteHeader } from "@/components/layout/SiteHeader";
import { SiteFooter } from "@/components/layout/SiteFooter";
import type { ReactNode } from "react";
import { getHeaderAuthState } from "@/lib/server/header-auth";

interface CustomerLayoutProps {
  children: ReactNode;
}

export default async function CustomerLayout({ children }: CustomerLayoutProps) {
  const { user } = await getHeaderAuthState();

  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader
        isAuthenticated={true}
        isAdmin={false}
        canAccessAccount={true}
        user={user}
      />
      <main id="main-content" className="flex-1">
        {children}
      </main>
      <SiteFooter />
    </div>
  );
}
