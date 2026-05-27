// app/(auth)/complete-registration/schema.ts
// Zod validation schema for the registration completion form.
// Source: tasks.md T046; spec.md US2 SC1-5; data-model.md §3

import { z } from "zod";
import { isValidCpf } from "@/lib/utils/cpf";
import { isValidPhone } from "@/lib/utils/phone";
import { CEP_REGEX_MASKED } from "@/lib/utils/cep";

export const registerSchema = z.object({
  firstName: z.string().min(1, "Nome é obrigatório"),
  lastName: z.string().min(1, "Sobrenome é obrigatório"),
  cpf: z
    .string()
    .min(1, "CPF é obrigatório")
    .refine(isValidCpf, "CPF inválido"),
  phone: z
    .string()
    .min(1, "Telefone é obrigatório")
    .refine(isValidPhone, "Telefone inválido"),
  cep: z
    .string()
    .min(1, "CEP é obrigatório")
    .regex(CEP_REGEX_MASKED, "CEP inválido (formato: 00000-000)"),
  streetNumber: z
    .string()
    .min(1, "Número é obrigatório")
    .max(20, "Número muito longo"),
});

export type RegisterFormValues = z.infer<typeof registerSchema>;
