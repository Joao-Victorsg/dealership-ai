"use client";

import { Search } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";

const CATEGORIES = [
  { label: "Any", value: "" },
  { label: "SUV", value: "SUV" },
  { label: "Sedan", value: "SEDAN" },
  { label: "Sport", value: "COUPE" },
  { label: "Hatch", value: "HATCHBACK" },
  { label: "Pick-up", value: "PICKUP" },
];

const TYPES = [
  { label: "Any", value: "" },
  { label: "Electric", value: "ELECTRIC" },
  { label: "Combustion", value: "GASOLINE" },
];

const CONDITIONS = [
  { label: "Any", value: "" },
  { label: "New", value: "NEW" },
  { label: "Pre-owned", value: "USED" },
];

export function HeroSearch() {
  const router = useRouter();
  const [category, setCategory] = useState("");
  const [type, setType] = useState("");
  const [condition, setCondition] = useState("");

  return (
    <form
      onSubmit={(event) => {
        event.preventDefault();
        const params = new URLSearchParams();
        if (category) params.set("category", category);
        if (type) params.set("type", type);
        if (condition) params.set("condition", condition);
        const query = params.toString();
        router.push(query ? `/inventory?${query}` : "/inventory");
      }}
      className="grid gap-2 rounded-xl border border-border bg-card p-3 shadow-sm sm:grid-cols-[repeat(3,1fr)_auto] sm:gap-2"
      role="search"
      aria-label="Find a car"
    >
      <SelectField label="Category" value={category} onChange={setCategory} options={CATEGORIES} />
      <SelectField label="Type" value={type} onChange={setType} options={TYPES} />
      <SelectField label="Condition" value={condition} onChange={setCondition} options={CONDITIONS} />
      <button type="submit" className="inline-flex h-12 items-center justify-center gap-2 rounded-lg bg-primary px-5 text-sm font-semibold text-primary-foreground hover:bg-primary/90">
        <Search className="size-4" aria-hidden /> Search
      </button>
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
  onChange: (value: string) => void;
  options: Array<{ label: string; value: string }>;
}) {
  return (
    <label className="flex flex-col gap-0.5 rounded-lg bg-surface px-3 py-2 text-left">
      <span className="text-[10px] font-semibold uppercase tracking-[0.16em] text-muted-foreground">
        {label}
      </span>
      <select
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="h-7 w-full bg-transparent text-sm font-medium text-foreground focus:outline-none"
      >
        {options.map((option) => (
          <option key={option.label} value={option.value}>{option.label}</option>
        ))}
      </select>
    </label>
  );
}
