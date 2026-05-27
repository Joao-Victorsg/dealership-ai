import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "About",
  description: "About Aurelio Motors.",
};

export default function AboutPage() {
  return (
    <div className="mx-auto w-full max-w-[1280px] px-4 py-16 sm:px-6">
      <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">About</p>
      <h1 className="mt-2 font-display">Built around confidence.</h1>
      <p className="mt-4 text-muted-foreground">
        Aurelio Motors is a digital-first dealership focused on transparent pricing,
        verified vehicle history, and a purchase journey that feels simple from start to finish.
      </p>
    </div>
  );
}
