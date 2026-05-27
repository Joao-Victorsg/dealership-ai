"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";

const SORT_OPTIONS = [
  { sortBy: "REGISTRATION_DATE", sortDirection: "DESC", label: "Recently added" },
  { sortBy: "PRICE", sortDirection: "ASC", label: "Price: low to high" },
  { sortBy: "PRICE", sortDirection: "DESC", label: "Price: high to low" },
  { sortBy: "YEAR", sortDirection: "DESC", label: "Year: newest first" },
  { sortBy: "YEAR", sortDirection: "ASC", label: "Year: oldest first" },
];

export function InventorySortSelect() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const currentValue = `${searchParams.get("sortBy") ?? "REGISTRATION_DATE"}_${searchParams.get("sortDirection") ?? "DESC"}`;

  return (
    <label className="flex items-center gap-2 text-sm">
      <span className="text-muted-foreground">Sort</span>
      <select
        value={currentValue}
        onChange={(e) => {
          const option = SORT_OPTIONS.find(
            (entry) => `${entry.sortBy}_${entry.sortDirection}` === e.target.value
          );
          const params = new URLSearchParams(searchParams.toString());
          if (option) {
            params.set("sortBy", option.sortBy);
            params.set("sortDirection", option.sortDirection);
          }
          params.delete("page");
          router.push(`${pathname}?${params.toString()}`);
        }}
        className="h-9 rounded-md border border-input bg-background px-2 text-sm focus-visible:border-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
      >
        {SORT_OPTIONS.map((option) => (
          <option
            key={`${option.sortBy}_${option.sortDirection}`}
            value={`${option.sortBy}_${option.sortDirection}`}
          >
            {option.label}
          </option>
        ))}
      </select>
    </label>
  );
}
