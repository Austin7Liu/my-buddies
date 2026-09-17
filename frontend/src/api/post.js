import http from './http.js'

export const listPublicPosts = (page = 1, size = 20) => http.get('/posts', {
  params: { page, size },
})

export const listTopicPosts = (topicId, page = 1, size = 20) => http.get(`/topics/${topicId}/posts`, {
  params: { page, size },
})

export const listCirclePosts = (circleId, page = 1, size = 20) => http.get(`/circles/${circleId}/posts`, {
  params: { page, size },
})

export const listMyPosts = (status, page = 1, size = 20) => http.get('/posts/mine', {
  params: { status: status || undefined, page, size },
})

export const createPost = (payload) => http.post('/posts', payload)

export const updatePost = (postId, content) => http.put(`/posts/${postId}`, { content })

export const deletePost = (postId) => http.delete(`/posts/${postId}`)

export const getPost = (postId) => http.get(`/posts/${postId}`)
export const likePost = (postId) => http.put(`/posts/${postId}/like`)
export const unlikePost = (postId) => http.delete(`/posts/${postId}/like`)
export const bookmarkPost = (postId) => http.put(`/posts/${postId}/bookmark`)
export const removePostBookmark = (postId) => http.delete(`/posts/${postId}/bookmark`)

export const listPostComments = (postId, page = 1, size = 20) => http.get(`/posts/${postId}/comments`, {
  params: { page, size },
})
export const getPostCommentLocation = (commentId, size = 20) => http.get(
  `/post-comments/${commentId}/location`, { params: { size } },
)
export const createPostComment = (postId, content, parentCommentId = null) => http.post(
  `/posts/${postId}/comments`, { content, parentCommentId },
)
export const updatePostComment = (postId, commentId, content) => http.put(
  `/posts/${postId}/comments/${commentId}`, { content },
)
export const deletePostComment = (postId, commentId) => http.delete(`/posts/${postId}/comments/${commentId}`)

export const listBookmarkedPosts = (page = 1, size = 20) => http.get('/me/bookmarked-posts', {
  params: { page, size },
})
export const listPostFeed = (page = 1, size = 20) => http.get('/me/post-feed', {
  params: { page, size },
})
