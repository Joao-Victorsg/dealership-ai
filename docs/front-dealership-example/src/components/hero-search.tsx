import { Search } from "lucide-react";
import { useNavigate } from "@tanstack/react-router";
import { useState } from "react";
import { Button } from "@/components/ui/button";

const CATEGORIES = ["Any", "SUV", "Sedan", "Sport", "Hatch", "Pick-up"];
const TYPES = ["Any", "Electric", "Combustion"];
const CONDITIONS = ["Any", "New", "Pre-owned"];

export function HeroSearch() {
  const navigate = useNavigate();
  const [category, setCategory] = useState("Any");
  const [type, setType] = useState("Any");
  const [condition, setCondition] = useState("Any");

  return (
    <form
      onSubmit={(e) => {
        e.preventDefault();
        navigate({ to: "/inventory" });
      }}
      className="grid gap-2 rounded-xl border border-border bg-card p-3 shadow-sm sm:grid-cols-[repeat(3,1fr)_auto] sm:gap-2"
      role="search"
      aria-label="Find a car"
    >
      <SelectField label="Category" value={category} onChange={setCategory} options={CATEGORIES} />
      <SelectField label="Type" value={type} onChange={setType} options={TYPES} />
      <SelectField label="Condition" value={condition} onChange={setCondition} options={CONDITIONS} />
      <Button type="submit" size="lg" className="h-12 gap-2">
        <Search className="size-4" /> Search
      </Button>
    </form>
  );
}

function SelectField({
  label,
  value,
  onChange,
  options,
}: {
  label: string;
  value: string;
  onChange: (v: string) => void;
  options: string[];
}) {
  return (
    <label className="flex flex-col gap-0.5 rounded-lg bg-surface px-3 py-2 text-left">
      <span className="text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
        {label}
      </span>
      <select
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="h-7 w-full bg-transparent text-sm font-medium text-foreground focus:outline-none"
      >
        {options.map((o) => (
          <option key={o}>{o}</option>
        ))}
      </select>
    </label>
  );
}
