<template>
  <div class="joblist-container">
    <div class="joblist-header">
      <h1 class="joblist-header-title">Projects</h1>
      <span class="joblist-header-year">Year: 2024/2025</span>
    </div>
    <div class="joblist-search-container">
      <v-row class="mb-4">
        <v-col cols="12" md="2">
          <span class="search-label">Search</span>
          <v-text-field
            v-model="searchJobName"
            clearable
            hide-details
            placeholder="Search Job Name..."
            density="compact"
            dense
            variant="outlined"
          ></v-text-field>
        </v-col>
      </v-row>
      <v-row class="mt-n8">
        <v-col cols="12" md="2">
          <AppDateRangePicker
            :startDate="startDate"
            :endDate="endDate"
            @update:startDate="startDate = $event"
            @update:endDate="endDate = $event"
          />
          <!-- <p>Selected Start Date: {{ startDate }}</p> <p>Selected End Date: {{ endDate }}</p> -->
        </v-col>
        <v-col cols="12" md="2" style="padding-top: 9px">
          <span class="search-label">Job Status</span>
          <v-select
            v-model="searchJobStatus"
            :items="jobStatuses"
            clearable
            hide-details
            dense
            density="compact"
            variant="outlined"
          >
          </v-select>
        </v-col>
        <v-col
          cols="12"
          md="8"
          class="d-flex justify-end"
          style="padding-top: 34px"
        >
          <v-menu>
            <template v-slot:activator="{ props }">
              <v-btn color="primary" v-bind="props">New Project</v-btn>
            </template>
            <v-list>
              <v-list-item
                v-for="(item, index) in runJobMenuLabels"
                :key="index"
                :value="index"
                @click="navigateTo(index)"
              >
                <v-list-item-title>{{ item.title }}</v-list-item-title>
              </v-list-item>
            </v-list>
          </v-menu>
        </v-col>
      </v-row>
    </div>
    <div>
      <v-row no-gutters>
        <v-data-table-server
          :loading="loading"
          loading-text="Loading... Please wait"
          :headers="headers"
          :items="jobs.items"
          :items-length="jobs.itemsLength"
          :page="tblOptions.page + 1"
          :items-per-page="tblOptions.itemsPerPage"
          @update:options="onUpdateOptions"
          :items-per-page-options="[5, 10, 25, 50]"
          class="elevation-0"
          hover
        >
          <template v-slot:item="{ item, index }">
            <tr :class="getRowClass(index)">
              <td>{{ formatDateTime(item.jobDateTime) }}</td>
              <td>{{ item.jobName }}</td>
              <td class="text-center">
                <v-chip
                  v-if="isChipStatus(item.jobStatus)"
                  :color="getChipColor(item.jobStatus)"
                  size="small"
                  class="rounded-chip"
                  variant="outlined"
                >
                  {{ item.jobStatus }}
                </v-chip>
                <span v-else>{{ item.jobStatus }}</span>
              </td>
              <td>{{ item.jobType }}</td>
              <td>{{ item.lastUpdated }}</td>
              <td class="text-center">
                <v-menu bottom :close-on-content-click="false" offset-y>
                  <template v-slot:activator="{ props }">
                    <v-icon small v-bind="props">mdi-dots-vertical</v-icon>
                  </template>
                  <v-list>
                    <v-list-item @click="viewInputs(item)">
                      <v-list-item-title>View Input</v-list-item-title>
                    </v-list-item>
                    <v-list-item @click="viewResults(item)">
                      <v-list-item-title>View Result</v-list-item-title>
                    </v-list-item>
                    <v-list-item @click="viewLogFile(item)">
                      <v-list-item-title>View Log File</v-list-item-title>
                    </v-list-item>
                    <v-list-item @click="viewErrorMessages(item)">
                      <v-list-item-title>View Error Messages</v-list-item-title>
                    </v-list-item>
                    <v-divider class="dotted-divider"></v-divider>
                    <v-list-item @click="cancelJob(item)">
                      <v-list-item-title>Cancel Job</v-list-item-title>
                    </v-list-item>
                    <v-list-item @click="deleteJob(item)">
                      <v-list-item-title>Delete</v-list-item-title>
                    </v-list-item>
                  </v-list>
                </v-menu>
              </td>
            </tr>
          </template>
        </v-data-table-server>
      </v-row>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import AppDateRangePicker from '@/components/common/AppDateRangePicker.vue'
import type TableOptions from '@/interfaces/TableOptions'
import { SORT_ORDER } from '@/constants/constants'
import { Util } from '@/utils/util'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  search()
})

const search = async () => {
  loading.value = true

  jobs.itemsLength = jobData.length
  jobs.items = jobData.slice(
    tblOptions.page * tblOptions.itemsPerPage,
    (tblOptions.page + 1) * tblOptions.itemsPerPage,
  )

  loading.value = false
}

/**
 * data table
 */
const loading = ref(false)

const headers: any = [
  { title: 'Date & Time', key: 'jobDateTime', sortable: true },
  { title: 'Job', key: 'jobName', sortable: true },
  { title: 'Job Status', key: 'jobStatus', align: 'center', sortable: true },
  { title: 'Job Type', key: 'jobType', sortable: true },
  { title: 'Last Updated', key: 'lastUpdated', sortable: true },
  { title: 'Actions', key: 'actions', align: 'center', sortable: false },
]

