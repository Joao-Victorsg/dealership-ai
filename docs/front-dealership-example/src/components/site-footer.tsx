import { Link } from "@tanstack/react-router";

export function SiteFooter() {
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

        <FooterColumn title="Browse">
          <Link to="/inventory" className="hover:text-foreground">All inventory</Link>
          <Link to="/inventory" className="hover:text-foreground">New cars</Link>
          <Link to="/inventory" className="hover:text-foreground">Pre-owned</Link>
          <Link to="/inventory" className="hover:text-foreground">Electric</Link>
        </FooterColumn>

        <FooterColumn title="Company">
          <Link to="/about" className="hover:text-foreground">About</Link>
          <a href="mailto:hello@aureliomotors.com" className="hover:text-foreground">Contact</a>
          <a href="#" className="hover:text-foreground">Terms</a>
          <a href="#" className="hover:text-foreground">Privacy</a>
        </FooterColumn>
      </div>
      <div className="border-t border-border">
        <div className="mx-auto flex w-full max-w-[1280px] flex-col items-start justify-between gap-2 px-4 py-6 text-xs text-muted-foreground sm:flex-row sm:items-center sm:px-6">
          <span>© {new Date().getFullYear()} Aurelio Motors. All rights reserved.</span>
          <span>Made in Brazil · Spoken in Portuguese & English</span>
        </div>
      </div>
    </footer>
  );
}

function FooterColumn({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <div>
      <div className="text-xs font-semibold uppercase tracking-[0.18em] text-foreground">{title}</div>
      <div className="mt-4 flex flex-col gap-2 text-sm text-muted-foreground">{children}</div>
    </div>
  );
}
