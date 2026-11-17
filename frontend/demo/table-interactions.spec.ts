import { expect, test } from '@playwright/test'
import type { UserSummary } from '../src/features/console/types'

test.use({ storageState: 'test-results/demo-owner.json' })

const users: UserSummary[] = Array.from({ length: 61 }, (_, index) => ({
  googleId: `test-user-${index}`, discordUserId: String(BigInt('100000000000000000') + BigInt(index)),
  displayName: `Person ${index + 1}`, role: 'VIEWER', accessStatus: 'APPROVED', requestedRole: null,
}))

test.describe('table interactions', () => {
  for (const authenticated of [true, false]) {
    test(`keeps the mobile header size during reload (${authenticated ? 'signed in' : 'signed out'})`, async ({ page }) => {
      await page.setViewportSize({ width: 390, height: 844 })
      let release = () => {}
      let pending: Promise<void>
      await page.route('**/api/console/access/me', async route => {
        await pending
        if (authenticated) await route.continue()
        else await route.fulfill({ status: 401 })
      })
      const header = page.locator('.console-header')
      let loadedBounds
      for (const reload of [false, true]) {
        pending = new Promise<void>(resolve => { release = resolve })
        if (reload) await page.reload()
        else await page.goto('/?tab=players')
        await expect(header.locator('[data-slot="skeleton"]')).toBeVisible()
        const loadingBounds = await header.boundingBox()
        if (reload) expect(loadingBounds).toEqual(loadedBounds)
        release()
        await expect(header.getByRole('button', { name: authenticated ? 'Log out' : 'Log in with Discord', exact: true })).toBeVisible()
        if (!authenticated) await expect(header.getByText('Guest', { exact: true })).toBeVisible()
        loadedBounds = await header.boundingBox()
        expect(loadedBounds).toEqual(loadingBounds)
      }
    })
  }

  test('loads demo Discord avatars', async ({ page }) => {
    await page.setViewportSize({ width: 1440, height: 900 })
    await page.goto('/?tab=players')
    const avatars = page.locator('.record-row img')
    await expect(avatars).toHaveCount(9)
    await expect.poll(() => avatars.evaluateAll(elements =>
      elements.every(element => element instanceof HTMLImageElement && element.complete && element.naturalWidth > 0),
    )).toBe(true)
  })

  test('expands aliases without the toggle adding row height', async ({ page }) => {
    await page.setViewportSize({ width: 390, height: 844 })
    await page.goto('/?tab=players')
    const toggle = page.getByRole('button', { name: /^(Show all|Collapse) aliases for Atlas$/ })
    await expect(toggle).toBeVisible()
    const aliases = page.locator('.mobile-aliases').filter({ has: toggle })
    const collapsedHeight = await aliases.evaluate(element => element.getBoundingClientRect().height)
    const heightWithoutToggle = await toggle.evaluate(element => {
      const parent = element.parentElement!
      element.style.display = 'none'
      const height = parent.getBoundingClientRect().height
      element.style.removeProperty('display')
      return height
    })
    expect(collapsedHeight).toBe(heightWithoutToggle)
    const alignment = await aliases.evaluate(element => {
      const text = element.querySelector('.alias-values')!.getBoundingClientRect()
      const button = element.querySelector('button')!.getBoundingClientRect()
      return { textTop: text.top, textBottom: text.bottom, buttonTop: button.top, buttonBottom: button.bottom }
    })
    expect(alignment.buttonTop).toBeGreaterThanOrEqual(alignment.textTop - 1)
    expect(alignment.buttonBottom).toBeLessThanOrEqual(alignment.textBottom + 1)
    await expect(toggle).toHaveText('More')
    const toggleBounds = await toggle.boundingBox()
    await toggle.hover()
    await page.mouse.down()
    expect(await toggle.boundingBox()).toEqual(toggleBounds)
    await page.mouse.up()
    await expect(toggle).toHaveAttribute('aria-expanded', 'true')
    await expect(toggle).toHaveText('Less')
    expect(await toggle.boundingBox()).toEqual(toggleBounds)
    await expect(aliases).toContainText('Atlas_Classic')
    expect(await aliases.evaluate(element => element.getBoundingClientRect().height)).toBeGreaterThan(collapsedHeight)
    await toggle.click()
    await expect(toggle).toHaveAttribute('aria-expanded', 'false')
    expect(await aliases.evaluate(element => element.getBoundingClientRect().height)).toBe(collapsedHeight)
  })

  test('returns focus and scroll to the top pagination after bottom controls load records', async ({ page }) => {
    await page.route('**/api/console/users', route => route.fulfill({ json: users }))
    await page.goto('/?tab=admin')
    const table = page.locator('[data-table-id="users"]')
    const rows = table.locator('.record-row:not([aria-hidden])')
    const top = table.getByRole('navigation', { name: 'Pagination above records' })
    const bottom = table.getByRole('navigation', { name: 'Pagination below records' })
    await expect(rows).toHaveCount(50)
    await bottom.getByRole('button', { name: 'Next page', exact: true }).click()
    await expect(rows).toHaveCount(11)
    await expect(top).toBeFocused()
    await expect(top).toBeInViewport()
    await expect(top).toContainText('51–61 of 61')
    await bottom.getByRole('combobox', { name: 'Rows per page', exact: true }).click()
    await page.getByRole('option', { name: '20', exact: true }).click()
    await expect(rows).toHaveCount(20)
    await expect(top).toBeFocused()
    await expect(top).toBeInViewport()
    await expect(top).toContainText('1–20 of 61')
  })

  test('ignores an older search response that arrives after the latest result', async ({ page }) => {
    let loads = 0
    let release: () => void = () => {}
    const held = new Promise<void>(resolve => { release = resolve })
    await page.route('**/api/console/users', async route => {
      loads++
      if (loads === 2) await held
      await route.fulfill({ json: users })
    })
    await page.goto('/?tab=admin')
    const table = page.locator('[data-table-id="users"]')
    const rows = table.locator('.record-row:not([aria-hidden])')
    const search = table.getByRole('textbox', { name: 'Search all rows' })
    await expect(rows).toHaveCount(50)
    await search.fill('Person 1')
    await expect.poll(() => loads).toBe(2)
    await search.fill('Person 61')
    await expect(rows).toHaveCount(1)
    await expect(rows).toContainText('Person 61')
    const olderResponse = page.waitForResponse(response => response.url().endsWith('/api/console/users'))
    release()
    await olderResponse
    await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))))
    await expect(rows).toHaveCount(1)
    await expect(rows).toContainText('Person 61')
  })

  test('offers an inline retry when loading records fails', async ({ page }) => {
    let failed = false
    await page.route('**/api/console/users', async route => {
      if (!failed) {
        failed = true
        await route.fulfill({ status: 500 })
      } else await route.fulfill({ json: users })
    })
    await page.goto('/?tab=admin')
    const table = page.locator('[data-table-id="users"]')
    await expect(table).toContainText('Could not load records.')
    await table.getByRole('button', { name: 'Retry', exact: true }).click()
    await expect(table.locator('.record-row:not([aria-hidden])')).toHaveCount(50)
    await expect(table.getByRole('button', { name: 'Retry', exact: true })).toHaveCount(0)
  })
})
