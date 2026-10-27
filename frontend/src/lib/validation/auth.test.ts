import { describe, expect, it } from "vitest";
import { authSchema } from "@/lib/validation/auth";

describe("authSchema", () => {
  it("accepts an email and a password in the allowed range", () => {
    const parsed = authSchema.parse({ email: " Ada@Example.com ", password: "password1" });
    expect(parsed.email).toBe("Ada@Example.com");
  });

  it("rejects a short password", () => {
    const result = authSchema.safeParse({ email: "ada@example.com", password: "short" });
    expect(result.success).toBe(false);
  });
});
