import request from './request'

export const feedbackApi = {
  submitFeedback(data) {
    return request.post('/feedback/submit', data)
  },
  getAllFeedback() {
    return request.get('/feedback/all')
  },
  getFeedbackByStatus(status) {
    return request.get(`/feedback/status/${status}`)
  },
  markAsRead(id) {
    return request.post(`/feedback/${id}/read`)
  },
  markAsResolved(id) {
    return request.post(`/feedback/${id}/resolved`)
  },
}
