import { login, getCurrentUser, changePassword } from '@/api/user'
import { getToken, setToken, removeToken } from '@/utils/auth'

const state = {
  token: getToken(),
  userInfo: null,
  permissions: [],
  roles: []
}

const mutations = {
  SET_TOKEN: (state, token) => {
    state.token = token
  },
  SET_USER_INFO: (state, userInfo) => {
    state.userInfo = userInfo
  },
  SET_PERMISSIONS: (state, permissions) => {
    state.permissions = permissions
  },
  SET_ROLES: (state, roles) => {
    state.roles = roles
  }
}

const actions = {
  login({ commit }, userInfo) {
    const { username, password } = userInfo
    return new Promise((resolve, reject) => {
      login({ username: username.trim(), password })
        .then(response => {
          const { token } = response
          commit('SET_TOKEN', token)
          setToken(token)
          resolve()
        })
        .catch(error => {
          reject(error)
        })
    })
  },

  getCurrentUser({ commit, state }) {
    return new Promise((resolve, reject) => {
      getCurrentUser()
        .then(response => {
          const userInfo = response
          const permissions = userInfo.authorities ? 
            userInfo.authorities.map(item => item.authority) : []
          const roles = permissions.filter(p => p.startsWith('ROLE_'))
          
          commit('SET_USER_INFO', userInfo)
          commit('SET_PERMISSIONS', permissions)
          commit('SET_ROLES', roles)
          
          resolve(userInfo)
        })
        .catch(error => {
          reject(error)
        })
    })
  },

  logout({ commit }) {
    return new Promise(resolve => {
      commit('SET_TOKEN', '')
      commit('SET_USER_INFO', null)
      commit('SET_PERMISSIONS', [])
      commit('SET_ROLES', [])
      removeToken()
      resolve()
    })
  },

  resetToken({ commit }) {
    return new Promise(resolve => {
      commit('SET_TOKEN', '')
      removeToken()
      resolve()
    })
  },

  changePassword({ commit }, data) {
    return new Promise((resolve, reject) => {
      changePassword(data)
        .then(() => {
          resolve()
        })
        .catch(error => {
          reject(error)
        })
    })
  }
}

export default {
  namespaced: true,
  state,
  mutations,
  actions
}
