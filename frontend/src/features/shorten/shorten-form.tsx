"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { Check, Copy, ExternalLink } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { toast } from "sonner";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { createUrl, errorMessage } from "@/lib/api/client";
import { createUrlSchema, toCreateUrlPayload, type CreateUrlFormValues } from "@/lib/validation/url";
import type { UrlResponse } from "@/types/url";

export function ShortenForm() {
  const [result, setResult] = useState<UrlResponse | null>(null);
  const [copied, setCopied] = useState(false);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<CreateUrlFormValues>({
    resolver: zodResolver(createUrlSchema),
    defaultValues: { originalUrl: "", alias: "", expiresAt: "" },
  });

  async function onSubmit(values: CreateUrlFormValues) {
    setCopied(false);
    try {
      const created = await createUrl(toCreateUrlPayload(values));
      setResult(created);
      toast.success("Short link created");
    } catch (error) {
      const message = errorMessage(error);
      setError("originalUrl", { message });
      toast.error(message);
    }
  }

  async function copy() {
    if (!result) {
      return;
    }
    await navigator.clipboard.writeText(result.shortUrl);
    setCopied(true);
    toast.success("Copied");
    window.setTimeout(() => setCopied(false), 1500);
  }

  return (
    <div className="rounded-3xl border border-border bg-card p-5 shadow-[0_20px_60px_-40px_rgba(20,34,28,0.6)] sm:p-6">
      <form className="space-y-4" onSubmit={handleSubmit(onSubmit)} noValidate>
        <div className="space-y-2">
          <Label htmlFor="originalUrl">Destination URL</Label>
          <Input
            id="originalUrl"
            type="url"
            inputMode="url"
            autoComplete="url"
            placeholder="https://example.com/a-very-long-path"
            aria-invalid={errors.originalUrl ? true : undefined}
            aria-describedby={errors.originalUrl ? "originalUrl-error" : undefined}
            {...register("originalUrl")}
          />
          {errors.originalUrl ? (
            <p id="originalUrl-error" className="text-sm text-danger" role="alert">
              {errors.originalUrl.message}
            </p>
          ) : null}
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div className="space-y-2">
            <Label htmlFor="alias">Custom alias</Label>
            <Input id="alias" placeholder="launch-notes" aria-invalid={errors.alias ? true : undefined} {...register("alias")} />
            {errors.alias ? <p className="text-sm text-danger">{errors.alias.message}</p> : <p className="text-xs text-muted-foreground">Optional. Leave blank for a generated code.</p>}
          </div>
          <div className="space-y-2">
            <Label htmlFor="expiresAt">Expiration</Label>
            <Input id="expiresAt" type="datetime-local" {...register("expiresAt")} />
            {errors.expiresAt ? <p className="text-sm text-danger">{errors.expiresAt.message}</p> : <p className="text-xs text-muted-foreground">Optional. The link stops redirecting after this time.</p>}
          </div>
        </div>
        <Button type="submit" disabled={isSubmitting} className="w-full sm:w-auto">
          {isSubmitting ? "Shortening..." : "Shorten link"}
        </Button>
      </form>
      <div aria-live="polite" className="mt-5">
        {result ? (
          <div className="rounded-2xl bg-muted p-4">
            <p className="text-xs uppercase tracking-[0.14em] text-muted-foreground">Your short link</p>
            <p className="mt-2 break-all font-mono text-sm text-foreground">{result.shortUrl}</p>
            <div className="mt-3 flex flex-wrap gap-2">
              <Button size="sm" variant="secondary" onClick={copy}>
                {copied ? <Check aria-hidden="true" className="h-4 w-4" /> : <Copy aria-hidden="true" className="h-4 w-4" />}
                {copied ? "Copied" : "Copy"}
              </Button>
              <Button size="sm" variant="outline" asChild>
                <a href={result.shortUrl} target="_blank" rel="noreferrer">
                  <ExternalLink aria-hidden="true" className="h-4 w-4" />
                  Open
                </a>
              </Button>
            </div>
          </div>
        ) : (
          <p className="text-sm text-muted-foreground">The short link appears here after the API accepts the destination.</p>
        )}
      </div>
    </div>
  );
}
