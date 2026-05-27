import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Field, MaskedInput, baseInputCls } from "@/components/form-field";
import { formatCEP, formatCPF, formatPhone } from "@/lib/format";
import { resolveCep } from "@/lib/mock-data";
import { login, updateUser } from "@/lib/mock-auth";
import { toast } from "sonner";
import { AuthShell } from "./login";
import { CheckCircle2 } from "lucide-react";

export const Route = createFileRoute("/register")({
  head: () => ({
    meta: [
      { title: "Create account — Aurelio Motors" },
      { name: "description", content: "Create your Aurelio Motors account in two short steps." },
    ],
  }),
  component: RegisterPage,
});

function RegisterPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState<1 | 2>(1);

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [cpf, setCpf] = useState("");
  const [phone, setPhone] = useState("");
  const [cep, setCep] = useState("");
  const [streetNumber, setStreetNumber] = useState("");
  const [errors, setErrors] = useState<Record<string, string>>({});

  const resolved = useMemo(() => resolveCep(cep), [cep]);

  function submitStep1(e: React.FormEvent) {
    e.preventDefault();
    const next: Record<string, string> = {};
    if (!/^\S+@\S+\.\S+$/.test(email)) next.email = "Enter a valid email.";
    if (password.length < 8) next.password = "Use at least 8 characters.";
    setErrors(next);
    if (!Object.keys(next).length) setStep(2);
  }

  function submitStep2(e: React.FormEvent) {
    e.preventDefault();
    const next: Record<string, string> = {};
    if (!firstName.trim()) next.firstName = "Required.";
    if (!lastName.trim()) next.lastName = "Required.";
    if (cpf.replace(/\D/g, "").length !== 11) next.cpf = "Enter your 11-digit CPF.";
    if (phone.replace(/\D/g, "").length < 10) next.phone = "Enter a valid phone number.";
    if (cep.replace(/\D/g, "").length !== 8) next.cep = "Enter a valid CEP.";
    if (!streetNumber.trim()) next.streetNumber = "Required.";
    setErrors(next);
    if (Object.keys(next).length) return;

    login(email);
    updateUser({
      firstName, lastName,
      cpf: cpf.replace(/\D/g, ""),
      phone: phone.replace(/\D/g, ""),
      postCode: cep.replace(/\D/g, ""),
      streetNumber,
      resolvedStreet: resolved?.street ?? "",
      resolvedNeighborhood: resolved?.neighborhood ?? "",
      resolvedCity: resolved?.city ?? "",
      resolvedState: resolved?.state ?? "",
    });
    toast.success("Account created.");
    navigate({ to: "/account" });
  }

  return (
    <AuthShell
      title={step === 1 ? "Create your account." : "Tell us a bit about you."}
      subtitle={
        step === 1
          ? "Two short steps. You can change everything except your CPF later."
          : "We'll use this for your invoice and delivery details."
      }
      footer={
        step === 1 ? (
          <>Already have an account? <Link to="/login" search={{ redirect: "/" }} className="font-medium text-primary hover:underline">Sign in</Link></>
        ) : null
      }
    >
      <Stepper step={step} />

      {step === 1 ? (
        <form onSubmit={submitStep1} noValidate className="mt-6 space-y-5">
          <Field label="Email" required error={errors.email}>
            {({ inputId, describedBy }) => (
              <input
                id={inputId} type="email" autoComplete="email" required
                aria-required="true" aria-invalid={!!errors.email || undefined}
                aria-describedby={describedBy}
                value={email} onChange={(e) => setEmail(e.target.value)}
                className={baseInputCls}
              />
            )}
          </Field>
          <Field label="Password" required hint="At least 8 characters." error={errors.password}>
            {({ inputId, describedBy }) => (
              <input
                id={inputId} type="password" autoComplete="new-password" required
                aria-required="true" aria-invalid={!!errors.password || undefined}
                aria-describedby={describedBy}
                value={password} onChange={(e) => setPassword(e.target.value)}
                className={baseInputCls}
              />
            )}
          </Field>
          <Button type="submit" size="lg" className="w-full">Continue</Button>
        </form>
      ) : (
        <form onSubmit={submitStep2} noValidate className="mt-6 space-y-5">
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="First name" required error={errors.firstName}>
              {({ inputId, describedBy }) => (
                <input id={inputId} value={firstName} onChange={(e) => setFirstName(e.target.value)}
                  required aria-required="true" aria-invalid={!!errors.firstName || undefined}
                  aria-describedby={describedBy} autoComplete="given-name" className={baseInputCls} />
              )}
            </Field>
            <Field label="Last name" required error={errors.lastName}>
              {({ inputId, describedBy }) => (
                <input id={inputId} value={lastName} onChange={(e) => setLastName(e.target.value)}
                  required aria-required="true" aria-invalid={!!errors.lastName || undefined}
                  aria-describedby={describedBy} autoComplete="family-name" className={baseInputCls} />
              )}
            </Field>
          </div>

          <Field label="CPF" required hint="Format: XXX.XXX.XXX-XX" error={errors.cpf}>
            {({ inputId, describedBy }) => (
              <MaskedInput
                id={inputId} value={cpf} onValueChange={setCpf} format={formatCPF}
                placeholder="000.000.000-00" inputMode="numeric"
                describedBy={describedBy} invalid={!!errors.cpf} required
                aria-required="true" autoComplete="off"
              />
            )}
          </Field>

          <Field label="Phone" required hint="Format: (11) 98765-4321" error={errors.phone}>
            {({ inputId, describedBy }) => (
              <MaskedInput
                id={inputId} value={phone} onValueChange={setPhone} format={formatPhone}
                placeholder="(11) 98765-4321" inputMode="tel"
                describedBy={describedBy} invalid={!!errors.phone} required
                aria-required="true" autoComplete="tel"
              />
            )}
          </Field>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-[1fr_140px]">
            <Field label="CEP" required hint="Format: XXXXX-XXX" error={errors.cep}>
              {({ inputId, describedBy }) => (
                <MaskedInput
                  id={inputId} value={cep} onValueChange={setCep} format={formatCEP}
                  placeholder="01310-100" inputMode="numeric"
                  describedBy={describedBy} invalid={!!errors.cep} required
                  aria-required="true" autoComplete="postal-code"
                />
              )}
            </Field>
            <Field label="Number" required error={errors.streetNumber}>
              {({ inputId, describedBy }) => (
                <input id={inputId} value={streetNumber} onChange={(e) => setStreetNumber(e.target.value)}
                  required aria-required="true" aria-invalid={!!errors.streetNumber || undefined}
                  aria-describedby={describedBy} className={baseInputCls} placeholder="1578" />
              )}
            </Field>
          </div>

          {resolved && (
            <div className="rounded-md border border-border bg-surface px-4 py-3 text-sm">
              <div className="text-xs uppercase tracking-[0.16em] text-muted-foreground">Address</div>
              <div className="mt-1">
                {resolved.street}{streetNumber && `, ${streetNumber}`} — {resolved.neighborhood},{" "}
                {resolved.city} / {resolved.state}
              </div>
            </div>
          )}

          <div className="flex flex-col-reverse gap-2 sm:flex-row">
            <Button type="button" variant="outline" size="lg" className="sm:flex-1" onClick={() => setStep(1)}>
              Back
            </Button>
            <Button type="submit" size="lg" className="sm:flex-1">
              Create account
            </Button>
          </div>
        </form>
      )}
    </AuthShell>
  );
}

function Stepper({ step }: { step: 1 | 2 }) {
  return (
    <ol className="flex items-center gap-3 text-xs" aria-label="Progress">
      {[1, 2].map((s) => {
        const done = step > s;
        const active = step === s;
        return (
          <li key={s} className="flex items-center gap-2">
            <span
              className={`grid size-6 place-items-center rounded-full border text-[11px] font-semibold ${
                done
                  ? "border-primary bg-primary text-primary-foreground"
                  : active
                    ? "border-primary text-primary"
                    : "border-border text-muted-foreground"
              }`}
              aria-current={active ? "step" : undefined}
            >
              {done ? <CheckCircle2 className="size-4" aria-hidden /> : s}
            </span>
            <span className={active ? "text-foreground" : "text-muted-foreground"}>
              {s === 1 ? "Account" : "Profile"}
            </span>
            {s === 1 && <span className="mx-1 h-px w-6 bg-border" aria-hidden />}
          </li>
        );
      })}
    </ol>
  );
}
