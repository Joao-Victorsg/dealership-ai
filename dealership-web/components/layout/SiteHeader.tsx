"use client";

import Link from "next/link";
import { ChevronDown, LogOut, Menu, Receipt, Shield, User2 } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { MobileDrawer } from "@/components/layout/MobileDrawer";

interface HeaderUserSummary {
  firstName: string;
  lastName: string;
  email: string;
}

interface SiteHeaderProps {
  isAuthenticated: boolean;
  isAdmin: boolean;
  canAccessAccount: boolean;
  user?: HeaderUserSummary | null;
}

const NAV = [
  { href: "/inventory", label: "Inventory" },
  { href: "/inventory?condition=NEW", label: "New" },
  { href: "/inventory?condition=USED", label: "Used" },
  { href: "/about", label: "About" },
];

export function SiteHeader({
  isAuthenticated,
  isAdmin,
  canAccessAccount,
  user,
}: SiteHeaderProps) {
  const [open, setOpen] = useState(false);
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const userMenuRef = useRef<HTMLDivElement>(null);
  const logoutUrl = `${process.env.NEXT_PUBLIC_BFF_URL}/api/v1/auth/logout`;
  const accountHref = isAdmin
    ? "/admin"
    : canAccessAccount
    ? "/account"
    : "/complete-registration";
  const accountLabel = isAdmin
    ? "Staff"
    : canAccessAccount
    ? "Account"
    : "Complete registration";
  const showCustomerDropdown =
    isAuthenticated && !isAdmin && canAccessAccount && Boolean(user);

  useEffect(() => {
    if (!isUserMenuOpen) return;

    const handleDocumentMouseDown = (event: MouseEvent) => {
      if (!userMenuRef.current?.contains(event.target as Node)) {
        setIsUserMenuOpen(false);
      }
    };

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        setIsUserMenuOpen(false);
      }
    };

    document.addEventListener("mousedown", handleDocumentMouseDown);
    document.addEventListener("keydown", handleEscape);
    return () => {
      document.removeEventListener("mousedown", handleDocumentMouseDown);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [isUserMenuOpen]);

  return (
    <header className="sticky top-0 z-40 border-b border-border bg-background/85 backdrop-blur supports-[backdrop-filter]:bg-background/70">
      <div className="mx-auto flex h-16 w-full max-w-[1280px] items-center justify-between gap-6 px-4 sm:px-6">
        <Link href="/" className="flex items-center gap-2">
          <Logo />
          <span className="font-display text-lg font-semibold tracking-tight">Aurelio</span>
          <span className="hidden text-xs uppercase tracking-[0.18em] text-muted-foreground sm:inline">
            Motors
          </span>
        </Link>

        <nav aria-label="Primary" className="hidden items-center gap-1 md:flex">
          {NAV.map((item) => (
            <Link
              key={item.label}
              href={item.href}
              className="rounded-md px-3 py-2 text-sm text-foreground/80 transition-colors hover:bg-muted hover:text-foreground"
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-2">
          {isAuthenticated ? (
            <>
              {showCustomerDropdown && user ? (
                <div className="relative hidden sm:block" ref={userMenuRef}>
                  <button
                    type="button"
                    onClick={() => setIsUserMenuOpen((currentOpen) => !currentOpen)}
                    className="inline-flex items-center gap-2 rounded-md border border-input bg-background px-3 py-2 text-sm font-medium hover:border-border-strong"
                    aria-haspopup="menu"
                    aria-expanded={isUserMenuOpen}
                    aria-label="Open account menu"
                  >
                    <User2 className="size-4" aria-hidden="true" />
                    <span>{user.firstName}</span>
                    <ChevronDown className="size-4 text-muted-foreground" aria-hidden="true" />
                  </button>

                  {isUserMenuOpen && (
                    <div
                      role="menu"
                      aria-label="Account menu"
                      className="absolute right-0 z-50 mt-2 w-60 overflow-hidden rounded-[var(--radius-lg)] border border-border bg-card shadow-xl"
                    >
                      <div className="border-b border-border px-4 py-3">
                        <p className="truncate text-sm font-semibold text-foreground">
                          {user.firstName} {user.lastName}
                        </p>
                        <p className="truncate text-xs text-muted-foreground">
                          {user.email}
                        </p>
                      </div>
                      <div className="p-1.5">
                        <Link
                          href="/account"
                          role="menuitem"
                          onClick={() => setIsUserMenuOpen(false)}
                          className="flex items-center gap-2 rounded-md px-3 py-2 text-sm text-foreground/90 hover:bg-muted hover:text-foreground"
                        >
                          <User2 className="size-4" aria-hidden="true" />
                          Profile
                        </Link>
                        <Link
                          href="/account/purchases"
                          role="menuitem"
                          onClick={() => setIsUserMenuOpen(false)}
                          className="flex items-center gap-2 rounded-md px-3 py-2 text-sm text-foreground/90 hover:bg-muted hover:text-foreground"
                        >
                          <Receipt className="size-4" aria-hidden="true" />
                          My purchases
                        </Link>
                        <a
                          href={logoutUrl}
                          role="menuitem"
                          onClick={() => setIsUserMenuOpen(false)}
                          className="mt-1 flex items-center gap-2 rounded-md px-3 py-2 text-sm text-foreground/90 hover:bg-muted hover:text-foreground"
                        >
                          <LogOut className="size-4" aria-hidden="true" />
                          Sign out
                        </a>
                      </div>
                    </div>
                  )}
                </div>
              ) : (
                <Link
                  href={accountHref}
                  className="hidden items-center gap-2 rounded-md border border-input bg-background px-3 py-2 text-sm font-medium hover:border-border-strong sm:inline-flex"
                >
                  {isAdmin ? <Shield className="size-4" aria-hidden="true" /> : <User2 className="size-4" aria-hidden="true" />}
                  <span>{accountLabel}</span>
                </Link>
              )}

              {!showCustomerDropdown && (
                <a
                  href={logoutUrl}
                  className="hidden rounded-md px-3 py-2 text-sm font-medium text-foreground/80 transition-colors hover:bg-muted hover:text-foreground disabled:opacity-60 sm:inline-flex"
                >
                  Sign out
                </a>
              )}
            </>
          ) : (
            <>
              <a
                href={`${process.env.NEXT_PUBLIC_BFF_URL}/oauth2/authorization/keycloak`}
                className="hidden rounded-md px-3 py-2 text-sm font-medium text-foreground/80 transition-colors hover:bg-muted hover:text-foreground sm:inline-flex"
              >
                Sign in
              </a>
              <a
                href={`${process.env.NEXT_PUBLIC_BFF_URL}/oauth2/authorization/keycloak-register`}
                className="hidden rounded-md bg-primary px-3 py-2 text-sm font-medium text-primary-foreground transition-colors hover:bg-primary/90 sm:inline-flex"
              >
                Create account
              </a>
            </>
          )}

          <button
            type="button"
            onClick={() => setOpen(true)}
            className="inline-flex h-9 w-9 items-center justify-center rounded-md text-muted-foreground hover:bg-muted hover:text-foreground md:hidden"
            aria-label="Open menu"
          >
            <Menu className="size-5" />
          </button>
        </div>
      </div>

      <MobileDrawer
        isOpen={open}
        onClose={() => setOpen(false)}
        isAuthenticated={isAuthenticated}
        isAdmin={isAdmin}
        canAccessAccount={canAccessAccount}
        logoutUrl={logoutUrl}
      />
    </header>
  );
}

function Logo() {
  return (
    <svg
      width="28"
      height="28"
      viewBox="0 0 28 28"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <rect x="1" y="1" width="26" height="26" rx="6" fill="var(--color-primary)" />
      <path d="M9 19 L14 7 L19 19 M11 15 H17" stroke="var(--color-primary-foreground)" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" fill="none" />
    </svg>
  );
}
