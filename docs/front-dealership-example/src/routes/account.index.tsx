import { createFileRoute } from "@tanstack/react-router";
import { useMemo, useState } from "react";
import { Info } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Field, MaskedInput, baseInputCls } from "@/components/form-field";
import { formatCEP, formatCPF, formatPhone } from "@/lib/format";
import { resolveCep } from "@/lib/mock-data";
import { updateUser, useAuth } from "@/lib/mock-auth";
import { toast } from "sonner";

export const Route = createFileRoute("/account/")({
  head: () => ({
    meta: [
      { title: "Profile — Aurelio Motors" },
      { name: "description", content: "View and edit your Aurelio Motors profile." },
    ],
  }),
  component: ProfilePage,
});

function ProfilePage() {
  const { user } = useAuth();
  const [firstName, setFirstName] = useState(user?.firstName ?? "");
  const [lastName, setLastName] = useState(user?.lastName ?? "");
  const [phone, setPhone] = useState(user ? formatPhone(user.phone) : "");
  const [cep, setCep] = useState(user ? formatCEP(user.postCode) : "");
  const [streetNumber, setStreetNumber] = useState(user?.streetNumber ?? "");

  const resolved = useMemo(() => resolveCep(cep), [cep]);

  if (!user) return null;

  function save(e: React.FormEvent) {
    e.preventDefault();
    updateUser({
      firstName,
      lastName,
      phone: phone.replace(/\D/g, ""),
      postCode: cep.replace(/\D/g, ""),
      streetNumber,
      resolvedStreet: resolved?.street ?? user!.resolvedStreet,
      resolvedNeighborhood: resolved?.neighborhood ?? user!.resolvedNeighborhood,
      resolvedCity: resolved?.city ?? user!.resolvedCity,
      resolvedState: resolved?.state ?? user!.resolvedState,
    });
    toast.success("Profile updated.");
  }

  return (
    <form onSubmit={save} className="space-y-8">
      <section className="rounded-xl border border-border bg-card p-6">
        <h2 className="font-display text-lg">Personal details</h2>
        <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="First name" required>
            {({ inputId }) => (
              <input id={inputId} value={firstName} onChange={(e) => setFirstName(e.target.value)} className={baseInputCls} required aria-required="true" />
            )}
          </Field>
          <Field label="Last name" required>
            {({ inputId }) => (
              <input id={inputId} value={lastName} onChange={(e) => setLastName(e.target.value)} className={baseInputCls} required aria-required="true" />
            )}
          </Field>
          <Field label="Email">
            {({ inputId }) => (
              <input id={inputId} value={user.email} className={baseInputCls} readOnly disabled />
            )}
          </Field>
          <Field
            label="CPF"
            hint="CPF cannot be changed after registration."
          >
            {({ inputId, describedBy }) => (
              <input id={inputId} value={formatCPF(user.cpf)} className={`${baseInputCls} bg-muted text-muted-foreground`} readOnly aria-readonly="true" aria-describedby={describedBy} />
            )}
          </Field>
          <Field label="Phone" required>
            {({ inputId }) => (
              <MaskedInput id={inputId} value={phone} onValueChange={setPhone} format={formatPhone} required aria-required="true" />
            )}
          </Field>
        </div>
      </section>

      <section className="rounded-xl border border-border bg-card p-6">
        <h2 className="font-display text-lg">Address</h2>
        <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-[1fr_140px]">
          <Field label="CEP" required hint="Format: XXXXX-XXX">
            {({ inputId }) => (
              <MaskedInput id={inputId} value={cep} onValueChange={setCep} format={formatCEP} required aria-required="true" inputMode="numeric" />
            )}
          </Field>
          <Field label="Number" required>
            {({ inputId }) => (
              <input id={inputId} value={streetNumber} onChange={(e) => setStreetNumber(e.target.value)} className={baseInputCls} required aria-required="true" />
            )}
          </Field>
        </div>

        <div className="mt-4 flex items-start gap-2 rounded-md border border-border bg-surface px-4 py-3 text-sm">
          <Info className="mt-0.5 size-4 text-primary" aria-hidden />
          <div>
            <div className="text-xs uppercase tracking-[0.16em] text-muted-foreground">Resolved address</div>
            <div className="mt-1">
              {resolved ? (
                <>{resolved.street}{streetNumber && `, ${streetNumber}`} — {resolved.neighborhood}, {resolved.city} / {resolved.state}</>
              ) : (
                <span className="text-muted-foreground">Enter a valid CEP to confirm your address.</span>
              )}
            </div>
          </div>
        </div>
      </section>

      <div className="flex justify-end">
        <Button type="submit" size="lg">Save changes</Button>
      </div>
    </form>
  );
}
