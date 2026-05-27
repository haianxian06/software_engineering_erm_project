import { defineStore } from 'pinia'

const savedUser = JSON.parse(localStorage.getItem('homework-user') || 'null')

export const useUserStore = defineStore('user', {
  state: () => ({
    id: savedUser?.id || null,
    role: savedUser?.role || '',
    username: savedUser?.username || '',
    realName: savedUser?.realName || '',
    studentNo: savedUser?.studentNo || '',
    workNo: savedUser?.workNo || '',
    major: savedUser?.major || '',
    classId: savedUser?.classId || 1
  }),
  getters: {
    isLoggedIn: (state) => Boolean(state.role && state.realName),
    roleLabel: (state) => (state.role === 'ADMIN' ? '管理员端' : '学生端')
  },
  actions: {
    login(payload) {
      this.id = payload.id || null
      this.role = payload.role
      this.username = payload.username || payload.studentNo || payload.realName
      this.realName = payload.realName
      this.studentNo = payload.studentNo || ''
      this.workNo = payload.workNo || ''
      this.major = payload.major || ''
      this.classId = Number(payload.classId || 1)
      localStorage.setItem('homework-user', JSON.stringify(this.$state))
    },
    logout() {
      this.id = null
      this.role = ''
      this.username = ''
      this.realName = ''
      this.studentNo = ''
      this.workNo = ''
      this.major = ''
      this.classId = 1
      localStorage.removeItem('homework-user')
    }
  }
})
