import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import './styles/index.scss'
import * as filters from './utils/filters'
import { getToken } from './utils/auth'

Vue.config.productionTip = false

Vue.use(ElementUI, { size: 'medium', zIndex: 3000 })

Object.keys(filters).forEach(key => {
  Vue.filter(key, filters[key])
})

router.beforeEach(async (to, from, next) => {
  const token = getToken()
  
  if (to.path === '/login') {
    if (token) {
      next({ path: '/' })
    } else {
      next()
    }
  } else {
    if (token) {
      if (!store.getters.userInfo) {
        try {
          await store.dispatch('user/getCurrentUser')
        } catch (error) {
          await store.dispatch('user/logout')
          next({ path: '/login' })
          return
        }
      }
      
      if (to.meta.permission && !store.getters.hasPermission(to.meta.permission)) {
        next({ path: '/403' })
      } else {
        next()
      }
    } else {
      next({ path: '/login', query: { redirect: to.fullPath } })
    }
  }
})

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
