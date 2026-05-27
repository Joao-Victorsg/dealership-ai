// app/(admin)/layout.tsx
// Admin route group layout — wraps admin panel pages.
// Middleware ensures only ROLE_ADMIN users can reach these routes.

import { SiteHeader } from "@/components/layout/SiteHeader";
import { SiteFooter } from "@/components/layout/SiteFooter";
import type { ReactNode } from "react";

interface AdminLayoutProps {
  children: ReactNode;
}

export default function AdminLayout({ children }: AdminLayoutProps) {
  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader isAuthenticated={true} isAdmin={true} canAccessAccount={true} />
      <main id="main-content" className="flex-1">
        <div className="mx-auto max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
          {children}
        </div>
      </main>
      <SiteFooter />
    </div>
  );
}
