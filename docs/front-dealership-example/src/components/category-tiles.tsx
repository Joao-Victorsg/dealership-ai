import { Link } from "@tanstack/react-router";
import { Mountain, Car as CarIcon, BatteryCharging, CircleDot, Truck } from "lucide-react";
import type { CarCategory } from "@/lib/mock-data";

const TILES: { category: CarCategory; icon: React.ComponentType<{ className?: string }>; copy: string }[] = [
  { category: "SUV", icon: Mountain, copy: "All-terrain comfort" },
  { category: "Sedan", icon: CarIcon, copy: "Refined daily drive" },
  { category: "Sport", icon: BatteryCharging, copy: "Pure performance" },
  { category: "Hatch", icon: CircleDot, copy: "City-ready agility" },
  { category: "Pick-up", icon: Truck, copy: "Built to work" },
];

export function CategoryTiles() {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
      {TILES.map(({ category, icon: Icon, copy }) => (
        <Link
          key={category}
          to="/inventory"
          className="group flex flex-col items-start gap-3 rounded-lg border border-border bg-card p-5 transition-all duration-200 ease-[var(--ease-out-expo)] hover:-translate-y-0.5 hover:border-border-strong"
        >
          <span className="grid size-10 place-items-center rounded-md bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-primary-foreground">
            <Icon className="size-5" aria-hidden />
          </span>
          <div>
            <div className="font-display text-lg font-semibold">{category}</div>
            <div className="text-xs text-muted-foreground">{copy}</div>
          </div>
        </Link>
      ))}
    </div>
  );
}
