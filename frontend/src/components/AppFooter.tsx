import Link from 'next/link'
import { BUSINESS_DETAILS } from '@/config/business'

const linkClassName = 'inline-flex min-h-7 items-center rounded-sm underline-offset-4 transition-colors hover:text-foreground hover:underline focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring'

export function AppFooter() {
  return (
    <footer className="site-footer shrink-0 px-2 sm:px-6">
      <div className="mx-auto flex w-full max-w-[1200px] flex-col items-center justify-center gap-x-5 border-t border-border/60 py-1 text-xs text-muted-foreground sm:flex-row sm:flex-wrap">
        <span className="max-w-full text-center wrap-anywhere">Operated by {BUSINESS_DETAILS.name}</span>
        <nav aria-label="Business information" className="flex max-w-full flex-wrap items-center justify-center gap-x-4">
          <Link href="/privacy" className={linkClassName}>Privacy Policy</Link>
          <Link href="/terms" className={linkClassName}>Terms of Service</Link>
          <Link href="/contact" className={linkClassName}>Contact</Link>
        </nav>
      </div>
    </footer>
  )
}
