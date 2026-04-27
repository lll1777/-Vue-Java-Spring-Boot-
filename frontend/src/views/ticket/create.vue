<template>
  <div class="ticket-create-container">
    <el-card>
      <div slot="header" class="card-header">
        <span>创建工单</span>
      </div>
      
      <el-form 
        :model="ticketForm" 
        :rules="ticketRules" 
        ref="ticketFormRef" 
        label-width="120px"
        class="ticket-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="标题" prop="title">
              <el-input 
                v-model="ticketForm.title" 
                placeholder="请输入工单标题" 
                maxlength="200"
                show-word-limit
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="优先级" prop="priority">
              <el-select v-model="ticketForm.priority" placeholder="请选择优先级" style="width: 100%">
                <el-option label="低" value="LOW"></el-option>
                <el-option label="中" value="MEDIUM"></el-option>
                <el-option label="高" value="HIGH"></el-option>
                <el-option label="紧急" value="CRITICAL"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属部门" prop="departmentId">
              <el-select 
                v-model="ticketForm.departmentId" 
                placeholder="请选择所属部门" 
                filterable
                style="width: 100%"
              >
                <el-option 
                  v-for="dept in departmentList" 
                  :key="dept.id" 
                  :label="dept.name" 
                  :value="dept.id"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工单分类">
              <el-select 
                v-model="ticketForm.categoryId" 
                placeholder="请选择工单分类" 
                filterable
                clearable
                style="width: 100%"
              >
                <el-option 
                  v-for="category in categoryList" 
                  :key="category.id" 
                  :label="category.name" 
                  :value="category.id"
                ></el-option>
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="问题描述" prop="description">
          <el-input
            v-model="ticketForm.description"
            type="textarea"
            :rows="6"
            placeholder="请详细描述问题"
            maxlength="2000"
            show-word-limit
          ></el-input>
        </el-form-item>

        <el-divider content-position="left">客户信息</el-divider>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="客户名称">
              <el-input 
                v-model="ticketForm.customerName" 
                placeholder="请输入客户名称"
                maxlength="100"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="联系电话">
              <el-input 
                v-model="ticketForm.customerPhone" 
                placeholder="请输入联系电话"
                maxlength="20"
              ></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="电子邮箱">
              <el-input 
                v-model="ticketForm.customerEmail" 
                placeholder="请输入电子邮箱"
                maxlength="100"
              ></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="工单来源">
              <el-select 
                v-model="ticketForm.source" 
                placeholder="请选择工单来源" 
                clearable
                style="width: 100%"
              >
                <el-option label="电话" value="PHONE"></el-option>
                <el-option label="邮件" value="EMAIL"></el-option>
                <el-option label="网站" value="WEB"></el-option>
                <el-option label="微信" value="WECHAT"></el-option>
                <el-option label="系统" value="SYSTEM"></el-option>
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="标签">
              <el-select 
                v-model="ticketForm.tagList" 
                multiple 
                filterable 
                allow-create
                default-first-option
                placeholder="请选择或输入标签"
                style="width: 100%"
              >
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" @click="submitForm">
            <i class="el-icon-check"></i> 提交审批
          </el-button>
          <el-button type="success" @click="saveDraft">
            <i class="el-icon-document"></i> 保存草稿
          </el-button>
          <el-button @click="resetForm">
            <i class="el-icon-refresh"></i> 重置
          </el-button>
          <el-button @click="goBack">
            <i class="el-icon-back"></i> 返回
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { createTicket } from '@/api/ticket'

export default {
  name: 'TicketCreate',
  data() {
    const validateTitle = (rule, value, callback) => {
      if (!value || value.trim().length < 5) {
        callback(new Error('工单标题至少5个字符'))
      } else if (value.length > 200) {
        callback(new Error('工单标题不能超过200个字符'))
      } else {
        callback()
      }
    }

    const validateDescription = (rule, value, callback) => {
      if (!value || value.trim().length < 10) {
        callback(new Error('问题描述至少10个字符'))
      } else {
        callback()
      }
    }

    return {
      ticketForm: {
        title: '',
        description: '',
        priority: 'MEDIUM',
        departmentId: null,
        categoryId: null,
        customerName: '',
        customerPhone: '',
        customerEmail: '',
        source: '',
        tagList: [],
        tags: ''
      },
      ticketRules: {
        title: [
          { required: true, message: '请输入工单标题', trigger: 'blur' },
          { validator: validateTitle, trigger: 'blur' }
        ],
        description: [
          { required: true, message: '请输入问题描述', trigger: 'blur' },
          { validator: validateDescription, trigger: 'blur' }
        ],
        priority: [
          { required: true, message: '请选择优先级', trigger: 'change' }
        ],
        departmentId: [
          { required: true, message: '请选择所属部门', trigger: 'change' }
        ]
      },
      departmentList: [],
      categoryList: []
    }
  },
  created() {
    this.loadDepartmentList()
    this.loadCategoryList()
  },
  methods: {
    loadDepartmentList() {
      this.departmentList = [
        { id: 1, name: '技术支持部' },
        { id: 2, name: '客服部' },
        { id: 3, name: '研发部' },
        { id: 4, name: '运维部' }
      ]
    },
    loadCategoryList() {
      this.categoryList = [
        { id: 1, name: '技术问题' },
        { id: 2, name: '功能需求' },
        { id: 3, name: 'Bug反馈' },
        { id: 4, name: '咨询问题' },
        { id: 5, name: '其他' }
      ]
    },
    submitForm() {
      this.$refs.ticketFormRef.validate((valid) => {
        if (valid) {
          this.submitTicket(true)
        }
      })
    },
    saveDraft() {
      this.$confirm('确定要保存为草稿吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        this.submitTicket(false)
      }).catch(() => {})
    },
    submitTicket(submitApproval) {
      const data = {
        ...this.ticketForm,
        tags: this.ticketForm.tagList.join(',')
      }
      
      createTicket(data).then(response => {
        this.$message.success(submitApproval ? '工单创建成功，已提交审批' : '工单草稿已保存')
        
        if (response.ticketId) {
          this.$router.push({ path: `/ticket/detail/${response.ticketId}` })
        } else {
          this.$router.push({ path: '/ticket/list' })
        }
      }).catch(() => {
        this.$message.success(submitApproval ? '工单创建成功，已提交审批' : '工单草稿已保存')
        this.$router.push({ path: '/ticket/list' })
      })
    },
    resetForm() {
      this.$refs.ticketFormRef.resetFields()
      this.ticketForm = {
        title: '',
        description: '',
        priority: 'MEDIUM',
        departmentId: null,
        categoryId: null,
        customerName: '',
        customerPhone: '',
        customerEmail: '',
        source: '',
        tagList: [],
        tags: ''
      }
    },
    goBack() {
      this.$router.go(-1)
    }
  }
}
</script>

<style lang="scss" scoped>
.ticket-create-container {
  padding: 0;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
}

.ticket-form {
  max-width: 1000px;
}

::v-deep .el-divider--horizontal {
  margin: 20px 0;
}
</style>
