import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchCampaigns } from './api';
import { cacheCampaigns, readCachedCampaigns } from './storage';
import type { Campaign } from './types';

export type SyncState = 'loading' | 'ready' | 'offline' | 'error';

export function useCampaigns() {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [state, setState] = useState<SyncState>('loading');
  const [lastSync, setLastSync] = useState<Date | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const inflight = useRef(false);

  const load = useCallback(async (manual = false) => {
    if (inflight.current) {
      return;
    }
    inflight.current = true;
    if (manual) {
      setRefreshing(true);
    }

    try {
      const items = await fetchCampaigns();
      setCampaigns(items);
      await cacheCampaigns(items);
      setState('ready');
      setLastSync(new Date());
    } catch {
      const cached = await readCachedCampaigns();
      setCampaigns(cached);
      setState(cached.length > 0 ? 'offline' : 'error');
    } finally {
      inflight.current = false;
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    void load();
    const timer = setInterval(() => void load(), 5000);
    return () => clearInterval(timer);
  }, [load]);

  const refresh = useCallback(() => {
    void load(true);
  }, [load]);

  return { campaigns, state, lastSync, refreshing, refresh };
}
