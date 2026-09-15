import http from './http.js'

export const listCategories = () => http.get('/categories')
export const listTopics = (categoryId) => http.get(`/categories/${categoryId}/topics`)
export const getTopic = (topicId) => http.get(`/topics/${topicId}`)
export const followTopic = (topicId) => http.put(`/topics/${topicId}/follow`)
export const unfollowTopic = (topicId) => http.delete(`/topics/${topicId}/follow`)
export const listFollowedTopics = (page = 1, size = 20) => http.get('/me/followed-topics', {
  params: { page, size },
})
