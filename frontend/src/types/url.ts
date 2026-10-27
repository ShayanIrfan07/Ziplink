export type UrlStatus = "ACTIVE" | "DISABLED";

export type UrlResponse = {
  id: string;
  originalUrl: string;
  shortCode: string;
  shortUrl: string;
  customAlias: string | null;
  status: UrlStatus;
  expired: boolean;
  expiresAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type PageResponse<T> = {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
};

export type FieldViolation = {
  field: string;
  message: string;
};

export type ApiErrorBody = {
  code: string;
  message: string;
  fields: FieldViolation[];
  timestamp: string;
  path: string;
};
