<template>
  <el-container class="app-container">
    <el-aside :width="sidebarWidth" class="app-sidebar">
      <div class="sidebar-logo">
        <img v-if="!sidebar.opened" src="@/assets/logo.png" alt="logo" />
        <h1 v-else>工单管理平台</h1>
      </div>
      <el-menu
        :default-active="activeMenu"
        :collapse="!sidebar.opened"
        :collapse-transition="false"
        router
        class="sidebar-menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
      >
        <el-menu-item index="/dashboard">
          <i class="el-icon-data-board"></i>
          <span slot="title">数据看板</span>
        </el-menu-item>
        
        <el-submenu index="ticket">
          <template slot="title">
            <i class="el-icon-tickets"></i>
            <span>工单管理</span>
          </template>
          <el-menu-item index="/ticket/list">工单列表</el-menu-item>
          <el-menu-item index="/ticket/create">创建工单</el-menu-item>
          <el-menu-item index="/ticket/my">我的工单</el-menu-item>
          <el-menu-item index="/ticket/created">我创建的</el-menu-item>
        </el-submenu>
        
        <el-submenu index="approval">
          <template slot="title">
            <i class="el-icon-document-checked"></i>
            <span>审批管理</span>
          </template>
          <el-menu-item index="/approval/pending">待我审批</el-menu-item>
          <el-menu-item index="/approval/my">我的审批</el-menu-item>
          <el-menu-item v-if="hasPermission('APPROVAL_CONFIG')" index="/approval/config">审批流配置</el-menu-item>
        </el-submenu>
        
        <el-menu-item v-if="hasPermission('TICKET_BATCH_ASSIGN')" index="/batch/process">
          <i class="el-icon-copy-document"></i>
          <span slot="title">批量处理</span>
        </el-menu-item>
        
        <el-submenu index="report">
          <template slot="title">
            <i class="el-icon-pie-chart"></i>
            <span>报表管理</span>
          </template>
          <el-menu-item index="/report/export">报表导出</el-menu-item>
          <el-menu-item index="/report/sla">SLA统计</el-menu-item>
          <el-menu-item v-if="hasPermission('REPORT_CONFIG')" index="/report/config">报表配置</el-menu-item>
        </el-submenu>
        
        <el-submenu index="collaboration">
          <template slot="title">
            <i class="el-icon-connection"></i>
            <span>部门协作</span>
          </template>
          <el-menu-item index="/collaboration/requests">协作请求</el-menu-item>
          <el-menu-item index="/collaboration/outgoing">我发起的</el-menu-item>
        </el-submenu>
        
        <el-submenu v-if="hasRole('ROLE_ADMIN')" index="system">
          <template slot="title">
            <i class="el-icon-setting"></i>
            <span>系统管理</span>
          </template>
          <el-menu-item index="/system/user">用户管理</el-menu-item>
          <el-menu-item index="/system/role">角色管理</el-menu-item>
          <el-menu-item index="/system/department">部门管理</el-menu-item>
          <el-menu-item index="/system/sla">SLA配置</el-menu-item>
        </el-submenu>
      </el-menu>
    </el-aside>
    
    <el-container>
      <el-header class="app-header">
        <div class="navbar-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path" :to="item.path">
              {{ item.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        
        <div class="navbar-right">
          <el-dropdown trigger="click" @command="handleCommand">
            <span class="user-dropdown">
              <el-avatar :size="32" icon="el-icon-user-solid"></el-avatar>
              <span>{{ userInfo?.realName || userInfo?.username }}</span>
              <i class="el-icon-arrow-down el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="profile">
                <i class="el-icon-user"></i>
                个人中心
              </el-dropdown-item>
              <el-dropdown-item command="changePassword">
                <i class="el-icon-key"></i>
                修改密码
              </el-dropdown-item>
              <el-dropdown-item divided command="logout">
                <i class="el-icon-switch-button"></i>
                退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-main class="app-main">
        <transition name="fade-transition" mode="out-in">
          <router-view />
        </transition>
      </el-main>
    </el-container>
    
    <el-dialog title="修改密码" :visible.sync="showChangePassword" width="400px">
      <el-form :model="passwordForm" :rules="passwordRules" ref="passwordFormRef" label-width="100px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码"></el-input>
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码"></el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请确认新密码"></el-input>
        </el-form-item>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="showChangePassword = false">取 消</el-button>
        <el-button type="primary" @click="submitChangePassword">确 定</el-button>
      </span>
    </el-dialog>
  </el-container>
</template>

<script>
import { mapGetters } from 'vuex'
import { changePassword } from '@/api/user'

export default {
  name: 'Layout',
  data() {
    const validateConfirmPassword = (rule, value, callback) => {
      if (value !== this.passwordForm.newPassword) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    
    return {
      showChangePassword: false,
      passwordForm: {
        oldPassword: '',
        newPassword: '',
        confirmPassword: ''
      },
      passwordRules: {
        oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
        newPassword: [
          { required: true, message: '请输入新密码', trigger: 'blur' },
          { min: 6, max: 20, message: '密码长度在 6 到 20 个字符', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认新密码', trigger: 'blur' },
          { validator: validateConfirmPassword, trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    ...mapGetters(['sidebar', 'userInfo', 'permissions', 'roles']),
    sidebarWidth() {
      return this.sidebar.opened ? '220px' : '64px'
    },
    activeMenu() {
      const route = this.$route
      const { meta, path } = route
      if (meta.activeMenu) {
        return meta.activeMenu
      }
      return path
    },
    breadcrumbs() {
      return this.$route.matched
        .filter(item => item.meta && item.meta.title)
        .map(item => ({
          path: item.path,
          title: item.meta.title
        }))
    }
  },
  methods: {
    hasPermission(permission) {
      return this.$store.getters.hasPermission(permission)
    },
    hasRole(role) {
      return this.roles.includes(role)
    },
    handleCommand(command) {
      switch (command) {
        case 'profile':
          this.$message.info('个人中心功能开发中')
          break
        case 'changePassword':
          this.showChangePassword = true
          this.resetPasswordForm()
          break
        case 'logout':
          this.handleLogout()
          break
      }
    },
    handleLogout() {
      this.$confirm('确定要退出登录吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$store.dispatch('user/logout').then(() => {
          this.$router.push({ path: '/login' })
          this.$message.success('已退出登录')
        })
      }).catch(() => {})
    },
    resetPasswordForm() {
      this.$nextTick(() => {
        this.$refs.passwordFormRef?.resetFields()
      })
    },
    submitChangePassword() {
      this.$refs.passwordFormRef.validate((valid) => {
        if (valid) {
          changePassword(this.passwordForm).then(() => {
            this.$message.success('密码修改成功，请重新登录')
            this.showChangePassword = false
            this.$store.dispatch('user/logout').then(() => {
              this.$router.push({ path: '/login' })
            })
          }).catch(() => {})
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  height: 100%;
}

.app-sidebar {
  background-color: #304156;
  transition: width 0.3s;
  overflow-x: hidden;
}

.sidebar-logo {
  height: 50px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #263445;
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  
  img {
    height: 32px;
    width: 32px;
  }
  
  h1 {
    margin: 0;
    font-size: 16px;
    color: #fff;
  }
}

.app-header {
  height: 50px;
  padding: 0 20px;
  background-color: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.navbar-left {
  display: flex;
  align-items: center;
}

.navbar-right {
  display: flex;
  align-items: center;
}

.user-dropdown {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 4px;
  transition: background-color 0.3s;
  
  &:hover {
    background-color: #f5f7fa;
  }
  
  span {
    margin-left: 8px;
    color: #606266;
  }
}

.app-main {
  background-color: #f0f2f5;
  min-height: calc(100vh - 50px);
}

.fade-transition-enter-active,
.fade-transition-leave-active {
  transition: opacity 0.28s ease;
}

.fade-transition-enter,
.fade-transition-leave-to {
  opacity: 0;
}
</style>
