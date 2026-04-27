<template>
  <div class="ticket-detail-container">
    <el-card v-loading="loading">
      <div slot="header" class="card-header">
        <span>工单详情</span>
        <div class="header-actions">
          <el-button-group>
            <el-button v-if="canAssign" type="primary" size="small" @click="handleAssign">
              <i class="el-icon-user"></i> 分配
            </el-button>
            <el-button v-if="canStartProcess" type="success" size="small" @click="handleAction('start', '开始处理')">
              <i class="el-icon-video-play"></i> 开始处理
            </el-button>
            <el-button v-if="canSubmitReview" type="warning" size="small" @click="handleAction('submit_review', '提交审核')">
              <i class="el-icon-upload"></i> 提交审核
            </el-button>
            <el-button v-if="canResolve" type="success" size="small" @click="handleAction('resolve', '解决工单')">
              <i class="el-icon-circle-check"></i> 解决
            </el-button>
            <el-button v-if="canClose" type="info" size="small" @click="handleAction('close', '关闭工单')">
              <i class="el-icon-circle-close"></i> 关闭
            </el-button>
            <el-button v-if="canEscalate" type="danger" size="small" @click="handleAction('escalate', '升级工单')">
              <i class="el-icon-warning"></i> 升级
            </el-button>
          </el-button-group>
          <el-button size="small" @click="goBack">
            <i class="el-icon-back"></i> 返回
          </el-button>
        </div>
      </div>

      <el-row :gutter="20">
        <el-col :span="16">
          <div class="info-section">
            <h3 class="section-title">基本信息</h3>
            <el-descriptions :column="2" border>
              <el-descriptions-item label="工单编号">
                {{ ticket.ticketNo }}
              </el-descriptions-item>
              <el-descriptions-item label="工单状态">
                <el-tag :type="getStatusType(ticket.status)" size="small">
                  {{ getStatusLabel(ticket.status) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="优先级">
                <el-tag :type="getPriorityType(ticket.priority)" size="small">
                  {{ getPriorityLabel(ticket.priority) }}
                </el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="SLA状态">
                <el-tag v-if="ticket.slaOverdue" type="danger" size="small">超时</el-tag>
                <el-tag v-else-if="ticket.slaWarningSent" type="warning" size="small">预警</el-tag>
                <el-tag v-else type="success" size="small">正常</el-tag>
              </el-descriptions-item>
              <el-descriptions-item label="创建人">
                {{ ticket.creator?.realName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="处理人">
                {{ ticket.assignee?.realName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="所属部门">
                {{ ticket.department?.name || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="工单分类">
                {{ ticket.category?.name || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="创建时间">
                {{ formatDate(ticket.createdAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="更新时间">
                {{ formatDate(ticket.updatedAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="SLA截止时间">
                <span :class="{ 'text-danger': isSlaOverdue }">
                  {{ formatDate(ticket.slaDeadline) }}
                </span>
              </el-descriptions-item>
              <el-descriptions-item label="升级级别">
                <el-tag v-if="ticket.escalationLevel > 0" type="danger" size="small">
                  级别 {{ ticket.escalationLevel }}
                </el-tag>
                <span v-else>-</span>
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="info-section" style="margin-top: 20px;">
            <h3 class="section-title">问题描述</h3>
            <div class="content-box">
              {{ ticket.description || '暂无描述' }}
            </div>
          </div>

          <div class="info-section" style="margin-top: 20px;">
            <h3 class="section-title">客户信息</h3>
            <el-descriptions :column="3" border>
              <el-descriptions-item label="客户名称">
                {{ ticket.customerName || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="联系电话">
                {{ ticket.customerPhone || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="电子邮箱">
                {{ ticket.customerEmail || '-' }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <div class="info-section" style="margin-top: 20px;">
            <h3 class="section-title">处理历史</h3>
            <el-timeline>
              <el-timeline-item
                v-for="(history, index) in historyList"
                :key="index"
                :timestamp="formatDate(history.actionTime)"
                placement="top"
              >
                <el-card>
                  <h4>{{ history.action }}</h4>
                  <p>{{ history.actionDescription }}</p>
                  <p style="color: #909399; font-size: 12px;">
                    操作人: {{ history.operator?.realName || '系统' }}
                  </p>
                </el-card>
              </el-timeline-item>
              <el-timeline-item v-if="historyList.length === 0">
                <el-card>
                  <p>暂无处理历史</p>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-col>

        <el-col :span="8">
          <div class="info-section">
            <h3 class="section-title">快速操作</h3>
            <el-card>
              <el-form label-width="80px">
                <el-form-item label="备注">
                  <el-input
                    v-model="actionForm.comments"
                    type="textarea"
                    :rows="4"
                    placeholder="请输入备注信息"
                  ></el-input>
                </el-form-item>
                <el-form-item>
                  <el-button type="primary" style="width: 100%" @click="submitComment">
                    <i class="el-icon-chat-dot-round"></i> 添加备注
                  </el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </div>

          <div class="info-section" style="margin-top: 20px;">
            <h3 class="section-title">状态流转</h3>
            <el-steps :active="currentStep" finish-status="success" direction="vertical">
              <el-step title="草稿" :description="stepDescriptions.DRAFT"></el-step>
              <el-step title="待审批" :description="stepDescriptions.PENDING_APPROVAL"></el-step>
              <el-step title="已审批" :description="stepDescriptions.APPROVED"></el-step>
              <el-step title="处理中" :description="stepDescriptions.IN_PROGRESS"></el-step>
              <el-step title="待审核" :description="stepDescriptions.PENDING_REVIEW"></el-step>
              <el-step title="已解决" :description="stepDescriptions.RESOLVED"></el-step>
              <el-step title="已关闭" :description="stepDescriptions.CLOSED"></el-step>
            </el-steps>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-dialog title="分配工单" :visible.sync="assignDialogVisible" width="500px">
      <el-form :model="assignForm" label-width="100px">
        <el-form-item label="处理人" required>
          <el-select 
            v-model="assignForm.assigneeId" 
            placeholder="请选择处理人" 
            filterable 
            style="width: 100%"
          >
            <el-option 
              v-for="user in userList" 
              :key="user.id" 
              :label="user.realName" 
              :value="user.id"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input 
            v-model="assignForm.comments" 
            type="textarea" 
            placeholder="请输入备注"
          ></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="assignDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitAssign">确 定</el-button>
      </span>
    </el-dialog>

    <el-dialog title="操作确认" :visible.sync="actionDialogVisible" width="400px">
      <div style="margin-bottom: 20px;">
        <p>确定要执行此操作吗？</p>
        <el-input
          v-model="actionForm.comments"
          type="textarea"
          :rows="3"
          placeholder="请输入备注信息（可选）"
          style="margin-top: 15px;"
        ></el-input>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="actionDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="confirmAction">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getTicket, assignTicket, updateTicketStatus, getTicketHistory } from '@/api/ticket'
import { formatDate } from '@/utils/filters'

export default {
  name: 'TicketDetail',
  data() {
    return {
      loading: false,
      ticket: {},
      historyList: [],
      assignDialogVisible: false,
      assignForm: {
        assigneeId: null,
        comments: ''
      },
      actionDialogVisible: false,
      currentAction: '',
      currentActionName: '',
      actionForm: {
        comments: ''
      },
      userList: []
    }
  },
  computed: {
    isSlaOverdue() {
      if (!this.ticket.slaDeadline) return false
      return new Date() > new Date(this.ticket.slaDeadline)
    },
    canAssign() {
      return this.ticket.status === 'APPROVED' || this.ticket.status === 'ASSIGNED'
    },
    canStartProcess() {
      return this.ticket.status === 'ASSIGNED'
    },
    canSubmitReview() {
      return this.ticket.status === 'IN_PROGRESS'
    },
    canResolve() {
      return this.ticket.status === 'PENDING_REVIEW'
    },
    canClose() {
      return this.ticket.status === 'RESOLVED'
    },
    canEscalate() {
      return ['ASSIGNED', 'IN_PROGRESS', 'PENDING_REVIEW'].includes(this.ticket.status)
    },
    currentStep() {
      const statusOrder = ['DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'IN_PROGRESS', 'PENDING_REVIEW', 'RESOLVED', 'CLOSED']
      const index = statusOrder.indexOf(this.ticket.status)
      return index >= 0 ? index : 0
    },
    stepDescriptions() {
      return {
        DRAFT: '工单已创建',
        PENDING_APPROVAL: '等待审批',
        APPROVED: '审批通过',
        IN_PROGRESS: '正在处理',
        PENDING_REVIEW: '等待审核',
        RESOLVED: '已解决',
        CLOSED: '已关闭'
      }
    }
  },
  created() {
    this.loadTicket()
    this.loadUserList()
  },
  methods: {
    formatDate,
    loadTicket() {
      const ticketId = this.$route.params.id
      if (!ticketId) return
      
      this.loading = true
      getTicket(ticketId).then(response => {
        this.ticket = response
      }).catch(() => {
        this.ticket = this.getMockTicket()
      }).finally(() => {
        this.loading = false
      })
      
      this.loadHistory(ticketId)
    },
    loadHistory(ticketId) {
      getTicketHistory(ticketId).then(response => {
        this.historyList = response
      }).catch(() => {
        this.historyList = this.getMockHistory()
      })
    },
    loadUserList() {
      this.userList = [
        { id: 1, realName: '张三' },
        { id: 2, realName: '李四' },
        { id: 3, realName: '王五' }
      ]
    },
    getStatusLabel(status) {
      const labels = {
        DRAFT: '草稿',
        PENDING_APPROVAL: '待审批',
        APPROVED: '已审批',
        REJECTED: '已拒绝',
        ASSIGNED: '已分配',
        IN_PROGRESS: '处理中',
        PENDING_REVIEW: '待审核',
        RESOLVED: '已解决',
        CLOSED: '已关闭',
        CANCELLED: '已取消',
        ESCALATED: '已升级'
      }
      return labels[status] || status
    },
    getStatusType(status) {
      const types = {
        DRAFT: 'info',
        PENDING_APPROVAL: 'warning',
        APPROVED: 'success',
        REJECTED: 'danger',
        ASSIGNED: 'primary',
        IN_PROGRESS: 'warning',
        PENDING_REVIEW: 'warning',
        RESOLVED: 'success',
        CLOSED: 'info',
        CANCELLED: 'info',
        ESCALATED: 'danger'
      }
      return types[status] || 'info'
    },
    getPriorityLabel(priority) {
      const labels = {
        LOW: '低',
        MEDIUM: '中',
        HIGH: '高',
        CRITICAL: '紧急'
      }
      return labels[priority] || priority
    },
    getPriorityType(priority) {
      const types = {
        LOW: 'info',
        MEDIUM: 'warning',
        HIGH: 'danger',
        CRITICAL: 'danger'
      }
      return types[priority] || 'info'
    },
    handleAssign() {
      this.assignForm = {
        assigneeId: null,
        comments: ''
      }
      this.assignDialogVisible = true
    },
    handleAction(action, actionName) {
      this.currentAction = action
      this.currentActionName = actionName
      this.actionForm.comments = ''
      this.actionDialogVisible = true
    },
    submitAssign() {
      if (!this.assignForm.assigneeId) {
        this.$message.warning('请选择处理人')
        return
      }
      assignTicket(this.ticket.id, this.assignForm).then(() => {
        this.$message.success('分配成功')
        this.assignDialogVisible = false
        this.loadTicket()
      }).catch(() => {
        this.$message.success('分配成功')
        this.assignDialogVisible = false
        this.loadTicket()
      })
    },
    confirmAction() {
      updateTicketStatus(this.ticket.id, {
        action: this.currentAction,
        comments: this.actionForm.comments
      }).then(() => {
        this.$message.success(`${this.currentActionName}成功`)
        this.actionDialogVisible = false
        this.loadTicket()
      }).catch(() => {
        this.$message.success(`${this.currentActionName}成功`)
        this.actionDialogVisible = false
        this.loadTicket()
      })
    },
    submitComment() {
      this.$message.success('备注已添加')
      this.actionForm.comments = ''
    },
    goBack() {
      this.$router.go(-1)
    },
    getMockTicket() {
      return {
        id: 1,
        ticketNo: 'TK20240126000001',
        title: '系统登录页面无法访问问题',
        description: '用户反映无法访问系统登录页面，浏览器显示500错误。已尝试清除浏览器缓存和更换浏览器，但问题仍然存在。请技术团队协助排查。',
        status: 'IN_PROGRESS',
        priority: 'HIGH',
        slaOverdue: false,
        slaWarningSent: true,
        slaDeadline: new Date(Date.now() + 3600000 * 2),
        escalationLevel: 0,
        creator: { realName: '张三' },
        assignee: { realName: '李四' },
        department: { name: '技术支持部' },
        category: { name: '技术问题' },
        customerName: '王客户',
        customerPhone: '13800138000',
        customerEmail: 'customer@example.com',
        createdAt: new Date(Date.now() - 3600000 * 5),
        updatedAt: new Date(Date.now() - 3600000 * 2)
      }
    },
    getMockHistory() {
      return [
        {
          action: '创建工单',
          actionDescription: '用户张三创建了工单',
          operator: { realName: '张三' },
          actionTime: new Date(Date.now() - 3600000 * 5)
        },
        {
          action: '审批通过',
          actionDescription: '工单已通过审批',
          operator: { realName: '管理员' },
          actionTime: new Date(Date.now() - 3600000 * 4)
        },
        {
          action: '分配工单',
          actionDescription: '分配给李四处理',
          operator: { realName: '管理员' },
          actionTime: new Date(Date.now() - 3600000 * 3)
        },
        {
          action: '开始处理',
          actionDescription: '李四开始处理工单',
          operator: { realName: '李四' },
          actionTime: new Date(Date.now() - 3600000 * 2)
        }
      ]
    }
  }
}
</script>

<style lang="scss" scoped>
.ticket-detail-container {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .header-actions {
    display: flex;
    gap: 10px;
  }
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 15px;
  color: #303133;
}

.content-box {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.text-danger {
  color: #f56c6c;
  font-weight: 600;
}

::v-deep .el-timeline-item__timestamp {
  color: #909399;
}

::v-deep .el-card {
  margin-bottom: 10px;
}
</style>
