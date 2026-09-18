import http from './http.js'

export const listAdminCategories = () => http.get('/admin/catalog/categories')
export const listAdminTopics = (categoryId) => http.get(`/admin/catalog/categories/${categoryId}/topics`)
export const createAdminCategory = (payload) => http.post('/admin/catalog/categories', payload)
export const updateAdminCategory = (categoryId, payload) => http.put(`/admin/catalog/categories/${categoryId}`, payload)
export const setAdminCategoryEnabled = (categoryId, enabled) => http.patch(`/admin/catalog/categories/${categoryId}/enabled`, { enabled })
export const createAdminTopic = (categoryId, payload) => http.post(`/admin/catalog/categories/${categoryId}/topics`, payload)
export const updateAdminTopic = (topicId, payload) => http.put(`/admin/catalog/topics/${topicId}`, payload)
export const setAdminTopicEnabled = (topicId, enabled) => http.patch(`/admin/catalog/topics/${topicId}/enabled`, { enabled })
