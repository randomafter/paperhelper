import request from './request'

export const materialApi = {
  searchMaterials(params) {
    return request.get('/materials', { params })
  },
  getMaterial(id) {
    return request.get(`/materials/${id}`)
  },
  getAllTags() {
    return request.get('/materials/tags')
  },
  createMaterial(data) {
    return request.post('/materials', data)
  },
  updateMaterial(data) {
    return request.put('/materials', data)
  },
  deleteMaterial(id) {
    return request.delete(`/materials/${id}`)
  },
  toggleFavorite(id) {
    return request.post(`/materials/${id}/favorite`)
  },
  getFavorites() {
    return request.get('/materials/favorites')
  },
  importMaterials(formData) {
    return request.post('/materials/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}
