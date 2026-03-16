import request from './request'

export const worksApi = {
  list(page = 1, size = 20) {
    return request.get('/works', { params: { page, size } })
  },
  get(id) {
    return request.get(`/works/${id}`)
  },
  create(data = {}) {
    return request.post('/works', data)
  },
  save(id, data) {
    return request.put(`/works/${id}`, data)
  },
  updateGroup(id, groupName) {
    return request.put(`/works/${id}/group`, { groupName })
  },
  delete(id) {
    return request.delete(`/works/${id}`)
  },
}

export const workGroupsApi = {
  list() {
    return request.get('/work-groups')
  },
  create(name) {
    return request.post('/work-groups', { name })
  },
  rename(id, name) {
    return request.put(`/work-groups/${id}`, { name })
  },
  delete(id) {
    return request.delete(`/work-groups/${id}`)
  },
}

export const favoriteGroupsApi = {
  list() {
    return request.get('/favorite-groups')
  },
  create(name) {
    return request.post('/favorite-groups', { name })
  },
  rename(id, name) {
    return request.put(`/favorite-groups/${id}`, { name })
  },
  delete(id) {
    return request.delete(`/favorite-groups/${id}`)
  },
}

