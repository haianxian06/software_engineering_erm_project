<template>
  <el-container class="app-shell">
    <el-aside v-if="user.isLoggedIn" class="sidebar" width="220px">
      <div class="brand">
        <div class="brand-mark">作</div>
        <div>
          <strong>作业收集系统</strong>
          <span>期末作业平台</span>
        </div>
      </div>

      <el-menu router :default-active="route.path" class="side-menu">
        <el-menu-item v-if="user.role === 'STUDENT'" index="/student/assignments">
          <el-icon><Document /></el-icon>
          <span>我的作业</span>
        </el-menu-item>
        <el-menu-item v-if="user.role === 'ADMIN'" index="/admin/assignments">
          <el-icon><FolderOpened /></el-icon>
          <span>作业管理</span>
        </el-menu-item>
        <el-menu-item v-if="user.role === 'ADMIN'" index="/admin/classes">
          <el-icon><UserFilled /></el-icon>
          <span>班级管理</span>
        </el-menu-item>
        <el-menu-item v-if="user.role === 'ADMIN'" index="/admin/assignments/new">
          <el-icon><CirclePlus /></el-icon>
          <span>发布作业</span>
        </el-menu-item>
      </el-menu>

      <div class="user-box">
        <div>
          <strong>{{ user.realName }}</strong>
          <span>{{ user.studentNo || user.username }}</span>
        </div>
        <el-button size="small" plain @click="logout">退出</el-button>
      </div>
    </el-aside>

    <el-container>
      <el-header v-if="user.isLoggedIn" class="topbar">
        <div class="topbar-title">
          <strong>{{ pageTitle }}</strong>
          <span>基于 Web 与关系型数据库的期末作业项目</span>
        </div>
        <div class="topbar-meta">
          <el-tag effect="plain">{{ user.roleLabel }}</el-tag>
          <el-tag type="success" effect="plain">{{ topbarIdentity }}</el-tag>
        </div>
      </el-header>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { CirclePlus, Document, FolderOpened, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from './stores/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const titleMap = {
  '/student/assignments': '我的作业',
  '/admin/assignments': '作业管理',
  '/admin/classes': '班级管理',
  '/admin/assignments/new': '发布作业'
}

const pageTitle = computed(() => {
  if (route.path.includes('/statistics')) return '提交统计'
  if (route.path.includes('/submit')) return '提交作业'
  if (route.path.includes('/edit')) return '编辑草稿'
  return titleMap[route.path] || '作业收集管理系统'
})

const topbarIdentity = computed(() => {
  return user.realName || user.username
})

function logout() {
  user.logout()
  router.push('/login')
}
</script>
