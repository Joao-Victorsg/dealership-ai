// components/layout/SiteFooter.tsx

import Link from "next/link";

export function SiteFooter() {
  const year = new Date().getFullYear();

  return (
    <footer className="border-t border-border bg-surface">
      <div className="mx-auto grid w-full max-w-[1280px] gap-10 px-4 py-14 sm:px-6 md:grid-cols-4">
        <div className="md:col-span-2">
          <div className="font-display text-xl font-semibold">Aurelio Motors</div>
          <p className="mt-3 text-sm text-muted-foreground">
            A modern dealership for buyers who value clarity. New and pre-owned vehicles,
            transparent pricing, and a digital purchase experience built on trust.
          </p>
          <p className="mt-6 text-xs text-muted-foreground">
            Av. Paulista, 1578 — São Paulo, SP · CNPJ 00.000.000/0001-00
          </p>
        </div>

        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-foreground">Browse</p>
          <div className="mt-4 flex flex-col gap-2 text-sm text-muted-foreground">
            <Link href="/inventory" className="hover:text-foreground">All inventory</Link>
            <Link href="/inventory?condition=NEW" className="hover:text-foreground">New cars</Link>
            <Link href="/inventory?condition=USED" className="hover:text-foreground">Pre-owned</Link>
            <Link href="/inventory?type=ELECTRIC" className="hover:text-foreground">Electric</Link>
          </div>
        </div>

        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-foreground">Company</p>
          <div className="mt-4 flex flex-col gap-2 text-sm text-muted-foreground">
            <Link href="/about" className="hover:text-foreground">About</Link>
            <a href="mailto:hello@aureliomotors.com" className="hover:text-foreground">Contact</a>
            <a href="#" className="hover:text-foreground">Terms</a>
            <a href="#" className="hover:text-foreground">Privacy</a>
          </div>
        </div>
      </div>

      <div className="border-t border-border">
        <div className="mx-auto flex w-full max-w-[1280px] flex-col items-start justify-between gap-2 px-4 py-6 text-xs text-muted-foreground sm:flex-row sm:items-center sm:px-6">
          <span>&copy; {year} Aurelio Motors. All rights reserved.</span>
          <span>Made in Brazil · Spoken in Portuguese & English</span>
        </div>
      </div>
    </footer>
  );
}
