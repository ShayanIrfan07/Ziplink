export type User = {
  id: string;
  email: string;
  createdAt: string;
};

export type TopLink = {
  id: string;
  shortCode: string;
  shortUrl: string;
  originalUrl: string;
  clicks: number;
};

export type RecentClick = {
  linkId: string;
  shortCode: string;
  clickedAt: string;
  clientCategory: string;
  referrer: string | null;
};

export type ClickPoint = {
  date: string;
  clicks: number;
};

export type CategoryCount = {
  category: string;
  clicks: number;
};

export type ReferrerCount = {
  referrer: string | null;
  clicks: number;
};

export type ClickTrend = {
  from: string;
  to: string;
  totalClicks: number;
  points: ClickPoint[];
  categories: CategoryCount[];
  referrers: ReferrerCount[];
};

export type DashboardSummary = {
  totalLinks: number;
  totalClicks: number;
  activeLinks: number;
  expiredLinks: number;
  recentLinks: import("@/types/url").UrlResponse[];
  topLinks: TopLink[];
  recentClicks: RecentClick[];
};
