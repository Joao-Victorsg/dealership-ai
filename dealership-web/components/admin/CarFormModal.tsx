"use client";
// components/admin/CarFormModal.tsx
// Create / edit car form modal — lazy-loaded via next/dynamic.
// Source: tasks.md T067; spec.md US7 SC2-3; data-model.md §6
// ⚠️ Uses assumed contract shapes (R-08).

import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useTransition } from "react";
import type { Car, CarCategory, CarType } from "@/lib/api/types";

const CAR_CATEGORIES: CarCategory[] = [
  "SEDAN",
  "SUV",
  "HATCHBACK",
  "COUPE",
  "CONVERTIBLE",
  "MINIVAN",
  "PICKUP",
  "OTHER",
];

const CAR_TYPES: CarType[] = ["GASOLINE", "DIESEL", "ELECTRIC", "HYBRID"];

const carFormSchema = z.object({
  manufacturer: z.string().min(1, "Fabricante é obrigatório"),
  model: z.string().min(1, "Modelo é obrigatório"),
  manufacturingYear: z
    .number({ coerce: true })
    .int()
    .min(1886)
    .max(new Date().getFullYear() + 1),
  type: z.enum(["GASOLINE", "DIESEL", "ELECTRIC", "HYBRID"] as const),
  category: z.enum([
    "SEDAN",
    "SUV",
    "HATCHBACK",
    "COUPE",
    "CONVERTIBLE",
    "MINIVAN",
    "PICKUP",
    "OTHER",
  ] as const),
  isNew: z.boolean(),
  listedValue: z.number({ coerce: true }).positive("Valor deve ser positivo"),
  kilometers: z.number({ coerce: true }).min(0, "KM não pode ser negativo"),
  externalColor: z.string().min(1, "Cor externa é obrigatória"),
  internalColor: z.string().min(1, "Cor interna é obrigatória"),
  vin: z.string().min(1, "VIN é obrigatório").max(17),
  optionalItemsRaw: z.string(), // comma-separated
});

type CarFormValues = z.infer<typeof carFormSchema>;

interface CarFormModalProps {
  car?: Car;
  onSubmit: (values: CarFormValues) => Promise<void>;
  onClose: () => void;
}

