import request from './request'

export const authApi = {
  login(data) {
    return request.post('/auth/login', data)
  },
  register(data) {
    return request.post('/auth/register', data)
  },
  getProfile() {
    return request.get('/users/me')
  },
  updateProfile(params) {
    return request.put('/users/me', null, { params })
  },
}
