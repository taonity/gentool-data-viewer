import './globals.css'
import type { Metadata } from 'next'
import { Geist } from 'next/font/google'
import Script from 'next/script'
import { cn } from '@/lib/utils'
import { ThemeProvider, themeInitScript } from '@/components/theme/ThemeProvider'
import { ThemeToggle } from '@/components/theme/ThemeToggle'
import { DevToolsPanel } from '@/features/console/DevToolsPanel'

const geist = Geist({ subsets: ['latin'], variable: '--font-sans' })

export const metadata: Metadata = {
  title: 'Gentool Data Viewer',
  description: 'Browse and manage GenTool replay data with optional Discord authentication.',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode,
}) {
  return (
    <html lang="en" suppressHydrationWarning className={cn('font-sans', geist.variable)}>
      <body>
        <Script id="theme-init" strategy="beforeInteractive">
          {themeInitScript}
        </Script>
        <ThemeProvider>
          <ThemeToggle className="fixed right-3 top-3 z-50" />
          <main>{children}</main>
          <DevToolsPanel />
        </ThemeProvider>
      </body>
    </html>
  )
}