export function CarFormModal({ car, onSubmit, onClose }: CarFormModalProps) {
  const [isPending, startTransition] = useTransition();

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<CarFormValues>({
    resolver: zodResolver(carFormSchema),
    defaultValues: car
      ? {
          manufacturer: car.manufacturer,
          model: car.model,
          manufacturingYear: car.manufacturingYear,
          type: car.type,
          category: car.category,
          isNew: car.isNew,
          listedValue: car.listedValue,
          kilometers: car.kilometers,
          externalColor: car.externalColor,
          internalColor: car.internalColor,
          vin: car.vin,
          optionalItemsRaw: car.optionalItems.join(", "),
        }
      : {
          manufacturer: "",
          model: "",
          manufacturingYear: new Date().getFullYear(),
          type: "GASOLINE",
          category: "SEDAN",
          isNew: true,
          listedValue: 0,
          kilometers: 0,
          externalColor: "",
          internalColor: "",
          vin: "",
          optionalItemsRaw: "",
        },
  });

  function onFormSubmit(values: CarFormValues) {
    startTransition(async () => {
      await onSubmit(values);
    });
  }

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="car-form-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
    >
      <div className="flex max-h-[90vh] w-full max-w-2xl flex-col overflow-hidden rounded-[var(--radius-lg)] bg-card shadow-xl">
        <div className="flex items-center justify-between border-b border-border px-6 py-4">
          <h2
            id="car-form-title"
            className="text-base font-semibold text-foreground"
          >
            {car ? "Editar veículo" : "Novo veículo"}
          </h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar"
            className="rounded-[var(--radius-sm)] p-1 text-muted-foreground hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          >
            <svg
              aria-hidden="true"
              className="h-5 w-5"
              viewBox="0 0 24 24"
              fill="none"
              stroke="currentColor"
              strokeWidth={2}
            >
              <path d="M18 6 6 18M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form
          onSubmit={handleSubmit(onFormSubmit)}
          noValidate
          className="flex-1 overflow-y-auto px-6 py-5"
        >
          <div className="grid gap-4 sm:grid-cols-2">
            {/* Manufacturer */}
            <div>
              <label
                htmlFor="manufacturer"
                className="mb-1 block text-sm font-medium text-foreground"
              >
                Fabricante
              </label>
              <input
                id="manufacturer"
                type="text"
                {...register("manufacturer")}
                className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
              {errors.manufacturer && (
                <p role="alert" className="mt-1 text-xs text-destructive">
                  {errors.manufacturer.message}
                </p>
              )}
            </div>

            {/* Model */}
            <div>
              <label
                htmlFor="model"
                className="mb-1 block text-sm font-medium text-foreground"
              >
                Modelo
              </label>
              <input
                id="model"
                type="text"
                {...register("model")}
                className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
              {errors.model && (
                <p role="alert" className="mt-1 text-xs text-destructive">
                  {errors.model.message}
                </p>
              )}
            </div>

            {/* Year */}
            <div>
              <label
                htmlFor="manufacturingYear"
                className="mb-1 block text-sm font-medium text-foreground"
              >
                Ano de fabricação
              </label>
              <input
                id="manufacturingYear"
                type="number"
                {...register("manufacturingYear")}
                className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm tabular-nums focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
              {errors.manufacturingYear && (
                <p role="alert" className="mt-1 text-xs text-destructive">
                  {errors.manufacturingYear.message}
                </p>
              )}
            </div>

            {/* Type */}
            <div>
              <label
                htmlFor="type"
                className="mb-1 block text-sm font-medium text-foreground"
              >
                Combustível
              </label>
              <select
                id="type"
                {...register("type")}
                className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                {CAR_TYPES.map((t) => (
                  <option key={t} value={t}>
                    {t}
                  </option>
                ))}
              </select>
            </div>

            {/* Category */}
            <div>
              <label
                htmlFor="category"
                className="mb-1 block text-sm font-medium text-foreground"
              >
                Categoria
              </label>
              <select
                id="category"
                {...register("category")}
                className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              >
                {CAR_CATEGORIES.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </div>

            {/* isNew */}
            <div className="flex items-center gap-3 pt-5">
              <input
                id="isNew"
                type="checkbox"
                {...register("isNew")}
                className="h-4 w-4 rounded border-input text-primary focus-visible:ring-ring"
              />
              <label htmlFor="isNew" className="text-sm font-medium text-foreground">
                Veículo novo (0 km)
              </label>
            </div>

            {/* Listed value */}
            <div>
              <label
                htmlFor="listedValue"
                className="mb-1 block text-sm font-medium text-foreground"
              >
                Valor (R$)
              </label>
              <input
                id="listedValue"
                type="number"
                step="0.01"
                {...register("listedValue")}
                className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm tabular-nums focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
              {errors.listedValue && (
                <p role="alert" className="mt-1 text-xs text-destructive">
                  {errors.listedValue.message}
                </p>
              )}
            </div>

            {/* Kilometers */}
            <div>
              <label
                htmlFor="kilometers"
                className="mb-1 block text-sm font-medium text-foreground"
              >
                Kilometragem (KM)
              </label>
              <input
                id="kilometers"
                type="number"
                {...register("kilometers")}
                className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm tabular-nums focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
              {errors.kilometers && (
                <p role="alert" className="mt-1 text-xs text-destructive">
                  {errors.kilometers.message}
                </p>
              )}
            </div>

            {/* External color */}
            <div>
              <label
                htmlFor="externalColor"
                className="mb-1 block text-sm font-medium text-foreground"
              >
                Cor externa
              </label>
              <input
                id="externalColor"
                type="text"
                {...register("externalColor")}
                className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
              {errors.externalColor && (
                <p role="alert" className="mt-1 text-xs text-destructive">
                  {errors.externalColor.message}
                </p>
              )}
            </div>

            {/* Internal color */}
            <div>
              <label
                htmlFor="internalColor"
                className="mb-1 block text-sm font-medium text-foreground"
              >
                Cor interna
              </label>
              <input
                id="internalColor"
                type="text"
                {...register("internalColor")}
                className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
              {errors.internalColor && (
                <p role="alert" className="mt-1 text-xs text-destructive">
                  {errors.internalColor.message}
                </p>
              )}
            </div>

            {/* VIN */}
            <div className="sm:col-span-2">
              <label
                htmlFor="vin"
                className="mb-1 block text-sm font-medium text-foreground"
              >
                VIN (Chassi)
              </label>
              <input
                id="vin"
                type="text"
                maxLength={17}
                {...register("vin")}
                className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm font-mono uppercase focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
              {errors.vin && (
                <p role="alert" className="mt-1 text-xs text-destructive">
                  {errors.vin.message}
                </p>
              )}
            </div>

            {/* Optional items */}
            <div className="sm:col-span-2">
              <label
                htmlFor="optionalItemsRaw"
                className="mb-1 block text-sm font-medium text-foreground"
              >
                Opcionais{" "}
                <span className="font-normal text-muted-foreground">
                  (separados por vírgula)
                </span>
              </label>
              <input
                id="optionalItemsRaw"
                type="text"
                placeholder="Ex.: Ar-condicionado, Teto solar, Câmera de ré"
                {...register("optionalItemsRaw")}
                className="w-full rounded-[var(--radius)] border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              />
            </div>
          </div>

          <div className="mt-6 flex justify-end gap-3">
            <button
              type="button"
              onClick={onClose}
              className="rounded-[var(--radius)] bg-muted px-4 py-2 text-sm font-medium text-foreground hover:bg-muted/80 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              Cancelar
            </button>
            <button
              type="submit"
              disabled={isPending}
              className="rounded-[var(--radius)] bg-primary px-5 py-2 text-sm font-semibold text-primary-foreground hover:bg-primary/90 disabled:opacity-60 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            >
              {isPending ? "Salvando…" : car ? "Salvar alterações" : "Criar veículo"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
