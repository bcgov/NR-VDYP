import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'

import JobList from '@/views/JobList.vue'
import ModelParameterInput from '@/views/input-model-parameters/ModelParameterInput.vue'
import ModelParameterSelection from '@/views/input-model-parameters/ModelParameterSelection.vue'
import PageNotFound from '@/views/PageNotFound.vue'

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'Home',
    component: JobList,
  },
  {
    path: '/input-model-parameters',
    name: 'ModelParameterInput',
    component: ModelParameterInput,
    children: [
      {
        path: 'model-parameter-selection',
        name: 'ModelParameterSelection',
        component: ModelParameterSelection,
      },
    ],
  },
  { path: '/:pathMatch(.*)*', name: 'NotFound', component: PageNotFound },
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

export default router
