import type {
  CampaignDetail,
  CampaignPage,
  CreateCampaignInput,
  CreatedCampaign,
  Session,
} from './types'

async function request<T>(path: string, token: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`/api${path}`, {
    ...init,
    headers: {
      ...(init?.headers ?? {}),
      Authorization: `Bearer ${token}`,
    },
  })

  if (!response.ok) {
    const problem = await response.json().catch(() => null)
    throw new Error(problem?.detail ?? `Request failed (${response.status})`)
  }

  return response.json() as Promise<T>
}

export function getSession(): Promise<Session> {
  return fetch('/api/session').then(async (response) => {
    if (!response.ok) throw new Error('Could not load demo tenants.')
    return response.json() as Promise<Session>
  })
}

export function getCampaigns(
  token: string,
  params: { search: string; status: string; channel: string; page: number; pageSize: number },
): Promise<CampaignPage> {
  const query = new URLSearchParams()
  if (params.search) query.set('search', params.search)
  if (params.status) query.set('status', params.status)
  if (params.channel) query.set('channel', params.channel)
  query.set('page', String(params.page))
  query.set('pageSize', String(params.pageSize))
  return request<CampaignPage>(`/campaigns?${query}`, token)
}

export function getCampaign(token: string, id: string): Promise<CampaignDetail> {
  return request<CampaignDetail>(`/campaigns/${id}`, token)
}

export function retryFailures(token: string, id: string): Promise<{ requeued: number }> {
  return request<{ requeued: number }>(`/campaigns/${id}/retry-failures`, token, { method: 'POST' })
}

export function createCampaign(
  token: string,
  input: CreateCampaignInput,
  onProgress: (percent: number) => void,
): Promise<CreatedCampaign> {
  return new Promise((resolve, reject) => {
    const form = new FormData()
    form.append('tenantId', input.tenantId)
    form.append('name', input.name)
    form.append('channel', input.channel)
    form.append('template', input.template)
    form.append('scheduledAt', input.scheduledAt)
    form.append('transactional', String(input.transactional))
    form.append('csv', input.file)

    const xhr = new XMLHttpRequest()
    xhr.open('POST', '/api/campaigns')
    xhr.setRequestHeader('Authorization', `Bearer ${token}`)
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) onProgress(Math.round((event.loaded / event.total) * 100))
    }
    xhr.onload = () => {
      let body: { detail?: string } & Partial<CreatedCampaign> = {}
      try {
        body = JSON.parse(xhr.responseText)
      } catch {
        // Keep the status-code message below.
      }
      if (xhr.status >= 200 && xhr.status < 300 && body.id) {
        resolve(body as CreatedCampaign)
      } else {
        reject(new Error(body.detail ?? `Upload failed (${xhr.status})`))
      }
    }
    xhr.onerror = () => reject(new Error('Upload failed. Is the API running on port 8081?'))
    xhr.send(form)
  })
}
