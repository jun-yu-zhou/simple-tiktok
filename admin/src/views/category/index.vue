<template>
  <div class="category-page">
    <el-card shadow="never">
      <div slot="header" class="toolbar">
        <div class="title">分类管理</div>
        <div class="actions">
          <el-input
            v-model="keyword"
            size="small"
            clearable
            placeholder="按名称筛选"
            class="search-input"
          />
          <el-button size="small" @click="loadList">刷新</el-button>
          <el-button type="primary" size="small" @click="openCreate">新增分类</el-button>
        </div>
      </div>

      <el-table :data="viewList" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" min-width="140" />
        <el-table-column prop="description" label="描述" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template slot-scope="{ row }">
            <el-tag size="mini" :type="Number(row.open) === 1 ? 'success' : 'info'">
              {{ Number(row.open) === 1 ? '公开' : '私有' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column label="标签" min-width="220">
          <template slot-scope="{ row }">
            <span>{{ formatLabels(row.labelNames) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="{ row }">
            <el-button
              type="text"
              size="mini"
              style="color: #409eff; margin-right: 16px;"
              @click="openEdit(row)"
            >编辑</el-button>
            <el-button
              type="text"
              size="mini"
              style="color: #f56c6c;"
              :loading="deletingId === row.id"
              @click="confirmDelete(row)"
            >删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      :title="form.id ? '编辑分类' : '新增分类'"
      :visible.sync="dialogVisible"
      width="560px"
      custom-class="category-dialog"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="分类描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="255" show-word-limit />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.open">
            <el-radio :label="1">公开</el-radio>
            <el-radio :label="0">私有</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="可选，例如：mdi-view-grid" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" controls-position="right" />
        </el-form-item>
        <el-form-item label="标签">
          <div class="tag-editor" @click="focusTagInput">
            <el-tag
              v-for="(tag, index) in form.labelNames"
              :key="`${tag}-${index}`"
              size="small"
              closable
              effect="plain"
              class="tag-chip"
              @close="removeTag(index)"
            >
              {{ tag }}
            </el-tag>
            <el-input
              ref="tagInputRef"
              v-model="tagInput"
              size="small"
              class="tag-input"
              placeholder="输入标签后按回车"
              @keyup.enter.native.prevent="addTagsFromInput"
              @keydown.native="onTagInputKeydown"
            />
          </div>
          <div class="tag-tip">按回车添加标签，输入逗号可一次添加多个</div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listType, saveType, deleteType } from '@/api/type'

const defaultForm = () => ({
  id: null,
  name: '',
  description: '',
  open: 1,
  icon: '',
  sort: 0,
  labelNames: []
})

export default {
  name: 'CategoryManage',
  data() {
    return {
      loading: false,
      submitLoading: false,
      list: [],
      keyword: '',
      dialogVisible: false,
      tagInput: '',
      deletingId: null,
      form: defaultForm(),
      rules: {
        name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }]
      }
    }
  },
  computed: {
    viewList() {
      const key = String(this.keyword || '').trim().toLowerCase()
      if (!key) {
        return this.list
      }
      return this.list.filter(item => String(item.name || '').toLowerCase().includes(key))
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    async loadList() {
      this.loading = true
      try {
        const res = await listType()
        const rows = Array.isArray(res.data) ? res.data : []
        this.list = rows.map(item => ({
          ...item,
          open: Number(item.open ?? 1),
          sort: Number(item.sort ?? 0),
          labelNames: Array.isArray(item.labelNames) ? item.labelNames : []
        }))
      } finally {
        this.loading = false
      }
    },
    openCreate() {
      this.resetForm()
      this.dialogVisible = true
    },
    openEdit(row) {
      this.form = {
        id: row.id,
        name: row.name || '',
        description: row.description || '',
        open: Number(row.open ?? 1),
        icon: row.icon || '',
        sort: Number(row.sort ?? 0),
        labelNames: Array.isArray(row.labelNames) ? Array.from(new Set(row.labelNames.filter(Boolean))) : []
      }
      this.tagInput = ''
      this.dialogVisible = true
    },
    resetForm() {
      this.form = defaultForm()
      this.tagInput = ''
      this.$nextTick(() => {
        if (this.$refs.formRef) {
          this.$refs.formRef.clearValidate()
        }
      })
    },
    focusTagInput() {
      if (this.$refs.tagInputRef) {
        this.$refs.tagInputRef.focus()
      }
    },
    addTagsFromInput() {
      const raw = String(this.tagInput || '').trim()
      if (!raw) {
        return
      }
      const parts = raw.split(/[,，]/).map(v => v.trim()).filter(Boolean)
      parts.forEach(item => this.addTag(item))
      this.tagInput = ''
    },
    addTag(tag) {
      const value = String(tag || '').trim()
      if (!value) {
        return
      }
      if (!Array.isArray(this.form.labelNames)) {
        this.$set(this.form, 'labelNames', [])
      }
      if (!this.form.labelNames.includes(value)) {
        this.form.labelNames.push(value)
      }
    },
    removeTag(index) {
      if (Array.isArray(this.form.labelNames)) {
        this.form.labelNames.splice(index, 1)
      }
    },
    onTagInputKeydown(event) {
      // 输入框为空时，按退格快速删除最后一个标签
      if (event.key === 'Backspace' && !this.tagInput && this.form.labelNames.length > 0) {
        this.form.labelNames.pop()
      }
    },
    formatLabels(labels) {
      if (!Array.isArray(labels) || labels.length === 0) {
        return '-'
      }
      return labels.join('，')
    },
    async confirmDelete(row) {
      if (!row || !row.id) {
        return
      }
      try {
        await this.$confirm('删除后不可恢复，确认删除该分类吗？', '删除提醒', {
          confirmButtonText: '确认删除',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await this.handleDelete(row)
      } catch (e) {
        // 用户取消删除时不提示错误
      }
    },
    async handleDelete(row) {
      if (!row || !row.id) {
        return
      }
      this.deletingId = row.id
      try {
        const res = await deleteType(row.id)
        this.$message.success(res.message || '删除成功')
        await this.loadList()
      } finally {
        this.deletingId = null
      }
    },
    submitForm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) {
          return
        }
        this.submitLoading = true
        try {
          const payload = {
            id: this.form.id || undefined,
            name: String(this.form.name || '').trim(),
            description: this.form.description || '',
            open: Number(this.form.open),
            icon: this.form.icon || '',
            sort: Number(this.form.sort || 0),
            labelNames: Array.from(new Set((this.form.labelNames || []).map(v => String(v || '').trim()).filter(Boolean)))
          }
          const res = await saveType(payload)
          this.$message.success(res.message || '保存成功')
          this.dialogVisible = false
          await this.loadList()
        } finally {
          this.submitLoading = false
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.category-page {
  margin: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.title {
  font-size: 15px;
  font-weight: 600;
}

.actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 220px;
}

.tag-editor {
  min-height: 40px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 6px 8px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  background: #ffffff;
  cursor: text;
}

.tag-editor:hover {
  border-color: #409eff;
  box-shadow: 0 0 0 1px rgba(64, 158, 255, 0.12);
}

.tag-chip {
  margin: 0;
}

::v-deep .tag-input {
  flex: 1;
  min-width: 160px;
}

::v-deep .tag-input .el-input__inner {
  border: none;
  height: 28px;
  line-height: 28px;
  padding: 0;
  background: transparent;
  box-shadow: none;
}

::v-deep .tag-input .el-input__inner:hover,
::v-deep .tag-input .el-input__inner:focus {
  border: none;
  background: transparent;
  box-shadow: none;
}

.tag-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}
</style>
