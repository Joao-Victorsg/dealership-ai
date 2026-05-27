"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { Receipt, User2 } from "lucide-react";

const ACCOUNT_TABS = [
  { href: "/account", label: "Perfil", icon: User2 },
  { href: "/account/purchases", label: "Minhas compras", icon: Receipt },
];

export function AccountSidebarNav() {
  const pathname = usePathname();

  return (
    <nav aria-label="Conta" className="flex flex-row gap-1 overflow-x-auto lg:flex-col">
      {ACCOUNT_TABS.map(({ href, label, icon: Icon }) => {
        const isActive = pathname === href;

        return (
          <Link
            key={href}
            href={href}
            className={
              isActive
                ? "inline-flex items-center gap-2 rounded-md bg-primary px-3 py-2 text-sm text-primary-foreground"
                : "inline-flex items-center gap-2 rounded-md px-3 py-2 text-sm text-foreground/80 transition-colors hover:bg-muted hover:text-foreground"
            }
            aria-current={isActive ? "page" : undefined}
          >
            <Icon className="size-4" aria-hidden="true" />
            {label}
          </Link>
        );
      })}
    </nav>
  );
}
