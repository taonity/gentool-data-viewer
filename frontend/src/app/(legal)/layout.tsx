import Link from 'next/link'

const links = [
  { href: '/privacy', label: 'Privacy' },
  { href: '/terms', label: 'Terms' },
]

export default function LegalLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <div className="min-h-screen bg-background text-foreground">
      <header className="border-b bg-background/95">
        <div className="mx-auto flex w-full max-w-4xl flex-wrap items-center justify-between gap-4 py-4 pl-5 pr-14 sm:pl-8 sm:pr-16">
          <Link className="font-semibold tracking-normal" href="/">
            GenTool Data Viewer
          </Link>
          <nav aria-label="Legal documents" className="flex items-center gap-5 text-sm text-muted-foreground">
            {links.map((link) => (
              <Link
                className="underline-offset-4 hover:text-foreground hover:underline"
                href={link.href}
                key={link.href}
              >
                {link.label}
              </Link>
            ))}
          </nav>
        </div>
      </header>

      {children}

      <footer className="border-t">
        <div className="mx-auto flex w-full max-w-4xl flex-wrap gap-x-5 gap-y-2 px-5 py-6 text-sm text-muted-foreground sm:px-8">
          <span>Taonity</span>
          <a className="underline-offset-4 hover:text-foreground hover:underline" href="mailto:taonity.org@gmail.com">
            taonity.org@gmail.com
          </a>
          <Link className="underline-offset-4 hover:text-foreground hover:underline" href="/">
            Return to application
          </Link>
        </div>
      </footer>
    </div>
  )
}