export default interface JobSearchParams {
  pageNumber: number
  pageSize: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
  searchJobName?: string
  startDate?: string | null
  endDate?: string | null
  status?: string
}
