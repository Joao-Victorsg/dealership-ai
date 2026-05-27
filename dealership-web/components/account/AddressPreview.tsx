// components/account/AddressPreview.tsx
// Displays the customer's address fields in read-only mode.
// Source: tasks.md T060; spec.md US5 SC2; research.md R-04
//
// NOTE: streetNumber is not updatable via PATCH /api/v1/profile — this is a
// known BFF contract gap (R-04). streetNumber was set at registration and
// cannot be changed. The UI shows it as read-only with an explanatory note.

import type { CustomerAddress } from "@/lib/api/types";

interface AddressPreviewProps {
  address: CustomerAddress;
}

function ReadOnlyField({
  label,
  value,
}: {
  label: string;
  value: string;
}) {
  return (
    <div>
      <p className="mb-1 text-xs font-medium uppercase tracking-wide text-muted-foreground">
        {label}
      </p>
      <p className="text-sm text-foreground">{value || "—"}</p>
    </div>
  );
}

export function AddressPreview({ address }: AddressPreviewProps) {
  if (!address) {
    return (
      <div className="rounded-[var(--radius-lg)] border border-border bg-muted/30 p-4">
        <h3 className="mb-4 text-sm font-semibold text-foreground">
          Endereço atual
        </h3>
        <p className="text-sm text-muted-foreground">Nenhum endereço registrado.</p>
      </div>
    );
  }
  return (
    <div className="rounded-[var(--radius-lg)] border border-border bg-muted/30 p-4">
      <h3 className="mb-4 text-sm font-semibold text-foreground">
        Endereço atual
      </h3>
      <div className="grid gap-3 sm:grid-cols-2">
        <ReadOnlyField label="Logradouro" value={address.street ?? ""} />
        <div>
          <p className="mb-1 text-xs font-medium uppercase tracking-wide text-muted-foreground">
            Número
          </p>
          <p className="text-sm text-foreground">{address.number || "—"}</p>
          <p className="mt-0.5 text-[11px] text-muted-foreground">
            O número não pode ser alterado após o cadastro.
          </p>
        </div>
        <ReadOnlyField label="Bairro" value={address.neighborhood ?? ""} />
        <ReadOnlyField
          label="Cidade / Estado"
          value={address.city ? `${address.city} / ${address.state}` : ""}
        />
        {address.complement && (
          <ReadOnlyField label="Complemento" value={address.complement} />
        )}
      </div>
    </div>
  );
}
