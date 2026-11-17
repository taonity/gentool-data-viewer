import type { Metadata } from 'next'
import { BUSINESS_DETAILS } from '@/config/business'

export const metadata: Metadata = { title: 'Contact | GenTool Data Viewer' }

export default function ContactPage() {
  return (
    <article className="mx-auto w-full max-w-4xl space-y-4 px-5 py-12 sm:px-8 sm:py-16">
      <h1 className="text-3xl font-semibold tracking-tight sm:text-4xl">Contact</h1>
      <p className="text-sm leading-relaxed text-muted-foreground">Get in touch with {BUSINESS_DETAILS.name}.</p>
      <dl className="grid grid-cols-[auto_minmax(0,1fr)] gap-x-6 gap-y-3 text-sm leading-relaxed">
        <dt className="text-muted-foreground">Email</dt>
        <dd><a className="rounded-sm underline underline-offset-4 focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-ring" href={`mailto:${BUSINESS_DETAILS.email}`}>{BUSINESS_DETAILS.email}</a></dd>
      </dl>
    </article>
  )
}
