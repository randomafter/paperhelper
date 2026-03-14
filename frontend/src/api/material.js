import request from './request'

// 统一的素材接口封装，兼容老的 AdminMaterials 调用方式
export const materialApi = {
  // 新接口命名
  search(params) {
    return request.get('/materials', { params })
  },
  getById(id) {
    return request.get(`/materials/${id}`)
  },
  create(data) {
    return request.post('/materials', data)
  },
  update(data) {
    return request.put('/materials', data)
  },
  remove(id) {
    return request.delete(`/materials/${id}`)
  },
  toggleFavorite(id) {
    return request.post(`/materials/${id}/favorite`)
  },
  getFavorites() {
    return request.get('/materials/favorites')
  },
  updateFavoriteGroup(id, groupName) {
    return request.post(`/materials/${id}/favorite/group`, null, {
      params: { groupName },
    })
  },
  getTags() {
    return request.get('/materials/tags')
  },
  // 兼容旧命名（AdminMaterials.vue 中使用）
  searchMaterials(params) {
    return this.search(params)
  },
  getMaterial(id) {
    return this.getById(id)
  },
  createMaterial(data) {
    return this.create(data)
  },
  updateMaterial(data) {
    return this.update(data)
  },
  deleteMaterial(id) {
    return this.remove(id)
  },
  importMaterials(formData) {
    return request.post('/materials/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  },
}
