const COLOR_HINTS: Record<string, string> = {
  white: "oklch(0.95 0.005 85)",
  pearl: "oklch(0.95 0.005 85)",
  silver: "oklch(0.82 0.005 85)",
  grey: "oklch(0.55 0.005 75)",
  granite: "oklch(0.50 0.008 75)",
  nardo: "oklch(0.62 0.005 75)",
  black: "oklch(0.22 0.005 60)",
  carbon: "oklch(0.25 0.005 60)",
  red: "oklch(0.55 0.18 25)",
  guards: "oklch(0.50 0.18 28)",
  tornado: "oklch(0.55 0.18 25)",
  orange: "oklch(0.68 0.16 50)",
  blue: "oklch(0.50 0.12 240)",
  ocean: "oklch(0.50 0.12 235)",
  atlantic: "oklch(0.50 0.12 240)",
  green: "oklch(0.55 0.10 145)",
};

function tintFor(color: string) {
  const key = color.toLowerCase();
  for (const token of Object.keys(COLOR_HINTS)) {
    if (key.includes(token)) return COLOR_HINTS[token]!;
  }
  return "oklch(0.55 0.02 60)";
}

interface CarPlaceholderProps {
  externalColor: string;
  className?: string;
}

export function CarPlaceholder({ externalColor, className }: CarPlaceholderProps) {
  const tint = tintFor(externalColor);

  return (
    <div className={`relative flex items-center justify-center ${className ?? ""}`} aria-hidden>
      <div
        className="absolute inset-0"
        style={{
          background: `radial-gradient(120% 80% at 50% 110%, ${tint} 0%, color-mix(in oklab, ${tint} 70%, var(--color-surface)) 55%, var(--color-surface) 100%)`,
        }}
      />
      <svg viewBox="0 0 320 180" className="relative h-full w-full" preserveAspectRatio="xMidYMid meet">
        <line x1="0" y1="135" x2="320" y2="135" stroke="oklch(0 0 0 / 0.06)" strokeWidth="1" />
        <g fill={tint} stroke="oklch(0 0 0 / 0.18)" strokeWidth="1.2">
          <path d="M40,130 Q60,95 110,90 L200,86 Q245,86 270,108 L292,118 Q300,122 300,128 L300,135 L40,135 Z" />
          <path d="M110,90 Q140,60 200,60 Q230,60 245,86" fill="color-mix(in oklab, currentColor 10%, transparent)" />
          <path d="M125,86 Q150,68 195,68 Q225,68 235,86 Z" fill="oklch(0.85 0.02 240 / 0.7)" stroke="none" />
        </g>
        <circle cx="100" cy="138" r="14" fill="oklch(0.18 0.005 60)" />
        <circle cx="100" cy="138" r="6" fill="oklch(0.55 0.005 75)" />
        <circle cx="240" cy="138" r="14" fill="oklch(0.18 0.005 60)" />
        <circle cx="240" cy="138" r="6" fill="oklch(0.55 0.005 75)" />
      </svg>
    </div>
  );
}
