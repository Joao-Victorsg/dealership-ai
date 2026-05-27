// components/inventory/CarGrid.tsx
// Responsive grid wrapper for car cards.
// Source: tasks.md T036

import type { ReactNode } from "react";

interface CarGridProps {
  children: ReactNode;
}

export function CarGrid({ children }: CarGridProps) {
  return (
    <div className="grid grid-cols-[repeat(auto-fit,minmax(280px,1fr))] gap-5">
      {children}
    </div>
  );
}
