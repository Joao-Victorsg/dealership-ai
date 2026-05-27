// lib/api/client.ts
// Typed fetch wrapper for the BFF REST API.
// Reads BFF_URL (server-side) or NEXT_PUBLIC_BFF_URL (client-side) from env.
// Throws typed BffError on all non-2xx responses.

import type { BffErrorCode, BffErrorResponse } from "@/lib/api/types";

/** Typed error thrown by bffFetch for all non-2xx BFF responses */
export class BffError extends Error {
  public readonly code: BffErrorCode;
  public readonly requestId: string;
  public readonly details?: Array<{ field: string; message: string }>;

  constructor(
    code: BffErrorCode,
    message: string,
    requestId: string,
    details?: Array<{ field: string; message: string }>
  ) {
    super(message);
    this.name = "BffError";
    this.code = code;
    this.requestId = requestId;
    this.details = details;
  }
}

function getBffBaseUrl(): string {
  // Server-side: use the private BFF_URL (not exposed to browser)
  if (typeof window === "undefined") {
    const url = process.env.BFF_URL;
    if (!url) {
      throw new Error("BFF_URL env var is not set. Check .env.local.");
    }
    return url;
  }
  // Client-side: use the public BFF URL
  const url = process.env.NEXT_PUBLIC_BFF_URL;
  if (!url) {
    throw new Error(
      "NEXT_PUBLIC_BFF_URL env var is not set. Check .env.local."
    );
  }
  return url;
}

export interface BffFetchOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
}

async function tryGetIncomingCookieHeader(): Promise<string | null> {
  if (typeof window !== "undefined") return null;
  try {
    const { headers } = await import("next/headers");
    const requestHeaders = await headers();
    return requestHeaders.get("cookie");
  } catch {
    return null;
  }
}

/**
 * Typed fetch wrapper for the BFF REST API.
 *
 * - Constructs absolute URL from BFF_URL / NEXT_PUBLIC_BFF_URL
 * - Sets `Content-Type: application/json` when a body is provided
 * - Forwards cookies automatically (credentials: 'include') for auth
 * - Parses BffErrorResponse on non-2xx and throws BffError with typed code
 * - Throws AUTHENTICATION_REQUIRED on 401
 */
export async function bffFetch<T>(
  path: string,
  options: BffFetchOptions = {}
): Promise<T> {
  const base = getBffBaseUrl();
  const url = `${base}${path}`;

  const { body, headers, ...rest } = options;

  const requestHeaders: Record<string, string> = {
    Accept: "application/json",
    ...(headers as Record<string, string> | undefined),
  };

  const hasCookieHeader = Object.keys(requestHeaders).some(
    (key) => key.toLowerCase() === "cookie"
  );
  if (!hasCookieHeader) {
    const incomingCookie = await tryGetIncomingCookieHeader();
    if (incomingCookie) {
      requestHeaders.cookie = incomingCookie;
    }
  }

  if (body !== undefined) {
    requestHeaders["Content-Type"] = "application/json";
  }

  const response = await fetch(url, {
    cache: "no-store",
    credentials: "include",
    headers: requestHeaders,
    body: body !== undefined ? JSON.stringify(body) : undefined,
    ...rest,
  });

  if (response.ok) {
    // 204 No Content — return null cast to T
    if (response.status === 204) {
      return null as T;
    }
    return response.json() as Promise<T>;
  }

  // Non-2xx — attempt to parse BffErrorResponse
  let errorBody: BffErrorResponse | null = null;
  try {
    errorBody = (await response.json()) as BffErrorResponse;
  } catch {
    // JSON parse failed — fall through to generic error
  }

  if (errorBody?.error) {
    const { code, message, details } = errorBody.error;
    const requestId = errorBody.meta?.requestId ?? "unknown";
    throw new BffError(code, message, requestId, details);
  }

  // 401 without a parseable error body
  if (response.status === 401) {
    throw new BffError(
      "AUTHENTICATION_REQUIRED",
      "Authentication required",
      "unknown"
    );
  }

  throw new BffError(
    "INTERNAL_ERROR",
    `BFF returned ${response.status}`,
    "unknown"
  );
}
