/**
 * @vitest-environment jsdom
 */
import { cleanup, render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, describe, expect, it, vi } from "vitest";
import { ClickTrendPanel, formatUtcDay } from "@/features/dashboard/click-trend";
import type { ClickTrend } from "@/types/account";

afterEach(() => {
  cleanup();
});

const trend: ClickTrend = {
  from: "2026-10-25",
  to: "2026-10-27",
  totalClicks: 2,
  points: [
    { date: "2026-10-25", clicks: 0 },
    { date: "2026-10-26", clicks: 2 },
    { date: "2026-10-27", clicks: 0 },
  ],
  categories: [{ category: "DESKTOP", clicks: 2 }],
  referrers: [{ referrer: null, clicks: 2 }],
};

describe("ClickTrendPanel", () => {
  it("renders the stored daily counts without shifting the UTC date", () => {
    render(
      <ClickTrendPanel
        trend={trend}
        status="ready"
        message=""
        days={14}
        onDaysChange={() => undefined}
        links={[{ id: "link-1", shortCode: "notes" }]}
        linkId=""
        onLinkChange={() => undefined}
      />,
    );

    expect(screen.getByText("2 clicks from Oct 25 to Oct 27.")).toBeTruthy();
    expect(screen.getByRole("img", { name: "Daily clicks" })).toBeTruthy();
    expect(screen.getByRole("cell", { name: "2026-10-26" }).parentElement?.textContent).toContain("2");
    expect(screen.getByText("desktop").parentElement?.textContent).toContain("2");
    expect(screen.getByText("Direct").parentElement?.textContent).toContain("2");
    expect(formatUtcDay("2026-10-27")).toBe("Oct 27");
  });

  it("shows an empty range instead of a chart", () => {
    render(
      <ClickTrendPanel
        trend={{
          ...trend,
          totalClicks: 0,
          points: trend.points.map((point) => ({ ...point, clicks: 0 })),
          categories: [],
          referrers: [],
        }}
        status="ready"
        message=""
        days={7}
        onDaysChange={() => undefined}
        links={[]}
        linkId=""
        onLinkChange={() => undefined}
      />,
    );

    expect(screen.getByText("No clicks in this range.")).toBeTruthy();
    expect(screen.queryByRole("img", { name: "Daily clicks" })).toBeNull();
  });

  it("reports the selected range and link", async () => {
    const onDaysChange = vi.fn();
    const onLinkChange = vi.fn();
    const user = userEvent.setup();
    render(
      <ClickTrendPanel
        trend={trend}
        status="ready"
        message=""
        days={14}
        onDaysChange={onDaysChange}
        links={[{ id: "link-1", shortCode: "notes" }]}
        linkId=""
        onLinkChange={onLinkChange}
      />,
    );

    await user.click(screen.getByRole("button", { name: "7 days" }));
    await user.selectOptions(screen.getByRole("combobox", { name: "Link" }), "link-1");
    expect(onDaysChange).toHaveBeenCalledWith(7);
    expect(onLinkChange).toHaveBeenCalledWith("link-1");
  });
});
