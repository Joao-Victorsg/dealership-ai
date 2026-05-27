// components/shared/LoadingSkeleton.tsx
// Reusable animated skeleton primitive.
// Used in loading.tsx files and composite skeleton components.

interface LoadingSkeletonProps {
  className?: string;
}

export function LoadingSkeleton({ className = "" }: LoadingSkeletonProps) {
  return (
    <div
      aria-hidden="true"
      className={`animate-pulse rounded-[var(--radius)] bg-muted ${className}`}
    />
  );
}
