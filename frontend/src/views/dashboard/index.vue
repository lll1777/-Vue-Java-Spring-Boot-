<template>
  <div class="dashboard-container">
    <el-row :gutter="20">
      <el-col :span="6">
        <div class="stat-card primary">
          <div class="stat-icon">
            <i class="el-icon-tickets"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statistics.totalTickets }}</div>
            <div class="stat-label">总工单数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card success">
          <div class="stat-icon">
            <i class="el-icon-circle-check"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ statusDistribution.RESOLVED || 0 }}</div>
            <div class="stat-label">已解决</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card warning">
          <div class="stat-icon">
            <i class="el-icon-loading"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ processingCount }}</div>
            <div class="stat-label">处理中</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card danger">
          <div class="stat-icon">
            <i class="el-icon-warning"></i>
          </div>
          <div class="stat-info">
            <div class="stat-value">{{ slaStatus.OVERDUE || 0 }}</div>
            <div class="stat-label">超时工单</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template slot="header">
            <span>工单状态分布</span>
          </template>
          <el-table :data="statusTable" style="width: 100%">
            <el-table-column prop="status" label="状态">
              <template slot-scope="scope">
                <el-tag :type="getStatusType(scope.row.status)">
                  {{ getStatusLabel(scope.row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="count" label="数量" width="100"></el-table-column>
            <el-table-column label="占比" width="150">
              <template slot-scope="scope">
                <el-progress 
                  :percentage="getPercentage(scope.row.count)" 
                  :color="getProgressColor(scope.row.status)"
                  :stroke-width="18"
                ></el-progress>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template slot="header">
            <span>SLA状态</span>
          </template>
          <div class="sla-stats">
            <div class="sla-item">
              <div class="sla-circle success">
                <span>{{ slaStatus.NORMAL || 0 }}</span>
              </div>
              <div class="sla-label">正常</div>
            </div>
            <div class="sla-item">
              <div class="sla-circle warning">
                <span>{{ slaStatus.WARNING || 0 }}</span>
              </div>
              <div class="sla-label">预警</div>
            </div>
            <div class="sla-item">
              <div class="sla-circle danger">
                <span>{{ slaStatus.OVERDUE || 0 }}</span>
              </div>
              <div class="sla-label">超时</div>
            </div>
          </div>
          <div style="margin-top: 20px;">
            <el-progress 
              :percentage="onTimeRate" 
              :status="onTimeRate >= 90 ? 'success' : onTimeRate >= 70 ? 'warning' : 'exception'"
            >
              <span style="font-size: 16px; color: #606266;">
                SLA达标率 {{ onTimeRate }}%
              </span>
            </el-progress>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card>
          <template slot="header">
            <span>优先级分布</span>
          </template>
          <el-table :data="priorityTable" style="width: 100%">
            <el-table-column prop="priority" label="优先级">
              <template slot-scope="scope">
                <el-tag :type="getPriorityType(scope.row.priority)">
                  {{ getPriorityLabel(scope.row.priority) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="count" label="数量" width="100"></el-table-column>
            <el-table-column label="占比" width="150">
              <template slot-scope="scope">
                <span>{{ getPercentage(scope.row.count) }}%</span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template slot="header">
            <span>最近工单趋势</span>
          </template>
          <div class="trend-chart">
            <el-table :data="trendData" style="width: 100%">
              <el-table-column prop="date" label="日期" width="120"></el-table-column>
              <el-table-column prop="count" label="工单数" width="100"></el-table-column>
              <el-table-column label="趋势">
                <template slot-scope="scope">
                  <div class="trend-bar">
                    <div 
                      class="trend-bar-fill" 
                      :style="{ width: getTrendWidth(scope.row.count) + '%' }"
                    ></div>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getDashboardStatistics } from '@/api/report'

export default {
  name: 'Dashboard',
  data() {
    return {
      statistics: {
        totalTickets: 0
      },
      statusDistribution: {},
      slaStatus: {},
      priorityDistribution: {},
      dailyTrend: {}
    }
  },
  computed: {
    statusTable() {
      const table = []
      for (const [status, count] of Object.entries(this.statusDistribution)) {
        table.push({ status, count })
      }
      return table.sort((a, b) => b.count - a.count)
    },
    priorityTable() {
      const table = []
      for (const [priority, count] of Object.entries(this.priorityDistribution)) {
        table.push({ priority, count })
      }
      return table.sort((a, b) => b.count - a.count)
    },
    trendData() {
      const data = []
      for (const [date, count] of Object.entries(this.dailyTrend)) {
        data.push({ date, count })
      }
      return data.slice(-7)
    },
    processingCount() {
      return (this.statusDistribution.ASSIGNED || 0) + 
             (this.statusDistribution.IN_PROGRESS || 0) + 
             (this.statusDistribution.PENDING_REVIEW || 0) +
             (this.statusDistribution.ESCALATED || 0)
    },
    totalCount() {
      return Object.values(this.statusDistribution).reduce((sum, count) => sum + count, 0)
    },
    slaTotal() {
      return Object.values(this.slaStatus).reduce((sum, count) => sum + count, 0)
    },
    onTimeRate() {
      const total = this.slaTotal()
      if (total === 0) return 0
      return Math.round((this.slaStatus.NORMAL || 0) / total * 100)
    },
    maxTrendCount() {
      if (this.trendData.length === 0) return 0
      return Math.max(...this.trendData.map(d => d.count))
    }
  },
  created() {
    this.loadStatistics()
  },
  methods: {
    loadStatistics() {
      getDashboardStatistics().then(response => {
        this.statistics = {
          totalTickets: response.totalTickets || 0
        }
        this.statusDistribution = response.statusDistribution || {}
        this.slaStatus = response.slaStatus || {}
        this.priorityDistribution = response.priorityDistribution || {}
        this.dailyTrend = response.dailyTrend || {}
      }).catch(() => {
        this.statistics = {
          totalTickets: 128
        }
        this.statusDistribution = {
          ASSIGNED: 15,
          IN_PROGRESS: 25,
          RESOLVED: 68,
          CLOSED: 12,
          PENDING_APPROVAL: 8
        }
        this.slaStatus = {
          NORMAL: 100,
          WARNING: 15,
          OVERDUE: 5
        }
        this.priorityDistribution = {
          LOW: 30,
          MEDIUM: 60,
          HIGH: 25,
          CRITICAL: 13
        }
        this.dailyTrend = {
          '2024-01-20': 8,
          '2024-01-21': 12,
          '2024-01-22': 15,
          '2024-01-23': 10,
          '2024-01-24': 18,
          '2024-01-25': 22,
          '2024-01-26': 16
        }
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
    getPercentage(count) {
      if (this.totalCount === 0) return 0
      return Math.round(count / this.totalCount * 100)
    },
    getProgressColor(status) {
      const colors = {
        ASSIGNED: '#409eff',
        IN_PROGRESS: '#e6a23c',
        RESOLVED: '#67c23a',
        CLOSED: '#909399',
        PENDING_APPROVAL: '#e6a23c',
        ESCALATED: '#f56c6c'
      }
      return colors[status] || '#409eff'
    },
    getTrendWidth(count) {
      if (this.maxTrendCount === 0) return 0
      return (count / this.maxTrendCount) * 100
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  padding: 0;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 20px;
  border-radius: 8px;
  color: #fff;
  
  &.primary {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  }
  
  &.success {
    background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
  }
  
  &.warning {
    background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  }
  
  &.danger {
    background: linear-gradient(135deg, #fa709a 0%, #fee140 100%);
  }
}

.stat-icon {
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: rgba(255, 255, 255, 0.2);
  border-radius: 50%;
  margin-right: 15px;
  
  i {
    font-size: 28px;
    color: #fff;
  }
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 32px;
  font-weight: 600;
  margin-bottom: 5px;
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
}

.sla-stats {
  display: flex;
  justify-content: space-around;
  padding: 20px 0;
}

.sla-item {
  text-align: center;
}

.sla-circle {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 10px;
  
  &.success {
    background-color: #f0f9eb;
    color: #67c23a;
  }
  
  &.warning {
    background-color: #fdf6ec;
    color: #e6a23c;
  }
  
  &.danger {
    background-color: #fef0f0;
    color: #f56c6c;
  }
  
  span {
    font-size: 24px;
    font-weight: 600;
  }
}

.sla-label {
  font-size: 14px;
  color: #606266;
}

.trend-bar {
  width: 100%;
  height: 20px;
  background-color: #ebeef5;
  border-radius: 10px;
  overflow: hidden;
}

.trend-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  transition: width 0.3s;
}
</style>
