import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '../stores/user'
import LoginView from '../views/LoginView.vue'
import StudentAssignmentsView from '../views/StudentAssignmentsView.vue'
import SubmitView from '../views/SubmitView.vue'
import AdminAssignmentsView from '../views/AdminAssignmentsView.vue'
import AssignmentFormView from '../views/AssignmentFormView.vue'
import StatisticsView from '../views/StatisticsView.vue'
import ClassManagementView from '../views/ClassManagementView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/login' },
    { path: '/login', component: LoginView },
    { path: '/student/assignments', component: StudentAssignmentsView },
    { path: '/student/assignments/:id/submit', component: SubmitView },
    { path: '/admin/assignments', component: AdminAssignmentsView },
    { path: '/admin/assignments/new', component: AssignmentFormView },
    { path: '/admin/assignments/:id/edit', component: AssignmentFormView },
    { path: '/admin/assignments/:id/statistics', component: StatisticsView },
    { path: '/admin/classes', component: ClassManagementView }
  ]
})

router.beforeEach((to) => {
  const user = useUserStore()
  if (to.path !== '/login' && !user.isLoggedIn) {
    return '/login'
  }
})

export default router
