import request from '@/utils/request'

export function listType() {
  return request({
    url: '/api/type/list',
    method: 'get'
  })
}

export function saveType(data) {
  return request({
    url: '/api/type/save',
    method: 'post',
    data
  })
}

export function deleteType(id) {
  return request({
    url: `/api/type/delete/${id}`,
    method: 'post'
  })
}