<template>
  <div class="page">
    <div class="student-page-head">
      <div class="head-copy">
        <strong>我的作业</strong>
        <p>每个班级内按发布时间从新到旧排列，优先处理未截止任务。</p>
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
            <p class="meta">{{ group.items.length }} 项作业</p>
          </div>
          <el-tag effect="plain">最新 {{ formatDate(group.latestAt) }}</el-tag>
        </div>

        <div class="assignment-list">
          <article v-for="item in group.items" :key="item.id" class="assignment-card">
            <div class="assignment-main">
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
.student-page-head {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  margin-bottom: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid var(--app-border);
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.03);
}

.head-copy {
  min-width: 0;
}

.head-copy strong {
  display: block;
  color: #0f172a;
  font-size: 24px;
  line-height: 1.3;
}

.head-copy p {
  margin: 3px 0 0;
  color: var(--app-muted);
  font-size: 13px;
}

.class-assignment-list {
  display: grid;
  gap: 22px;
}

.assignment-class-section {
  display: grid;
  gap: 10px;
}

.class-section-head {
  display: flex;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
  padding: 0 2px;
}

.class-section-head h3 {
  margin: 0 0 4px;
  color: #0f172a;
  font-size: 18px;
  line-height: 1.35;
}

.assignment-list {
  display: grid;
  gap: 10px;
}

.assignment-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 18px;
  align-items: stretch;
  padding: 16px 18px;
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
  margin-bottom: 6px;
  color: #0f172a;
  font-size: 17px;
}

.assignment-source {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 18px;
  margin-top: 12px;
  color: #374151;
  font-size: 13px;
}

.card-actions {
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  min-width: 132px;
}

@media (max-width: 760px) {
  .student-page-head,
  .class-section-head,
  .assignment-card {
    align-items: flex-start;
    grid-template-columns: 1fr;
  }

  .student-page-head {
    flex-direction: column;
  }

  .card-actions {
    width: 100%;
    min-width: 0;
    justify-content: flex-start;
  }
}
</style>
