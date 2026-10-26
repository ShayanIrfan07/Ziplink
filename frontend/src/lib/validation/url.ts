import { z } from "zod";

const httpUrl = z
  .string()
  .trim()
  .min(1, "Enter a URL")
  .max(2048, "URL must be at most 2048 characters")
  .refine((value) => {
    try {
      const url = new URL(value);
      return url.protocol === "http:" || url.protocol === "https:";
    } catch {
      return false;
    }
  }, "Only http and https URLs are allowed")
  .refine((value) => {
    const url = new URL(value);
    return url.username.length === 0 && url.password.length === 0;
  }, "URLs with embedded credentials are not allowed");

export const createUrlSchema = z
  .object({
    originalUrl: httpUrl,
    alias: z
      .string()
      .trim()
      .refine(
        (value) => value.length === 0 || /^[A-Za-z0-9_-]{3,32}$/.test(value),
        "Alias must be 3-32 characters and use only letters, numbers, hyphens, or underscores",
      ),
    expiresAt: z.string(),
  })
  .superRefine((value, context) => {
    if (value.expiresAt.length === 0) {
      return;
    }
    const expiresAt = new Date(value.expiresAt);
    if (Number.isNaN(expiresAt.getTime()) || expiresAt.getTime() <= Date.now()) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["expiresAt"],
        message: "Expiration must be in the future",
      });
    }
  });

export type CreateUrlFormValues = z.infer<typeof createUrlSchema>;

export function toCreateUrlPayload(values: CreateUrlFormValues) {
  const payload: { originalUrl: string; alias?: string; expiresAt?: string } = {
    originalUrl: values.originalUrl.trim(),
  };
  if (values.alias.trim().length > 0) {
    payload.alias = values.alias.trim();
  }
  if (values.expiresAt.length > 0) {
    payload.expiresAt = new Date(values.expiresAt).toISOString();
  }
  return payload;
}
