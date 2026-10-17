import { test, expect } from '@playwright/test'

test('admin login reaches dashboard', async ({ page }) => {
  await page.goto('/login')
  await page.getByPlaceholder('用户名').fill('admin')
  await page.getByPlaceholder('密码').fill('admin123')
  // The isolated test profile disables captcha verification; the UI still requires input.
  await expect(page.getByAltText('验证码')).toBeVisible()
  await page.getByPlaceholder('验证码').fill('0000')
  const loginResponse = page.waitForResponse(r => r.url().endsWith('/api/auth/login'))
  await page.getByRole('button', { name: '进入系统' }).click()
  const loginResult = await (await loginResponse).json()
  expect(loginResult.code, loginResult.message).toBe(200)
  await expect(page).toHaveURL(/\/dashboard/)
  await expect(page.getByRole('heading', { name: '评审任务整体进展一览' })).toBeVisible()
})

test('applicant login reaches my projects', async ({ page }) => {
  await page.goto('/login')
  await page.getByPlaceholder('用户名').fill('applicant')
  await page.getByPlaceholder('密码').fill('applicant123')
  await expect(page.getByAltText('验证码')).toBeVisible()
  await page.getByPlaceholder('验证码').fill('0000')
  const loginResponse = page.waitForResponse(r => r.url().endsWith('/api/auth/login'))
  await page.getByRole('button', { name: '进入系统' }).click()
  const loginResult = await (await loginResponse).json()
  expect(loginResult.code, loginResult.message).toBe(200)
  await expect(page).toHaveURL(/\/my-projects/)
  await expect(page.getByRole('heading', { name: '我的申报项目' })).toBeVisible()
})
