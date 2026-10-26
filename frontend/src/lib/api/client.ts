import type { ApiErrorBody, PageResponse, UrlResponse, UrlStatus } from "@/types/url";

export class ApiRequestError extends Error {
  readonly status: number;
  readonly body: ApiErrorBody;

  constructor(status: number, body: ApiErrorBody) {
    super(body.message);
    this.status = status;
    this.body = body;
  }
}

export function apiBaseUrl() {
  return process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";
}

function readCookie(name: string) {
  const prefix = `${name}=`;
  const parts = document.cookie.split("; ");
  const match = parts.find((part) => part.startsWith(prefix));
  return match ? decodeURIComponent(match.slice(prefix.length)) : null;
}

async function primeCsrf() {
  await fetch(`${apiBaseUrl()}/api/v1/urls?page=0&size=1`, {
    credentials: "include",
  });
}

type RequestOptions = {
  method?: string;
  body?: unknown;
};

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const method = options.method ?? "GET";
  const headers = new Headers();
  if (options.body !== undefined) {
    headers.set("Content-Type", "application/json");
  }
  if (method !== "GET" && method !== "HEAD") {
    if (!readCookie("XSRF-TOKEN")) {
      await primeCsrf();
    }
    const token = readCookie("XSRF-TOKEN");
    if (token) {
      headers.set("X-XSRF-TOKEN", token);
    }
  }

  const response = await send(path, method, headers, options.body);
  if (response.status === 403 && method !== "GET" && method !== "HEAD") {
    await primeCsrf();
    const token = readCookie("XSRF-TOKEN");
    if (token) {
      headers.set("X-XSRF-TOKEN", token);
    }
    return readBody<T>(await send(path, method, headers, options.body));
  }
  return readBody<T>(response);
}

function send(path: string, method: string, headers: Headers, body: unknown) {
  return fetch(`${apiBaseUrl()}${path}`, {
    method,
    headers,
    credentials: "include",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
}

async function readBody<T>(response: Response): Promise<T> {
  if (response.status === 204) {
    return undefined as T;
  }
  const text = await response.text();
  const payload = text.length > 0 ? (JSON.parse(text) as T | ApiErrorBody) : undefined;
  if (!response.ok) {
    const body = isApiError(payload)
      ? payload
      : {
          code: "REQUEST_FAILED",
          message: "The request could not be completed",
          fields: [],
          timestamp: new Date().toISOString(),
          path: response.url,
        };
    throw new ApiRequestError(response.status, body);
  }
  return payload as T;
}

function isApiError(value: unknown): value is ApiErrorBody {
  return (
    typeof value === "object" &&
    value !== null &&
    "code" in value &&
    "message" in value &&
    typeof value.message === "string"
  );
}

export function errorMessage(error: unknown) {
  if (error instanceof ApiRequestError) {
    return error.body.fields[0]?.message ?? error.body.message;
  }
  return "Ziplink could not reach the API. Start the backend and try again.";
}

export function createUrl(body: { originalUrl: string; alias?: string; expiresAt?: string }) {
  return apiRequest<UrlResponse>("/api/v1/urls", { method: "POST", body });
}

export function listUrls(params: { page: number; size: number; sort: string; q?: string }) {
  const search = new URLSearchParams({
    page: String(params.page),
    size: String(params.size),
    sort: params.sort,
  });
  if (params.q && params.q.trim().length > 0) {
    search.set("q", params.q.trim());
  }
  return apiRequest<PageResponse<UrlResponse>>(`/api/v1/urls?${search.toString()}`);
}

export function updateUrl(id: string, body: { status?: UrlStatus; originalUrl?: string; expiresAt?: string | null }) {
  return apiRequest<UrlResponse>(`/api/v1/urls/${id}`, { method: "PATCH", body });
}

export function deleteUrl(id: string) {
  return apiRequest<void>(`/api/v1/urls/${id}`, { method: "DELETE" });
}
