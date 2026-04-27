<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>

<style>
#app {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  height: 100%;
}

html,
body {
  height: 100%;
  margin: 0;
  padding: 0;
}

.el-table .warning-row {
  background: #fdf6ec !important;
}

.el-table .danger-row {
  background: #fef0f0 !important;
}

.el-table .success-row {
  background: #f0f9eb !important;
}
</style>
