import { expect, test } from '@playwright/test'
import { createScreenshotCapture } from '@taonity/demo-screenshots'

const capture = createScreenshotCapture(expect)
const scenes = [
  { name: 'players', tab: 'players', ready: 'Atlas' },
  { name: 'replays', tab: 'replays', ready: 'Demo Alpine Pass' },
  { name: 'replay-details', tab: 'replays', ready: 'Demo Alpine Pass' },
  { name: 'gentool', tab: 'gentool', ready: 'Your linked players' },
  { name: 'collection', tab: 'collection', ready: 'Refresh benchmarks' },
  { name: 'config', tab: 'config', ready: 'Search settings' },
  { name: 'admin', tab: 'admin', ready: 'Pending access requests' },
  { name: 'about', tab: 'about', ready: 'GenTool Data Viewer' },
] as const

for (const scene of scenes) {
  test.describe(scene.name, () => {
    test('capture', async ({ page, baseURL }, testInfo) => {
      const failures: string[] = []
      page.on('pageerror', error => failures.push(error.message))
      page.on('response', response => {
        if (response.url().startsWith(`${baseURL}/api/`) && response.status() >= 400) failures.push(`${response.status()} ${response.url()}`)
      })
      await page.clock.setFixedTime(new Date('2026-11-08T10:30:00Z'))
      await page.addInitScript(theme => localStorage.setItem('theme', theme), testInfo.project.use.colorScheme === 'dark' ? 'dark' : 'light')
      await page.route('**/api/actuator/{info,backend}', async route => {
        const response = await route.fetch()
        const data = await response.json()
        if (data.build?.time) data.build.time = '2026-11-08T10:30:00.000Z'
        if (data.runtime) data.runtime.uptime = 3600
        await route.fulfill({ response, json: data })
      })
      await page.goto('/?tab=players')
      await expect(page.getByText('OWNER', { exact: true })).toBeVisible()
      if (scene.tab !== 'players') {
        const label = { replays: 'Replays', gentool: 'My GenTool', collection: 'Collection', config: 'Config', admin: 'Admin', about: 'About' }[scene.tab]
        if (testInfo.project.name.startsWith('phone')) {
          await page.getByRole('combobox').filter({ hasText: /^Players/ }).click()
          await page.getByRole('option', { name: label, exact: true }).click()
        } else {
          await page.getByRole('tab', { name: new RegExp(`^${label}`) }).click()
        }
      }
      await page.addStyleTag({ content: '*, *::before, *::after { animation: none !important; transition: none !important; }' })
      if (scene.name === 'config') await expect(page.locator('input[placeholder^="Search settings"]:visible')).toBeVisible()
      else if (scene.name === 'collection') await expect(page.getByRole('button', { name: scene.ready, exact: true })).toBeVisible()
      else await expect(page.getByText(scene.ready, { exact: true }).first()).toBeVisible()

      if (scene.tab === 'replays') {
        await page.getByRole('combobox').filter({ hasText: /^50/ }).click()
        await page.getByRole('option', { name: '20', exact: true }).click()
        await expect(page.getByRole('button', { name: 'Expand row', exact: true })).toHaveCount(20)
        await page.evaluate(() => scrollTo(0, 0))
      }
      if (scene.name === 'replay-details') {
        await page.getByRole('button', { name: 'Expand row', exact: true }).first().click()
        await expect(page.getByRole('heading', { name: 'Teams', exact: true })).toBeVisible()
        await expect(page.getByRole('heading', { name: 'All parsed fields', exact: true })).toBeVisible()
      }
      await expect(page.locator('[data-slot="skeleton"]:visible')).toHaveCount(0)
      await expect(page.locator('.error-notification')).toHaveCount(0)
      const text = await page.locator('main, .login-card').allInnerTexts()
      expect(text.join('\n')).not.toMatch(/\p{Script=Cyrillic}|\p{Script=Han}|\p{Script=Arabic}/u)
      expect(await page.evaluate(() => document.documentElement.scrollWidth)).toBeLessThanOrEqual(testInfo.project.use.viewport!.width)
      expect(failures).toEqual([])
      if (testInfo.project.name.startsWith('desktop')) {
        const clip = await page.locator('main main').evaluate(element => {
          const bounds = element.getBoundingClientRect()
          return { x: Math.floor(bounds.left) - 16, y: 0, width: Math.ceil(bounds.width) + 32, height: Math.max(document.documentElement.scrollHeight, document.body.scrollHeight, innerHeight) }
        })
        await capture(page, testInfo, scene.name, { clip })
      } else {
        await capture(page, testInfo, scene.name)
      }
    })
  })
}
