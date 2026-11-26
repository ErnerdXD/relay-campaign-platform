import AsyncStorage from '@react-native-async-storage/async-storage';
import type { Campaign } from './types';

const KEY = 'relay.campaigns.v1';

export async function cacheCampaigns(campaigns: Campaign[]): Promise<void> {
  await AsyncStorage.setItem(KEY, JSON.stringify(campaigns));
}

export async function readCachedCampaigns(): Promise<Campaign[]> {
  try {
    const raw = await AsyncStorage.getItem(KEY);
    return raw ? (JSON.parse(raw) as Campaign[]) : [];
  } catch {
    return [];
  }
}
