import { useSyncExternalStore } from "react";
import { DEFAULT_USER, type MockUser } from "./mock-data";

const KEY = "aurelio.auth.v1";

export type Role = "customer" | "admin";

interface AuthState {
  isAuthenticated: boolean;
  user: (MockUser & { role: Role }) | null;
}

let state: AuthState = load();
const listeners = new Set<() => void>();

function load(): AuthState {
  if (typeof window === "undefined") return { isAuthenticated: false, user: null };
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) return { isAuthenticated: false, user: null };
    const parsed = JSON.parse(raw) as AuthState;
    // Backwards compat: ensure role exists
    if (parsed.user && !("role" in parsed.user)) {
      (parsed.user as MockUser & { role: Role }).role = "customer";
    }
    return parsed;
  } catch {
    return { isAuthenticated: false, user: null };
  }
}

function persist() {
  if (typeof window === "undefined") return;
  localStorage.setItem(KEY, JSON.stringify(state));
  listeners.forEach((l) => l());
}

export function login(email = DEFAULT_USER.email, role: Role = "customer") {
  const baseUser =
    role === "admin"
      ? {
          ...DEFAULT_USER,
          email: email || "admin@aurelio.com",
          firstName: "Marcos",
          lastName: "Aurélio",
        }
      : { ...DEFAULT_USER, email };
  state = { isAuthenticated: true, user: { ...baseUser, role } };
  persist();
}

export function logout() {
  state = { isAuthenticated: false, user: null };
  persist();
}

export function updateUser(patch: Partial<MockUser>) {
  if (!state.user) return;
  state = { ...state, user: { ...state.user, ...patch } };
  persist();
}

export function getAuthSnapshot() {
  return state;
}

export function isAdmin() {
  return state.user?.role === "admin";
}

export function useAuth() {
  return useSyncExternalStore(
    (cb) => {
      listeners.add(cb);
      return () => listeners.delete(cb);
    },
    () => state,
    () => ({ isAuthenticated: false, user: null }),
  );
}
