import { Link } from "@tanstack/react-router";
import { Menu, User2, LogOut, Shield } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetTrigger,
} from "@/components/ui/sheet";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useAuth, logout } from "@/lib/mock-auth";

const NAV = [
  { to: "/inventory", label: "Inventory" },
  { to: "/inventory", label: "New", search: { condition: "new" } as const },
  { to: "/inventory", label: "Used", search: { condition: "used" } as const },
  { to: "/about", label: "About" },
];

export function SiteHeader() {
  const { isAuthenticated, user } = useAuth();
  const [open, setOpen] = useState(false);

  return (
    <header className="sticky top-0 z-40 border-b border-border bg-background/85 backdrop-blur supports-[backdrop-filter]:bg-background/70">
      <div className="mx-auto flex h-16 w-full max-w-[1280px] items-center justify-between gap-6 px-4 sm:px-6">
        <Link to="/" className="flex items-center gap-2">
          <Logo />
          <span className="font-display text-lg font-semibold tracking-tight">Aurelio</span>
          <span className="hidden text-xs uppercase tracking-[0.18em] text-muted-foreground sm:inline">
            Motors
          </span>
        </Link>

        <nav aria-label="Primary" className="hidden items-center gap-1 md:flex">
          {NAV.map((item) => (
            <Link
              key={item.label}
              to={item.to}
              className="rounded-md px-3 py-2 text-sm text-foreground/80 transition-colors hover:bg-muted hover:text-foreground"
              activeProps={{ className: "text-foreground bg-muted" }}
            >
              {item.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-2">
          {isAuthenticated && user ? (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="outline" size="sm" className="gap-2">
                  {user.role === "admin" ? (
                    <Shield className="size-4" aria-hidden="true" />
                  ) : (
                    <User2 className="size-4" aria-hidden="true" />
                  )}
                  <span className="hidden sm:inline">{user.firstName}</span>
                  {user.role === "admin" && (
                    <span className="hidden rounded-sm bg-primary/15 px-1.5 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-primary sm:inline">
                      Staff
                    </span>
                  )}
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-56">
                <DropdownMenuLabel className="font-normal">
                  <div className="text-sm font-medium">{user.firstName} {user.lastName}</div>
                  <div className="text-xs text-muted-foreground">{user.email}</div>
                </DropdownMenuLabel>
                <DropdownMenuSeparator />
                {user.role === "admin" && (
                  <DropdownMenuItem asChild>
                    <Link to="/admin">
                      <Shield className="mr-2 size-4" /> Inventory management
                    </Link>
                  </DropdownMenuItem>
                )}
                <DropdownMenuItem asChild>
                  <Link to="/account">Profile</Link>
                </DropdownMenuItem>
                <DropdownMenuItem asChild>
                  <Link to="/account/purchases">My purchases</Link>
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem onClick={() => logout()}>
                  <LogOut className="mr-2 size-4" /> Sign out
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          ) : (
            <>
              <Button asChild variant="ghost" size="sm" className="hidden sm:inline-flex">
                <Link to="/login" search={{ redirect: "/" }}>Sign in</Link>
              </Button>
              <Button asChild size="sm">
                <Link to="/register">Create account</Link>
              </Button>
            </>
          )}

          <Sheet open={open} onOpenChange={setOpen}>
            <SheetTrigger asChild>
              <Button variant="ghost" size="icon" className="md:hidden" aria-label="Open menu">
                <Menu className="size-5" />
              </Button>
            </SheetTrigger>
            <SheetContent side="right" className="w-[280px]">
              <SheetHeader>
                <SheetTitle className="font-display">Menu</SheetTitle>
              </SheetHeader>
              <nav className="mt-6 flex flex-col gap-1" aria-label="Mobile">
                {NAV.map((item) => (
                  <Link
                    key={item.label}
                    to={item.to}
                    onClick={() => setOpen(false)}
                    className="rounded-md px-3 py-3 text-base hover:bg-muted"
                  >
                    {item.label}
                  </Link>
                ))}
                <div className="my-2 h-px bg-border" />
                {isAuthenticated && user ? (
                  <>
                    {user.role === "admin" && (
                      <Link to="/admin" onClick={() => setOpen(false)} className="rounded-md px-3 py-3 font-medium text-primary hover:bg-muted">
                        Inventory management
                      </Link>
                    )}
                    <Link to="/account" onClick={() => setOpen(false)} className="rounded-md px-3 py-3 hover:bg-muted">Profile</Link>
                    <Link to="/account/purchases" onClick={() => setOpen(false)} className="rounded-md px-3 py-3 hover:bg-muted">My purchases</Link>
                    <button onClick={() => { logout(); setOpen(false); }} className="rounded-md px-3 py-3 text-left hover:bg-muted">Sign out</button>
                  </>
                ) : (
                  <>
                    <Link to="/login" search={{ redirect: "/" }} onClick={() => setOpen(false)} className="rounded-md px-3 py-3 hover:bg-muted">Sign in</Link>
                    <Link to="/register" onClick={() => setOpen(false)} className="rounded-md px-3 py-3 hover:bg-muted">Create account</Link>
                  </>
                )}
              </nav>
            </SheetContent>
          </Sheet>
        </div>
      </div>
    </header>
  );
}

function Logo() {
  return (
    <svg
      width="28"
      height="28"
      viewBox="0 0 28 28"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <rect x="1" y="1" width="26" height="26" rx="6" fill="var(--color-primary)" />
      <path d="M9 19 L14 7 L19 19 M11 15 H17" stroke="var(--color-primary-foreground)" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" fill="none" />
    </svg>
  );
}
