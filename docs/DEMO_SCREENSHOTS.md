# Demo screenshots

Requires Java 17, Maven, Node.js 24+, and Python 3. Use a Linux checkout in WSL for the same browser rendering as CI.

```bash
cd frontend
npm ci
npx playwright install --with-deps chromium
npm run demo:screenshots
```

Open `screenshots/index.html` from the repository root. Captures include full-page desktop and phone views in light and dark, at 4× pixel density.

The `demo` backend profile uses an isolated in-memory H2 database, local Discord stubs, fictional players and replays, and a fixed clock. Scheduled jobs are disabled; no production services or credentials are needed. Ports 3000, 8080, and 9561 must be free.

Edit scenes in `frontend/demo/screenshots.spec.ts`. Fixtures live in the existing feature demo contributors.

Run **Demo Screenshots** manually in GitHub Actions, or let **Release** call it after a successful production deployment. Main-branch captures publish `screenshots.zip` in a `portfolio-screenshots-<commit-SHA>` prerelease. The portfolio imports the latest bundle during its build.
