// app/(marketing)/layout.tsx
// Marketing route group layout — wraps public-facing pages (home, inventory).
// Includes header (unauthenticated state) and footer.

import { SiteHeader } from "@/components/layout/SiteHeader";
import { SiteFooter } from "@/components/layout/SiteFooter";
import type { ReactNode } from "react";
import { getHeaderAuthState } from "@/lib/server/header-auth";

interface MarketingLayoutProps {
  children: ReactNode;
}

export default async function MarketingLayout({ children }: MarketingLayoutProps) {
  const { isAuthenticated, isAdmin, canAccessAccount, user } =
    await getHeaderAuthState();

  return (
    <div className="flex min-h-screen flex-col">
      <SiteHeader
        isAuthenticated={isAuthenticated}
        isAdmin={isAdmin}
        canAccessAccount={canAccessAccount}
        user={user}
      />
      <main id="main-content" className="flex-1">
        {children}
      </main>
      <SiteFooter />
    </div>
  );
}
