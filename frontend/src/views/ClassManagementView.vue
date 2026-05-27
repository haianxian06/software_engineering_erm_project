<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>班级管理</h2>
        <p class="meta">创建班级后自动加入同专业学生，也可以手动拉入、移出学生和设置班级管理员。</p>
      </div>
      <div class="toolbar-actions">
        <el-tag effect="plain">班级 {{ classes.length }}</el-tag>
        <el-tag type="success" effect="plain">成员 {{ members.length }}</el-tag>
        <el-button :icon="Refresh" @click="loadClasses">刷新</el-button>
      </div>
    </div>

    <div class="grid class-layout">
      <div class="panel">
        <div class="section-title">
          <h3>创建班级</h3>
          <p class="meta">按专业自动匹配学生</p>
        </div>
        <el-form label-position="top" :model="form">
          <el-form-item label="班级名称">
            <el-input v-model="form.className" placeholder="例如 计算机科学与技术-软件工程班" />
          </el-form-item>
          <el-form-item label="专业">
            <el-input v-model="form.major" placeholder="例如 计算机科学与技术" />
          </el-form-item>
          <el-form-item label="课程名称">
            <el-input v-model="form.courseName" placeholder="例如 软件工程" />
          </el-form-item>
          <el-form-item label="年级">
            <el-input v-model="form.grade" placeholder="例如 2023" />
          </el-form-item>
          <el-button type="primary" :loading="creating" @click="createNewClass">
            创建班级
          </el-button>
        </el-form>
      </div>

      <div class="panel">
        <div class="section-title">
          <h3>我的班级</h3>
          <p class="meta">点击表格行切换班级</p>
        </div>
        <el-table :data="classes" stripe highlight-current-row @current-change="selectClass">
          <el-table-column prop="className" label="班级" min-width="180" />
          <el-table-column prop="major" label="专业" min-width="150" />
          <el-table-column prop="courseName" label="课程" width="120" />
          <el-table-column prop="memberCount" label="人数" width="80" />
        </el-table>
      </div>
    </div>

    <div class="panel member-panel">
      <div class="toolbar">
        <div>
          <h3>班级成员</h3>
          <p class="meta">{{ selectedClass?.className || '请选择一个班级' }}</p>
        </div>
      </div>
      <div v-if="selectedClass" class="member-actions">
        <el-select
          v-model="studentNoToAdd"
          filterable
          clearable
          placeholder="选择要拉入班级的学生"
          class="student-select"
        >
          <el-option
            v-for="student in availableStudents"
            :key="student.userId"
            :label="studentOptionLabel(student)"
            :value="student.studentNo"
          />
        </el-select>
        <el-button type="primary" :icon="Plus" :loading="adding" :disabled="!studentNoToAdd" @click="addSelectedStudent">
          拉入班级
        </el-button>
      </div>
      <el-empty v-if="!selectedClass" description="请选择班级" />
      <el-table :data="members" stripe>
        <el-table-column prop="studentNo" label="学号" width="130" />
        <el-table-column prop="workNo" label="学工号" width="130" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="major" label="专业" min-width="150" />
        <el-table-column prop="memberRole" label="班级权限" width="120">
          <template #default="{ row }">
            <el-tag :type="row.memberRole === 'ADMIN' ? 'warning' : row.memberRole === 'TEACHER' ? 'success' : 'info'">
              {{ roleText(row.memberRole) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300">
          <template #default="{ row }">
            <el-button
              v-if="row.systemRole === 'STUDENT' && row.memberRole !== 'ADMIN'"
              size="small"
              type="primary"
              @click="setAdmin(row)"
            >
              设为管理员
            </el-button>
            <el-button
              v-if="row.systemRole === 'STUDENT' && row.memberRole === 'ADMIN'"
              size="small"
              @click="setStudent(row)"
            >
              取消管理员
            </el-button>
            <el-button
              v-if="row.systemRole === 'STUDENT'"
              size="small"
              type="danger"
              plain
              :icon="Delete"
              @click="removeMember(row)"
            >
              移出班级
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, Plus, Refresh } from '@element-plus/icons-vue'
import {
  addStudentToClass,
  createClass,
  listAvailableStudents,
  listClasses,
  listMembers,
  removeStudentFromClass,
  updateMemberRole
} from '../api/class'
import { useUserStore } from '../stores/user'

const user = useUserStore()
const classes = ref([])
const members = ref([])
const availableStudents = ref([])
const selectedClass = ref(null)
const studentNoToAdd = ref('')
const creating = ref(false)
const adding = ref(false)

const form = reactive({
  className: '',
  major: '',
  courseName: '',
  grade: '2023'
})

async function loadClasses() {
  classes.value = await listClasses(user.id)
  if (classes.value.length > 0) {
    const current = classes.value.find((item) => item.id === selectedClass.value?.id)
    await selectClass(current || classes.value[0])
  } else {
    await selectClass(null)
  }
}

async function selectClass(row) {
  selectedClass.value = row
  studentNoToAdd.value = ''
  if (!row) {
    members.value = []
    availableStudents.value = []
    return
  }
  const [memberList, availableList] = await Promise.all([
    listMembers(row.id),
    listAvailableStudents(row.id)
  ])
  members.value = memberList
  availableStudents.value = availableList
}

async function createNewClass() {
  if (!form.className || !form.major || !form.courseName) {
    ElMessage.warning('请填写班级名称、专业和课程名称')
    return
  }
  creating.value = true
  try {
    const created = await createClass({ ...form, createdBy: user.id })
    ElMessage.success('班级已创建，并已加入同专业学生')
    form.className = ''
    form.courseName = ''
    await loadClasses()
    await selectClass(created)
  } finally {
    creating.value = false
  }
}

async function setAdmin(row) {
  await updateMemberRole(selectedClass.value.id, row.userId, 'ADMIN')
  ElMessage.success('已设为班级管理员')
  await refreshSelectedClass()
}

async function setStudent(row) {
  await updateMemberRole(selectedClass.value.id, row.userId, 'STUDENT')
  ElMessage.success('已取消管理员权限')
  await refreshSelectedClass()
}

async function addSelectedStudent() {
  if (!selectedClass.value || !studentNoToAdd.value) {
    ElMessage.warning('请选择要拉入的学生')
    return
  }
  adding.value = true
  try {
    await addStudentToClass(selectedClass.value.id, studentNoToAdd.value)
    ElMessage.success('已拉入班级')
    studentNoToAdd.value = ''
    await refreshSelectedClass()
  } finally {
    adding.value = false
  }
}

async function removeMember(row) {
  try {
    await ElMessageBox.confirm(`确定把 ${row.realName} 移出这个班级吗？`, '移出班级', {
      type: 'warning',
      confirmButtonText: '移出',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  await removeStudentFromClass(selectedClass.value.id, row.userId)
  ElMessage.success('已移出班级')
  await refreshSelectedClass()
}

async function refreshSelectedClass() {
  if (!selectedClass.value) {
    return
  }
  const classId = selectedClass.value.id
  const [classList, memberList, availableList] = await Promise.all([
    listClasses(user.id),
    listMembers(classId),
    listAvailableStudents(classId)
  ])
  classes.value = classList
  selectedClass.value = classList.find((item) => item.id === classId) || selectedClass.value
  members.value = memberList
  availableStudents.value = availableList
}

function roleText(value) {
  return {
    TEACHER: '老师',
    ADMIN: '管理员',
    STUDENT: '学生'
  }[value] || value
}

function studentOptionLabel(student) {
  return `${student.realName} / ${student.studentNo} / ${student.major || '未填写专业'}`
}

onMounted(loadClasses)
</script>

<style scoped>
.class-layout {
  grid-template-columns: minmax(320px, 380px) 1fr;
}

.section-title {
  margin-bottom: 16px;
}

.section-title h3 {
  margin: 0 0 6px;
}

.member-panel {
  margin-top: 16px;
}

.member-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
  margin-bottom: 14px;
}

.student-select {
  width: min(420px, 100%);
}

@media (max-width: 980px) {
  .class-layout {
    grid-template-columns: 1fr;
  }
}
</style>
