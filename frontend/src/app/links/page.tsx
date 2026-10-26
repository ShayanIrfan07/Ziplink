import Link from "next/link";
import { LinkManager } from "@/features/links/link-manager";

export default function LinksPage() {
  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-10 sm:px-6">
      <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-end">
        <div>
          <p className="text-sm uppercase tracking-[0.16em] text-accent">Library</p>
          <h1 className="mt-2 font-display text-4xl tracking-tight">Your links</h1>
          <p className="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">
            Signed-in links stay with your account. Without an account, this list follows the browser cookie. Charts over time come later.
          </p>
        </div>
        <Link href="/#shorten" className="inline-flex h-11 items-center justify-center rounded-full bg-primary px-5 text-sm font-medium text-primary-foreground">
          New link
        </Link>
      </div>
      <LinkManager />
    </div>
  );
}
