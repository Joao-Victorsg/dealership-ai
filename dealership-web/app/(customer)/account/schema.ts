// app/(customer)/account/schema.ts
// Zod validation schema for the profile update form.
// Source: tasks.md T059; spec.md US5 SC1-4; research.md R-04
//
// NOTE: cpf and streetNumber are intentionally excluded — the BFF does not
// accept them in PATCH /api/v1/profile (R-04).

import { z } from "zod";
import { isValidPhone } from "@/lib/utils/phone";
import { CEP_REGEX_MASKED } from "@/lib/utils/cep";

export const profileSchema = z.object({
  firstName: z.string().min(1, "Nome é obrigatório"),
  lastName: z.string().min(1, "Sobrenome é obrigatório"),
  phone: z
    .string()
    .min(1, "Telefone é obrigatório")
    .refine(isValidPhone, "Telefone inválido"),
  cep: z
    .string()
    .min(1, "CEP é obrigatório")
    .regex(CEP_REGEX_MASKED, "CEP inválido (formato: 00000-000)"),
});

export type ProfileFormValues = z.infer<typeof profileSchema>;
