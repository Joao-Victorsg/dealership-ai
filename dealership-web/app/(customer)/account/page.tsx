// app/(customer)/account/page.tsx
// Customer profile management page — authenticated only.
// Source: tasks.md T063; spec.md US5 SC1-4

import type { Metadata } from "next";
import { getProfile } from "@/lib/api/profile";
import { ProfileForm } from "@/components/account/ProfileForm";

export const metadata: Metadata = {
  title: "Minha conta | Aurelio Motors",
};

export default async function AccountPage() {
  const profile = await getProfile();

  const hasMissingStreetNumber = !profile.address.number;

  return (
    <div className="max-w-3xl">
      <h2 className="mb-6 font-display text-2xl font-bold text-foreground sm:text-3xl">
        Perfil
      </h2>

      {hasMissingStreetNumber && (
        <div
          role="alert"
          className="mb-6 rounded-[var(--radius)] border border-yellow-300 bg-yellow-50 px-4 py-3 text-sm text-yellow-800 dark:border-yellow-700 dark:bg-yellow-900/20 dark:text-yellow-300"
        >
          Seu endereço está incompleto. O número da residência não foi
          informado no cadastro e não pode ser adicionado aqui. Entre em
          contato com o suporte se precisar atualizá-lo.
        </div>
      )}

      <ProfileForm profile={profile} />
    </div>
  );
}
