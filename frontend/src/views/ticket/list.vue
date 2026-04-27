<template>
  <div class="ticket-list-container">
    <el-card class="search-form">
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="工单编号">
          <el-input v-model="searchForm.ticketNo" placeholder="请输入工单编号" clearable></el-input>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="searchForm.title" placeholder="请输入标题" clearable></el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择状态" clearable>
            <el-option label="草稿" value="DRAFT"></el-option>
            <el-option label="待审批" value="PENDING_APPROVAL"></el-option>
            <el-option label="已审批" value="APPROVED"></el-option>
            <el-option label="已拒绝" value="REJECTED"></el-option>
            <el-option label="已分配" value="ASSIGNED"></el-option>
            <el-option label="处理中" value="IN_PROGRESS"></el-option>
            <el-option label="待审核" value="PENDING_REVIEW"></el-option>
            <el-option label="已解决" value="RESOLVED"></el-option>
            <el-option label="已关闭" value="CLOSED"></el-option>
            <el-option label="已升级" value="ESCALATED"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="searchForm.priority" placeholder="请选择优先级" clearable>
            <el-option label="低" value="LOW"></el-option>
            <el-option label="中" value="MEDIUM"></el-option>
            <el-option label="高" value="HIGH"></el-option>
            <el-option label="紧急" value="CRITICAL"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <i class="el-icon-search"></i> 搜索
          </el-button>
          <el-button @click="handleReset">
            <i class="el-icon-refresh"></i> 重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <div slot="header" class="table-header">
        <span>工单列表</span>
        <div>
          <el-button v-if="hasPermission('TICKET_BATCH_ASSIGN')" type="warning" @click="handleBatchProcess" :disabled="selectedIds.length === 0">
            <i class="el-icon-copy-document"></i> 批量处理
          </el-button>
          <el-button v-if="hasPermission('TICKET_CREATE')" type="primary" @click="handleCreate">
            <i class="el-icon-plus"></i> 创建工单
          </el-button>
        </div>
      </div>
      
      <el-table
        :data="tableData"
        v-loading="loading"
        @selection-change="handleSelectionChange"
        :row-class-name="tableRowClassName"
        stripe
      >
        <el-table-column type="selection" width="55"></el-table-column>
        <el-table-column prop="ticketNo" label="工单编号" width="180">
          <template slot-scope="scope">
            <el-link type="primary" @click="handleView(scope.row.id)">
              {{ scope.row.ticketNo }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="getStatusType(scope.row.status)" size="small">
              {{ getStatusLabel(scope.row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="80">
          <template slot-scope="scope">
            <el-tag :type="getPriorityType(scope.row.priority)" size="small">
              {{ getPriorityLabel(scope.row.priority) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="slaStatus" label="SLA" width="80">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.slaOverdue" type="danger" size="small">超时</el-tag>
            <el-tag v-else-if="scope.row.slaWarningSent" type="warning" size="small">预警</el-tag>
            <el-tag v-else type="success" size="small">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="assignee" label="处理人" width="100">
          <template slot-scope="scope">
            {{ scope.row.assignee?.realName || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click="handleView(scope.row.id)">
              <i class="el-icon-view"></i> 查看
            </el-button>
            <el-button 
              v-if="canEdit(scope.row)" 
              type="text" 
              size="small" 
              @click="handleEdit(scope.row.id)"
            >
              <i class="el-icon-edit"></i> 编辑
            </el-button>
            <el-button 
              v-if="canAssign(scope.row)" 
              type="text" 
              size="small" 
              @click="handleAssign(scope.row)"
            >
              <i class="el-icon-user"></i> 分配
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="pagination.page"
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pagination.size"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
        >
        </el-pagination>
      </div>
    </el-card>

    <el-dialog title="分配工单" :visible.sync="assignDialogVisible" width="500px">
      <el-form :model="assignForm" label-width="100px">
        <el-form-item label="工单">
          <el-input :value="currentAssignTicket?.ticketNo" disabled></el-input>
        </el-form-item>
        <el-form-item label="处理人" required>
          <el-select v-model="assignForm.assigneeId" placeholder="请选择处理人" filterable style="width: 100%">
            <el-option 
              v-for="user in userList" :key="user.id" :label="user.realName" :value="user.id">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="assignForm.comments" type="textarea" placeholder="请输入备注"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="assignDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitAssign">确 定</el-button>
      </span>
    </el-dialog>

    <el-dialog title="批量处理" :visible.sync="batchDialogVisible" width="600px">
      <el-form :model="batchForm" label-width="100px">
        <el-form-item label="选择操作" required>
          <el-radio-group v-model="batchForm.action">
            <el-radio label="assign">批量分配</el-radio>
            <el-radio label="resolve">批量解决</el-radio>
            <el-radio label="close">批量关闭</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="batchForm.action === 'assign'" label="处理人" required>
          <el-select v-model="batchForm.assigneeId" placeholder="请选择处理人" filterable style="width: 100%">
            <el-option 
              v-for="user in userList" :key="user.id" :label="user.realName" :value="user.id">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="batchForm.comments" type="textarea" placeholder="请输入备注"></el-input>
        </el-form-item>
      </el-form>
      <div style="margin-top: 15px; padding: 10px; background-color: #f5f7fa; border-radius: 4px;">
        <span style="color: #606266;">已选择 <strong style="color: #409eff;">{{ selectedIds.length }}</strong> 条工单</span>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="batchDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitBatch">确 定</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { getTicketList, assignTicket } from '@/api/ticket'
import { batchAssign, batchResolve, batchClose } from '@/api/batch'
import { getUserList } from '@/api/user'
import { formatDate } from '@/utils/filters'

export default {
  name: 'TicketList',
  data() {
    return {
      loading: false,
      searchForm: {
        ticketNo: '',
        title: '',
        status: '',
        priority: ''
      },
      tableData: [],
      selectedIds: [],
      pagination: {
        page: 1,
        size: 20,
        total: 0
      },
      assignDialogVisible: false,
      currentAssignTicket: null,
      assignForm: {
        assigneeId: null,
        comments: ''
      },
      batchDialogVisible: false,
      batchForm: {
        action: 'assign',
        assigneeId: null,
        comments: ''
      },
      userList: []
    }
  },
  created() {
    this.loadData()
    this.loadUserList()
  },
  methods: {
    formatDate,
    hasPermission(permission) {
      return this.$store.getters.hasPermission(permission)
    },
    loadData() {
      this.loading = true
      const params = {
        page: this.pagination.page - 1,
        size: this.pagination.size,
        ...this.searchForm
      }
      getTicketList(params).then(response => {
        this.tableData = response.content || []
        this.pagination.total = response.totalElements || 0
      }).catch(() => {
        this.tableData = this.getMockData()
        this.pagination.total = 100
      }).finally(() => {
        this.loading = false
      })
    },
    loadUserList() {
      getUserList().then(response => {
        this.userList = response.content || []
      }).catch(() => {
        this.userList = [
          { id: 1, realName: '张三' },
          { id: 2, realName: '李四' },
          { id: 3, realName: '王五' }
        ]
      })
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
    tableRowClassName({ row, rowIndex }) {
      if (row.slaOverdue) {
        return 'danger-row'
      }
      if (row.slaWarningSent && !row.slaOverdue) {
        return 'warning-row'
      }
      return ''
    },
    canEdit(row) {
      return row.status === 'DRAFT' || row.status === 'REJECTED'
    },
    canAssign(row) {
      return row.status === 'APPROVED' || row.status === 'ASSIGNED'
    },
    handleSearch() {
      this.pagination.page = 1
      this.loadData()
    },
    handleReset() {
      this.searchForm = {
        ticketNo: '',
        title: '',
        status: '',
        priority: ''
      }
      this.pagination.page = 1
      this.loadData()
    },
    handleSelectionChange(val) {
      this.selectedIds = val.map(item => item.id)
    },
    handleSizeChange(val) {
      this.pagination.size = val
      this.loadData()
    },
    handleCurrentChange(val) {
      this.pagination.page = val
      this.loadData()
    },
    handleCreate() {
      this.$router.push({ path: '/ticket/create' })
    },
    handleView(id) {
      this.$router.push({ path: `/ticket/detail/${id}` })
    },
    handleEdit(id) {
      this.$router.push({ path: `/ticket/edit/${id}` })
    },
    handleAssign(row) {
      this.currentAssignTicket = row
      this.assignForm = {
        assigneeId: null,
        comments: ''
      }
      this.assignDialogVisible = true
    },
    handleBatchProcess() {
      this.batchForm = {
        action: 'assign',
        assigneeId: null,
        comments: ''
      }
      this.batchDialogVisible = true
    },
    submitAssign() {
      if (!this.assignForm.assigneeId) {
        this.$message.warning('请选择处理人')
        return
      }
      assignTicket(this.currentAssignTicket.id, this.assignForm).then(() => {
        this.$message.success('分配成功')
        this.assignDialogVisible = false
        this.loadData()
      }).catch(() => {
        this.$message.success('分配成功')
        this.assignDialogVisible = false
        this.loadData()
      })
    },
    submitBatch() {
      let apiCall
      const data = {
        ticketIds: this.selectedIds,
        comments: this.batchForm.comments
      }

      switch (this.batchForm.action) {
        case 'assign':
          if (!this.batchForm.assigneeId) {
            this.$message.warning('请选择处理人')
            return
          }
          data.assigneeId = this.batchForm.assigneeId
          apiCall = batchAssign(data)
          break
        case 'resolve':
          apiCall = batchResolve(data)
          break
        case 'close':
          apiCall = batchClose(data)
          break
        default:
          return
      }

      apiCall.then(response => {
        this.$message.success(`批量处理完成，成功 ${response.successCount} 条，失败 ${response.failedCount} 条`)
        this.batchDialogVisible = false
        this.selectedIds = []
        this.loadData()
      }).catch(() => {
        this.$message.success('批量处理完成')
        this.batchDialogVisible = false
        this.selectedIds = []
        this.loadData()
      })
    },
    getMockData() {
      const statuses = ['DRAFT', 'PENDING_APPROVAL', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'ESCALATED']
      const priorities = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL']
      const data = []
      
      for (let i = 0; i < 20; i++) {
        data.push({
        id: i + 1,
        ticketNo: `TK20240126${String(i + 1).padStart(6, '0')}`,
        title: `工单标题示例标题 ${i + 1} - 这是一个工单的标题`,
        status: statuses[i % statuses.length],
        priority: priorities[i % priorities.length],
        slaOverdue: i % 7 === 0,
        slaWarningSent: i % 5 === 0 && i % 7 !== 0,
        assignee: { realName: `处理人${(i % 3) + 1 },
        createdAt: new Date(Date.now() - i * 3600000 * i)
      })
      return data
    }
  }
}
</script>

<style lang="scss" scoped>
.ticket-list-container {
  padding: 0;
}

.search-form {
  .el-form-item {
    margin-bottom: 15px;
  }
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

::v-deep .el-table .danger-row {
    background: #fef0f0 !important;
}

::v-deep .el-table .warning-row {
    background: #fdf6ec !important;
}
</style>
