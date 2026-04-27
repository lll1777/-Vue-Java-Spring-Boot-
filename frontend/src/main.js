import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import './styles/index.scss'
import * as filters from './utils/filters'
import {
  PermissionDirective,
  RoleDirective,
  LevelDirective,
  AdminOnlyDirective,
  TicketViewDirective,
  TicketEditDirective
} from './utils/permission'

Vue.config.productionTip = false

Vue.use(ElementUI, { size: 'medium', zIndex: 3000 })

Object.keys(filters).forEach(key => {
  Vue.filter(key, filters[key])
})

Vue.directive('permission', PermissionDirective)
Vue.directive('role', RoleDirective)
Vue.directive('level', LevelDirective)
Vue.directive('admin-only', AdminOnlyDirective)
Vue.directive('ticket-view', TicketViewDirective)
Vue.directive('ticket-edit', TicketEditDirective)

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
