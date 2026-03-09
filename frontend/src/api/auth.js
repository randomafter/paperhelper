import request from './request'

export const authApi = {
  login(data) {
    return request.post('/auth/login', data)
  },
  register(data) {
    return request.post('/auth/register', data)
  },
  getSecurityQuestion(username) {
    return request.get('/auth/security-question', { params: { username } })
  },
  verifySecurityAnswer(data) {
    return request.post('/auth/verify-answer', data)
  },
  resetPasswordByAnswer(data) {
    return request.post('/auth/reset-password-by-answer', data)
  },
  getProfile() {
    return request.get('/users/me')
  },
  updateProfile(params) {
    return request.put('/users/me', null, { params })
  },
  changePassword(data) {
    return request.post('/users/me/password', data)
  },
}
