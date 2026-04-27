<template>
  <div class="created-tickets-container">
    <el-card>
      <div slot="header" class="card-header">
        <span>我创建的工单</span>
      </div>
      
      <el-table
        :data="tableData"
        v-loading="loading"
        :row-class-name="tableRowClassName"
        stripe
        @row-click="handleRowClick"
        style="cursor: pointer;"
      >
        <el-table-column prop="ticketNo" label="工单编号" width="180">
          <template slot-scope="scope">
            <el-link type="primary">{{ scope.row.ticketNo }}</el-link>
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
        <el-table-column prop="assignee" label="处理人" width="100">
          <template slot-scope="scope">
            {{ scope.row.assignee?.realName || '-' }}
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
        <el-table-column prop="createdAt" label="创建时间" width="160">
          <template slot-scope="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template slot-scope="scope">
            <el-button type="text" size="small" @click.stop="handleView(scope.row.id)">
              <i class="el-icon-view"></i> 查看
            </el-button>
            <el-button 
              v-if="canEdit(scope.row)" 
              type="text" 
              size="small" 
              @click.stop="handleEdit(scope.row.id)"
            >
              <i class="el-icon-edit"></i> 编辑
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
  </div>
</template>

<script>
import { getCreatedTickets } from '@/api/ticket'
import { formatDate } from '@/utils/filters'

export default {
  name: 'CreatedTickets',
  data() {
    return {
      loading: false,
      tableData: [],
      pagination: {
        page: 1,
        size: 20,
        total: 0
      }
    }
  },
  created() {
    this.loadData()
  },
  methods: {
    formatDate,
    loadData() {
      this.loading = true
      const params = {
        page: this.pagination.page - 1,
        size: this.pagination.size
      }
      getCreatedTickets(params).then(response => {
        this.tableData = response.content || []
        this.pagination.total = response.totalElements || 0
      }).catch(() => {
        this.tableData = this.getMockData()
        this.pagination.total = 30
      }).finally(() => {
        this.loading = false
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
    handleSizeChange(val) {
      this.pagination.size = val
      this.loadData()
    },
    handleCurrentChange(val) {
      this.pagination.page = val
      this.loadData()
    },
    handleRowClick(row) {
      this.$router.push({ path: `/ticket/detail/${row.id}` })
    },
    handleView(id) {
      this.$router.push({ path: `/ticket/detail/${id}` })
    },
    handleEdit(id) {
      this.$router.push({ path: `/ticket/edit/${id}` })
    },
    getMockData() {
      const statuses = ['DRAFT', 'PENDING_APPROVAL', 'ASSIGNED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED']
      const priorities = ['LOW', 'MEDIUM', 'HIGH']
      const assignees = ['张三', '李四', '王五', null]
      const data = []
      
      for (let i = 0; i < 15; i++) {
        data.push({
          id: i + 1,
          ticketNo: `TK20240126${String(i + 1).padStart(6, '0')}`,
          title: `我创建的工单示例标题 ${i + 1}`,
          status: statuses[i % statuses.length],
          priority: priorities[i % priorities.length],
          slaOverdue: i % 8 === 0,
          slaWarningSent: i % 4 === 0 && i % 8 !== 0,
          assignee: assignees[i % assignees.length] ? { realName: assignees[i % assignees.length] } : null,
          createdAt: new Date(Date.now() - i * 3600000 * i)
        })
      }
      return data
    }
  }
}
</script>

<style lang="scss" scoped>
.created-tickets-container {
  padding: 0;
}

.card-header {
  font-size: 16px;
  font-weight: 600;
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
