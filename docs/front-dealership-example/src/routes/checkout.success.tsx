import { createFileRoute, Link } from "@tanstack/react-router";
import { CheckCircle2, Mail } from "lucide-react";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/checkout/success")({
  head: () => ({
    meta: [
      { title: "Purchase complete — Aurelio Motors" },
      { name: "robots", content: "noindex" },
    ],
  }),
  component: SuccessPage,
});

function SuccessPage() {
  return (
    <div className="mx-auto flex w-full max-w-[640px] flex-col items-center px-4 py-20 text-center sm:px-6">
      <div className="grid size-16 place-items-center rounded-full bg-primary/10 reveal reveal-1">
        <CheckCircle2 className="size-8 text-primary" aria-hidden />
      </div>
      <h1 className="mt-6 font-display reveal reveal-2">Your purchase is complete.</h1>
      <p className="mt-4 max-w-md text-muted-foreground reveal reveal-3">
        Thank you for choosing Aurelio Motors. An invoice will be sent to your email
        shortly — there's nothing else you need to do right now.
      </p>

      <div className="mt-6 inline-flex items-center gap-2 rounded-full border border-border bg-surface px-4 py-2 text-xs text-muted-foreground reveal reveal-3">
        <Mail className="size-3.5 text-primary" aria-hidden /> Invoice on its way
      </div>

      <div className="mt-10 flex flex-col gap-2 reveal reveal-4 sm:flex-row">
        <Button asChild size="lg">
          <Link to="/account/purchases">View my purchases</Link>
        </Button>
        <Button asChild size="lg" variant="outline">
          <Link to="/inventory">Browse more cars</Link>
        </Button>
      </div>
    </div>
  );
}
