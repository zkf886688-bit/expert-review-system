import { test, expect } from '@playwright/test'

test('public health via API proxy', async ({ request }) => {
  const res = await request.get('/api/public/health')
  expect(res.ok()).toBeTruthy()
  const body = await res.json()
  expect(body.status).toBe('UP')
  expect(body.app).toBeTruthy()
  expect(body.version).toBeTruthy()
})

test('actuator health is reachable', async ({ request }) => {
  const res = await request.get(`http://127.0.0.1:${process.env.E2E_BACKEND_PORT || 8090}/actuator/health`)
  expect(res.ok()).toBeTruthy()
  const body = await res.json()
  expect(body.status).toBe('UP')
})
