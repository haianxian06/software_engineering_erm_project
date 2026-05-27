<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>提交统计</h2>
        <p class="meta">{{ stats.assignmentTitle }} {{ stats.className ? ` / ${stats.className}` : '' }}</p>
      </div>
      <div class="toolbar-actions">
        <el-tag :type="stats.channelClosed ? 'danger' : 'success'" size="large">
          {{ stats.channelClosed ? '通道已关闭' : '收集中' }}
        </el-tag>
        <el-button type="primary" :loading="packaging" @click="packageArchive">一键打包</el-button>
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </div>

    <div class="grid">
      <div class="panel">
        <el-statistic title="应交人数" :value="stats.totalStudents || 0" />
      </div>
      <div class="panel">
        <el-statistic title="已交人数" :value="stats.submittedCount || 0" />
      </div>
      <div class="panel">
        <el-statistic title="提交率" :value="stats.submitRate || 0" suffix="%" />
      </div>
      <div class="panel">
        <el-statistic title="未交人数" :value="missingCount" />
      </div>
      <div class="panel">
        <div class="check-time">
          <span>DDL</span>
          <strong>{{ formatDate(stats.deadline) }}</strong>
        </div>
      </div>
      <div class="panel">
        <div class="check-time">
          <span>后台最近检查</span>
          <strong>{{ formatDate(stats.lastCheckedAt) }}</strong>
        </div>
      </div>
    </div>

    <div class="panel dashboard-panel">
      <div class="dashboard-head">
        <div>
          <h3>提交率看板</h3>
          <p class="meta">
            {{ stats.channelClosed ? 'DDL已到达，收集通道已关闭。' : '收集通道仍开放，后台会持续刷新提交状态。' }}
          </p>
        </div>
      </div>
      <div class="chart-row">
        <div class="pie-chart" :style="pieStyle">
          <div class="pie-center">
            <strong>{{ stats.submitRate || 0 }}%</strong>
            <span>提交率</span>
          </div>
        </div>
        <div class="chart-legend">
          <div class="legend-item">
            <span class="legend-dot submitted"></span>
            <strong>已交</strong>
            <em>{{ stats.submittedCount || 0 }} 人</em>
          </div>
          <div class="legend-item">
            <span class="legend-dot missing"></span>
            <strong>未交</strong>
            <em>{{ missingCount }} 人</em>
          </div>
        </div>
      </div>
    </div>

    <div class="panel stats-table">
      <h3>已交名单</h3>
      <el-table :data="stats.submissions || []" stripe>
        <el-table-column prop="studentNo" label="学号" width="140" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="storedName" label="文件名" min-width="220" />
        <el-table-column prop="processedName" label="归档PDF" min-width="220" />
        <el-table-column prop="processedStorageKey" label="归档目录" min-width="260" />
        <el-table-column prop="submitTime" label="提交时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.submitTime) }}</template>
        </el-table-column>
      </el-table>
    </div>

    <div class="panel stats-table">
      <h3>未交名单</h3>
      <el-table :data="stats.missingStudents || []" stripe>
        <el-table-column prop="studentNo" label="学号" width="140" />
        <el-table-column prop="realName" label="姓名" />
      </el-table>
    </div>

    <el-dialog v-model="packageDialogVisible" title="归档Zip下载链接" width="680px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="Zip文件">{{ packageInfo.zipName }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">{{ formatSize(packageInfo.packageSize) }}</el-descriptions-item>
        <el-descriptions-item label="有效期至">{{ formatDate(packageInfo.expiresAt) }}</el-descriptions-item>
        <el-descriptions-item label="加密下载链接">
          <el-input :model-value="fullDownloadUrl" readonly />
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="packageDialogVisible = false">关闭</el-button>
        <el-button type="primary" @click="openDownloadLink">下载Zip</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAssignmentStatistics, packageAssignmentArchive } from '../api/assignment'

const route = useRoute()
const stats = ref({})
const packaging = ref(false)
const packageDialogVisible = ref(false)
const packageInfo = ref({})

const missingCount = computed(() =>
  Math.max(Number(stats.value.totalStudents || 0) - Number(stats.value.submittedCount || 0), 0)
)

const pieStyle = computed(() => {
  const rate = Math.min(Math.max(Number(stats.value.submitRate || 0), 0), 100)
  return {
    background: `conic-gradient(#16a34a 0 ${rate}%, #ef4444 ${rate}% 100%)`
  }
})

const fullDownloadUrl = computed(() => {
  if (!packageInfo.value.downloadUrl) {
    return ''
  }
  return `${window.location.origin}${packageInfo.value.downloadUrl}`
})

async function load() {
  stats.value = await getAssignmentStatistics(route.params.id)
}

async function packageArchive() {
  packaging.value = true
  try {
    packageInfo.value = await packageAssignmentArchive(route.params.id)
    packageDialogVisible.value = true
    ElMessage.success('打包完成，下载链接已生成')
  } finally {
    packaging.value = false
  }
}

function openDownloadLink() {
  if (!fullDownloadUrl.value) {
    return
  }
  window.open(fullDownloadUrl.value, '_blank')
}

function formatDate(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function formatSize(value) {
  const size = Number(value || 0)
  if (size >= 1024 * 1024) {
    return `${(size / 1024 / 1024).toFixed(2)} MB`
  }
  if (size >= 1024) {
    return `${(size / 1024).toFixed(1)} KB`
  }
  return `${size} B`
}

onMounted(load)
</script>

<style scoped>
.toolbar-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.stats-table {
  margin-top: 16px;
}

.dashboard-panel {
  margin-top: 16px;
}

.dashboard-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.dashboard-head h3,
.stats-table h3 {
  margin: 0 0 8px;
}

.chart-row {
  display: flex;
  gap: 28px;
  align-items: center;
  margin-top: 16px;
}

.pie-chart {
  display: grid;
  width: 180px;
  height: 180px;
  flex: 0 0 180px;
  place-items: center;
  border-radius: 50%;
}

.pie-center {
  display: grid;
  width: 112px;
  height: 112px;
  place-items: center;
  background: #ffffff;
  border-radius: 50%;
  box-shadow: inset 0 0 0 1px #e5e7eb;
}

.pie-center strong {
  color: #111827;
  font-size: 28px;
  line-height: 1;
}

.pie-center span {
  color: #6b7280;
  font-size: 13px;
}

.chart-legend {
  display: grid;
  gap: 14px;
  min-width: 180px;
}

.legend-item {
  display: grid;
  grid-template-columns: 12px 48px 1fr;
  gap: 10px;
  align-items: center;
  color: #374151;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.legend-dot.submitted {
  background: #16a34a;
}

.legend-dot.missing {
  background: #ef4444;
}

.legend-item em {
  color: #6b7280;
  font-style: normal;
}

.check-time {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.check-time span {
  color: #6b7280;
  font-size: 13px;
}

.check-time strong {
  color: #111827;
  font-size: 18px;
  font-weight: 600;
}

@media (max-width: 760px) {
  .chart-row {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
