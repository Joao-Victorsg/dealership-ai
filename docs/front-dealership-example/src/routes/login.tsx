import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { Shield, User } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Field, baseInputCls } from "@/components/form-field";
import { login, type Role } from "@/lib/mock-auth";
import { cn } from "@/lib/utils";
import { toast } from "sonner";

export const Route = createFileRoute("/login")({
  validateSearch: (search: Record<string, unknown>) => ({
    redirect: typeof search.redirect === "string" ? search.redirect : "/",
  }),
  head: () => ({
    meta: [
      { title: "Sign in — Aurelio Motors" },
      { name: "description", content: "Sign in to your Aurelio Motors account." },
    ],
  }),
  component: LoginPage,
});

function LoginPage() {
  const navigate = useNavigate();
  const { redirect } = Route.useSearch();
  const [role, setRole] = useState<Role>("customer");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [errors, setErrors] = useState<{ email?: string; password?: string }>({});

  function submit(e: React.FormEvent) {
    e.preventDefault();
    const next: typeof errors = {};
    if (!/^\S+@\S+\.\S+$/.test(email)) next.email = "Enter a valid email address.";
    if (password.length < 6) next.password = "Password must be at least 6 characters.";
    setErrors(next);
    if (Object.keys(next).length) return;
    login(email, role);
    toast.success(role === "admin" ? "Signed in as staff." : "Welcome back.");
    if (role === "admin") {
      navigate({ to: "/admin" });
      return;
    }
    navigate({ to: redirect.startsWith("/") ? (redirect as "/") : "/" });
  }

  function fillDemo() {
    if (role === "admin") {
      setEmail("admin@aurelio.com");
      setPassword("aurelio2025");
    } else {
      setEmail("ana.silva@example.com");
      setPassword("password");
    }
  }

  return (
    <AuthShell
      title="Welcome back."
      subtitle="Sign in to continue browsing, save vehicles, and complete a purchase."
      footer={
        <>
          New to Aurelio?{" "}
          <Link to="/register" className="font-medium text-primary hover:underline">
            Create an account
          </Link>
        </>
      }
    >
      <div
        role="tablist"
        aria-label="Account type"
        className="mb-6 grid grid-cols-2 gap-1.5 rounded-lg border border-border bg-surface p-1"
      >
        <RoleTab
          active={role === "customer"}
          onClick={() => setRole("customer")}
          icon={<User className="size-4" aria-hidden />}
          label="Customer"
        />
        <RoleTab
          active={role === "admin"}
          onClick={() => setRole("admin")}
          icon={<Shield className="size-4" aria-hidden />}
          label="Staff / Admin"
        />
      </div>

      {role === "admin" && (
        <div className="mb-5 rounded-md border border-primary/30 bg-primary/5 px-3 py-2.5 text-xs text-foreground/80">
          <span className="font-medium text-primary">Staff sign-in.</span> You'll
          land on the inventory management dashboard.{" "}
          <button
            type="button"
            onClick={fillDemo}
            className="font-medium text-primary underline-offset-2 hover:underline"
          >
            Use demo credentials
          </button>
          .
        </div>
      )}

      <form onSubmit={submit} noValidate className="space-y-5">
        <Field label="Email" required error={errors.email}>
          {({ inputId, describedBy }) => (
            <input
              id={inputId}
              type="email"
              autoComplete="email"
              required
              aria-required="true"
              aria-invalid={!!errors.email || undefined}
              aria-describedby={describedBy}
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className={baseInputCls}
            />
          )}
        </Field>
        <Field label="Password" required error={errors.password}>
          {({ inputId, describedBy }) => (
            <input
              id={inputId}
              type="password"
              autoComplete="current-password"
              required
              aria-required="true"
              aria-invalid={!!errors.password || undefined}
              aria-describedby={describedBy}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              className={baseInputCls}
            />
          )}
        </Field>
        <div className="flex items-center justify-between text-sm">
          <label className="flex items-center gap-2 text-muted-foreground">
            <input type="checkbox" className="size-4 rounded border-input text-primary" /> Remember me
          </label>
          <a href="#" className="text-primary hover:underline">
            Forgot password?
          </a>
        </div>
        <Button type="submit" size="lg" className="w-full">
          {role === "admin" ? "Sign in as staff" : "Sign in"}
        </Button>
      </form>
    </AuthShell>
  );
}

function RoleTab({
  active,
  onClick,
  icon,
  label,
}: {
  active: boolean;
  onClick: () => void;
  icon: React.ReactNode;
  label: string;
}) {
  return (
    <button
      type="button"
      role="tab"
      aria-selected={active}
      onClick={onClick}
      className={cn(
        "inline-flex h-10 items-center justify-center gap-2 rounded-md text-sm font-medium transition-colors",
        active
          ? "bg-background text-foreground shadow-sm ring-1 ring-border"
          : "text-muted-foreground hover:text-foreground",
      )}
    >
      {icon}
      {label}
    </button>
  );
}

export function AuthShell({
  title,
  subtitle,
  children,
  footer,
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
}) {
  return (
    <div className="mx-auto grid w-full max-w-[1280px] gap-10 px-4 py-16 sm:px-6 lg:grid-cols-[1fr_1fr]">
      <div className="hidden flex-col justify-between rounded-2xl border border-border bg-surface p-10 lg:flex grain">
        <div>
          <span className="inline-flex items-center gap-2 rounded-full border border-border bg-card px-3 py-1 text-xs uppercase tracking-[0.18em] text-muted-foreground">
            <span className="size-1.5 rounded-full bg-primary" /> Aurelio Motors
          </span>
          <h2 className="mt-6 font-display text-3xl">A dealership built around you.</h2>
          <p className="mt-4 max-w-md text-sm text-muted-foreground">
            Browse without limits. Save what you love. Complete the purchase
            online — your invoice is delivered straight to your inbox.
          </p>
        </div>
        <ul className="grid gap-3 text-sm text-muted-foreground">
          <li>· Verified vehicle histories</li>
          <li>· Transparent pricing — no surprise fees</li>
          <li>· White-glove delivery available</li>
        </ul>
      </div>

      <div className="mx-auto w-full max-w-md self-center reveal reveal-1">
        <h1 className="font-display">{title}</h1>
        {subtitle && <p className="mt-2 text-muted-foreground">{subtitle}</p>}
        <div className="mt-8">{children}</div>
        {footer && <p className="mt-6 text-sm text-muted-foreground">{footer}</p>}
      </div>
    </div>
  );
}
