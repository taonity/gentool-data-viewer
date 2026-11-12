import { chromium, expect } from '@playwright/test'
import { mkdir } from 'node:fs/promises'

export default async function setup() {
  await mkdir('test-results', { recursive: true })
  const browser = await chromium.launch()
  try {
    const context = await browser.newContext()
    const page = await context.newPage()
    await page.goto('http://127.0.0.1:8080/oauth2/authorization/discord-gentool-data-viewer')
    await expect(page).toHaveURL(/http:\/\/127\.0\.0\.1:3000\//)
    await expect(page.getByRole('tab', { name: /^Admin/ })).toBeVisible()
    await expect(page.getByText('Atlas', { exact: true }).filter({ visible: true }).first()).toBeVisible()
    await context.storageState({ path: 'test-results/demo-owner.json' })
    await context.close()
  } finally {
    await browser.close()
  }
}
