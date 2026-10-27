"use client";

import { Button } from "@/components/ui/button";
import type { ClickTrend } from "@/types/account";

const RANGES = [7, 14, 30] as const;

const MONTHS = ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"];

export type TrendLink = {
  id: string;
  shortCode: string;
};

export function formatUtcDay(isoDate: string) {
  const [, month, day] = isoDate.split("-");
  return `${MONTHS[Number(month) - 1]} ${Number(day)}`;
}

export function ClickTrendPanel({
  trend,
  status,
  message,
  days,
  onDaysChange,
  links,
  linkId,
  onLinkChange,
}: {
  trend: ClickTrend | null;
  status: "loading" | "ready" | "error";
  message: string;
  days: number;
  onDaysChange: (days: number) => void;
  links: TrendLink[];
  linkId: string;
  onLinkChange: (linkId: string) => void;
}) {
  const max = Math.max(1, ...(trend?.points.map((point) => point.clicks) ?? [1]));

  return (
    <section className="mt-6 rounded-3xl border border-border bg-card p-5">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h2 className="font-display text-2xl">Clicks over time</h2>
          <p className="mt-1 text-sm text-muted-foreground">Each bar is one UTC day. A refresh counts as another click.</p>
        </div>
        <div className="flex flex-wrap items-center gap-2">
          {RANGES.map((value) => (
            <Button
              key={value}
              size="sm"
              variant={days === value ? "default" : "outline"}
              aria-pressed={days === value}
              onClick={() => onDaysChange(value)}
            >
              {value} days
            </Button>
          ))}
          <label className="text-sm text-muted-foreground">
            <span className="sr-only">Link</span>
            <select
              aria-label="Link"
              value={linkId}
              onChange={(event) => onLinkChange(event.target.value)}
              className="h-9 rounded-full border border-border bg-card px-3 text-sm text-foreground"
            >
              <option value="">All links</option>
              {links.map((link) => (
                <option key={link.id} value={link.id}>
                  {link.shortCode}
                </option>
              ))}
            </select>
          </label>
        </div>
      </div>

      {status === "loading" && !trend ? <div className="mt-5 h-40 animate-pulse rounded-2xl bg-muted" /> : null}
      {status === "error" ? (
        <p className="mt-5 text-sm text-danger" role="alert">
          {message}
        </p>
      ) : null}
      {trend ? (
        <>
          <p className="mt-4 text-sm text-muted-foreground">
            {trend.totalClicks.toLocaleString()} clicks from {formatUtcDay(trend.from)} to {formatUtcDay(trend.to)}.
          </p>
          {trend.totalClicks === 0 ? (
            <p className="mt-4 text-sm text-muted-foreground">No clicks in this range.</p>
          ) : (
            <svg viewBox={`0 0 ${trend.points.length * 12} 100`} className="mt-4 h-40 w-full" role="img" aria-labelledby="click-trend-title">
              <title id="click-trend-title">Daily clicks</title>
              {trend.points.map((point, index) => {
                const height = point.clicks === 0 ? 0 : Math.max(4, (point.clicks / max) * 100);
                return (
                  <rect key={point.date} x={index * 12 + 2} y={100 - height} width={8} height={height} rx={2} className="fill-primary">
                    <title>{`${formatUtcDay(point.date)}: ${point.clicks}`}</title>
                  </rect>
                );
              })}
            </svg>
          )}
          <table className="sr-only">
            <caption>Daily clicks in UTC</caption>
            <thead>
              <tr>
                <th>Date</th>
                <th>Clicks</th>
              </tr>
            </thead>
            <tbody>
              {trend.points.map((point) => (
                <tr key={point.date}>
                  <td>{point.date}</td>
                  <td>{point.clicks}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <div className="mt-5 grid gap-4 sm:grid-cols-2">
            <div>
              <h3 className="text-sm font-medium">Clients</h3>
              {trend.categories.length === 0 ? (
                <p className="mt-2 text-sm text-muted-foreground">No client categories in this range.</p>
              ) : (
                <ul className="mt-2 space-y-1 text-sm">
                  {trend.categories.map((row) => (
                    <li key={row.category} className="flex justify-between gap-3">
                      <span>{row.category.toLowerCase()}</span>
                      <span className="text-muted-foreground">{row.clicks}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
            <div>
              <h3 className="text-sm font-medium">Referrers</h3>
              {trend.referrers.length === 0 ? (
                <p className="mt-2 text-sm text-muted-foreground">No referrers in this range.</p>
              ) : (
                <ul className="mt-2 space-y-1 text-sm">
                  {trend.referrers.map((row) => (
                    <li key={row.referrer ?? "direct"} className="flex justify-between gap-3">
                      <span className="truncate">{row.referrer ?? "Direct"}</span>
                      <span className="shrink-0 text-muted-foreground">{row.clicks}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
        </>
      ) : null}
    </section>
  );
}
