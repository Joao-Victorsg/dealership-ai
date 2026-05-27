import { useEffect, useId, useState } from "react";
import { cn } from "@/lib/utils";

export interface FieldProps {
  label: string;
  htmlFor?: string;
  required?: boolean;
  hint?: string;
  error?: string;
  children: (ids: { inputId: string; describedBy?: string }) => React.ReactNode;
  className?: string;
}

export function Field({ label, htmlFor, required, hint, error, children, className }: FieldProps) {
  const auto = useId();
  const inputId = htmlFor ?? `f-${auto}`;
  const hintId = hint ? `${inputId}-hint` : undefined;
  const errId = error ? `${inputId}-err` : undefined;
  const describedBy = [hintId, errId].filter(Boolean).join(" ") || undefined;

  return (
    <div className={cn("flex flex-col gap-1.5", className)}>
      <label htmlFor={inputId} className="text-sm font-medium text-foreground">
        {label}
        {required && (
          <span className="ml-1 text-primary" aria-hidden>
            *
          </span>
        )}
      </label>
      {children({ inputId, describedBy })}
      {hint && !error && (
        <p id={hintId} className="text-xs text-muted-foreground">
          {hint}
        </p>
      )}
      {error && (
        <p id={errId} role="alert" className="text-xs font-medium text-destructive">
          {error}
        </p>
      )}
    </div>
  );
}

export interface MaskedInputProps
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, "value" | "onChange"> {
  value: string;
  onValueChange: (val: string) => void;
  format: (raw: string) => string;
  describedBy?: string;
  invalid?: boolean;
}

export function MaskedInput({
  value,
  onValueChange,
  format,
  describedBy,
  invalid,
  className,
  ...rest
}: MaskedInputProps) {
  const [display, setDisplay] = useState(format(value));
  useEffect(() => setDisplay(format(value)), [value, format]);
  return (
    <input
      {...rest}
      value={display}
      onChange={(e) => {
        const next = format(e.target.value);
        setDisplay(next);
        onValueChange(next);
      }}
      aria-describedby={describedBy}
      aria-invalid={invalid || undefined}
      className={cn(
        "h-11 w-full rounded-md border border-input bg-background px-3 text-sm tabular text-foreground transition-colors placeholder:text-muted-foreground/60 focus-visible:border-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background",
        invalid && "border-destructive focus-visible:border-destructive",
        className,
      )}
    />
  );
}

export const baseInputCls =
  "h-11 w-full rounded-md border border-input bg-background px-3 text-sm text-foreground transition-colors placeholder:text-muted-foreground/60 focus-visible:border-primary focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background";
