import { describe, it, expect } from 'vitest'
import { expertStatusLabel, taskStatusLabel, roleNameLabel, mapStatLabels } from './labels'

describe('labels', () => {
  it('maps expert status', () => {
    expect(expertStatusLabel('PENDING')).toBe('待审核')
    expect(expertStatusLabel('ACTIVE')).toBe('正常')
  })

  it('maps task status', () => {
    expect(taskStatusLabel('PUBLISHED')).toBe('已发布')
  })

  it('maps stat object keys to Chinese labels', () => {
    expect(mapStatLabels({ IN_REVIEW: 2, UNASSIGNED: 1 }, taskStatusLabel)).toEqual({
      评审中: 2,
      待分配: 1
    })
  })

  it('prefers db role name when valid', () => {
    expect(roleNameLabel('ADMIN', '系统管理员')).toBe('系统管理员')
    expect(roleNameLabel('ADMIN', null)).toBe('系统管理员')
  })
})
