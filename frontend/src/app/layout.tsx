import './globals.css'
import type { Metadata, Viewport } from 'next'
import { connection } from 'next/server'
import { getAppIdentity } from '@/lib/appIdentity'
import { Geist } from 'next/font/google'
import { cn } from '@/lib/utils'
import { ThemeProvider } from '@/components/theme/ThemeProvider'
import { themeInitScript } from '@/components/theme/themeInit'
import { ThemeToggle } from '@/components/theme/ThemeToggle'
import { DevToolsPanel } from '@/features/console/DevToolsPanel'
import { AppFooter } from '@/components/AppFooter'

const geist = Geist({ subsets: ['latin'], variable: '--font-sans' })

export async function generateMetadata(): Promise<Metadata> {
  await connection()
  const identity = getAppIdentity()
  return {
    description: 'Browse and manage GenTool replay data with optional Discord authentication.',
    title: identity.name, applicationName: identity.name, manifest: '/manifest.webmanifest',
    icons: {
      icon: [{ url: identity.favicon, sizes: '16x16 32x32 48x48' }],
      apple: [{ url: `${identity.iconPrefix}-180.png`, sizes: '180x180', type: 'image/png' }],
    },
    appleWebApp: { capable: true, title: identity.name, statusBarStyle: 'default' },
  }
}

export const viewport: Viewport = { themeColor: '#0a0a0a' }

export default function RootLayout({
  children,
}: {
  children: React.ReactNode,
}) {
  return (
    <html lang="en" suppressHydrationWarning className={cn('font-sans', geist.variable)}>
      <head>
        <script id="theme-init" dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body className="flex min-h-svh flex-col">
        <ThemeProvider>
          <ThemeToggle className="absolute right-3 top-2 z-50 min-h-11 min-w-11 sm:top-3 sm:min-h-0 sm:min-w-0" />
          <div className="flex flex-1 flex-col">{children}</div>
          <AppFooter />
          <DevToolsPanel />
        </ThemeProvider>
      </body>
    </html>
  )
}
