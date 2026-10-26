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

export type DashboardSummary = {
  totalLinks: number;
  totalClicks: number;
  activeLinks: number;
  expiredLinks: number;
  recentLinks: import("@/types/url").UrlResponse[];
  topLinks: TopLink[];
  recentClicks: RecentClick[];
};
