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
import type { CarCategory, CarStatus, CarType } from "@/lib/mock-data";
import { cn } from "@/lib/utils";

const baseBadge =
  "inline-flex items-center gap-1.5 rounded-md border px-2 py-1 text-xs font-medium tabular";

export function ConditionBadge({ isNew }: { isNew: boolean }) {
  return (
    <span
      className={cn(
        baseBadge,
        isNew
          ? "border-primary/30 bg-primary/5 text-primary"
          : "border-border bg-muted text-foreground/80",
      )}
    >
      {isNew ? <Sparkles className="size-3.5" aria-hidden /> : <History className="size-3.5" aria-hidden />}
      {isNew ? "New" : "Pre-owned"}
    </span>
  );
}

export function TypeBadge({ type }: { type: CarType }) {
  const isElectric = type === "Electric";
  return (
    <span
      className={cn(
        baseBadge,
        isElectric
          ? "border-emerald-700/20 bg-emerald-700/5 text-emerald-800 dark:text-emerald-300"
          : "border-amber-700/20 bg-amber-700/5 text-amber-900 dark:text-amber-200",
      )}
    >
      {isElectric ? <Zap className="size-3.5" aria-hidden /> : <Fuel className="size-3.5" aria-hidden />}
      {type}
    </span>
  );
}

const CATEGORY_ICON: Record<CarCategory, React.ComponentType<{ className?: string }>> = {
  SUV: Mountain,
  Sedan: CarIcon,
  Sport: BatteryCharging,
  Hatch: CircleDot,
  "Pick-up": Truck,
};

export function CategoryBadge({ category }: { category: CarCategory }) {
  const Icon = CATEGORY_ICON[category];
  return (
    <span className={cn(baseBadge, "border-border bg-surface text-foreground/80")}>
      <Icon className="size-3.5" aria-hidden />
      {category}
    </span>
  );
}

export function StatusBadge({ status }: { status: CarStatus }) {
  if (status === 1)
    return (
      <span className={cn(baseBadge, "border-success/30 bg-success/10 text-success")}>
        <CheckCircle2 className="size-3.5" aria-hidden /> Available
      </span>
    );
  if (status === 2)
    return (
      <span className={cn(baseBadge, "border-border bg-muted text-muted-foreground")}>
        <Lock className="size-3.5" aria-hidden /> Sold
      </span>
    );
  return (
    <span className={cn(baseBadge, "border-warning/30 bg-warning/10 text-warning-foreground")}>
      <Clock className="size-3.5" aria-hidden /> Unavailable
    </span>
  );
}
