import { test, expect } from '@playwright/test'

test('sso mock login flow', async ({ page }) => {
  await page.goto('/login')
  const ssoBtn = page.getByRole('button', { name: /演示 SSO/ })
  if (await ssoBtn.count()) {
    await ssoBtn.click()
    await expect(page).toHaveURL(/dashboard|leader|my-reviews|my-projects/, { timeout: 15000 })
  }
})
