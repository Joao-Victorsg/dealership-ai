// app/(admin)/admin/settings/page.tsx
// Admin settings placeholder page.
// Source: tasks.md T070; spec.md US7

import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Configurações | Admin | Aurelio Motors",
};

export default function AdminSettingsPage() {
  return (
    <div className="mx-auto max-w-2xl px-4 py-10 sm:px-6 lg:px-8">
      <h1 className="mb-4 font-display text-2xl font-bold text-foreground">
        Configurações
      </h1>
      <p className="text-muted-foreground">
        Esta seção estará disponível em breve.
      </p>
    </div>
  );
}
