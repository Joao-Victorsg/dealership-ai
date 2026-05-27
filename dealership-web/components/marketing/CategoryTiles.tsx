import Link from "next/link";
import { Mountain, Car as CarIcon, BatteryCharging, CircleDot, Truck } from "lucide-react";

const TILES = [
  { label: "SUV", href: "/inventory?category=SUV", icon: Mountain, copy: "All-terrain comfort" },
  { label: "Sedan", href: "/inventory?category=SEDAN", icon: CarIcon, copy: "Refined daily drive" },
  { label: "Sport", href: "/inventory?category=COUPE", icon: BatteryCharging, copy: "Pure performance" },
  { label: "Hatch", href: "/inventory?category=HATCHBACK", icon: CircleDot, copy: "City-ready agility" },
  { label: "Pick-up", href: "/inventory?category=PICKUP", icon: Truck, copy: "Built to work" },
];

export function CategoryTiles() {
  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
      {TILES.map(({ label, href, icon: Icon, copy }) => (
        <Link
          key={label}
          href={href}
          className="group flex flex-col items-start gap-3 rounded-lg border border-border bg-card p-5 transition-all duration-200 ease-[var(--ease-out-expo)] hover:-translate-y-0.5 hover:border-border-strong"
        >
          <span className="grid size-10 place-items-center rounded-md bg-primary/10 text-primary transition-colors group-hover:bg-primary group-hover:text-primary-foreground">
            <Icon className="size-5" aria-hidden />
          </span>
          <div>
            <div className="font-display text-lg font-semibold">{label}</div>
            <div className="text-xs text-muted-foreground">{copy}</div>
          </div>
        </Link>
      ))}
    </div>
  );
}
