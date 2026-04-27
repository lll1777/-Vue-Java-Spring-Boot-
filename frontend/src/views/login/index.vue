<template>
  <div class="login-container">
    <div class="login-box">
      <h2 class="login-title">工单管理平台</h2>
      <el-form
        :model="loginForm"
        :rules="loginRules"
        ref="loginFormRef"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            prefix-icon="el-icon-user"
            size="large"
            @keyup.enter.native="handleLogin"
          ></el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            prefix-icon="el-icon-lock"
            size="large"
            show-password
            @keyup.enter.native="handleLogin"
          ></el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            class="login-btn"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <p>默认账号: admin / admin123</p>
      </div>
    </div>
  </div>
</template>

<script>
import { mapActions } from 'vuex'

export default {
  name: 'Login',
  data() {
    const validateUsername = (rule, value, callback) => {
      if (!value) {
        return callback(new Error('请输入用户名'))
      }
      if (value.length < 2) {
        callback(new Error('用户名至少2个字符'))
      } else {
        callback()
      }
    }
    const validatePassword = (rule, value, callback) => {
      if (value.length < 6) {
        callback(new Error('密码至少6个字符'))
      } else {
        callback()
      }
    }
    return {
      loading: false,
      loginForm: {
        username: '',
        password: ''
      },
      loginRules: {
        username: [{ required: true, trigger: 'blur', validator: validateUsername }],
        password: [{ required: true, trigger: 'blur', validator: validatePassword }]
      }
    }
  },
  methods: {
    ...mapActions('user', ['login']),
    handleLogin() {
      this.$refs.loginFormRef.validate((valid) => {
        if (valid) {
          this.loading = true
          this.login(this.loginForm)
            .then(() => {
              this.$message.success('登录成功')
              const redirect = this.$route.query.redirect || '/'
              this.$router.push({ path: redirect })
            })
            .catch(() => {
              this.loading = false
            })
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.login-container {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.2);
}

.login-title {
  text-align: center;
  margin: 0 0 30px 0;
  font-size: 28px;
  font-weight: 600;
  color: #303133;
}

.login-form {
  .el-form-item {
    margin-bottom: 25px;
  }
}

.login-btn {
  width: 100%;
}

.login-footer {
  margin-top: 20px;
  text-align: center;
  color: #909399;
  font-size: 12px;
  
  p {
    margin: 0;
  }
}
</style>
