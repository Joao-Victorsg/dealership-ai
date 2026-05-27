import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { Field, baseInputCls } from "@/components/form-field";
import {
  ALL_CATEGORIES,
  ALL_TYPES,
} from "@/components/car-filters";
import {
  type Car,
  type CarCategory,
  type CarStatus,
  type CarType,
} from "@/lib/mock-data";
import { slugify, upsertCar } from "@/lib/car-store";
import { toast } from "sonner";

interface VehicleFormProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  initial?: Car | null;
}

const STATUS_OPTIONS: { value: CarStatus; label: string }[] = [
  { value: 1, label: "Available" },
  { value: 2, label: "Sold" },
  { value: 3, label: "Unavailable" },
];

interface Errors {
  manufacturer?: string;
  model?: string;
  year?: string;
  value?: string;
  externalColor?: string;
  internalColor?: string;
  kilometers?: string;
}

export function VehicleForm({ open, onOpenChange, initial }: VehicleFormProps) {
  const editing = !!initial;
  const [manufacturer, setManufacturer] = useState("");
  const [model, setModel] = useState("");
  const [year, setYear] = useState<number>(new Date().getFullYear());
  const [value, setValue] = useState<number>(0);
  const [externalColor, setExternalColor] = useState("");
  const [internalColor, setInternalColor] = useState("");
  const [kilometers, setKilometers] = useState<number>(0);
  const [category, setCategory] = useState<CarCategory>("Sedan");
  const [type, setType] = useState<CarType>("Combustion");
  const [status, setStatus] = useState<CarStatus>(1);
  const [isNew, setIsNew] = useState(true);
  const [optionalItems, setOptionalItems] = useState("");
  const [errors, setErrors] = useState<Errors>({});

  useEffect(() => {
    if (!open) return;
    if (initial) {
      setManufacturer(initial.manufacturer);
      setModel(initial.model);
      setYear(initial.year);
      setValue(initial.value);
      setExternalColor(initial.externalColor);
      setInternalColor(initial.internalColor);
      setKilometers(initial.kilometers);
      setCategory(initial.category);
      setType(initial.type);
      setStatus(initial.status);
      setIsNew(initial.isNew);
      setOptionalItems(initial.optionalItems.join(", "));
    } else {
      setManufacturer("");
      setModel("");
      setYear(new Date().getFullYear());
      setValue(0);
      setExternalColor("");
      setInternalColor("");
      setKilometers(0);
      setCategory("Sedan");
      setType("Combustion");
      setStatus(1);
      setIsNew(true);
      setOptionalItems("");
    }
    setErrors({});
  }, [open, initial]);

  function validate(): boolean {
    const next: Errors = {};
    if (!manufacturer.trim()) next.manufacturer = "Manufacturer is required.";
    if (!model.trim()) next.model = "Model is required.";
    if (year < 1990 || year > 2030) next.year = "Year must be between 1990 and 2030.";
    if (value <= 0) next.value = "Price must be greater than zero.";
    if (!externalColor.trim()) next.externalColor = "Required.";
    if (!internalColor.trim()) next.internalColor = "Required.";
    if (kilometers < 0) next.kilometers = "Cannot be negative.";
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!validate()) return;
    const car: Car = {
      id: initial?.id ?? slugify(`${manufacturer}-${model}-${year}-${Date.now().toString(36).slice(-4)}`),
      manufacturer: manufacturer.trim(),
      model: model.trim(),
      year,
      value,
      externalColor: externalColor.trim(),
      internalColor: internalColor.trim(),
      kilometers,
      category,
      type,
      status,
      isNew,
      optionalItems: optionalItems
        .split(",")
        .map((s) => s.trim())
        .filter(Boolean),
      registrationDate: initial?.registrationDate ?? new Date().toISOString().slice(0, 10),
      imageQuery: `${externalColor} ${manufacturer} ${model}`.toLowerCase(),
    };
    upsertCar(car);
    toast.success(editing ? "Vehicle updated." : "Vehicle added to inventory.");
    onOpenChange(false);
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-h-[90vh] max-w-2xl overflow-y-auto">
        <DialogHeader>
          <DialogTitle className="font-display text-xl">
            {editing ? "Edit vehicle" : "Add new vehicle"}
          </DialogTitle>
        </DialogHeader>

        <form onSubmit={submit} noValidate className="space-y-5">
          <div className="grid gap-4 sm:grid-cols-2">
            <Field label="Manufacturer" required error={errors.manufacturer}>
              {({ inputId, describedBy }) => (
                <input
                  id={inputId}
                  className={baseInputCls}
                  value={manufacturer}
                  onChange={(e) => setManufacturer(e.target.value)}
                  aria-describedby={describedBy}
                  aria-invalid={!!errors.manufacturer || undefined}
                />
              )}
            </Field>
            <Field label="Model" required error={errors.model}>
              {({ inputId, describedBy }) => (
                <input
                  id={inputId}
                  className={baseInputCls}
                  value={model}
                  onChange={(e) => setModel(e.target.value)}
                  aria-describedby={describedBy}
                  aria-invalid={!!errors.model || undefined}
                />
              )}
            </Field>

            <Field label="Year" required error={errors.year}>
              {({ inputId }) => (
                <input
                  id={inputId}
                  type="number"
                  className={baseInputCls + " tabular"}
                  value={year}
                  onChange={(e) => setYear(Number(e.target.value) || 0)}
                />
              )}
            </Field>
            <Field label="Price (BRL)" required error={errors.value}>
              {({ inputId }) => (
                <input
                  id={inputId}
                  type="number"
                  step={1000}
                  className={baseInputCls + " tabular"}
                  value={value}
                  onChange={(e) => setValue(Number(e.target.value) || 0)}
                />
              )}
            </Field>

            <Field label="Kilometers" required error={errors.kilometers}>
              {({ inputId }) => (
                <input
                  id={inputId}
                  type="number"
                  className={baseInputCls + " tabular"}
                  value={kilometers}
                  onChange={(e) => setKilometers(Number(e.target.value) || 0)}
                />
              )}
            </Field>

            <Field label="Status">
              {({ inputId }) => (
                <select
                  id={inputId}
                  className={baseInputCls}
                  value={status}
                  onChange={(e) => setStatus(Number(e.target.value) as CarStatus)}
                >
                  {STATUS_OPTIONS.map((s) => (
                    <option key={s.value} value={s.value}>
                      {s.label}
                    </option>
                  ))}
                </select>
              )}
            </Field>

            <Field label="Category">
              {({ inputId }) => (
                <select
                  id={inputId}
                  className={baseInputCls}
                  value={category}
                  onChange={(e) => setCategory(e.target.value as CarCategory)}
                >
                  {ALL_CATEGORIES.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              )}
            </Field>
            <Field label="Powertrain">
              {({ inputId }) => (
                <select
                  id={inputId}
                  className={baseInputCls}
                  value={type}
                  onChange={(e) => setType(e.target.value as CarType)}
                >
                  {ALL_TYPES.map((t) => (
                    <option key={t} value={t}>
                      {t}
                    </option>
                  ))}
                </select>
              )}
            </Field>

            <Field label="Exterior color" required error={errors.externalColor}>
              {({ inputId }) => (
                <input
                  id={inputId}
                  className={baseInputCls}
                  value={externalColor}
                  onChange={(e) => setExternalColor(e.target.value)}
                />
              )}
            </Field>
            <Field label="Interior color" required error={errors.internalColor}>
              {({ inputId }) => (
                <input
                  id={inputId}
                  className={baseInputCls}
                  value={internalColor}
                  onChange={(e) => setInternalColor(e.target.value)}
                />
              )}
            </Field>
          </div>

          <Field label="Optional items" hint="Comma-separated, e.g. Sunroof, GPS, Adaptive cruise">
            {({ inputId }) => (
              <input
                id={inputId}
                className={baseInputCls}
                value={optionalItems}
                onChange={(e) => setOptionalItems(e.target.value)}
              />
            )}
          </Field>

          <fieldset className="rounded-md border border-border bg-surface p-3">
            <legend className="px-1 text-xs font-semibold uppercase tracking-[0.16em] text-muted-foreground">
              Condition
            </legend>
            <div className="flex gap-4">
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="radio"
                  name="condition"
                  checked={isNew}
                  onChange={() => setIsNew(true)}
                />
                New
              </label>
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="radio"
                  name="condition"
                  checked={!isNew}
                  onChange={() => setIsNew(false)}
                />
                Pre-owned
              </label>
            </div>
          </fieldset>

          <DialogFooter>
            <Button type="button" variant="ghost" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit">{editing ? "Save changes" : "Add vehicle"}</Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  );
}
