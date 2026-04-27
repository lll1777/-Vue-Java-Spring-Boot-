import Vue from 'vue'
import VueRouter from 'vue-router'
import Layout from '@/layout/index.vue'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/403',
    name: 'Forbidden',
    component: () => import('@/views/error/403.vue'),
    meta: { title: '无权限' }
  },
  {
    path: '/404',
    name: 'NotFound',
    component: () => import('@/views/error/404.vue'),
    meta: { title: '页面不存在' }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据看板', icon: 'el-icon-data-board' }
      }
    ]
  },
  {
    path: '/ticket',
    component: Layout,
    redirect: '/ticket/list',
    name: 'Ticket',
    meta: { title: '工单管理', icon: 'el-icon-tickets' },
    children: [
      {
        path: 'list',
        name: 'TicketList',
        component: () => import('@/views/ticket/list.vue'),
        meta: { title: '工单列表', permission: 'TICKET_VIEW' }
      },
      {
        path: 'create',
        name: 'TicketCreate',
        component: () => import('@/views/ticket/create.vue'),
        meta: { title: '创建工单', permission: 'TICKET_CREATE' }
      },
      {
        path: 'detail/:id',
        name: 'TicketDetail',
        component: () => import('@/views/ticket/detail.vue'),
        meta: { title: '工单详情', permission: 'TICKET_VIEW' },
        hidden: true
      },
      {
        path: 'my',
        name: 'MyTickets',
        component: () => import('@/views/ticket/my.vue'),
        meta: { title: '我的工单' }
      },
      {
        path: 'created',
        name: 'CreatedTickets',
        component: () => import('@/views/ticket/created.vue'),
        meta: { title: '我创建的' }
      }
    ]
  },
  {
    path: '/approval',
    component: Layout,
    redirect: '/approval/pending',
    name: 'Approval',
    meta: { title: '审批管理', icon: 'el-icon-document-checked' },
    children: [
      {
        path: 'pending',
        name: 'PendingApproval',
        component: () => import('@/views/approval/pending.vue'),
        meta: { title: '待我审批' }
      },
      {
        path: 'my',
        name: 'MyApproval',
        component: () => import('@/views/approval/my.vue'),
        meta: { title: '我的审批' }
      },
      {
        path: 'config',
        name: 'ApprovalConfig',
        component: () => import('@/views/approval/config.vue'),
        meta: { title: '审批流配置', permission: 'APPROVAL_CONFIG' }
      }
    ]
  },
  {
    path: '/batch',
    component: Layout,
    redirect: '/batch/process',
    name: 'Batch',
    meta: { title: '批量处理', icon: 'el-icon-copy-document' },
    children: [
      {
        path: 'process',
        name: 'BatchProcess',
        component: () => import('@/views/batch/process.vue'),
        meta: { title: '批量处理', permission: 'TICKET_BATCH_ASSIGN' }
      }
    ]
  },
  {
    path: '/report',
    component: Layout,
    redirect: '/report/export',
    name: 'Report',
    meta: { title: '报表管理', icon: 'el-icon-pie-chart' },
    children: [
      {
        path: 'export',
        name: 'ReportExport',
        component: () => import('@/views/report/export.vue'),
        meta: { title: '报表导出', permission: 'REPORT_EXPORT' }
      },
      {
        path: 'sla',
        name: 'SLAReport',
        component: () => import('@/views/report/sla.vue'),
        meta: { title: 'SLA统计', permission: 'REPORT_SLA_VIEW' }
      },
      {
        path: 'config',
        name: 'ReportConfig',
        component: () => import('@/views/report/config.vue'),
        meta: { title: '报表配置', permission: 'REPORT_CONFIG' }
      }
    ]
  },
  {
    path: '/collaboration',
    component: Layout,
    redirect: '/collaboration/requests',
    name: 'Collaboration',
    meta: { title: '部门协作', icon: 'el-icon-connection' },
    children: [
      {
        path: 'requests',
        name: 'CollaborationRequests',
        component: () => import('@/views/collaboration/requests.vue'),
        meta: { title: '协作请求' }
      },
      {
        path: 'outgoing',
        name: 'OutgoingRequests',
        component: () => import('@/views/collaboration/outgoing.vue'),
        meta: { title: '我发起的' }
      }
    ]
  },
  {
    path: '/system',
    component: Layout,
    redirect: '/system/user',
    name: 'System',
    meta: { title: '系统管理', icon: 'el-icon-setting', permission: 'ROLE_ADMIN' },
    children: [
      {
        path: 'user',
        name: 'UserManage',
        component: () => import('@/views/system/user.vue'),
        meta: { title: '用户管理', permission: 'ROLE_ADMIN' }
      },
      {
        path: 'role',
        name: 'RoleManage',
        component: () => import('@/views/system/role.vue'),
        meta: { title: '角色管理', permission: 'ROLE_ADMIN' }
      },
      {
        path: 'department',
        name: 'DepartmentManage',
        component: () => import('@/views/system/department.vue'),
        meta: { title: '部门管理', permission: 'ROLE_ADMIN' }
      },
      {
        path: 'sla',
        name: 'SLAManage',
        component: () => import('@/views/system/sla.vue'),
        meta: { title: 'SLA配置', permission: 'ROLE_ADMIN' }
      }
    ]
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

export default router
