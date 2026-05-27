<template>
  <div class="login-page">
    <section class="login-visual">
      <div class="login-board">
        <div class="brand-mark board-mark">作</div>
        <h1>作业收集管理系统</h1>
        <p>软件工程课程设计 / 数据库系统设计 / 本地归档演示</p>
        <div class="board-lines">
          <span></span>
          <span></span>
          <span></span>
        </div>
      </div>
    </section>

    <section class="login-card-wrap">
      <div class="login-card">
        <h1>{{ mode === 'login' ? '进入系统' : '创建账号' }}</h1>
        <p>{{ mode === 'login' ? '请选择身份并填写信息' : '注册后会进入对应端口' }}</p>

        <el-tabs v-model="mode" stretch>
          <el-tab-pane label="登录" name="login" />
          <el-tab-pane label="注册" name="register" />
        </el-tabs>

        <el-form label-position="top" @submit.prevent>
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

          <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="submit">
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

<style scoped>
.board-mark {
  margin-bottom: 24px;
}

.board-lines {
  display: grid;
  gap: 12px;
  margin-top: 34px;
}

.board-lines span {
  display: block;
  height: 12px;
  background: linear-gradient(90deg, rgba(37, 99, 235, 0.2), rgba(22, 155, 107, 0.12));
  border-radius: 8px;
}

.board-lines span:nth-child(2) {
  width: 72%;
}

.board-lines span:nth-child(3) {
  width: 46%;
}
</style>
