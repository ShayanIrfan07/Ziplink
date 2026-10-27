import { describe, expect, it } from "vitest";
import { createUrlSchema, toCreateUrlPayload } from "@/lib/validation/url";

describe("createUrlSchema", () => {
  it("accepts an https URL and omits an empty alias", () => {
    const parsed = createUrlSchema.parse({
      originalUrl: " https://Example.com/docs ",
      alias: "   ",
      expiresAt: "",
    });

    expect(toCreateUrlPayload(parsed)).toEqual({ originalUrl: "https://Example.com/docs" });
  });

  it("rejects javascript, data, and credential URLs", () => {
    for (const originalUrl of ["javascript:alert(1)", "data:text/html,hi", "https://user:secret@example.com"]) {
      const result = createUrlSchema.safeParse({ originalUrl, alias: "", expiresAt: "" });
      expect(result.success).toBe(false);
    }
  });

  it("rejects a short alias and a past expiration", () => {
    const alias = createUrlSchema.safeParse({
      originalUrl: "https://example.com",
      alias: "no",
      expiresAt: "",
    });
    expect(alias.success).toBe(false);

    const expired = createUrlSchema.safeParse({
      originalUrl: "https://example.com",
      alias: "",
      expiresAt: "2000-01-01T00:00",
    });
    expect(expired.success).toBe(false);
  });
});
