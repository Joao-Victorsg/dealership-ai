import type { ReactNode } from "react";
import { AccountSidebarNav } from "@/components/account/AccountSidebarNav";

interface AccountLayoutProps {
  children: ReactNode;
}

export default function AccountLayout({ children }: AccountLayoutProps) {
  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <header className="mb-8">
        <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">
          Conta
        </p>
        <h1 className="mt-2 font-display text-3xl font-semibold text-foreground">
          Minha conta
        </h1>
      </header>

      <div className="grid gap-8 lg:grid-cols-[220px_1fr]">
        <aside>
          <AccountSidebarNav />
        </aside>
        <section className="min-w-0">{children}</section>
      </div>
    </div>
  );
}
