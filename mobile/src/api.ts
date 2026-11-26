import Constants from 'expo-constants';
import type { Campaign } from './types';

const host = Constants.expoConfig?.hostUri?.split(':')[0] ?? 'localhost';

export const API_BASE =
  process.env.EXPO_PUBLIC_API_URL ?? `http://${host}:8081/api`;
const TOKEN = 'demo-northstar-local';

export async function fetchCampaigns(): Promise<Campaign[]> {
  const response = await fetch(`${API_BASE}/campaigns?pageSize=20`, {
    headers: { Authorization: `Bearer ${TOKEN}` },
  });

  if (!response.ok) {
    throw new Error(`API returned ${response.status}`);
  }

  const body = (await response.json()) as { items: Campaign[] };
  return body.items;
}
