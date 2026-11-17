import Link from 'next/link'
import { getAppIdentity } from '@/lib/appIdentity'

export default function BusinessLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="flex min-w-0 flex-1 flex-col">
      <header className="border-b bg-background/95">
        <div className="mx-auto flex w-full max-w-4xl flex-wrap items-center justify-between gap-x-4 gap-y-1 py-3 pr-14 pl-5 sm:pr-16 sm:pl-8">
          <Link href="/" className="inline-flex min-h-9 items-center rounded-sm font-semibold focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring">{getAppIdentity().name}</Link>
          <nav aria-label="Legal documents" className="flex items-center gap-5 text-sm text-muted-foreground">
            <Link href="/privacy" className="inline-flex min-h-9 items-center rounded-sm underline-offset-4 hover:text-foreground hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring">Privacy</Link>
            <Link href="/terms" className="inline-flex min-h-9 items-center rounded-sm underline-offset-4 hover:text-foreground hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring">Terms</Link>
          </nav>
        </div>
      </header>
      <main className="min-w-0 flex-1 wrap-anywhere">{children}</main>
    </div>
  )
}
