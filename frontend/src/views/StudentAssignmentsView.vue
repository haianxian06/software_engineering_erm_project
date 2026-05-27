<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>我的作业</h2>
        <p class="meta">查看老师发布的收集任务，并在截止时间前提交文件。</p>
      </div>
      <div class="toolbar-actions">
        <el-tag effect="plain">共 {{ assignments.length }} 项</el-tag>
        <el-button :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <el-empty v-if="assignments.length === 0" class="panel" description="暂无作业" />

    <div v-else class="class-assignment-list">
      <section v-for="group in groupedAssignments" :key="group.key" class="assignment-class-section">
        <div class="class-section-head">
          <div>
            <h3>{{ group.className }}</h3>
            <p class="meta">{{ group.items.length }} 项作业，按发布时间从新到旧排列</p>
          </div>
          <el-tag effect="plain">{{ formatDate(group.latestAt) }}</el-tag>
        </div>

        <div class="assignment-grid">
          <article v-for="item in group.items" :key="item.id" class="assignment-card">
            <div class="assignment-card-head">
              <div>
                <strong>{{ item.title }}</strong>
                <p class="meta">{{ item.description || '暂无说明' }}</p>
              </div>
              <el-tag :type="isExpired(item.deadline) ? 'danger' : 'success'">
                {{ isExpired(item.deadline) ? '已截止' : '可提交' }}
              </el-tag>
            </div>

            <div class="assignment-source">
              <span>班级：{{ item.className || '未指定班级' }}</span>
              <span>老师：{{ item.teacherName || item.createdBy || '未指定老师' }}</span>
              <span>发布时间：{{ formatDate(item.createdAt) }}</span>
            </div>

            <div class="status-row">
              <el-tag type="info">截止 {{ formatDate(item.deadline) }}</el-tag>
              <el-tag>{{ item.fileTypes || '不限格式' }}</el-tag>
              <el-tag type="warning">最大 {{ item.maxSizeMb }}MB</el-tag>
            </div>

            <div class="card-actions">
              <el-button type="primary" :icon="Upload" @click="goSubmit(item.id)">
                {{ isExpired(item.deadline) ? '查看详情' : '提交作业' }}
              </el-button>
            </div>
          </article>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Upload } from '@element-plus/icons-vue'
import { listAssignments } from '../api/assignment'
import { useUserStore } from '../stores/user'

const router = useRouter()
const user = useUserStore()
const assignments = ref([])

const groupedAssignments = computed(() => {
  const groups = new Map()

  assignments.value.forEach((item) => {
    const key = item.classId || item.className || 'unknown'
    if (!groups.has(key)) {
      groups.set(key, {
        key,
        className: item.className || '未指定班级',
        latestAt: item.createdAt,
        items: []
      })
    }

    const group = groups.get(key)
    group.items.push(item)
    if (dateTimeOf(item.createdAt) > dateTimeOf(group.latestAt)) {
      group.latestAt = item.createdAt
    }
  })

  return Array.from(groups.values())
    .map((group) => ({
      ...group,
      items: [...group.items].sort((a, b) => {
        const timeDiff = dateTimeOf(b.createdAt) - dateTimeOf(a.createdAt)
        return timeDiff || Number(b.id || 0) - Number(a.id || 0)
      })
    }))
    .sort((a, b) => {
      const timeDiff = dateTimeOf(b.latestAt) - dateTimeOf(a.latestAt)
      return timeDiff || a.className.localeCompare(b.className, 'zh-CN')
    })
})

async function load() {
  assignments.value = await listAssignments({ userId: user.id, role: user.role })
}

function goSubmit(id) {
  router.push(`/student/assignments/${id}/submit`)
}

function formatDate(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function isExpired(value) {
  return value ? new Date() > new Date(value) : false
}

function dateTimeOf(value) {
  const time = value ? new Date(value).getTime() : 0
  return Number.isNaN(time) ? 0 : time
}

onMounted(load)
</script>

<style scoped>
.class-assignment-list {
  display: grid;
  gap: 24px;
}

.assignment-class-section {
  display: grid;
  gap: 14px;
}

.class-section-head {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  justify-content: space-between;
  padding: 0 2px;
}

.class-section-head h3 {
  margin: 0 0 4px;
  color: #0f172a;
  font-size: 19px;
  line-height: 1.35;
}

.assignment-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 16px;
}

.assignment-card {
  display: flex;
  min-height: 260px;
  flex-direction: column;
  justify-content: space-between;
  padding: 18px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid var(--app-border);
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
}

.assignment-card-head {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  justify-content: space-between;
}

.assignment-card-head strong {
  display: block;
  margin-bottom: 8px;
  color: #0f172a;
  font-size: 17px;
}

.assignment-source {
  display: grid;
  gap: 6px;
  margin: 12px 0;
  color: #374151;
  font-size: 13px;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}
</style>
