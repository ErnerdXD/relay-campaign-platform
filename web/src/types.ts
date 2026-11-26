export type Channel = 'EMAIL' | 'SMS' | 'PUSH'

export type CampaignStatus = 'SCHEDULED' | 'RUNNING' | 'COMPLETED' | 'FAILED'

export interface Tenant {
  id: string
  name: string
  timezone: string
  token?: string
}

export interface Session {
  tenants: Tenant[]
}

export interface CampaignRow {
  id: string
  name: string
  channel: Channel
  status: CampaignStatus
  scheduledAt: string
  createdAt: string
  totalRecipients: number
  sent: number
  failed: number
  skipped: number
}

export interface CampaignPage {
  items: CampaignRow[]
  page: number
  pageSize: number
  total: number
}

export interface MinuteMetric {
  minute: string
  sent: number
  failed: number
}

export interface DeliveryError {
  error: string
  count: number
}

export interface CampaignDetail {
  id: string
  name: string
  channel: Channel
  status: CampaignStatus
  template: string
  transactional: boolean
  scheduledAt: string
  createdAt: string
  startedAt?: string
  completedAt?: string
  totalRecipients: number
  sent: number
  failed: number
  skipped: number
  pending: number
  retryQueue: number
  metrics: MinuteMetric[]
  errors: DeliveryError[]
}

export interface CreateCampaignInput {
  tenantId: string
  name: string
  channel: Channel
  template: string
  scheduledAt: string
  transactional: boolean
  file: File
}

export interface CreatedCampaign {
  id: string
  status: CampaignStatus
  totalRecipients: number
}
