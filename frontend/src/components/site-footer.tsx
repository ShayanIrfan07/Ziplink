import Link from "next/link";
import { apiBaseUrl } from "@/lib/api/client";

export function SiteFooter() {
  const docsUrl = `${apiBaseUrl()}/swagger-ui.html`;

  return (
    <footer className="border-t border-border">
      <div className="mx-auto flex w-full max-w-6xl flex-col gap-4 px-4 py-8 text-sm text-muted-foreground sm:flex-row sm:items-center sm:justify-between sm:px-6">
        <p>Ziplink. Short links. Smart insights.</p>
        <nav aria-label="Footer" className="flex flex-wrap gap-4">
          <Link href="/#how" className="hover:text-foreground">
            How it works
          </Link>
          <Link href="/links" className="hover:text-foreground">
            Your links
          </Link>
          <a href={docsUrl} className="hover:text-foreground">
            API docs
          </a>
        </nav>
      </div>
    </footer>
  );
}
