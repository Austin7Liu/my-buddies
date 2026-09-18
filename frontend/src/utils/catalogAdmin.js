const CODE_PATTERNS = {
  category: /^[a-z][a-z0-9-]{1,31}$/,
  topic: /^[a-z][a-z0-9-]{1,63}$/,
}

export function validateCatalogEditor(type, form, editing = false) {
  if (!editing && !CODE_PATTERNS[type]?.test(form.code?.trim() ?? '')) {
    return `${type === 'category' ? '分类' : '话题'}编码格式不正确`
  }
  const name = form.name?.trim() ?? ''
  if (!name) return '名称不能为空'
  if (name.length > 64) return '名称不能超过 64 个字符'
  if ((form.description?.trim() ?? '').length > 255) return '描述不能超过 255 个字符'
  if (!Number.isInteger(form.sortOrder) || form.sortOrder < 0) return '排序值必须是大于等于 0 的整数'
  return ''
}

export function catalogPayload(form, editing = false) {
  const payload = {
    name: form.name.trim(),
    description: form.description?.trim() || null,
    sortOrder: form.sortOrder,
  }
  if (!editing) payload.code = form.code.trim()
  return payload
}
