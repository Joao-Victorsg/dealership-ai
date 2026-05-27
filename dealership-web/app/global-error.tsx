// app/global-error.tsx
// Global error boundary — catches unhandled errors in the root layout.
// Renders a minimal shell (html+body) because the root layout is unavailable.
// Source: tasks.md T083; Next.js docs — global-error
"use client";

import { ErrorDisplay } from "@/components/shared/ErrorDisplay";

interface GlobalErrorProps {
  error: Error & { digest?: string };
  reset: () => void;
}

export default function GlobalError({ error, reset }: GlobalErrorProps) {
  return (
    <html lang="pt-BR">
      <body
        style={{
          display: "flex",
          minHeight: "100vh",
          flexDirection: "column",
          alignItems: "center",
          justifyContent: "center",
          fontFamily: "sans-serif",
          padding: "2rem",
        }}
      >
        <ErrorDisplay
          message={error.message || "Ocorreu um erro inesperado. Por favor, tente novamente."}
          requestId={error.digest}
        />
        <button
          type="button"
          onClick={reset}
          style={{
            marginTop: "1rem",
            padding: "0.5rem 1.25rem",
            border: "1px solid #ccc",
            borderRadius: "0.375rem",
            cursor: "pointer",
            fontSize: "0.875rem",
          }}
        >
          Tentar novamente
        </button>
      </body>
    </html>
  );
}
