import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { RotateCcw, Save } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogTrigger,
} from "@/components/ui/alert-dialog";
import { Field, baseInputCls } from "@/components/form-field";
import { resetStore } from "@/lib/car-store";
import { toast } from "sonner";

export const Route = createFileRoute("/admin/settings")({
  head: () => ({
    meta: [{ title: "Settings · Admin — Aurelio Motors" }],
  }),
  component: AdminSettingsPage,
});

interface DealershipSettings {
  name: string;
  email: string;
  phone: string;
  address: string;
  taxRate: number; // percent
  featuredCount: number;
  enableTestDrive: boolean;
  enableTradeIn: boolean;
}

const DEFAULTS: DealershipSettings = {
  name: "Aurelio Motors",
  email: "contact@aurelio.com",
  phone: "(11) 4002-8922",
  address: "Av. Paulista, 1578 — Bela Vista, São Paulo/SP",
  taxRate: 8,
  featuredCount: 6,
  enableTestDrive: true,
  enableTradeIn: false,
};

function AdminSettingsPage() {
  const [settings, setSettings] = useState<DealershipSettings>(DEFAULTS);
  const [saved, setSaved] = useState(false);

  function update<K extends keyof DealershipSettings>(key: K, value: DealershipSettings[K]) {
    setSettings((s) => ({ ...s, [key]: value }));
    setSaved(false);
  }

  function save(e: React.FormEvent) {
    e.preventDefault();
    toast.success("Settings saved.");
    setSaved(true);
  }

  function performReset() {
    resetStore();
    toast.success("Inventory and sales restored to factory data.");
  }

  return (
    <form onSubmit={save} className="space-y-8">
      <Section
        title="Dealership profile"
        description="Public information shown on the storefront, invoices, and the about page."
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Dealership name">
            {({ inputId }) => (
              <input
                id={inputId}
                className={baseInputCls}
                value={settings.name}
                onChange={(e) => update("name", e.target.value)}
              />
            )}
          </Field>
          <Field label="Contact email">
            {({ inputId }) => (
              <input
                id={inputId}
                type="email"
                className={baseInputCls}
                value={settings.email}
                onChange={(e) => update("email", e.target.value)}
              />
            )}
          </Field>
          <Field label="Phone">
            {({ inputId }) => (
              <input
                id={inputId}
                className={baseInputCls}
                value={settings.phone}
                onChange={(e) => update("phone", e.target.value)}
              />
            )}
          </Field>
          <Field label="Address">
            {({ inputId }) => (
              <input
                id={inputId}
                className={baseInputCls}
                value={settings.address}
                onChange={(e) => update("address", e.target.value)}
              />
            )}
          </Field>
        </div>
      </Section>

      <Section
        title="Storefront"
        description="Control how the inventory is presented to shoppers."
      >
        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            label="Transaction tax (%)"
            hint="Applied to the car value at checkout."
          >
            {({ inputId }) => (
              <input
                id={inputId}
                type="number"
                step={0.5}
                min={0}
                max={50}
                className={baseInputCls + " tabular"}
                value={settings.taxRate}
                onChange={(e) => update("taxRate", Number(e.target.value) || 0)}
              />
            )}
          </Field>
          <Field
            label="Featured vehicles on home"
            hint="How many cars to highlight on the homepage."
          >
            {({ inputId }) => (
              <input
                id={inputId}
                type="number"
                min={3}
                max={12}
                className={baseInputCls + " tabular"}
                value={settings.featuredCount}
                onChange={(e) => update("featuredCount", Number(e.target.value) || 0)}
              />
            )}
          </Field>
        </div>

        <div className="mt-6 grid gap-3">
          <Toggle
            label="Allow test-drive scheduling"
            description="Show the 'Schedule a test drive' CTA on car detail pages."
            checked={settings.enableTestDrive}
            onChange={(v) => update("enableTestDrive", v)}
          />
          <Toggle
            label="Accept trade-ins at checkout"
            description="Customers can submit their current vehicle as part of the purchase."
            checked={settings.enableTradeIn}
            onChange={(v) => update("enableTradeIn", v)}
          />
        </div>
      </Section>

      <Section
        title="Data"
        description="Manage local mock data — useful when demoing the store."
      >
        <div className="flex flex-wrap items-center gap-3">
          <AlertDialog>
            <AlertDialogTrigger asChild>
              <Button type="button" variant="outline">
                <RotateCcw className="size-4" /> Reset inventory & sales
              </Button>
            </AlertDialogTrigger>
            <AlertDialogContent>
              <AlertDialogHeader>
                <AlertDialogTitle>Restore factory data?</AlertDialogTitle>
                <AlertDialogDescription>
                  This clears any vehicle you've added or removed and re-seeds the
                  sample inventory and sales history. Your account stays the same.
                </AlertDialogDescription>
              </AlertDialogHeader>
              <AlertDialogFooter>
                <AlertDialogCancel>Cancel</AlertDialogCancel>
                <AlertDialogAction onClick={performReset}>Reset data</AlertDialogAction>
              </AlertDialogFooter>
            </AlertDialogContent>
          </AlertDialog>
          <p className="text-xs text-muted-foreground">
            Data is stored locally in your browser for demo purposes.
          </p>
        </div>
      </Section>

      <div className="sticky bottom-4 flex items-center justify-between gap-3 rounded-xl border border-border bg-card/95 p-4 backdrop-blur">
        <p className="text-xs text-muted-foreground">
          {saved ? "All changes saved." : "You have unsaved changes."}
        </p>
        <Button type="submit">
          <Save className="size-4" /> Save settings
        </Button>
      </div>
    </form>
  );
}

function Section({
  title,
  description,
  children,
}: {
  title: string;
  description?: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-xl border border-border bg-card p-6">
      <h2 className="font-display text-lg">{title}</h2>
      {description && <p className="mt-1 text-sm text-muted-foreground">{description}</p>}
      <div className="mt-5">{children}</div>
    </section>
  );
}

function Toggle({
  label,
  description,
  checked,
  onChange,
}: {
  label: string;
  description?: string;
  checked: boolean;
  onChange: (v: boolean) => void;
}) {
  return (
    <label className="flex cursor-pointer items-start justify-between gap-4 rounded-md border border-border bg-surface p-4">
      <span>
        <span className="block text-sm font-medium">{label}</span>
        {description && (
          <span className="mt-0.5 block text-xs text-muted-foreground">{description}</span>
        )}
      </span>
      <input
        type="checkbox"
        className="mt-1 size-5 rounded border-input text-primary"
        checked={checked}
        onChange={(e) => onChange(e.target.checked)}
      />
    </label>
  );
}
