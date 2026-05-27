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
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'PUBLISHED'" size="small" :icon="DataAnalysis" @click="showStats(row.id)">
              统计
            </el-button>
            <el-button v-else size="small" type="primary" :icon="Edit" @click="editDraft(row.id)">
              继续编辑
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { DataAnalysis, Edit, Plus, Refresh } from '@element-plus/icons-vue'
import { listAssignments } from '../api/assignment'
import { useUserStore } from '../stores/user'

const router = useRouter()
const user = useUserStore()
const assignments = ref([])
const activeTab = ref('published')

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

function formatDate(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

onMounted(load)
</script>

<style scoped>
.summary-grid {
  margin-bottom: 16px;
}
</style>
