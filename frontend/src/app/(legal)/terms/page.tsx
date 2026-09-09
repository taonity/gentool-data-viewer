import type { Metadata } from 'next'
import Link from 'next/link'

export const metadata: Metadata = {
  title: 'Terms of Service | GenTool Data Viewer',
  description: 'Terms governing use of GenTool Data Viewer.',
}

const headingClass = 'mt-10 scroll-mt-6 text-xl font-semibold'
const paragraphClass = 'mt-3 leading-7 text-muted-foreground'
const listClass = 'mt-3 list-disc space-y-2 pl-6 leading-7 text-muted-foreground marker:text-foreground'
const linkClass = 'font-medium text-foreground underline underline-offset-4'

export default function TermsPage() {
  return (
    <article className="mx-auto w-full max-w-4xl px-5 py-12 sm:px-8 sm:py-16">
      <div className="max-w-2xl">
        <p className="text-sm font-medium text-muted-foreground">Effective September 9, 2026</p>
        <h1 className="mt-3 text-4xl font-semibold tracking-normal sm:text-5xl">Terms of Service</h1>
        <p className="mt-5 text-lg leading-8 text-muted-foreground">
          These terms govern your use of GenTool Data Viewer, an independent project operated by
          Taonity. By accessing or using the Service, you agree to these terms.
        </p>
      </div>

      <section aria-labelledby="service">
        <h2 className={headingClass} id="service">1. The Service</h2>
        <p className={paragraphClass}>
          GenTool Data Viewer collects and presents publicly available GenTool replay information,
          supports replay collection and player linking, and provides optional access-controlled
          tools through Discord authentication. Some browsing features are available without an
          account. Features and access levels may change or be discontinued.
        </p>
      </section>

      <section aria-labelledby="eligibility">
        <h2 className={headingClass} id="eligibility">2. Eligibility</h2>
        <p className={paragraphClass}>
          You must be at least 13 and meet any higher minimum age required in your country. If local
          law requires consent from a parent or legal guardian, they must agree to these terms for
          you. Discord users must also comply with Discord’s{' '}
          <a className={linkClass} href="https://discord.com/terms">Terms of Service</a> and{' '}
          <a className={linkClass} href="https://discord.com/guidelines">Community Guidelines</a>.
        </p>
      </section>

      <section aria-labelledby="accounts">
        <h2 className={headingClass} id="accounts">3. Accounts and access</h2>
        <ul className={listClass}>
          <li>You are responsible for activity performed through your Discord-authenticated session.</li>
          <li>You must not share access, impersonate another person, or provide a Discord user ID you are not authorized to link.</li>
          <li>Roles and protected features may require approval and may be changed or revoked to protect the Service or its users.</li>
          <li>You must promptly report suspected unauthorized account access to Taonity.</li>
        </ul>
      </section>

      <section aria-labelledby="acceptable-use">
        <h2 className={headingClass} id="acceptable-use">4. Acceptable use</h2>
        <p className={paragraphClass}>You may not use the Service to:</p>
        <ul className={listClass}>
          <li>break applicable law or violate another person’s privacy, publicity, or intellectual-property rights;</li>
          <li>harass, threaten, deceive, discriminate against, or expose sensitive information about another person;</li>
          <li>scrape, bulk extract, resell, profile, or commercialize Discord data or bypass technical limits;</li>
          <li>probe or defeat security, introduce malicious code, disrupt availability, or access data without authorization;</li>
          <li>misrepresent the Service as sponsored, approved, or operated by Discord or GenTool; or</li>
          <li>use Discord APIs or data contrary to Discord’s terms, policies, or user choices.</li>
        </ul>
        <p className={paragraphClass}>
          Report security issues, abuse, policy violations, or unlawful content to{' '}
          <a className={linkClass} href="mailto:taonity.org@gmail.com">taonity.org@gmail.com</a>.
          Reports will be reviewed and appropriate action may be taken.
        </p>
      </section>

      <section aria-labelledby="data">
        <h2 className={headingClass} id="data">5. Replay data and privacy</h2>
        <p className={paragraphClass}>
          Replay records come from third-party public sources and may include user-submitted names,
          addresses, hardware details, and other metadata. Taonity does not guarantee that this data
          is complete, current, accurate, or authorized by every person mentioned. Do not use it to
          identify, contact, harass, or make consequential decisions about anyone.
        </p>
        <p className={paragraphClass}>
          Personal data is handled as described in the{' '}
          <Link className={linkClass} href="/privacy">Privacy Policy</Link>. Send correction,
          removal, privacy, and intellectual-property requests to the contact address above with
          enough detail to identify the relevant record.
        </p>
      </section>

      <section aria-labelledby="ownership">
        <h2 className={headingClass} id="ownership">6. Ownership and third parties</h2>
        <p className={paragraphClass}>
          Taonity retains its rights in the Service’s software, design, and original content. These
          terms do not transfer ownership of Discord, GenTool, game, replay, or user content. Those
          materials remain subject to their owners’ rights and applicable third-party terms. Links
          and integrations are provided for convenience; third-party services are independently
          controlled and may change or become unavailable.
        </p>
      </section>

      <section aria-labelledby="availability">
        <h2 className={headingClass} id="availability">7. Availability and changes</h2>
        <p className={paragraphClass}>
          The Service is a non-commercial pet project provided without a service-level commitment.
          Taonity may modify, suspend, restrict, or discontinue any part of it, including in response
          to legal, security, hosting, Discord, or GenTool requirements. Where reasonably possible,
          material changes affecting users will be communicated in advance.
        </p>
      </section>

      <section aria-labelledby="termination">
        <h2 className={headingClass} id="termination">8. Suspension and termination</h2>
        <p className={paragraphClass}>
          Taonity may restrict or terminate access when you violate these terms, create legal or
          security risk, abuse the Service, or when required by a platform provider or authority.
          You may stop using the Service at any time. To request deletion of your stored Discord
          account data, follow the process in the Privacy Policy. Provisions that by their nature
          should survive termination, including ownership, disclaimers, and liability limits, remain
          in effect.
        </p>
      </section>

      <section aria-labelledby="disclaimer">
        <h2 className={headingClass} id="disclaimer">9. Disclaimers</h2>
        <p className={paragraphClass}>
          To the fullest extent permitted by law, the Service and all data are provided “as is” and
          “as available,” without warranties of accuracy, availability, fitness for a particular
          purpose, non-infringement, or uninterrupted operation. Nothing in these terms excludes a
          warranty or consumer right that cannot lawfully be excluded.
        </p>
      </section>

      <section aria-labelledby="liability">
        <h2 className={headingClass} id="liability">10. Limitation of liability</h2>
        <p className={paragraphClass}>
          To the fullest extent permitted by law, Taonity is not liable for indirect, incidental,
          special, consequential, exemplary, or punitive loss, or loss of data, profits, goodwill,
          or opportunity arising from use of the Service. Taonity’s aggregate liability relating to
          the Service will not exceed the greater of the amount you paid Taonity for the Service in
          the preceding twelve months or EUR 50. These limits do not apply where liability cannot
          legally be limited, including for fraud or deliberate misconduct.
        </p>
      </section>

      <section aria-labelledby="general">
        <h2 className={headingClass} id="general">11. General terms</h2>
        <p className={paragraphClass}>
          Applicable mandatory law governs these terms. Courts with jurisdiction under applicable
          law may hear disputes. If any provision is unenforceable, the remainder stays effective.
          A failure to enforce a provision is not a waiver. You may not transfer your rights under
          these terms without Taonity’s consent. These terms and the Privacy Policy are the entire
          agreement about your use of the Service.
        </p>
      </section>

      <section aria-labelledby="updates-contact">
        <h2 className={headingClass} id="updates-contact">12. Updates and contact</h2>
        <p className={paragraphClass}>
          Updated terms will be posted here with a new effective date. Material changes will receive
          reasonable additional notice when possible. Continuing to use the Service after updated
          terms take effect means you accept them; otherwise, stop using the Service. Contact
          Taonity at <a className={linkClass} href="mailto:taonity.org@gmail.com">taonity.org@gmail.com</a>.
        </p>
      </section>
    </article>
  )
}