"use client";
import { useEffect } from "react";
import Link from "next/link";

interface MobileDrawerProps {
  isOpen: boolean;
  onClose: () => void;
  isAuthenticated: boolean;
  isAdmin: boolean;
  canAccessAccount: boolean;
  logoutUrl: string;
}

export function MobileDrawer({
  isOpen,
  onClose,
  isAuthenticated,
  isAdmin,
  canAccessAccount,
  logoutUrl,
}: MobileDrawerProps) {
  useEffect(() => {
    if (!isOpen) return;
    const handleKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    document.addEventListener("keydown", handleKey);
    return () => document.removeEventListener("keydown", handleKey);
  }, [isOpen, onClose]);

  useEffect(() => {
    document.body.style.overflow = isOpen ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [isOpen]);

  if (!isOpen) return null;

  return (
    <>
      <div
        className="fixed inset-0 z-40 bg-foreground/20 backdrop-blur-sm"
        aria-hidden="true"
        onClick={onClose}
      />

      <div
        role="dialog"
        aria-modal="true"
        aria-label="Navigation menu"
        className="fixed inset-y-0 right-0 z-50 flex w-[300px] flex-col border-l border-border bg-card shadow-xl"
      >
        <div className="flex items-center justify-between border-b border-border px-5 py-4">
          <p className="font-display text-lg font-semibold">Menu</p>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar menu"
            className="flex h-9 w-9 items-center justify-center rounded-[var(--radius-sm)] text-muted-foreground hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <svg
              aria-hidden="true"
              className="h-5 w-5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth={2}
              strokeLinecap="round"
              strokeLinejoin="round"
            >
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>

        <nav aria-label="Mobile navigation" className="flex flex-1 flex-col gap-1 px-4 py-5">
          <Link
            href="/"
            onClick={onClose}
            className="rounded-md px-3 py-3 text-base hover:bg-muted"
          >
            Home
          </Link>
          <Link
            href="/inventory"
            onClick={onClose}
            className="rounded-md px-3 py-3 text-base hover:bg-muted"
          >
            Inventory
          </Link>
          <Link
            href="/inventory?condition=NEW"
            onClick={onClose}
            className="rounded-md px-3 py-3 text-base hover:bg-muted"
          >
            New
          </Link>
          <Link
            href="/inventory?condition=USED"
            onClick={onClose}
            className="rounded-md px-3 py-3 text-base hover:bg-muted"
          >
            Used
          </Link>
          <Link
            href="/about"
            onClick={onClose}
            className="rounded-md px-3 py-3 text-base hover:bg-muted"
          >
            About
          </Link>

          {isAdmin && (
            <Link
              href="/admin"
              onClick={onClose}
              className="rounded-md px-3 py-3 font-medium text-primary hover:bg-muted"
            >
              Inventory management
            </Link>
          )}

          {isAuthenticated && (
            <>
              {canAccessAccount ? (
                <>
                  <Link href="/account" onClick={onClose} className="rounded-md px-3 py-3 text-base hover:bg-muted">
                    Profile
                  </Link>
                  <Link href="/account/purchases" onClick={onClose} className="rounded-md px-3 py-3 text-base hover:bg-muted">
                    My purchases
                  </Link>
                </>
              ) : (
                <Link href="/complete-registration" onClick={onClose} className="rounded-md px-3 py-3 text-base hover:bg-muted">
                  Complete registration
                </Link>
              )}
            </>
          )}
        </nav>

        <div className="border-t border-border p-4">
          {isAuthenticated ? (
            <a
              href={logoutUrl}
              onClick={onClose}
              className="flex w-full items-center justify-center rounded-[var(--radius)] bg-muted px-4 py-2 text-sm font-medium text-foreground hover:bg-muted/80 disabled:opacity-60"
            >
              Sign out
            </a>
          ) : (
            <div className="flex flex-col gap-2">
              <a
                href={`${process.env.NEXT_PUBLIC_BFF_URL}/oauth2/authorization/keycloak`}
                className="flex items-center justify-center rounded-[var(--radius)] border border-border px-4 py-2 text-sm font-medium text-foreground hover:bg-muted"
              >
                Sign in
              </a>
              <a
                href={`${process.env.NEXT_PUBLIC_BFF_URL}/oauth2/authorization/keycloak-register`}
                className="flex items-center justify-center rounded-[var(--radius)] bg-primary px-4 py-2 text-sm font-medium text-primary-foreground hover:bg-primary/90"
              >
                Create account
              </a>
            </div>
          )}
        </div>
      </div>
    </>
  );
}
