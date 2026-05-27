const brl = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL", maximumFractionDigits: 0 });
const brlFull = new Intl.NumberFormat("pt-BR", { style: "currency", currency: "BRL" });
const dateFmt = new Intl.DateTimeFormat("pt-BR");
const numFmt = new Intl.NumberFormat("pt-BR");

export const formatBRL = (v: number, opts?: { full?: boolean }) =>
  opts?.full ? brlFull.format(v) : brl.format(v);

export const formatDate = (d: Date | string) =>
  dateFmt.format(typeof d === "string" ? new Date(d) : d);

export const formatKm = (km: number) => `${numFmt.format(km)} km`;

export function formatCPF(raw: string) {
  const d = raw.replace(/\D/g, "").slice(0, 11);
  return d
    .replace(/^(\d{3})(\d)/, "$1.$2")
    .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/\.(\d{3})(\d)/, ".$1-$2");
}

export function formatCEP(raw: string) {
  const d = raw.replace(/\D/g, "").slice(0, 8);
  return d.replace(/^(\d{5})(\d)/, "$1-$2");
}

export function formatPhone(raw: string) {
  const d = raw.replace(/\D/g, "").slice(0, 11);
  if (d.length <= 10) {
    return d.replace(/^(\d{2})(\d{4})(\d)/, "($1) $2-$3").replace(/^(\d{2})(\d)/, "($1) $2");
  }
  return d.replace(/^(\d{2})(\d{5})(\d)/, "($1) $2-$3").replace(/^(\d{2})(\d)/, "($1) $2");
}

export const TAX_RATE = 0.08; // 8% — referenced as "x%" in design copy
export const TAX_LABEL = `${Math.round(TAX_RATE * 100)}%`;
export const computeTotal = (value: number) => value * (1 + TAX_RATE);
