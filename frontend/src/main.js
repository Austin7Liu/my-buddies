import { createApp } from 'vue'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import './styles/main.css'
import App from './App.vue'
import router from './router/index.js'

createApp(App).use(router).mount('#app')
