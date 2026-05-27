<template>
  <div class="page">
    <div class="panel">
      <div class="toolbar">
        <div>
          <h2>{{ isEditing ? '编辑草稿' : '发布作业' }}</h2>
          <p class="meta">设置标题、截止时间和文件限制；暂时不发布时可以先保存草稿。</p>
        </div>
        <el-button @click="$router.back()">返回</el-button>
      </div>

      <el-form label-position="top" :model="form">
        <div class="form-grid">
          <section class="form-section">
            <h3>基础信息</h3>
            <el-form-item label="作业标题">
              <el-input v-model="form.title" placeholder="例如 软件工程作业 2" />
            </el-form-item>
            <el-form-item label="作业说明">
              <el-input v-model="form.description" type="textarea" :rows="5" />
            </el-form-item>
            <el-form-item label="截止时间">
              <el-date-picker
                v-model="form.deadline"
                type="datetime"
                value-format="YYYY-MM-DDTHH:mm:ss"
                placeholder="选择截止时间"
                style="width: 100%"
              />
            </el-form-item>
            <el-form-item label="发布班级">
              <el-select v-model="form.classId" placeholder="请选择班级" style="width: 100%">
                <el-option
                  v-for="item in classes"
                  :key="item.id"
                  :label="`${item.className} / ${item.courseName}`"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </section>

          <section class="form-section">
            <h3>提交规则</h3>
            <el-form-item label="允许文件类型">
              <el-checkbox-group v-model="form.fileTypes" class="file-type-options">
                <el-checkbox
                  v-for="option in fileTypeOptions"
                  :key="option.value"
                  :label="option.value"
                  border
                >
                  {{ option.label }}
                </el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="最大文件大小">
              <div class="soft-row">
                <el-input-number v-model="form.maxSizeMb" :min="1" :max="500" />
                <span class="meta">MB</span>
              </div>
            </el-form-item>
            <el-form-item label="文件命名规则">
              <el-select v-model="form.renamePattern" placeholder="请选择命名规则" style="width: 100%">
                <el-option
                  v-for="option in renamePatternOptions"
                  :key="option.value"
                  :label="option.label"
                  :value="option.value"
                />
              </el-select>
              <p class="meta naming-meta">提交后系统会按此规则自动重命名，并在本地生成 PDF 归档副本。</p>
            </el-form-item>
          </section>
        </div>
        <div class="form-actions">
          <el-button size="large" :loading="savingDraft" @click="save('DRAFT')">
            保存草稿
          </el-button>
          <el-button type="primary" size="large" :loading="publishing" @click="save('PUBLISHED')">
            保存并发布
          </el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { createAssignment, getAssignment, updateAssignment } from '../api/assignment'
import { listClasses } from '../api/class'
import { useUserStore } from '../stores/user'

const router = useRouter()
const route = useRoute()
const user = useUserStore()
const savingDraft = ref(false)
const publishing = ref(false)
const classes = ref([])
const isEditing = computed(() => Boolean(route.params.id))
const fileTypeOptions = [
  { label: 'Word 文档 docx', value: 'docx' },
  { label: 'Word 旧版 doc', value: 'doc' },
  { label: 'PDF 文档 pdf', value: 'pdf' },
  { label: '压缩包 zip', value: 'zip' },
  { label: '压缩包 rar', value: 'rar' },
  { label: '压缩包 7z', value: '7z' },
  { label: 'PPT 演示 pptx', value: 'pptx' },
  { label: 'PPT 旧版 ppt', value: 'ppt' },
  { label: 'Excel 表格 xlsx', value: 'xlsx' },
  { label: 'Excel 旧版 xls', value: 'xls' },
  { label: 'SQL 文件 sql', value: 'sql' },
  { label: 'Java 代码 java', value: 'java' },
  { label: 'Python 代码 py', value: 'py' },
  { label: '网页文件 html', value: 'html' },
  { label: '样式文件 css', value: 'css' },
  { label: '图片 png', value: 'png' },
  { label: '图片 jpg', value: 'jpg' }
]
const renamePatternOptions = [
  { label: '学号_姓名_作业名', value: '学号_姓名_作业名' },
  { label: '班级_学号_姓名_作业名', value: '班级_学号_姓名_作业名' },
  { label: '学号_姓名_作业名_版本', value: '学号_姓名_作业名_版本' },
  { label: '学号_作业名', value: '学号_作业名' }
]
const form = reactive({
  title: '',
  description: '',
  deadline: '',
  classId: null,
  fileTypes: ['zip', 'docx', 'pdf'],
  maxSizeMb: 100,
  renamePattern: '学号_姓名_作业名'
})

async function loadClasses() {
  classes.value = await listClasses(user.id)
  if (!form.classId && classes.value.length > 0) {
    form.classId = classes.value[0].id
  }
}

async function loadAssignment() {
  if (!isEditing.value) {
    return
  }
  const assignment = await getAssignment(route.params.id)
  form.title = assignment.title || ''
  form.description = assignment.description || ''
  form.deadline = assignment.deadline || ''
  form.classId = assignment.classId || null
  form.fileTypes = assignment.fileTypes
    ? assignment.fileTypes.split(',').map((item) => item.trim()).filter(Boolean)
    : ['zip', 'docx', 'pdf']
  form.maxSizeMb = assignment.maxSizeMb || 100
  form.renamePattern = assignment.renamePattern || '学号_姓名_作业名'
}

function isInvalidDeadline() {
  if (!form.deadline) {
    return false
  }
  return new Date(form.deadline).getTime() <= Date.now()
}

async function save(status) {
  if (!form.title || !form.deadline || !form.classId) {
    ElMessage.warning('请填写标题、截止时间并选择班级')
    return
  }
  if (isInvalidDeadline()) {
    ElMessage.warning('时间设置无效')
    return
  }
  if (form.fileTypes.length === 0) {
    ElMessage.warning('请至少选择一种允许文件类型')
    return
  }
  const loading = status === 'DRAFT' ? savingDraft : publishing
  loading.value = true
  try {
    const payload = {
      ...form,
      fileTypes: form.fileTypes.join(','),
      createdBy: user.username || user.realName,
      status
    }
    if (isEditing.value) {
      await updateAssignment(route.params.id, payload)
    } else {
      await createAssignment(payload)
    }
    ElMessage.success(status === 'DRAFT' ? '草稿已保存' : '作业已发布')
    router.push('/admin/assignments')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadClasses()
  await loadAssignment()
})
</script>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: minmax(320px, 1fr) minmax(360px, 1.1fr);
  gap: 22px;
}

.form-section h3 {
  margin: 0 0 16px;
  font-size: 17px;
}

.file-type-options {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(138px, 1fr));
  gap: 10px;
  width: 100%;
}

.file-type-options :deep(.el-checkbox) {
  height: 36px;
  margin-right: 0;
}

.form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
  margin-top: 20px;
  padding-top: 18px;
  border-top: 1px solid var(--app-border-soft);
}

.naming-meta {
  margin: 8px 0 0;
}

@media (max-width: 980px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}
</style>
