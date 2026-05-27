import http from './http'

export function listClasses(userId) {
  return http.get('/classes', { params: { userId } })
}

export function createClass(data) {
  return http.post('/classes', data)
}

export function listMembers(classId) {
  return http.get(`/classes/${classId}/members`)
}

export function listAvailableStudents(classId) {
  return http.get(`/classes/${classId}/available-students`)
}

export function addStudentToClass(classId, studentNo) {
  return http.post(`/classes/${classId}/members`, { studentNo })
}

export function updateMemberRole(classId, userId, memberRole) {
  return http.put(`/classes/${classId}/members/${userId}/role`, { memberRole })
}

export function removeStudentFromClass(classId, userId) {
  return http.delete(`/classes/${classId}/members/${userId}`)
}
