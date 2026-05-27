// app/_vitals.tsx
// Web Vitals reporting hook — called by Next.js instrumentation.
// Sends LCP, CLS, INP, TTFB, and FCP metrics to the analytics endpoint when
// NEXT_PUBLIC_ANALYTICS_URL is defined.  No external library required; the
// Next.js built-in `useReportWebVitals` hook is used.
// Source: tasks.md T082; Next.js docs — app/reportWebVitals
"use client";

import { useReportWebVitals } from "next/web-vitals";

export function WebVitals() {
  useReportWebVitals((metric) => {
    const analyticsUrl = process.env.NEXT_PUBLIC_ANALYTICS_URL;
    if (!analyticsUrl) return;

    // navigator.sendBeacon is preferred to avoid blocking page unload.
    if (typeof navigator !== "undefined" && navigator.sendBeacon) {
      const body = JSON.stringify({
        name: metric.name,
        value: metric.value,
        id: metric.id,
        rating: metric.rating,
        navigationType: metric.navigationType,
      });
      navigator.sendBeacon(analyticsUrl, new Blob([body], { type: "application/json" }));
    }
  });

  return null;
}
