"use client";

import { Copy, ExternalLink, Trash2 } from "lucide-react";
import { useEffect, useRef, useState } from "react";
import { toast } from "sonner";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { ConfirmDialog } from "@/components/ui/confirm-dialog";
import { Input } from "@/components/ui/input";
import { deleteUrl, errorMessage, listUrls, updateUrl } from "@/lib/api/client";
import type { PageResponse, UrlResponse } from "@/types/url";

const pageSize = 8;

export function LinkManager() {
  const [draft, setDraft] = useState("");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [sort, setSort] = useState("createdAt,desc");
  const [data, setData] = useState<PageResponse<UrlResponse> | null>(null);
  const [status, setStatus] = useState<"loading" | "ready" | "error">("loading");
  const [message, setMessage] = useState("");
  const [pendingId, setPendingId] = useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = useState<UrlResponse | null>(null);
  const [version, setVersion] = useState(0);

  const skipInitialSearch = useRef(true);

  useEffect(() => {
    if (skipInitialSearch.current) {
      skipInitialSearch.current = false;
      return;
    }
    const handle = window.setTimeout(() => {
      setStatus("loading");
      setPage(0);
      setQuery(draft.trim());
    }, 300);
    return () => window.clearTimeout(handle);
  }, [draft]);

  useEffect(() => {
    let ignore = false;
    listUrls({ page, size: pageSize, sort, q: query })
      .then((result) => {
        if (!ignore) {
          setData(result);
          setStatus("ready");
        }
      })
      .catch((error: unknown) => {
        if (!ignore) {
          setStatus("error");
          setMessage(errorMessage(error));
        }
      });
    return () => {
      ignore = true;
    };
  }, [page, query, sort, version]);

  async function copy(url: string) {
    await navigator.clipboard.writeText(url);
    toast.success("Copied");
  }

  async function toggle(link: UrlResponse) {
    setPendingId(link.id);
    try {
      await updateUrl(link.id, { status: link.status === "ACTIVE" ? "DISABLED" : "ACTIVE" });
      toast.success(link.status === "ACTIVE" ? "Link disabled" : "Link enabled");
      setVersion((current) => current + 1);
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setPendingId(null);
    }
  }

  async function remove(link: UrlResponse) {
    setPendingId(link.id);
    try {
      await deleteUrl(link.id);
      toast.success("Link deleted");
      setPendingDelete(null);
      setVersion((current) => current + 1);
    } catch (error) {
      toast.error(errorMessage(error));
    } finally {
      setPendingId(null);
    }
  }

  return (
    <section className="space-y-4">
      <div className="flex flex-col gap-3 sm:flex-row">
        <Input
          aria-label="Search links"
          placeholder="Search by code or destination"
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
        />
        <label className="sr-only" htmlFor="sort">
          Sort links
        </label>
        <select
          id="sort"
          className="h-11 rounded-xl border border-border bg-card px-3 text-sm"
          value={sort}
          onChange={(event) => {
            setStatus("loading");
            setPage(0);
            setSort(event.target.value);
          }}
        >
          <option value="createdAt,desc">Newest</option>
          <option value="createdAt,asc">Oldest</option>
          <option value="shortCode,asc">Code A–Z</option>
          <option value="expiresAt,asc">Expiration</option>
        </select>
      </div>

      {status === "loading" ? <LinkSkeleton /> : null}
      {status === "error" ? (
        <div className="rounded-2xl border border-danger/30 bg-card p-6" role="alert">
          <p className="font-medium">Links could not be loaded</p>
          <p className="mt-1 text-sm text-muted-foreground">{message}</p>
          <Button
            className="mt-4"
            variant="outline"
            onClick={() => {
              setStatus("loading");
              setVersion((current) => current + 1);
            }}
          >
            Try again
          </Button>
        </div>
      ) : null}
      {status === "ready" && data && data.items.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-border bg-card p-8 text-center">
          <p className="font-display text-2xl">No links yet</p>
          <p className="mx-auto mt-2 max-w-md text-sm text-muted-foreground">
            Shorten a destination on the home page. Links created in this browser stay with the secure Ziplink cookie.
          </p>
        </div>
      ) : null}
      {status === "ready" && data && data.items.length > 0 ? (
        <div className="overflow-x-auto rounded-2xl border border-border">
          <table className="w-full min-w-[720px] border-collapse text-left text-sm">
            <thead className="bg-muted text-muted-foreground">
              <tr>
                <th className="px-4 py-3 font-medium">Short link</th>
                <th className="px-4 py-3 font-medium">Destination</th>
                <th className="px-4 py-3 font-medium">Status</th>
                <th className="px-4 py-3 font-medium">Created</th>
                <th className="px-4 py-3 font-medium">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((link) => (
                <tr key={link.id} className="border-t border-border bg-card">
                  <td className="max-w-[220px] px-4 py-3">
                    <p className="truncate font-mono" title={link.shortUrl}>
                      {link.shortUrl}
                    </p>
                  </td>
                  <td className="max-w-[240px] px-4 py-3">
                    <p className="truncate" title={link.originalUrl}>
                      {link.originalUrl}
                    </p>
                  </td>
                  <td className="px-4 py-3">
                    <Badge tone={link.expired ? "warning" : link.status === "ACTIVE" ? "active" : "danger"}>
                      {link.expired ? "Expired" : link.status === "ACTIVE" ? "Active" : "Disabled"}
                    </Badge>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground">{new Date(link.createdAt).toLocaleString()}</td>
                  <td className="px-4 py-3">
                    <div className="flex flex-wrap gap-2">
                      <Button size="sm" variant="secondary" onClick={() => copy(link.shortUrl)}>
                        <Copy aria-hidden="true" className="h-3.5 w-3.5" />
                        Copy
                      </Button>
                      <Button size="sm" variant="outline" asChild>
                        <a href={link.shortUrl} target="_blank" rel="noreferrer">
                          <ExternalLink aria-hidden="true" className="h-3.5 w-3.5" />
                          Open
                        </a>
                      </Button>
                      <Button size="sm" variant="outline" disabled={pendingId === link.id} onClick={() => toggle(link)}>
                        {link.status === "ACTIVE" ? "Disable" : "Enable"}
                      </Button>
                      <Button size="sm" variant="danger" disabled={pendingId === link.id} onClick={() => setPendingDelete(link)}>
                        <Trash2 aria-hidden="true" className="h-3.5 w-3.5" />
                        Delete
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      ) : null}

      {data && data.totalPages > 1 ? (
        <div className="flex items-center justify-between">
          <p className="text-sm text-muted-foreground">
            Page {data.page + 1} of {data.totalPages}
          </p>
          <div className="flex gap-2">
            <Button
              variant="outline"
              size="sm"
              disabled={page === 0}
              onClick={() => {
                setStatus("loading");
                setPage((current) => current - 1);
              }}
            >
              Previous
            </Button>
            <Button
              variant="outline"
              size="sm"
              disabled={page + 1 >= data.totalPages}
              onClick={() => {
                setStatus("loading");
                setPage((current) => current + 1);
              }}
            >
              Next
            </Button>
          </div>
        </div>
      ) : null}

      <ConfirmDialog
        open={pendingDelete !== null}
        title="Delete this link?"
        description="The short code stops working immediately. This does not change the original website."
        confirmLabel="Delete link"
        pending={pendingId !== null}
        onOpenChange={(open) => {
          if (!open) {
            setPendingDelete(null);
          }
        }}
        onConfirm={() => {
          if (pendingDelete) {
            void remove(pendingDelete);
          }
        }}
      />
    </section>
  );
}

function LinkSkeleton() {
  return (
    <div className="space-y-3" aria-hidden="true">
      {Array.from({ length: 4 }, (_, index) => (
        <div key={index} className="h-16 animate-pulse rounded-2xl bg-muted motion-reduce:animate-none" />
      ))}
    </div>
  );
}
