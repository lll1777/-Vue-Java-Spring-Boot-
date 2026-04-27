<template>
  <div class="error-container">
    <div class="error-content">
      <div class="error-code">403</div>
      <div class="error-message">抱歉，您没有权限访问此页面</div>
      <div class="error-actions">
        <el-button type="primary" @click="goHome">
          <i class="el-icon-home"></i> 返回首页
        </el-button>
        <el-button @click="goBack">
          <i class="el-icon-back"></i> 返回上页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Forbidden',
  methods: {
    goHome() {
      this.$router.push({ path: '/' })
    },
    goBack() {
      this.$router.go(-1)
    }
  }
}
</script>

<style lang="scss" scoped>
.error-container {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
}

.error-content {
  text-align: center;
  padding: 40px;
}

.error-code {
  font-size: 120px;
  font-weight: 700;
  color: #e6a23c;
  line-height: 1;
  margin-bottom: 20px;
}

.error-message {
  font-size: 18px;
  color: #606266;
  margin-bottom: 30px;
}

.error-actions {
  .el-button {
    margin: 0 10px;
  }
}
</style>