const tblOptions: TableOptions = reactive({
  page: 1,
  itemsPerPage: 5,
  sortBy: '',
  sortDesc: SORT_ORDER.ASC,
})

const jobs = reactive({
  items: [] as Array<any>,
  itemsLength: 0,
})

const jobData = [
  {
    jobDateTime: new Date('2024-04-17T11:59:04'),
    jobName: 'Job Name 04/17/2024',
    jobStatus: '4%',
    jobType: 'Parameter Selection',
    lastUpdated: new Date('2024-04-17T11:59:04'),
  },
  {
    jobDateTime: new Date('2024-02-08T16:46:27'),
    jobName: 'Job Name 02/08/2024',
    jobStatus: 'Complete',
    jobType: 'Input File',
    lastUpdated: new Date('2024-04-17T11:59:04'),
  },
  {
    jobDateTime: new Date('2023-07-17T10:13:22'),
    jobName: 'Job Name 07/17/2023',
    jobStatus: 'Complete',
    jobType: 'Parameter Selection',
    lastUpdated: new Date('2024-04-17T11:59:04'),
  },
  {
    jobDateTime: new Date('2024-04-17T11:59:04'),
    jobName: 'Job Name 04/17/2024',
    jobStatus: 'InComplete',
    jobType: 'Parameter Selection',
    lastUpdated: new Date('2024-04-17T11:59:04'),
  },
  {
    jobDateTime: new Date('2024-02-08T15:12:20'),
    jobName: 'Job Name 02/08/2024',
    jobStatus: 'Complete',
    jobType: 'Input File',
    lastUpdated: new Date('2024-04-17T11:59:04'),
  },
  {
    jobDateTime: new Date('2023-07-17T10:13:22'),
    jobName: 'Job Name 07/17/2023',
    jobStatus: 'Complete',
    jobType: 'Parameter Selection',
    lastUpdated: new Date('2024-04-17T11:59:04'),
  },
  {
    jobDateTime: new Date('2024-04-17T11:59:04'),
    jobName: 'Job Name 04/17/2024',
    jobStatus: 'Complete',
    jobType: 'Parameter Selection',
    lastUpdated: new Date('2024-04-17T11:59:04'),
  },
  {
    jobDateTime: new Date('2024-02-08T16:46:27'),
    jobName: 'Job Name 02/08/2024',
    jobStatus: 'Complete',
    jobType: 'Input File',
    lastUpdated: new Date('2024-04-17T11:59:04'),
  },
  {
    jobDateTime: new Date('2023-07-17T10:13:22'),
    jobName: 'Job Name 07/17/2023',
    jobStatus: 'Complete',
    jobType: 'Parameter Selection',
    lastUpdated: new Date('2024-04-17T11:59:04'),
  },
]

const onUpdateOptions = async (_options: any) => {
  tblOptions.page = _options.page - 1
  tblOptions.itemsPerPage = _options.itemsPerPage

  if (!loading.value) {
    search().catch((error) => {
      console.error(error)
    })
  }
}

const getRowClass = (index: number) => {
  return index % 2 === 0 ? 'data-table-even-row' : 'data-table-odd-row'
}

const formatDateTime = (date: Date | null) => {
  return Util.formatDateTime(date)
}

const isChipStatus = (jobStatus: string) => {
  const result = ['InComplete', 'Complete'].includes(jobStatus)
  return result
}

const getChipColor = (jobStatus: string) => {
  if (!jobStatus) return 'grey'

  switch (jobStatus.toLocaleLowerCase()) {
    case 'complete':
      return 'success'
    case 'incomplete':
      return 'red'
    default:
      return 'grey'
  }
}

const viewInputs = (item: any) => {
  console.log(item)
}

const viewResults = (item: any) => {
  console.log(item)
}

const viewLogFile = (item: any) => {
  console.log(item)
}

const viewErrorMessages = (item: any) => {
  console.log(item)
}

const cancelJob = (item: any) => {
  console.log(item)
}

const deleteJob = (item: any) => {
  console.log(item)
}

/**
 * search filters
 */
const searchJobName = ref('')

const startDate = ref(null)
const endDate = ref(null)

const searchJobStatus = ref('All')
const jobStatuses = ['All', 'In Progress', 'Complete', 'Incomplete']

/**
 * buttons
 */
const runJobMenuLabels = [
  { title: 'Input Model Parameters', routeName: 'ModelParameterInput' },
  { title: 'File Upload', routeName: 'FileUpload' },
]

const navigateTo = (index: number) => {
  if (index === 1) return // File Upload
  const routeName = runJobMenuLabels[index].routeName
  router.push({ name: routeName })
}
</script>

<style scoped>
.joblist-container {
  padding: 20px 15px 10px 15px;
}

.joblist-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  padding-bottom: 60px;
  border-bottom: 2px solid #dfdcdc;
}

.joblist-header-title {
  font-size: 24px;
  font-weight: bold;
}

.joblist-header-year {
  font-size: 24px;
  font-weight: bold;
}

.joblist-search-container {
  margin-bottom: 10px;
  padding-bottom: 20px;
  border-bottom: 2px solid #dfdcdc;
}
</style>
