import { defineScreenshotConfig } from '@taonity/demo-screenshots'

export default defineScreenshotConfig({
  configDir: __dirname,
  title: 'GenTool Data Viewer demo screenshots',
  screenshotDir: '../screenshots',
  globalSetup: './demo/setup.ts',
  use: {
    baseURL: 'http://127.0.0.1:3000',
    storageState: './test-results/demo-owner.json',
  },
  webServer: [
    {
      command: 'node scripts/demo-server.mjs backend',
      stdout: 'pipe',
      url: 'http://127.0.0.1:8080/actuator/health/readiness',
      timeout: 120_000,
      reuseExistingServer: false,
      gracefulShutdown: { signal: 'SIGTERM', timeout: 10_000 },
    },
    {
      command: 'node scripts/demo-server.mjs frontend',
      url: 'http://127.0.0.1:3000/login',
      timeout: 60_000,
      reuseExistingServer: false,
      gracefulShutdown: { signal: 'SIGTERM', timeout: 10_000 },
    },
  ],
})
