// app/(auth)/layout.tsx
// Auth route group layout — minimal shell for registration completion page.
// Uses the same top header for consistency with app navigation.

import type { Metadata } from "next";
import type { ReactNode } from "react";
import { SiteHeader } from "@/components/layout/SiteHeader";
import { SiteFooter } from "@/components/layout/SiteFooter";

export const metadata: Metadata = {
  title: "Completar cadastro",
  description: "Finalize o seu cadastro para continuar.",
};

interface AuthLayoutProps {
  children: ReactNode;
}

export default function AuthLayout({ children }: AuthLayoutProps) {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      <SiteHeader isAuthenticated={true} isAdmin={false} canAccessAccount={false} />
      <main
        id="main-content"
        className="flex flex-1 items-center justify-center px-4 py-12"
      >
        {children}
      </main>
      <SiteFooter />
    </div>
  );
}
