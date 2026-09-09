import type { Metadata } from 'next'
import Link from 'next/link'

export const metadata: Metadata = {
  title: 'Privacy Policy | GenTool Data Viewer',
  description: 'How GenTool Data Viewer collects, uses, shares, and deletes personal data.',
}

const headingClass = 'mt-10 scroll-mt-6 text-xl font-semibold'
const paragraphClass = 'mt-3 leading-7 text-muted-foreground'
const listClass = 'mt-3 list-disc space-y-2 pl-6 leading-7 text-muted-foreground marker:text-foreground'
const linkClass = 'font-medium text-foreground underline underline-offset-4'

export default function PrivacyPolicyPage() {
  return (
    <article className="mx-auto w-full max-w-4xl px-5 py-12 sm:px-8 sm:py-16">
      <div className="max-w-2xl">
        <p className="text-sm font-medium text-muted-foreground">Effective September 9, 2026</p>
        <h1 className="mt-3 text-4xl font-semibold tracking-normal sm:text-5xl">Privacy Policy</h1>
        <p className="mt-5 text-lg leading-8 text-muted-foreground">
          This policy explains how Taonity processes personal data when you use GenTool Data Viewer,
          including when you sign in with Discord. Taonity is an independent project and is the
          controller of the data described here.
        </p>
      </div>

      <section aria-labelledby="scope">
        <h2 className={headingClass} id="scope">1. Scope and contact</h2>
        <p className={paragraphClass}>
          This policy applies to the GenTool Data Viewer website, its API, and its Discord
          integration (the “Service”). Questions, privacy requests, and account or data deletion
          requests may be sent to{' '}
          <a className={linkClass} href="mailto:taonity.org@gmail.com">taonity.org@gmail.com</a>.
          The Service is not affiliated with or endorsed by Discord or GenTool.
        </p>
      </section>

      <section aria-labelledby="data-collected">
        <h2 className={headingClass} id="data-collected">2. Data we collect</h2>
        <ul className={listClass}>
          <li>
            <strong className="text-foreground">Discord profile data.</strong> If you sign in, the
            Service receives your Discord user ID, username or global display name, and avatar from
            Discord using the <code className="text-foreground">identify</code> OAuth scope. It does
            not request your email, messages, contacts, server memberships, or password. An
            administrator may also retrieve the same basic profile fields for a Discord user ID
            supplied for player-account linking.
          </li>
          <li>
            <strong className="text-foreground">Account and activity data.</strong> The Service
            stores access status, assigned role, access requests, links between a Discord account
            and a GenTool player, rescan requests, and administrative actions.
          </li>
          <li>
            <strong className="text-foreground">GenTool replay data.</strong> The Service collects
            publicly available records from GenTool, including player and reporter identifiers and
            names, replay and game details, network addresses recorded in replays, system and CPU
            information, source URLs, and associated-file metadata. This data may concern people
            who do not have a Service account.
          </li>
          <li>
            <strong className="text-foreground">Technical data.</strong> Servers may process IP
            address, request time and path, browser or user-agent information, response status,
            security events, and diagnostic logs to operate and protect the Service.
          </li>
        </ul>
      </section>

      <section aria-labelledby="uses">
        <h2 className={headingClass} id="uses">3. How and why we use data</h2>
        <ul className={listClass}>
          <li>Authenticate users and maintain sessions.</li>
          <li>Administer access, roles, player links, replay collection, and rescan limits.</li>
          <li>Display, search, compare, and maintain GenTool replay and hardware information.</li>
          <li>Prevent abuse, investigate incidents, debug failures, and keep an audit trail.</li>
          <li>Meet legal obligations and enforce the <Link className={linkClass} href="/terms">Terms of Service</Link>.</li>
        </ul>
        <p className={paragraphClass}>
          Where a legal basis is required, processing is based on providing the Service you request,
          Taonity’s legitimate interests in operating and securing the Service and maintaining
          accurate community replay records, compliance with legal obligations, or consent where
          the law requires it. You may object to processing based on legitimate interests by using
          the contact above.
        </p>
      </section>

      <section aria-labelledby="disclosure">
        <h2 className={headingClass} id="disclosure">4. Disclosure and public visibility</h2>
        <p className={paragraphClass}>Data may be disclosed only as follows:</p>
        <ul className={listClass}>
          <li>
            Discord processes authentication and profile requests under its own{' '}
            <a className={linkClass} href="https://discord.com/privacy">Privacy Policy</a>.
          </li>
          <li>
            Infrastructure providers may process data solely to host, store, monitor, back up, and
            secure the Service on Taonity’s behalf.
          </li>
          <li>
            Replay, player, hardware, and linked Discord display information may be visible to
            visitors or authorized users as part of the Service’s stated functionality.
          </li>
          <li>
            Data may be disclosed when required by law, to protect rights and safety, or when you
            expressly direct the disclosure.
          </li>
        </ul>
        <p className={paragraphClass}>
          Taonity does not sell personal data, share Discord API data with data brokers or advertising
          networks, use it for targeted advertising, or use it to train artificial intelligence models.
        </p>
      </section>

      <section aria-labelledby="cookies">
        <h2 className={headingClass} id="cookies">5. Cookies and local storage</h2>
        <p className={paragraphClass}>
          The Service uses only functionality and security storage: an HTTP-only session cookie
          (configured to expire after seven days), a CSRF-protection cookie, temporary OAuth or
          authentication-error state, and browser local storage for your theme preference. These are
          not used for advertising or cross-site tracking. Blocking them may prevent login or other
          protected actions.
        </p>
      </section>

      <section aria-labelledby="retention">
        <h2 className={headingClass} id="retention">6. Retention and deletion</h2>
        <ul className={listClass}>
          <li>Authentication sessions are configured to expire after seven days.</li>
          <li>Administrative audit records are deleted after 14 days.</li>
          <li>
            Discord profile and account data is retained only while needed to provide account,
            access-control, and player-linking functions. It is promptly deleted when no longer
            needed, when Discord requires deletion, when the Service closes, or following a valid
            deletion request, unless retention is legally required.
          </li>
          <li>
            GenTool replay and player records are retained while they remain useful for the Service’s
            historical data-viewing purpose. They are reviewed or removed when inaccurate,
            unlawfully held, no longer needed, or subject to a valid request.
          </li>
          <li>
            Security logs and backups are retained for the shortest period reasonably needed for
            operations, incident response, and legal obligations, then deleted or overwritten.
          </li>
        </ul>
      </section>

      <section aria-labelledby="rights">
        <h2 className={headingClass} id="rights">7. Your choices and rights</h2>
        <p className={paragraphClass}>
          Depending on where you live, you may ask to access, correct, update, export, restrict,
          object to, or delete your personal data, or withdraw consent. You may also complain to
          your local data-protection authority. Taonity does not discriminate against users for
          exercising privacy rights.
        </p>
        <p className={paragraphClass}>
          To make a request, email <a className={linkClass} href="mailto:taonity.org@gmail.com">taonity.org@gmail.com</a>{' '}
          from an address where you can receive a reply and include your Discord user ID or the
          GenTool record involved. Additional information will be requested only when reasonably
          necessary to verify the request. Disconnecting the app in Discord stops future OAuth
          access but does not itself delete stored data; use the same email address to request
          deletion. Taonity will respond within the period required by applicable law.
        </p>
      </section>

      <section aria-labelledby="transfers">
        <h2 className={headingClass} id="transfers">8. International processing</h2>
        <p className={paragraphClass}>
          The Service is available internationally, and data may be processed in countries other
          than your own. Where required, Taonity relies on legally recognized safeguards for such
          transfers. Discord API data transferred from the EEA or United Kingdom is also governed
          by the transfer terms incorporated into the Discord Developer Terms of Service.
        </p>
      </section>

      <section aria-labelledby="security">
        <h2 className={headingClass} id="security">9. Security</h2>
        <p className={paragraphClass}>
          Taonity uses reasonable technical and organizational safeguards, including encrypted
          network transport in production, restricted administrative access, HTTP-only and secure
          session cookies, CSRF protection, access controls, and data minimization. No system is
          completely secure. If an incident creates a legally reportable risk, affected users and
          relevant authorities will be notified as required.
        </p>
      </section>

      <section aria-labelledby="children">
        <h2 className={headingClass} id="children">10. Children</h2>
        <p className={paragraphClass}>
          The Service is not directed to anyone under 13 or under the higher minimum age required
          in their country. Users who authenticate with Discord must also meet Discord’s age
          requirements. Contact Taonity if you believe data from an ineligible child has been
          processed so it can be deleted.
        </p>
      </section>

      <section aria-labelledby="changes">
        <h2 className={headingClass} id="changes">11. Changes to this policy</h2>
        <p className={paragraphClass}>
          This policy may change as the Service, law, or data practices change. The updated version
          will be posted here with a new effective date. Material changes will receive additional
          notice when reasonably possible.
        </p>
      </section>
    </article>
  )
}