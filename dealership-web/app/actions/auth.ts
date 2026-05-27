"use server";
// app/actions/auth.ts
// Server Actions for authentication flows.
// Source: tasks.md T051; spec.md US3 SC3; contracts/bff-api.md §Auth

import { redirect } from "next/navigation";
import { logoutUser } from "@/lib/api/auth";
import { BffError } from "@/lib/api/client";

/**
 * Signs the user out by calling POST /api/v1/auth/logout on the BFF.
 * On success (204) or 401 (already logged out), redirects to home.
 */
export async function logoutAction(): Promise<void> {
  try {
    await logoutUser();
  } catch (err: unknown) {
    // 401 means the session is already gone — treat as success
    if (err instanceof BffError && err.code === "AUTHENTICATION_REQUIRED") {
      redirect("/");
    }
    throw err;
  }
  redirect("/");
}
