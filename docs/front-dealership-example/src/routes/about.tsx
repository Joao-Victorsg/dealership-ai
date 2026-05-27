import { createFileRoute } from "@tanstack/react-router";
import { ShieldCheck, Sparkles, Users } from "lucide-react";

export const Route = createFileRoute("/about")({
  head: () => ({
    meta: [
      { title: "About — Aurelio Motors" },
      {
        name: "description",
        content:
          "Aurelio Motors is a modern dealership for buyers who value clarity, transparency, and a refined ownership experience.",
      },
    ],
  }),
  component: AboutPage,
});

function AboutPage() {
  return (
    <div className="mx-auto w-full max-w-[1100px] px-4 py-16 sm:px-6">
      <header className="max-w-2xl">
        <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">About</p>
        <h1 className="mt-2 font-display">A dealership reimagined for the way people actually buy cars.</h1>
        <p className="mt-5 text-lg text-muted-foreground">
          Aurelio Motors is built on a simple idea: buying a car should feel as
          considered as the cars themselves. Clear prices. Full inventory. A
          purchase you can complete from your sofa, with the keys delivered to
          your door.
        </p>
      </header>

      <div className="mt-14 grid gap-5 md:grid-cols-3">
        <Pillar icon={ShieldCheck} title="Trust by default">
          Verified histories, transparent pricing, and a 180-point inspection on every pre-owned car.
        </Pillar>
        <Pillar icon={Sparkles} title="Premium, not precious">
          High-end service without high-pressure sales. Browse for hours; we won't follow you.
        </Pillar>
        <Pillar icon={Users} title="People-first">
          Real humans on the other end when you need them. Plain-language answers, never jargon.
        </Pillar>
      </div>

      <section className="mt-16 grid gap-10 rounded-2xl border border-border bg-surface p-10 md:grid-cols-2">
        <div>
          <h2 className="font-display">Visit our showroom.</h2>
          <p className="mt-3 text-muted-foreground">
            We love a good test drive. Stop by during business hours — coffee's on us.
          </p>
        </div>
        <dl className="space-y-3 text-sm">
          <div className="flex justify-between border-b border-border pb-2">
            <dt className="text-muted-foreground">Address</dt>
            <dd>Av. Paulista, 1578 — São Paulo, SP</dd>
          </div>
          <div className="flex justify-between border-b border-border pb-2">
            <dt className="text-muted-foreground">Hours</dt>
            <dd>Mon–Sat · 9:00 to 19:00</dd>
          </div>
          <div className="flex justify-between">
            <dt className="text-muted-foreground">Email</dt>
            <dd>hello@aureliomotors.com</dd>
          </div>
        </dl>
      </section>
    </div>
  );
}

function Pillar({
  icon: Icon,
  title,
  children,
}: {
  icon: React.ComponentType<{ className?: string }>;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <div className="rounded-xl border border-border bg-card p-6">
      <span className="grid size-10 place-items-center rounded-md bg-primary/10 text-primary">
        <Icon className="size-5" aria-hidden />
      </span>
      <h3 className="mt-4 font-display text-lg">{title}</h3>
      <p className="mt-2 text-sm text-muted-foreground">{children}</p>
    </div>
  );
}
