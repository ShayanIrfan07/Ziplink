/**
 * @vitest-environment jsdom
 */
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { DashboardSummaryCards } from "@/features/dashboard/dashboard-screen";
import type { DashboardSummary } from "@/types/account";

const summary: DashboardSummary = {
  totalLinks: 12,
  totalClicks: 34,
  activeLinks: 9,
  expiredLinks: 2,
  recentLinks: [],
  topLinks: [],
  recentClicks: [],
};

describe("DashboardSummaryCards", () => {
  it("renders the counts returned by the API", () => {
    render(<DashboardSummaryCards summary={summary} />);
    expect(screen.getByText("Total links").parentElement?.textContent).toContain("12");
    expect(screen.getByText("Total clicks").parentElement?.textContent).toContain("34");
    expect(screen.getByText("Active links").parentElement?.textContent).toContain("9");
    expect(screen.getByText("Expired links").parentElement?.textContent).toContain("2");
  });
});
