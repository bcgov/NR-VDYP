import type { App } from 'vue'
import pinia from '../stores'
import router from '../router'
import vuetify from './vuetify'

export const registerPlugins = (app: App) => {
  app.use(pinia).use(router).use(vuetify)
}
