import type { CampaignStatus } from '../types'

export default function StatusBadge({ status }: { status: CampaignStatus }) {
  return <span className={`badge ${status.toLowerCase()}`}>{status}</span>
}
