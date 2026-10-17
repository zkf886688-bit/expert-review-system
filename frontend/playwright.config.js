import { defineConfig } from '@playwright/test'

const e2eBackendPort = process.env.E2E_BACKEND_PORT || 8090
const backendBase = `http://127.0.0.1:${e2eBackendPort}`
const isWin = process.platform === 'win32'
const backendCmd = isWin
  ? `mvn -s ..\\maven-settings.xml -q test-compile spring-boot:test-run -Dspring-boot.run.profiles=test -Dspring-boot.run.arguments=--server.port=${e2eBackendPort}`
  : `mvn -s ../maven-settings.xml -q test-compile spring-boot:test-run -Dspring-boot.run.profiles=test -Dspring-boot.run.arguments=--server.port=${e2eBackendPort}`

export default defineConfig({
  testDir: './e2e',
  timeout: 60000,
  use: {
    baseURL: 'http://127.0.0.1:4173',
    headless: true,
    ...(process.env.CI ? {} : { channel: 'chrome' })
  },
  webServer: [
    {
      command: backendCmd,
      cwd: '../backend',
      url: `${backendBase}/api/public/health`,
      reuseExistingServer: false,
      stdout: 'pipe',
      env: {
        SPRING_PROFILES_ACTIVE: 'test',
        SPRING_DATASOURCE_URL: 'jdbc:h2:mem:expert_review;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE',
        SPRING_DATASOURCE_DRIVER_CLASS_NAME: 'org.h2.Driver',
        APP_CAPTCHA_ENABLED: 'false'
      },
      timeout: 180000
    },
    {
      command: 'npm run preview -- --host 127.0.0.1 --port 4173',
      url: 'http://127.0.0.1:4173',
      reuseExistingServer: false,
      timeout: 120000,
      env: {
        E2E_API_TARGET: backendBase
      }
    }
  ]
})

