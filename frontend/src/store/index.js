import Vue from 'vue'
import Vuex from 'vuex'
import user from './modules/user'
import app from './modules/app'

Vue.use(Vuex)

const store = new Vuex.Store({
  modules: {
    user,
    app
  },
  getters: {
    userInfo: state => state.user.userInfo,
    token: state => state.user.token,
    permissions: state => state.user.permissions,
    roles: state => state.user.roles,
    hasPermission: state => (permission) => {
      return state.user.permissions.includes(permission) || 
             state.user.roles.includes('ROLE_ADMIN')
    },
    sidebar: state => state.app.sidebar,
    device: state => state.app.device
  }
})

export default store
