"use client";

import { Link2 } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Button } from "@/components/ui/button";
import { useAuth } from "@/features/auth/auth-provider";
import { logoutAccount } from "@/lib/api/client";
import { cn } from "@/lib/utils";

export function SiteHeader() {
  const pathname = usePathname();
  const router = useRouter();
  const { user, loading, setUser } = useAuth();
  const links = [
    { href: "/#product", label: "Product", compact: true },
    { href: "/#how", label: "How it works", compact: true },
    user ? { href: "/dashboard", label: "Dashboard", compact: false } : { href: "/links", label: "Your links", compact: false },
  ];

  async function logout() {
    await logoutAccount();
    setUser(null);
    router.push("/");
  }

  return (
    <header className="sticky top-0 z-30 border-b border-border/80 bg-background/90 backdrop-blur">
      <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-3 px-4 py-3 sm:px-6">
        <Link href="/" className="flex items-center gap-2 font-display text-xl tracking-tight">
          <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-primary text-primary-foreground">
            <Link2 aria-hidden="true" className="h-4 w-4" />
          </span>
          Ziplink
        </Link>
        <nav aria-label="Primary" className="flex items-center gap-1 sm:gap-3">
          {links.map((link) => (
            <Link
              key={link.href}
              href={link.href}
              className={cn(
                "rounded-full px-3 py-2 text-sm text-muted-foreground hover:text-foreground",
                link.compact && "hidden md:inline",
                pathname === link.href && "bg-muted text-foreground",
              )}
            >
              {link.label}
            </Link>
          ))}
          {loading ? null : user ? (
            <Button size="sm" variant="outline" onClick={() => void logout()}>
              Log out
            </Button>
          ) : (
            <Link href="/login" className="rounded-full px-3 py-2 text-sm text-foreground">
              Sign in
            </Link>
          )}
        </nav>
      </div>
    </header>
  );
}
