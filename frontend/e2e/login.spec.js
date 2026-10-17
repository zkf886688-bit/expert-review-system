import { test, expect } from '@playwright/test'

test('login page renders', async ({ page }) => {
  await page.goto('/login')
  await expect(page.getByPlaceholder('用户名')).toBeVisible()
  await expect(page.getByPlaceholder('密码')).toBeVisible()
  await expect(page.getByRole('button', { name: '进入系统' })).toBeVisible()
  await expect(page.getByRole('heading', { name: '专家评审系统' })).toBeVisible()
  await expect(page.getByText('可追踪 · 可审计的在线评审平台')).toBeVisible()
})
