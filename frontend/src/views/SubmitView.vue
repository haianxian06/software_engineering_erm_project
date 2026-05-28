<template>
  <div class="page">
    <div class="panel">
      <div class="toolbar">
        <div>
          <h2>{{ assignment.title }}</h2>
          <p class="meta">{{ assignment.description }}</p>
        </div>
        <div class="toolbar-actions">
          <el-tag :type="isExpired ? 'danger' : 'success'" size="large">
            {{ isExpired ? '通道已关闭' : '可提交' }}
          </el-tag>
          <el-button @click="openHistory">历史提交记录</el-button>
          <el-button @click="$router.back()">返回</el-button>
        </div>
      </div>

      <el-descriptions class="submit-descriptions" :column="2" border>
        <el-descriptions-item label="截止时间">{{ formatDate(assignment.deadline) }}</el-descriptions-item>
        <el-descriptions-item label="允许格式">{{ assignment.fileTypes || '不限格式' }}</el-descriptions-item>
        <el-descriptions-item label="文件大小">最大 {{ assignment.maxSizeMb }}MB</el-descriptions-item>
        <el-descriptions-item label="命名规则">{{ assignment.renamePattern || '学号_姓名_作业名' }}</el-descriptions-item>
        <el-descriptions-item label="提交人">{{ user.realName }} {{ user.studentNo }}</el-descriptions-item>
        <el-descriptions-item label="来源班级">{{ assignment.className || '未指定班级' }}</el-descriptions-item>
        <el-descriptions-item label="发布老师">{{ assignment.teacherName || assignment.createdBy || '未指定老师' }}</el-descriptions-item>
        <el-descriptions-item label="提交状态">
          <el-tag v-if="currentSubmission" type="success">已提交，第 {{ currentSubmission.versionNo }} 版</el-tag>
          <el-tag v-else type="info">未提交</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="批阅状态">
          <el-tag v-if="currentSubmission" :type="isReviewed ? 'success' : 'warning'">
            {{ isReviewed ? '已批阅' : '未批阅' }}
          </el-tag>
          <el-tag v-else type="info">未提交</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="是否可修改">
          <el-tag :type="canModify ? 'success' : 'danger'">{{ canModify ? '截止前可修改' : '已截止' }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>

      <div v-if="currentSubmission" class="current-submission">
        <h3>当前已交文件</h3>
        <div class="submission-info">
          <span>文件：{{ currentSubmission.originalName || currentSubmission.storedName }}</span>
          <span>重命名：{{ currentSubmission.storedName }}</span>
          <span>归档PDF：{{ currentSubmission.processedName || '待生成' }}</span>
          <span>提交时间：{{ formatDate(currentSubmission.submitTime) }}</span>
          <span>版本：第 {{ currentSubmission.versionNo }} 版</span>
        </div>
        <div class="review-result" :class="{ reviewed: isReviewed }">
          <div>
            <span class="review-kicker">批阅结果</span>
            <strong>{{ isReviewed ? `${formatScore(currentSubmission.score)} 分` : '未批阅' }}</strong>
          </div>
          <p>
            {{ isReviewed ? currentSubmission.reviewComment || '老师未填写评语' : '老师还没有批阅这份作业，请稍后查看。' }}
          </p>
          <span v-if="isReviewed" class="review-time">
            {{ currentSubmission.reviewerName || '老师' }} / {{ formatDate(currentSubmission.reviewedAt) }}
          </span>
        </div>
      </div>

      <el-alert
        v-if="isExpired"
        class="deadline-alert"
        title="已过截止时间，通道已关闭"
        type="warning"
        :closable="false"
      />

      <el-form label-position="top" class="submit-form">
        <el-form-item label="选择作业文件">
          <el-upload
            ref="uploadRef"
            drag
            :auto-upload="false"
            :limit="1"
            :disabled="isExpired"
            :accept="uploadAccept"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖入文件或点击选择</div>
          </el-upload>
        </el-form-item>
        <div class="submit-actions">
          <el-button type="primary" size="large" :loading="submitting" :disabled="isExpired" @click="submit">
            {{ currentSubmission ? '修改作业' : '提交作业' }}
          </el-button>
        </div>
      </el-form>

      <el-dialog v-model="historyVisible" title="历史提交记录" width="760px">
        <el-table v-loading="historyLoading" :data="submissionHistory" stripe>
          <el-table-column prop="versionNo" label="版本" width="90">
            <template #default="{ row }">第 {{ row.versionNo }} 版</template>
          </el-table-column>
          <el-table-column label="状态" width="130">
            <template #default="{ row }">
              <el-tag :type="row.finalVersion ? 'success' : 'info'">
                {{ row.finalVersion ? '最终有效版本' : '旧版本已隔离' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="originalName" label="原文件名" min-width="160" />
          <el-table-column prop="storedName" label="系统重命名" min-width="210" />
          <el-table-column label="提交时间" width="160">
            <template #default="{ row }">{{ formatDate(row.submitTime) }}</template>
          </el-table-column>
        </el-table>

        <el-alert
          v-if="isExpired"
          class="history-closed-alert"
          title="已过截止时间，通道已关闭"
          type="warning"
          :closable="false"
        />

        <div v-else class="history-actions">
          <el-button type="primary" @click="replacing = true">
            重新上传并覆盖
          </el-button>
        </div>

        <div v-if="replacing && !isExpired" class="replace-upload">
          <el-alert
            title="提交新文件后，系统会把它标记为最终有效版本，并将旧版本软删除隔离。"
            type="info"
            :closable="false"
          />
          <el-upload
            ref="historyUploadRef"
            drag
            :auto-upload="false"
            :limit="1"
            :disabled="isExpired"
            :accept="uploadAccept"
            :on-change="handleHistoryFileChange"
            :on-remove="handleHistoryFileRemove"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖入新文件或点击选择</div>
          </el-upload>
          <div class="replace-buttons">
            <el-button @click="cancelReplace">取消</el-button>
            <el-button type="primary" :loading="submitting" :disabled="isExpired" @click="submitReplacement">
              提交新文件并设为最终版本
            </el-button>
          </div>
        </div>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { getAssignment, getMySubmission, getSubmissionHistory, submitHomework } from '../api/assignment'
import { useUserStore } from '../stores/user'

const route = useRoute()
const user = useUserStore()
const assignment = ref({})
const currentSubmission = ref(null)
const selectedFile = ref(null)
const submitting = ref(false)
const uploadRef = ref(null)
const historyUploadRef = ref(null)
const historyVisible = ref(false)
const historyLoading = ref(false)
const submissionHistory = ref([])
const replacing = ref(false)
const historySelectedFile = ref(null)

const isExpired = computed(() => {
  if (!assignment.value.deadline) {
    return false
  }
  return new Date() > new Date(assignment.value.deadline)
})

const canModify = computed(() => !isExpired.value)

const isReviewed = computed(() => currentSubmission.value?.reviewStatus === 'REVIEWED')

const allowedExtensions = computed(() => {
  if (!assignment.value.fileTypes) {
    return []
  }
  return assignment.value.fileTypes
    .split(',')
    .map((item) => item.trim().replace(/^\./, '').toLowerCase())
    .filter(Boolean)
})

const uploadAccept = computed(() =>
  allowedExtensions.value.map((item) => `.${item}`).join(',')
)

function handleFileChange(file) {
  const rawFile = file.raw
  if (!rawFile) {
    selectedFile.value = null
    return
  }
  if (!isValidFile(rawFile)) {
    rejectSelectedFile()
    return
  }
  selectedFile.value = rawFile
}

function handleFileRemove() {
  selectedFile.value = null
}

function handleHistoryFileChange(file) {
  const rawFile = file.raw
  if (!rawFile) {
    historySelectedFile.value = null
    return
  }
  if (!isValidFile(rawFile)) {
    rejectHistoryFile()
    return
  }
  historySelectedFile.value = rawFile
}

function handleHistoryFileRemove() {
  historySelectedFile.value = null
}

function rejectSelectedFile() {
  selectedFile.value = null
  uploadRef.value?.clearFiles()
  ElMessage.warning('文件格式或大小不符，请检查作业要求')
}

function rejectHistoryFile() {
  historySelectedFile.value = null
  historyUploadRef.value?.clearFiles()
  ElMessage.warning('文件格式或大小不符，请检查作业要求')
}

function isValidFile(file) {
  const maxSizeMb = Number(assignment.value.maxSizeMb || 0)
  if (maxSizeMb > 0 && file.size > maxSizeMb * 1024 * 1024) {
    return false
  }
  if (allowedExtensions.value.length === 0) {
    return true
  }
  return allowedExtensions.value.includes(getExtension(file.name))
}

async function load() {
  assignment.value = await getAssignment(route.params.id)
  await loadCurrentSubmission()
}

async function loadCurrentSubmission() {
  if (!user.studentNo) {
    currentSubmission.value = null
    return
  }
  const result = await getMySubmission({
    assignmentId: route.params.id,
    studentNo: user.studentNo
  })
  currentSubmission.value = result || null
}

async function openHistory() {
  historyVisible.value = true
  replacing.value = false
  await loadHistory()
}

async function loadHistory() {
  if (!user.studentNo) {
    submissionHistory.value = []
    return
  }
  historyLoading.value = true
  try {
    submissionHistory.value = await getSubmissionHistory({
      assignmentId: route.params.id,
      studentNo: user.studentNo
    })
  } finally {
    historyLoading.value = false
  }
}

async function submit() {
  if (isExpired.value) {
    ElMessage.warning('已过截止时间，通道已关闭')
    return
  }
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  if (!isValidFile(selectedFile.value)) {
    rejectSelectedFile()
    return
  }
  const data = new FormData()
  data.append('assignmentId', route.params.id)
  data.append('studentNo', user.studentNo)
  data.append('realName', user.realName)
  data.append('file', selectedFile.value)

  submitting.value = true
  try {
    await submitHomework(data)
    ElMessage.success(currentSubmission.value ? '提交成功，已更新为最终有效版本' : '提交成功')
    selectedFile.value = null
    uploadRef.value?.clearFiles()
    await loadCurrentSubmission()
  } finally {
    submitting.value = false
  }
}

async function submitReplacement() {
  if (isExpired.value) {
    ElMessage.warning('已过截止时间，通道已关闭')
    return
  }
  if (!historySelectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  if (!isValidFile(historySelectedFile.value)) {
    rejectHistoryFile()
    return
  }
  const data = new FormData()
  data.append('assignmentId', route.params.id)
  data.append('studentNo', user.studentNo)
  data.append('realName', user.realName)
  data.append('file', historySelectedFile.value)

  submitting.value = true
  try {
    await submitHomework(data)
    ElMessage.success('提交成功，已更新为最终有效版本')
    historySelectedFile.value = null
    historyUploadRef.value?.clearFiles()
    replacing.value = false
    await loadCurrentSubmission()
    await loadHistory()
  } finally {
    submitting.value = false
  }
}

function cancelReplace() {
  replacing.value = false
  historySelectedFile.value = null
  historyUploadRef.value?.clearFiles()
}

function formatDate(value) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function formatScore(value) {
  if (value == null) {
    return '-'
  }
  return Number(value).toFixed(1).replace(/\.0$/, '')
}

function getExtension(filename) {
  if (!filename || !filename.includes('.')) {
    return ''
  }
  return filename.slice(filename.lastIndexOf('.') + 1).toLowerCase()
}

onMounted(load)
</script>

<style scoped>
.submit-form {
  margin-top: 20px;
}

.toolbar-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.current-submission {
  margin-top: 18px;
  padding: 16px;
  background: linear-gradient(135deg, #f8fafc, #eff6ff);
  border: 1px solid var(--app-border);
  border-radius: 8px;
}

.current-submission h3 {
  margin: 0 0 10px;
  font-size: 16px;
}

.submission-info {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 18px;
  color: #374151;
  font-size: 13px;
}

.review-result {
  display: grid;
  gap: 8px;
  margin-top: 14px;
  padding: 14px;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 8px;
}

.review-result.reviewed {
  background: #eff6ff;
  border-color: #bfdbfe;
}

.review-result strong,
.review-kicker,
.review-time {
  display: block;
}

.review-result strong {
  margin-top: 4px;
  color: #0f172a;
  font-size: 24px;
  line-height: 1;
}

.review-kicker,
.review-time {
  color: var(--app-muted);
  font-size: 12px;
}

.review-result p {
  margin: 0;
  color: #334155;
  line-height: 1.7;
}

.deadline-alert {
  margin-top: 18px;
}

.submit-descriptions {
  overflow: hidden;
  border-radius: 8px;
}

.submit-actions {
  display: flex;
  justify-content: flex-end;
}

.history-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.history-closed-alert {
  margin-top: 16px;
}

.replace-upload {
  margin-top: 16px;
}

.replace-upload :deep(.el-upload) {
  width: 100%;
  margin-top: 12px;
}

.replace-buttons {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
}
</style>
