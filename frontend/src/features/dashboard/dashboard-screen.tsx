"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { LinkManager } from "@/features/links/link-manager";
import { ApiRequestError, dashboardSummary, errorMessage } from "@/lib/api/client";
import type { DashboardSummary } from "@/types/account";

export function DashboardScreen() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [status, setStatus] = useState<"loading" | "ready" | "signed-out" | "error">("loading");
  const [message, setMessage] = useState("");

  useEffect(() => {
    let active = true;
    dashboardSummary()
      .then((next) => {
        if (active) {
          setSummary(next);
          setStatus("ready");
        }
      })
      .catch((error: unknown) => {
        if (!active) {
          return;
        }
        if (error instanceof ApiRequestError && error.status === 401) {
          setStatus("signed-out");
          return;
        }
        setMessage(errorMessage(error));
        setStatus("error");
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-10 sm:px-6">
      <p className="text-sm uppercase tracking-[0.16em] text-accent">Dashboard</p>
      <h1 className="mt-2 font-display text-4xl tracking-tight">Your links</h1>
      <p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">
        Counts come from links and clicks stored for this account. Another account cannot see them.
      </p>

      {status === "loading" ? <DashboardSkeleton /> : null}
      {status === "signed-out" ? (
        <div className="mt-8 rounded-3xl border border-dashed border-border bg-card p-8">
          <h2 className="font-display text-2xl">Sign in to see this dashboard</h2>
          <p className="mt-2 text-sm text-muted-foreground">The API rejects this page until a session cookie is present.</p>
          <div className="mt-5 flex flex-wrap gap-3">
            <Button asChild>
              <Link href="/login">Sign in</Link>
            </Button>
            <Button variant="outline" asChild>
              <Link href="/register">Create account</Link>
            </Button>
          </div>
        </div>
      ) : null}
      {status === "error" ? (
        <div className="mt-8 rounded-2xl border border-danger/30 bg-card p-6" role="alert">
          <p className="font-medium">Dashboard could not be loaded</p>
          <p className="mt-1 text-sm text-muted-foreground">{message}</p>
        </div>
      ) : null}
      {status === "ready" && summary ? (
        <>
          <DashboardSummaryCards summary={summary} />
          <div className="mt-6 grid gap-4 lg:grid-cols-2">
            <section className="rounded-3xl border border-border bg-card p-5">
              <h2 className="font-display text-2xl">Top links</h2>
              {summary.topLinks.length === 0 ? (
                <p className="mt-3 text-sm text-muted-foreground">No recorded clicks yet.</p>
              ) : (
                <ul className="mt-4 space-y-3">
                  {summary.topLinks.map((link) => (
                    <li key={link.id} className="flex items-center justify-between gap-3 text-sm">
                      <span className="truncate font-mono">{link.shortUrl}</span>
                      <span className="shrink-0 text-muted-foreground">{link.clicks} clicks</span>
                    </li>
                  ))}
                </ul>
              )}
            </section>
            <section className="rounded-3xl border border-border bg-card p-5">
              <h2 className="font-display text-2xl">Recent clicks</h2>
              {summary.recentClicks.length === 0 ? (
                <p className="mt-3 text-sm text-muted-foreground">Clicks appear here after someone opens a short link.</p>
              ) : (
                <ul className="mt-4 space-y-3">
                  {summary.recentClicks.map((click) => (
                    <li key={`${click.linkId}-${click.clickedAt}`} className="text-sm">
                      <p className="font-mono">{click.shortCode}</p>
                      <p className="text-muted-foreground">
                        {new Date(click.clickedAt).toLocaleString()} · {click.clientCategory.toLowerCase()}
                        {click.referrer ? ` · ${click.referrer}` : ""}
                      </p>
                    </li>
                  ))}
                </ul>
              )}
            </section>
          </div>
          <div className="mt-10">
            <LinkManager />
          </div>
        </>
      ) : null}
    </div>
  );
}

export function DashboardSummaryCards({ summary }: { summary: DashboardSummary }) {
  const cards = [
    ["Total links", summary.totalLinks],
    ["Total clicks", summary.totalClicks],
    ["Active links", summary.activeLinks],
    ["Expired links", summary.expiredLinks],
  ] as const;
  return (
    <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
      {cards.map(([label, value]) => (
        <article key={label} className="rounded-3xl border border-border bg-card p-5">
          <p className="text-sm text-muted-foreground">{label}</p>
          <p className="mt-2 font-display text-4xl">{value.toLocaleString()}</p>
        </article>
      ))}
    </div>
  );
}

function DashboardSkeleton() {
  return (
    <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-4" aria-hidden="true">
      {Array.from({ length: 4 }, (_, index) => (
        <div key={index} className="h-28 animate-pulse rounded-3xl bg-muted" />
      ))}
    </div>
  );
}
