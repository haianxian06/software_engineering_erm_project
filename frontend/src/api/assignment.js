import http from './http'

export function listAssignments(params = {}) {
  return http.get('/assignments', { params })
}

export function getAssignment(id) {
  return http.get(`/assignments/${id}`)
}

export function createAssignment(data) {
  return http.post('/assignments', data)
}

export function updateAssignment(id, data) {
  return http.put(`/assignments/${id}`, data)
}

export function submitHomework(formData) {
  return http.post('/submissions', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export function getMySubmission(params) {
  return http.get('/submissions/current', { params })
}

export function getSubmissionHistory(params) {
  return http.get('/submissions/history', { params })
}

export function getAssignmentStatistics(id) {
  return http.get(`/assignments/${id}/statistics`)
}

export function packageAssignmentArchive(id) {
  return http.post(`/packages/assignments/${id}`)
}
