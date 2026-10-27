/**
 * @vitest-environment jsdom
 */
import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { describe, expect, it, vi } from "vitest";
import { ShortenForm } from "@/features/shorten/shorten-form";

describe("ShortenForm", () => {
  it("shows a validation error and does not call the API for a javascript URL", async () => {
    const user = userEvent.setup();
    const calls: string[] = [];
    vi.stubGlobal("fetch", (input: RequestInfo | URL) => {
      calls.push(String(input));
      return Promise.resolve(new Response("{}", { status: 500 }));
    });

    render(<ShortenForm />);
    await user.type(screen.getByLabelText("Destination URL"), "javascript:alert(1)");
    await user.click(screen.getByRole("button", { name: "Shorten link" }));

    expect(await screen.findByRole("alert")).toHaveProperty(
      "textContent",
      expect.stringContaining("Only http and https URLs are allowed"),
    );
    expect(calls).toHaveLength(0);
    vi.unstubAllGlobals();
  });
});
