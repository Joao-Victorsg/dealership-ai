"use client";
// app/(auth)/complete-registration/page.tsx
// Post-Keycloak registration completion form.
// Middleware ensures the user has a valid SESSION before reaching this page.
// Source: tasks.md T048; spec.md US2 (SC1-5); research.md R-03, R-05, R-12

import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { phoneMask } from "@/lib/utils/phone";
import { cpfMask } from "@/lib/utils/cpf";
import { cepMask, cepUnmask } from "@/lib/utils/cep";
import { lookupCep } from "@/lib/api/cep";
import { registerUser } from "@/lib/api/auth";
import { BffError } from "@/lib/api/client";
import { getBFFErrorMessage } from "@/lib/errors";
import { ErrorDisplay } from "@/components/shared/ErrorDisplay";
import { registerSchema, type RegisterFormValues } from "./schema";

// generateMetadata cannot be used in a "use client" component; export separately.
// The title is set in the closest RSC ancestor (layout.tsx).

type CepLookupStatus = "idle" | "loading" | "found" | "error";

interface CepPreview {
  street: string;
  neighborhood: string;
  city: string;
  state: string;
}

export default function CompleteRegistrationPage() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [serverError, setServerError] = useState<{
    message: string;
    requestId?: string;
  } | null>(null);

  const [cepStatus, setCepStatus] = useState<CepLookupStatus>("idle");
  const [cepPreview, setCepPreview] = useState<CepPreview | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    setError,
    formState: { errors },
  } = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      firstName: "",
      lastName: "",
      cpf: "",
      phone: "",
      cep: "",
      streetNumber: "",
    },
  });

  const cepValue = watch("cep");

  function handleCpfChange(e: React.ChangeEvent<HTMLInputElement>) {
    setValue("cpf", cpfMask(e.target.value), { shouldValidate: false });
  }

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
      // CEP lookup failure is non-blocking per spec US2 SC2
      setCepStatus("error");
    }
  }

  async function onSubmit(values: RegisterFormValues) {
    setServerError(null);
    setIsSubmitting(true);
    try {
      const phoneDigits = values.phone.replace(/\D/g, "");
      const normalizedPhone = phoneDigits.startsWith("55")
        ? `+${phoneDigits}`
        : `+55${phoneDigits}`;

      await registerUser({
        firstName: values.firstName,
        lastName: values.lastName,
        cpf: values.cpf,
        phone: normalizedPhone,
        cep: cepUnmask(values.cep),
        streetNumber: values.streetNumber,
      });

      window.location.assign("/inventory");
      return;
    } catch (err: unknown) {
      if (err instanceof BffError) {
        if (err.code === "VALIDATION_ERROR" && err.details?.length) {
          for (const detail of err.details) {
            setError(detail.field as keyof RegisterFormValues, {
              message: detail.message,
            });
          }
          return;
        }
        if (err.code === "DUPLICATE_IDENTITY" && /keycloak/i.test(err.message)) {
          window.location.assign("/inventory");
          return;
        }
        setServerError({
          message: getBFFErrorMessage(err.code),
          requestId: err.requestId,
        });
        return;
      }
      setServerError({
        message: "Não foi possível concluir o cadastro. Tente novamente.",
      });
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <div className="w-full max-w-md">
      <div className="mb-8 text-center">
        <h1 className="font-display text-3xl font-bold text-foreground">
          Complete seu cadastro
        </h1>
        <p className="mt-2 text-muted-foreground">
          Só mais alguns dados para finalizar sua conta.
        </p>
      </div>

      {serverError && (
        <div className="mb-6">
          <ErrorDisplay
            message={serverError.message}
            requestId={serverError.requestId}
          />
        </div>
      )}

      <form
        onSubmit={handleSubmit(onSubmit)}
        noValidate
        className="space-y-5 rounded-[var(--radius-lg)] border border-border bg-card p-6"
      >
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
            placeholder="Seu nome"
            aria-invalid={!!errors.firstName}
            aria-describedby={errors.firstName ? "firstName-error" : undefined}
            {...register("firstName")}
            className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring aria-[invalid=true]:border-destructive"
          />
          {errors.firstName && (
            <p
              id="firstName-error"
              role="alert"
              className="mt-1 text-xs text-destructive"
            >
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
            placeholder="Seu sobrenome"
            aria-invalid={!!errors.lastName}
            aria-describedby={errors.lastName ? "lastName-error" : undefined}
            {...register("lastName")}
            className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring aria-[invalid=true]:border-destructive"
          />
          {errors.lastName && (
            <p
              id="lastName-error"
              role="alert"
              className="mt-1 text-xs text-destructive"
            >
              {errors.lastName.message}
            </p>
          )}
        </div>

        {/* CPF */}
        <div>
          <label
            htmlFor="cpf"
            className="mb-1.5 block text-sm font-medium text-foreground"
          >
            CPF
          </label>
          <input
            id="cpf"
            type="text"
            inputMode="numeric"
            autoComplete="off"
            placeholder="000.000.000-00"
            aria-invalid={!!errors.cpf}
            aria-describedby={errors.cpf ? "cpf-error" : undefined}
            {...register("cpf", { onChange: handleCpfChange })}
            className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring aria-[invalid=true]:border-destructive"
          />
          {errors.cpf && (
            <p id="cpf-error" role="alert" className="mt-1 text-xs text-destructive">
              {errors.cpf.message}
            </p>
          )}
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
            placeholder="(11) 99999-9999"
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
                ? "cep-status"
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
            <div id="cep-status" aria-live="polite">
              {cepStatus === "loading" && (
                <p className="mt-1 text-xs text-muted-foreground">
                  Consultando CEP…
                </p>
              )}
              {cepStatus === "error" && (
                <p className="mt-1 text-xs text-warning">
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

        {/* Street number */}
        <div>
          <label
            htmlFor="streetNumber"
            className="mb-1.5 block text-sm font-medium text-foreground"
          >
            Número
          </label>
          <input
            id="streetNumber"
            type="text"
            autoComplete="address-line2"
            placeholder="Ex.: 42"
            aria-invalid={!!errors.streetNumber}
            aria-describedby={
              errors.streetNumber ? "streetNumber-error" : undefined
            }
            {...register("streetNumber")}
            className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring aria-[invalid=true]:border-destructive"
          />
          {errors.streetNumber && (
            <p
              id="streetNumber-error"
              role="alert"
              className="mt-1 text-xs text-destructive"
            >
              {errors.streetNumber.message}
            </p>
          )}
        </div>

        <button
          type="submit"
          disabled={isSubmitting}
          className="w-full rounded-[var(--radius)] bg-primary py-2.5 text-sm font-semibold text-primary-foreground hover:bg-primary/90 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring disabled:cursor-not-allowed disabled:opacity-60"
        >
          {isSubmitting ? "Salvando…" : "Concluir cadastro"}
        </button>
      </form>
    </div>
  );
}
