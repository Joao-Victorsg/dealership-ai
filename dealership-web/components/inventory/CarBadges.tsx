import {
  BatteryCharging,
  Fuel,
  Mountain,
  Car as CarIcon,
  Zap,
  Truck,
  CircleDot,
  Sparkles,
  History,
  CheckCircle2,
  Lock,
  Clock,
} from "lucide-react";
import type { CarCategory, CarStatus, CarType } from "@/lib/api/types";

const baseBadge =
  "inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-xs font-medium tabular";

export function ConditionBadge({ isNew }: { isNew: boolean }) {
  return (
    <span
      className={
        isNew
          ? `${baseBadge} border-primary/30 bg-primary/5 text-primary`
          : `${baseBadge} border-border bg-muted text-foreground/80`
      }
    >
      {isNew ? <Sparkles className="size-3.5" aria-hidden /> : <History className="size-3.5" aria-hidden />}
      {isNew ? "New" : "Pre-owned"}
    </span>
  );
}

export function TypeBadge({ type }: { type: CarType }) {
  const normalizedType = normalizeType(type);
  const isElectric = normalizedType === "ELECTRIC";
  return (
    <span
      className={
        isElectric
          ? `${baseBadge} border-emerald-700/20 bg-emerald-700/5 text-emerald-800 dark:text-emerald-300`
          : `${baseBadge} border-amber-700/20 bg-amber-700/5 text-amber-900 dark:text-amber-200`
      }
    >
      {isElectric ? <Zap className="size-3.5" aria-hidden /> : <Fuel className="size-3.5" aria-hidden />}
      {isElectric ? "Electric" : "Combustion"}
    </span>
  );
}

const CATEGORY_ICON: Record<string, React.ComponentType<{ className?: string }>> = {
  SUV: Mountain,
  SEDAN: CarIcon,
  COUPE: BatteryCharging,
  HATCH: CircleDot,
  PICKUP: Truck,
  CONVERTIBLE: CarIcon,
  MINIVAN: CarIcon,
  OTHER: CarIcon,
};

const CATEGORY_LABELS: Record<string, string> = {
  SUV: "SUV",
  SEDAN: "Sedan",
  COUPE: "Sport",
  HATCH: "Hatch",
  PICKUP: "Pick-up",
  CONVERTIBLE: "Convertible",
  MINIVAN: "Minivan",
  OTHER: "Other",
};

export function CategoryBadge({ category }: { category: CarCategory }) {
  const normalizedCategory = normalizeCategory(category);
  const Icon = CATEGORY_ICON[normalizedCategory] ?? CarIcon;
  return (
    <span className={`${baseBadge} border-border bg-surface text-foreground/80`}>
      <Icon className="size-3.5" aria-hidden />
      {CATEGORY_LABELS[normalizedCategory] ?? normalizedCategory}
    </span>
  );
}

export function StatusBadge({ status }: { status: CarStatus }) {
  if (status === "AVAILABLE") {
    return (
      <span className={`${baseBadge} border-success/30 bg-success/10 text-success`}>
        <CheckCircle2 className="size-3.5" aria-hidden /> Available
      </span>
    );
  }
  if (status === "SOLD") {
    return (
      <span className={`${baseBadge} border-border bg-muted text-muted-foreground`}>
        <Lock className="size-3.5" aria-hidden /> Sold
      </span>
    );
  }
  return (
    <span className={`${baseBadge} border-warning/30 bg-warning/10 text-warning-foreground`}>
      <Clock className="size-3.5" aria-hidden /> Unavailable
    </span>
  );
}

function normalizeCategory(category: string): string {
  if (category === "HATCHBACK" || category === "HATCH") {
    return "HATCH";
  }
  if (CATEGORY_ICON[category]) {
    return category;
  }
  return "OTHER";
}

function normalizeType(type: CarType | null | undefined): "ELECTRIC" | "COMBUSTION" {
  return type === "ELECTRIC" ? "ELECTRIC" : "COMBUSTION";
}
