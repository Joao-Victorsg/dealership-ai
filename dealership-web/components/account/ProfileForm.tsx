"use client";
// components/account/ProfileForm.tsx
// Profile update form — name, phone, CEP.
// cpf and streetNumber are read-only (BFF contract R-04).
// Source: tasks.md T061; spec.md US5 SC1-4

import { useState, useTransition } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { phoneMask } from "@/lib/utils/phone";
import { cepMask, cepUnmask } from "@/lib/utils/cep";
import { lookupCep } from "@/lib/api/cep";
import { ErrorDisplay } from "@/components/shared/ErrorDisplay";
import { AddressPreview } from "@/components/account/AddressPreview";
import { updateProfileAction } from "@/app/(customer)/account/actions";
import { profileSchema, type ProfileFormValues } from "@/app/(customer)/account/schema";
import type { CustomerProfile } from "@/lib/api/types";

interface ProfileFormProps {
  profile: CustomerProfile;
}

type CepLookupStatus = "idle" | "loading" | "found" | "error";

export function ProfileForm({ profile }: ProfileFormProps) {
  const [isPending, startTransition] = useTransition();
  const [success, setSuccess] = useState(false);
  const [serverError, setServerError] = useState<{
    message: string;
    requestId?: string;
  } | null>(null);
  const [cepStatus, setCepStatus] = useState<CepLookupStatus>("idle");
  const [cepPreview, setCepPreview] = useState<{
    street: string;
    neighborhood: string;
    city: string;
    state: string;
  } | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    setError,
    formState: { errors },
  } = useForm<ProfileFormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: {
      firstName: profile.firstName ?? "",
      lastName: profile.lastName ?? "",
      phone: phoneMask(profile.phone ?? ""),
      cep: profile.address?.cep ? cepMask(profile.address.cep) : "",
    },
  });

  const cepValue = watch("cep");

  function handlePhoneChange(e: React.ChangeEvent<HTMLInputElement>) {
    setValue("phone", phoneMask(e.target.value), { shouldValidate: false });
  }

  function handleCepChange(e: React.ChangeEvent<HTMLInputElement>) {
    setValue("cep", cepMask(e.target.value), { shouldValidate: false });
    setCepPreview(null);
    setCepStatus("idle");
  }

  async function handleCepBlur() {
    const raw = cepUnmask(cepValue ?? "");
    if (raw.length !== 8) return;
    setCepStatus("loading");
    setCepPreview(null);
    const result = await lookupCep(raw);
    if (result.ok) {
      setCepStatus("found");
      setCepPreview({
        street: result.street,
        neighborhood: result.neighborhood,
        city: result.city,
        state: result.state,
      });
    } else {
      setCepStatus("error");
    }
  }

  function onSubmit(values: ProfileFormValues) {
    setServerError(null);
    setSuccess(false);
    startTransition(async () => {
      const result = await updateProfileAction(values);
      if (result && !result.ok) {
        if (result.fieldErrors) {
          for (const [field, message] of Object.entries(result.fieldErrors)) {
            setError(field as keyof ProfileFormValues, { message });
          }
        } else {
          setServerError({
            message: result.message ?? "Erro ao salvar alterações.",
            requestId: result.requestId,
          });
        }
        return;
      }
      setSuccess(true);
    });
  }

  return (
    <form
      onSubmit={handleSubmit(onSubmit)}
      noValidate
      className="space-y-6"
    >
      {success && (
        <div
          role="status"
          aria-live="polite"
          className="rounded-[var(--radius)] border border-green-300 bg-green-50 p-3 text-sm text-green-800 dark:border-green-700 dark:bg-green-900/20 dark:text-green-300"
        >
          Perfil atualizado com sucesso!
        </div>
      )}

      {serverError && (
        <ErrorDisplay
          message={serverError.message}
          requestId={serverError.requestId}
        />
      )}

      <div className="grid gap-4 sm:grid-cols-2">
        {/* First name */}
        <div>
          <label
            htmlFor="firstName"
            className="mb-1.5 block text-sm font-medium text-foreground"
          >
            Nome
          </label>
          <input
            id="firstName"
            type="text"
            autoComplete="given-name"
            aria-invalid={!!errors.firstName}
            aria-describedby={errors.firstName ? "firstName-error" : undefined}
            {...register("firstName")}
            className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring aria-[invalid=true]:border-destructive"
          />
          {errors.firstName && (
            <p id="firstName-error" role="alert" className="mt-1 text-xs text-destructive">
              {errors.firstName.message}
            </p>
          )}
        </div>

        {/* Last name */}
        <div>
          <label
            htmlFor="lastName"
            className="mb-1.5 block text-sm font-medium text-foreground"
          >
            Sobrenome
          </label>
          <input
            id="lastName"
            type="text"
            autoComplete="family-name"
            aria-invalid={!!errors.lastName}
            aria-describedby={errors.lastName ? "lastName-error" : undefined}
            {...register("lastName")}
            className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring aria-[invalid=true]:border-destructive"
          />
          {errors.lastName && (
            <p id="lastName-error" role="alert" className="mt-1 text-xs text-destructive">
              {errors.lastName.message}
            </p>
          )}
        </div>
      </div>

      {/* CPF — read-only */}
      <div>
        <label
          htmlFor="cpf"
          className="mb-1.5 block text-sm font-medium text-foreground"
        >
          CPF{" "}
          <span className="font-normal text-muted-foreground">(não editável)</span>
        </label>
        <input
          id="cpf"
          type="text"
          readOnly
          disabled
          value={profile.cpf ?? ""}
          className="w-full cursor-not-allowed rounded-[var(--radius)] border border-input bg-muted px-3 py-2 text-sm text-muted-foreground"
        />
      </div>

      {/* Phone */}
      <div>
        <label
          htmlFor="phone"
          className="mb-1.5 block text-sm font-medium text-foreground"
        >
          Telefone
        </label>
        <input
          id="phone"
          type="tel"
          inputMode="numeric"
          autoComplete="tel"
          aria-invalid={!!errors.phone}
          aria-describedby={errors.phone ? "phone-error" : undefined}
          {...register("phone", { onChange: handlePhoneChange })}
          className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring aria-[invalid=true]:border-destructive"
        />
        {errors.phone && (
          <p id="phone-error" role="alert" className="mt-1 text-xs text-destructive">
            {errors.phone.message}
          </p>
        )}
      </div>

      {/* CEP */}
      <div>
        <label
          htmlFor="cep"
          className="mb-1.5 block text-sm font-medium text-foreground"
        >
          CEP
        </label>
        <input
          id="cep"
          type="text"
          inputMode="numeric"
          autoComplete="postal-code"
          placeholder="00000-000"
          aria-invalid={!!errors.cep}
          aria-describedby={
            errors.cep
              ? "cep-error"
              : cepStatus !== "idle"
              ? "cep-preview"
              : undefined
          }
          {...register("cep", {
            onChange: handleCepChange,
            onBlur: handleCepBlur,
          })}
          className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring aria-[invalid=true]:border-destructive"
        />
        {errors.cep && (
          <p id="cep-error" role="alert" className="mt-1 text-xs text-destructive">
            {errors.cep.message}
          </p>
        )}
        {!errors.cep && (
          <div id="cep-preview" aria-live="polite">
            {cepStatus === "loading" && (
              <p className="mt-1 text-xs text-muted-foreground">Consultando CEP…</p>
            )}
            {cepStatus === "error" && (
              <p className="mt-1 text-xs text-muted-foreground">
                Não foi possível consultar o CEP. Prossiga normalmente.
              </p>
            )}
            {cepStatus === "found" && cepPreview && (
              <p className="mt-1 text-xs text-muted-foreground">
                {cepPreview.street}, {cepPreview.neighborhood} —{" "}
                {cepPreview.city}/{cepPreview.state}
              </p>
            )}
          </div>
        )}
      </div>

      {/* Current address (read-only) */}
      <AddressPreview address={profile.address} />

      <button
        type="submit"
        disabled={isPending}
        className="w-full rounded-[var(--radius)] bg-primary py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-60 sm:w-auto sm:px-8"
      >
        {isPending ? "Salvando…" : "Salvar alterações"}
      </button>
    </form>
  );
}
