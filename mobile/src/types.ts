export type Channel = 'EMAIL' | 'SMS' | 'PUSH';

export type CampaignStatus = 'SCHEDULED' | 'RUNNING' | 'COMPLETED' | 'FAILED';

export interface Campaign {
  id: string;
  name: string;
  channel: Channel;
  status: CampaignStatus;
  totalRecipients: number;
  sent: number;
  failed: number;
  skipped: number;
  createdAt: string;
}
