import { describe, expect, it } from 'vitest'
import { catalogPayload, validateCatalogEditor } from './catalogAdmin.js'

describe('catalog administration helpers', () => {
  it('validates immutable creation codes and common fields', () => {
    expect(validateCatalogEditor('category', { code: 'Sports', name: '运动', description: '', sortOrder: 0 })).toBe('分类编码格式不正确')
    expect(validateCatalogEditor('topic', { code: 'tennis', name: '', description: '', sortOrder: 0 })).toBe('名称不能为空')
    expect(validateCatalogEditor('topic', { code: 'tennis', name: '网球', description: '', sortOrder: -1 })).toBe('排序值必须是大于等于 0 的整数')
    expect(validateCatalogEditor('topic', { code: 'tennis', name: '网球', description: '', sortOrder: 1 })).toBe('')
  })

  it('omits code from update payloads', () => {
    const form = { code: 'tennis', name: ' 网球 ', description: ' ', sortOrder: 2 }
    expect(catalogPayload(form, true)).toEqual({ name: '网球', description: null, sortOrder: 2 })
    expect(catalogPayload(form, false).code).toBe('tennis')
  })
})
