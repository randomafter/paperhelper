import request from './request'

export const userMaterialApi = {
  // 获取我的自建素材列表
  list() {
    return request.get('/user-materials')
  },
  // 创建草稿
  create(data) {
    return request.post('/user-materials', data)
  },
  // 编辑
  update(id, data) {
    return request.put(`/user-materials/${id}`, data)
  },
  // 提交审核
  submit(id) {
    return request.post(`/user-materials/${id}/submit`)
  },
  // 删除
  delete(id) {
    return request.delete(`/user-materials/${id}`)
  },
  // 管理员：待审核列表
  listPending() {
    return request.get('/user-materials/admin/pending')
  },
  // 管理员：审核通过
  approve(id, comment) {
    return request.post(`/user-materials/admin/${id}/approve`, { comment })
  },
  // 管理员：审核拒绝
  reject(id, comment) {
    return request.post(`/user-materials/admin/${id}/reject`, { comment })
  },
}
