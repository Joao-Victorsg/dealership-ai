// components/shared/ErrorDisplay.tsx
// User-safe error card component.
// Accepts only pre-sanitized message strings — never raw Error objects.
// Source: FR-042, SC-008; research.md R-13

interface ErrorDisplayProps {
  message: string;
  requestId?: string;
}

/**
 * Displays a user-safe error message.
 * Never exposes stack traces, service names, or HTTP status codes.
 * When requestId is provided, shows it as a support reference.
 */
export function ErrorDisplay({ message, requestId }: ErrorDisplayProps) {
  return (
    <div
      role="alert"
      className="rounded-[var(--radius)] border border-destructive/30 bg-destructive/10 p-4 text-sm"
    >
      <p className="font-medium text-destructive">{message}</p>
      {requestId && (
        <p className="mt-1 text-muted-foreground text-xs">
          Código de referência: <span className="tabular font-mono">{requestId}</span>
        </p>
      )}
    </div>
  );
}
