import request from './request'

export const notificationApi = {
  list() {
    return request.get('/notifications')
  },
  unreadCount() {
    return request.get('/notifications/unread-count')
  },
  markRead(id) {
    return request.post(`/notifications/${id}/read`)
  },
  markAllRead() {
    return request.post('/notifications/read-all')
  },
}
