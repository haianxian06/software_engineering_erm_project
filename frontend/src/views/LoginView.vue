<template>
  <div class="login-page">
    <section
      class="login-visual"
      :style="{ '--login-cover-image': `url(${loginCover})` }"
      aria-label="作业收集管理系统封面"
    >
      <div class="login-cover-copy">
        <div class="login-kicker">课程设计演示平台</div>
        <div class="brand-mark login-cover-mark">作</div>
        <h1>作业收集管理系统</h1>
        <p>围绕班级、作业、提交、批阅与归档构建的期末作业平台。</p>
        <div class="login-cover-tags">
          <span>关系型数据库</span>
          <span>作业归档</span>
          <span>批阅评分</span>
        </div>
      </div>

      <div class="login-flow-strip">
        <span>发布</span>
        <span>提交</span>
        <span>批阅</span>
        <span>归档</span>
      </div>
    </section>

    <section class="login-card-wrap">
      <div class="login-card">
        <div class="login-card-head">
          <div class="brand-mark login-entry-mark">作</div>
          <div>
            <span>期末作业平台</span>
            <h1>{{ mode === 'login' ? '进入系统' : '创建账号' }}</h1>
          </div>
        </div>
        <p class="login-card-subtitle">
          {{ mode === 'login' ? '选择身份并填写信息后进入对应端。' : '填写基础信息后自动进入对应端。' }}
        </p>

        <el-tabs v-model="mode" stretch>
          <el-tab-pane label="登录" name="login" />
          <el-tab-pane label="注册" name="register" />
        </el-tabs>

        <el-form class="login-form" label-position="top" @submit.prevent>
          <el-form-item label="身份">
            <el-segmented v-model="form.role" :options="roleOptions" />
          </el-form-item>

          <el-form-item label="姓名">
            <el-input v-model="form.realName" placeholder="请输入姓名" />
          </el-form-item>

          <el-form-item v-if="form.role === 'STUDENT'" label="学号">
            <el-input v-model="form.studentNo" placeholder="例如 0241121900" />
          </el-form-item>

          <el-form-item v-if="form.role === 'STUDENT' && mode === 'register'" label="专业">
            <el-input v-model="form.major" placeholder="例如 计算机科学与技术" />
          </el-form-item>

          <el-form-item v-if="form.role === 'ADMIN'" label="学工号">
            <el-input v-model="form.workNo" placeholder="例如 T001" />
          </el-form-item>

          <el-button class="login-submit" type="primary" size="large" :loading="loading" @click="submit">
            {{ mode === 'login' ? '进入系统' : '注册并进入' }}
          </el-button>
        </el-form>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { login, register } from '../api/user'
import loginCover from '../assets/login-cover.png'

const router = useRouter()
const user = useUserStore()
const mode = ref('login')
const loading = ref(false)

const roleOptions = [
  { label: '学生', value: 'STUDENT' },
  { label: '老师/管理员', value: 'ADMIN' }
]

const form = reactive({
  role: 'STUDENT',
  realName: '',
  studentNo: '',
  workNo: 'T001',
  major: ''
})

async function submit() {
  if (!form.realName) {
    ElMessage.warning('请填写姓名')
    return
  }
  if (form.role === 'STUDENT' && !form.studentNo) {
    ElMessage.warning('请填写学号')
    return
  }
  if (form.role === 'STUDENT' && mode.value === 'register' && !form.major) {
    ElMessage.warning('请填写专业')
    return
  }
  if (form.role === 'ADMIN' && !form.workNo) {
    ElMessage.warning('请填写学工号')
    return
  }

  loading.value = true
  try {
    const action = mode.value === 'login' ? login : register
    const currentUser = await action({ ...form })
    user.login(currentUser)
    router.push(currentUser.role === 'ADMIN' ? '/admin/assignments' : '/student/assignments')
  } finally {
    loading.value = false
  }
}
</script>
