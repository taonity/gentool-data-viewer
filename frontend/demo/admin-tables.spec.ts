import { expect, test, type Locator, type Page } from '@playwright/test'
import type { ConsoleRole, PendingRequest, UserSummary } from '../src/features/console/types'

test.use({ storageState: 'test-results/demo-owner.json' })

const table = (page: Page, id: string) => page.locator(`[data-table-id="${id}"]`)
const records = (table: Locator) => table.locator('.record-row:not([aria-hidden])')
async function choose(page: Page, control: Locator, option: string) {
  await control.click()
  await page.getByRole('option', { name: option, exact: true }).click()
}

test.describe('admin tables', () => {
  test('keeps role restrictions and leaves the saved role intact when an edit fails', async ({ page }) => {
    await page.route('**/api/console/access/me', async route => {
      const response = await route.fetch()
      await route.fulfill({ response, json: { ...await response.json(), role: 'ADMIN', isOwner: false } })
    })
    await page.route('**/api/console/users', route => route.fulfill({ json: [
      { googleId: 'owner', discordUserId: '100', displayName: 'Test Owner', role: 'OWNER', accessStatus: 'APPROVED', requestedRole: null },
      { googleId: 'editor', discordUserId: '101', displayName: 'Bob Tester', role: 'EDITOR', accessStatus: 'APPROVED', requestedRole: null },
      { googleId: 'admin', discordUserId: '102', displayName: 'Admin User', role: 'ADMIN', accessStatus: 'APPROVED', requestedRole: null },
    ] }))
    await page.route('**/api/console/users/*/role', route => route.fulfill({ status: 403 }))
    await page.goto('/?tab=admin')
    const usersTable = table(page, 'users')
    await expect(records(usersTable)).toHaveCount(3)
    await expect(usersTable.getByRole('combobox', { name: 'Role for Test Owner', exact: true })).toHaveCount(0)
    await expect(usersTable.getByRole('combobox', { name: 'Role for Admin User', exact: true })).toHaveCount(0)
    const control = usersTable.getByRole('combobox', { name: 'Role for Bob Tester', exact: true }).filter({ visible: true })
    await control.click()
    await expect(page.getByRole('option', { name: 'Admin', exact: true })).toHaveCount(0)
    await expect(page.getByRole('option', { name: 'Owner', exact: true })).toHaveCount(0)
    await page.getByRole('option', { name: 'Viewer', exact: true }).click()
    await expect(page.locator('.error-notification')).toContainText('Failed to change role')
    await expect(control).toContainText('Editor')
  })

  test('searches all records, supports scopes and exact matching, and keeps table preferences separate', async ({ page }) => {
    const users: UserSummary[] = Array.from({ length: 61 }, (_, index) => ({
      googleId: `review-${index + 1}`, displayName: `Person ${index + 1}`, discordUserId: String(BigInt('100000000000000000') + BigInt(index)),
      role: index === 60 ? 'EDITOR' : 'VIEWER', accessStatus: 'APPROVED', requestedRole: null,
    }))
    await page.route('**/api/console/users', route => route.fulfill({ json: users }))
    await page.goto('/?tab=admin')
    const usersTable = table(page, 'users')
    const pending = table(page, 'pending-access-requests')
    const audit = table(page, 'audit-log')
    await expect(records(usersTable)).toHaveCount(50)
    await expect(usersTable.getByRole('combobox', { name: 'Search scope' })).toContainText('Search in:')
    await usersTable.getByRole('button', { name: 'Next page', exact: true }).first().click()
    await expect(records(usersTable)).toHaveCount(11)
    await expect(usersTable.getByRole('status')).toContainText('51–61 of 61')

    await usersTable.getByRole('textbox', { name: 'Search all rows' }).fill('editor')
    await expect(records(usersTable)).toHaveCount(1)
    await expect(records(usersTable)).toContainText('Person 61')
    await choose(page, usersTable.getByRole('combobox', { name: 'Search scope' }), 'Discord user ID')
    await expect(usersTable).toContainText('No rows match your search.')
    await choose(page, usersTable.getByRole('combobox', { name: 'Search scope' }), 'User')
    await usersTable.getByRole('textbox', { name: 'Search all rows' }).fill('Person 1')
    await expect(records(usersTable)).toHaveCount(11)
    await usersTable.getByRole('switch', { name: /Exact/ }).click()
    await expect(records(usersTable)).toHaveCount(1)
    await expect(records(pending)).not.toHaveCount(0)
    await expect(records(audit)).not.toHaveCount(0)

    await usersTable.getByRole('button', { name: 'Choose columns' }).click()
    await page.getByRole('switch', { name: 'Show Discord user ID column' }).click()
    await expect(page.locator('[data-slot="popover-content"]').getByRole('spinbutton')).toHaveCount(0)
    await page.keyboard.press('Escape')
    if (test.info().project.use.viewport!.width >= 1024) {
      const resize = usersTable.getByRole('separator', { name: 'Resize User column', exact: true })
      await resize.focus()
      await page.keyboard.press('ArrowRight')
    }
    await page.reload()
    await expect(records(usersTable)).toHaveCount(50)
    await expect(usersTable.locator('th[data-column-key="discordUserId"]')).toHaveCount(0)
    await expect(pending.locator('th[data-column-key="discordUserId"]')).toHaveCount(1)
    if (test.info().project.use.viewport!.width >= 1024) {
      await expect(usersTable.locator('th[data-column-key="displayName"]')).toHaveAttribute('style', /width: 196px/)
    }
    if (test.info().project.use.viewport!.width >= 1024) {
      await usersTable.locator('thead').getByRole('button', { name: 'User', exact: true }).click()
    } else {
      await usersTable.getByRole('button', { name: 'Sort descending', exact: true }).click()
    }
    await expect(records(usersTable).first()).toContainText('Person 61')
    if (test.info().project.use.viewport!.width >= 1024) {
      await usersTable.locator('thead').getByRole('button', { name: 'User', exact: true }).click()
    } else {
      await usersTable.getByRole('button', { name: 'Sort ascending', exact: true }).click()
    }
    await expect(records(usersTable).first()).toContainText('Person 1')
    await expect(usersTable.getByRole('button', { name: 'Expand row', exact: true })).toHaveCount(0)
    await expect(pending.getByRole('button', { name: 'Expand row', exact: true })).toHaveCount(0)
    await expect(pending.locator('.record-rank, .record-number')).toHaveCount(0)
    await records(usersTable).first().click({ position: { x: 12, y: 8 } })
    await expect(usersTable.locator('.record-detail')).toHaveCount(0)
    await usersTable.getByRole('button', { name: 'Refresh data' }).click()
    await expect(usersTable.locator('[aria-busy="true"]')).toHaveCount(0)
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth)).toBe(true)
  })

  test('preserves grant drafts through queries and refreshes related tables after approval and role edits', async ({ page }) => {
    let requests: PendingRequest[] = [{ googleId: 'review-request', displayName: 'Review Applicant', discordUserId: '100000000000000061', requestedRole: 'VIEWER' }]
    let users: UserSummary[] = [{ ...requests[0]!, role: 'NONE', accessStatus: 'PENDING' }]
    let auditLoads = 0
    let approvedRole: ConsoleRole | undefined
    await page.route('**/api/console/access/requests', route => route.fulfill({ json: requests }))
    await page.route('**/api/console/users', route => route.fulfill({ json: users }))
    await page.route(/\/api\/console\/audit-logs\?/, async route => { auditLoads++; await route.continue() })
    await page.route('**/api/console/access/requests/review-request/approve', async route => {
      approvedRole = route.request().postDataJSON().role
      requests = []
      users = [{ ...users[0]!, role: approvedRole!, accessStatus: 'APPROVED' }]
      await route.fulfill({ status: 204 })
    })
    await page.route('**/api/console/users/review-request/role', async route => {
      users = [{ ...users[0]!, role: route.request().postDataJSON().role }]
      await route.fulfill({ json: users[0] })
    })
    await page.goto('/?tab=admin')
    const pending = table(page, 'pending-access-requests')
    const usersTable = table(page, 'users')
    await expect(records(pending)).toHaveCount(1)
    await expect.poll(() => auditLoads).toBe(1)
    await choose(page, pending.getByRole('combobox', { name: 'Grant role for Review Applicant' }).filter({ visible: true }), 'Editor')
    await pending.getByRole('textbox', { name: 'Search all rows' }).fill('Applicant')
    await expect(pending).toContainText('1 match across all rows')
    await pending.getByRole('button', { name: 'Refresh data' }).click()
    await expect(pending.getByRole('combobox', { name: 'Grant role for Review Applicant' }).filter({ visible: true })).toContainText('Editor')
    await pending.getByRole('button', { name: 'Approve access for Review Applicant' }).filter({ visible: true }).click()
    await expect(records(pending)).toHaveCount(0)
    expect(approvedRole).toBe('EDITOR')
    await expect(records(usersTable)).toContainText('APPROVED')
    await expect.poll(() => auditLoads).toBe(2)
    await choose(page, usersTable.getByRole('combobox', { name: 'Role for Review Applicant', exact: true }).filter({ visible: true }), 'Viewer')
    await expect(usersTable.getByRole('combobox', { name: 'Role for Review Applicant', exact: true }).filter({ visible: true })).toContainText('Viewer')
    await expect.poll(() => auditLoads).toBe(3)
    if (test.info().project.use.viewport!.width < 1024) {
      for (const data of [pending, usersTable, table(page, 'audit-log')]) {
        expect(await data.evaluate(element => element.scrollWidth <= element.clientWidth)).toBe(true)
      }
    }
  })
})
