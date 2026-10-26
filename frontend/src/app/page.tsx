import { Clock3, Link2, ShieldCheck, SlidersHorizontal } from "lucide-react";
import Link from "next/link";
import { ShortenForm } from "@/features/shorten/shorten-form";

const features = [
  {
    title: "Custom aliases",
    copy: "Choose a readable code, or let Ziplink generate one. Aliases are unique and never silently replaced.",
    icon: Link2,
  },
  {
    title: "Expiration",
    copy: "Set a time when the short link should stop. Visitors then get a clear gone response instead of a stale redirect.",
    icon: Clock3,
  },
  {
    title: "Link controls",
    copy: "Copy, open, disable, or delete the links created in this browser. Another visitor cannot list them.",
    icon: SlidersHorizontal,
  },
  {
    title: "Careful redirects",
    copy: "Only http and https destinations are accepted. Redirects are temporary so a changed or expired link is not cached forever.",
    icon: ShieldCheck,
  },
];

export default function HomePage() {
  return (
    <div>
      <section id="product" className="mx-auto grid w-full max-w-6xl gap-10 px-4 py-14 sm:px-6 lg:grid-cols-[1.05fr_0.95fr] lg:items-center lg:py-20">
        <div>
          <p className="text-sm font-medium uppercase tracking-[0.18em] text-accent">URL shortener</p>
          <h1 className="mt-3 font-display text-5xl leading-none tracking-tight sm:text-6xl">
            Short links.
            <span className="block text-primary">Smart insights.</span>
          </h1>
          <p className="mt-5 max-w-xl text-lg leading-8 text-muted-foreground">
            Turn a long destination into a link you can share, expire, and turn off. Ziplink stores the original address and sends people there with a temporary redirect.
          </p>
          <div className="mt-6 flex flex-wrap gap-3">
            <a href="#shorten" className="inline-flex h-11 items-center rounded-full bg-primary px-5 text-sm font-medium text-primary-foreground">
              Shorten a link
            </a>
            <Link href="/links" className="inline-flex h-11 items-center rounded-full border border-border px-5 text-sm font-medium">
              Manage your links
            </Link>
          </div>
        </div>
        <div id="shorten">
          <ShortenForm />
        </div>
      </section>

      <section id="how" className="border-y border-border bg-card">
        <div className="mx-auto grid w-full max-w-6xl gap-8 px-4 py-14 sm:px-6 md:grid-cols-3">
          {[
            ["Paste a destination", "Ziplink accepts http and https URLs and rejects other schemes before anything is stored."],
            ["Share the short link", "You get a stable code. Open it, or copy it. The redirect does not wait on analytics work."],
            ["Keep control", "Sign in to keep a private library. Disable or delete a link when you are done."],
          ].map(([title, copy], index) => (
            <article key={title}>
              <p className="font-mono text-sm text-primary">0{index + 1}</p>
              <h2 className="mt-2 font-display text-2xl">{title}</h2>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">{copy}</p>
            </article>
          ))}
        </div>
      </section>

      <section id="features" className="mx-auto w-full max-w-6xl px-4 py-16 sm:px-6">
        <h2 className="font-display text-4xl tracking-tight">Built for links you actually operate</h2>
        <div className="mt-8 grid gap-4 md:grid-cols-2">
          {features.map((feature) => {
            const Icon = feature.icon;
            return (
            <article key={feature.title} className="rounded-3xl border border-border bg-card p-6">
              <Icon aria-hidden="true" className="h-5 w-5 text-primary" />
              <h3 className="mt-4 font-display text-2xl">{feature.title}</h3>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">{feature.copy}</p>
            </article>
            );
          })}
        </div>
      </section>

      <section className="mx-auto w-full max-w-6xl px-4 pb-16 sm:px-6">
        <div className="rounded-3xl bg-foreground px-6 py-10 text-background sm:px-10">
          <h2 className="font-display text-4xl text-background">Keep the links with your account</h2>
          <p className="mt-3 max-w-2xl text-sm leading-6 text-background/75">
            Create an account to see your own counts and links. Without one, a short link stays with this browser through an HTTP-only cookie.
          </p>
          <Link href="/register" className="mt-6 inline-flex h-11 items-center rounded-full bg-primary px-5 text-sm font-medium text-primary-foreground">
            Create an account
          </Link>
        </div>
      </section>
    </div>
  );
}
