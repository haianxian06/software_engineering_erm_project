<template>
  <div class="page">
    <div class="toolbar">
      <div>
        <h2>作业管理</h2>
        <p class="meta">发布收集任务，查看提交进度和未交名单。</p>
      </div>
      <div class="toolbar-actions">
        <el-button :icon="Refresh" @click="load">刷新</el-button>
        <el-button type="primary" :icon="Plus" @click="$router.push('/admin/assignments/new')">
          发布作业
        </el-button>
      </div>
    </div>

    <div class="grid summary-grid">
      <div class="panel metric-panel">
        <el-statistic title="已发布" :value="publishedAssignments.length" />
      </div>
      <div class="panel metric-panel">
        <el-statistic title="草稿" :value="draftAssignments.length" />
      </div>
      <div class="panel metric-panel">
        <el-statistic title="全部任务" :value="assignments.length" />
      </div>
    </div>

    <div class="panel">
      <el-tabs v-model="activeTab">
        <el-tab-pane :label="`已发布作业 ${publishedAssignments.length}`" name="published" />
        <el-tab-pane :label="`草稿箱 ${draftAssignments.length}`" name="draft" />
      </el-tabs>

      <el-table :data="currentAssignments" stripe>
        <el-table-column prop="title" label="作业标题" min-width="180" />
        <el-table-column prop="className" label="发布班级" min-width="180" />
        <el-table-column prop="deadline" label="截止时间" min-width="170">
          <template #default="{ row }">{{ formatDate(row.deadline) }}</template>
        </el-table-column>
        <el-table-column label="截止状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'DRAFT'" type="info">未发布</el-tag>
            <el-tag v-else :type="isExpired(row.deadline) ? 'danger' : 'success'">
              {{ isExpired(row.deadline) ? '已截止' : '未截止' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileTypes" label="格式限制" />
        <el-table-column prop="maxSizeMb" label="大小限制">
          <template #default="{ row }">{{ row.maxSizeMb }}MB</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'DRAFT' ? 'warning' : 'success'">
              {{ row.status === 'DRAFT' ? '草稿' : '已发布' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PUBLISHED'" size="small" :icon="DataAnalysis" @click="showStats(row.id)">
              统计
            </el-button>
            <el-button v-if="row.status === 'PUBLISHED'" size="small" type="warning" plain @click="openDelay(row)">
              延时
            </el-button>
            <el-button v-else size="small" type="primary" :icon="Edit" @click="editDraft(row.id)">
              继续编辑
            </el-button>
            <el-button size="small" type="danger" plain :icon="Delete" @click="removeAssignment(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="delayDialogVisible" title="作业延时" width="420px" class="deadline-dialog">
      <div v-if="delayingAssignment" class="delay-dialog-head">
        <div class="delay-title">{{ delayingAssignment.title }}</div>
        <div class="delay-meta">当前截止：{{ formatDate(delayingAssignment.deadline) }}</div>
      </div>

      <el-form label-position="top" class="delay-form">
        <el-form-item label="新的截止时间">
          <el-date-picker
            v-model="delayDeadline"
            type="datetime"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择新的截止时间"
            :disabled-date="disablePastDate"
            style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <p class="delay-tip">只能延长到更晚的时间，已过期作业也可以重新打开提交通道。</p>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="delayDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="savingDelay" @click="saveDelay">保存延时</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { DataAnalysis, Delete, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import { deleteAssignment, extendAssignmentDeadline, listAssignments } from '../api/assignment'
import { useUserStore } from '../stores/user'

const router = useRouter()
const user = useUserStore()
const assignments = ref([])
const activeTab = ref('published')
const delayDialogVisible = ref(false)
const delayingAssignment = ref(null)
const delayDeadline = ref('')
const savingDelay = ref(false)

const publishedAssignments = computed(() =>
  assignments.value.filter((item) => item.status !== 'DRAFT')
)
const draftAssignments = computed(() =>
  assignments.value.filter((item) => item.status === 'DRAFT')
)
const currentAssignments = computed(() =>
  activeTab.value === 'draft' ? draftAssignments.value : publishedAssignments.value
)

async function load() {
  assignments.value = await listAssignments({ userId: user.id, role: user.role })
}

function showStats(id) {
  router.push(`/admin/assignments/${id}/statistics`)
}

function editDraft(id) {
  router.push(`/admin/assignments/${id}/edit`)
}

function openDelay(row) {
  delayingAssignment.value = row
  delayDeadline.value = row.deadline
  delayDialogVisible.value = true
}

function disablePastDate(date) {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

async function saveDelay() {
  const row = delayingAssignment.value
  if (!row || !delayDeadline.value) {
    ElMessage.warning('请选择新的截止时间')
    return
  }

  const oldTime = new Date(row.deadline).getTime()
  const newTime = new Date(delayDeadline.value).getTime()
  if (Number.isNaN(newTime) || newTime <= Date.now()) {
    ElMessage.warning('时间设置无效')
    return
  }
  if (newTime <= oldTime) {
    ElMessage.warning('新截止时间必须晚于当前截止时间')
    return
  }

  savingDelay.value = true
  try {
    await extendAssignmentDeadline(row.id, { deadline: delayDeadline.value })
    ElMessage.success('截止时间已延长')
    delayDialogVisible.value = false
    await load()
  } finally {
    savingDelay.value = false
  }
}

async function removeAssignment(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除“${row.title}”吗？删除后会同时清理提交记录、统计记录和归档文件。`,
      '删除作业',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消'
      }
    )
  } catch {
    return
  }
  await deleteAssignment(row.id)
  ElMessage.success('作业已删除')
  await load()
}

function formatDate(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function isExpired(value) {
  return value ? new Date() > new Date(value) : false
}

onMounted(load)
</script>

<style scoped>
.summary-grid {
  margin-bottom: 16px;
}

.deadline-dialog :deep(.el-dialog__body) {
  padding-top: 8px;
}

.delay-dialog-head {
  padding: 14px 16px;
  margin-bottom: 16px;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  background: linear-gradient(135deg, #eff6ff, #f8fafc);
}

.delay-title {
  font-weight: 700;
  color: #0f172a;
}

.delay-meta,
.delay-tip {
  color: #64748b;
  font-size: 13px;
}

.delay-meta {
  margin-top: 6px;
}

.delay-form {
  margin-top: 4px;
}

.delay-tip {
  margin: 0;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
