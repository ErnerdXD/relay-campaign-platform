import { useEffect, useRef, useState } from 'react';
import * as Notifications from 'expo-notifications';
import { StatusBar } from 'expo-status-bar';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaProvider, SafeAreaView } from 'react-native-safe-area-context';
import HealthScreen from './src/HealthScreen';
import LockGate from './src/LockGate';
import PushPreviewScreen from './src/PushPreviewScreen';
import { colors } from './src/theme';
import { useCampaigns } from './src/useCampaigns';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: false,
    shouldSetBadge: false,
  }),
});

type Tab = 'health' | 'preview';

export default function App() {
  const [tab, setTab] = useState<Tab>('health');
  const { campaigns, state, lastSync, refreshing, refresh } = useCampaigns();
  const notified = useRef(new Set<string>());

  useEffect(() => {
    void Notifications.requestPermissionsAsync().catch(() => undefined);
  }, []);

  useEffect(() => {
    for (const campaign of campaigns) {
      const total = campaign.totalRecipients;
      const rate = total > 0 ? campaign.failed / total : 0;
      if (rate > 0.2 && !notified.current.has(campaign.id)) {
        notified.current.add(campaign.id);
        void Notifications.scheduleNotificationAsync({
          content: {
            title: 'Campaign failure rate high',
            body: `${campaign.name}: ${Math.round(rate * 100)}% of sends failed.`,
          },
          trigger: null,
        }).catch(() => undefined);
      }
    }
  }, [campaigns]);

  return (
    <SafeAreaProvider>
      <LockGate>
        <SafeAreaView style={styles.safe} edges={['top', 'bottom']}>
          <View style={styles.body}>
            {tab === 'health' ? (
              <HealthScreen
                campaigns={campaigns}
                state={state}
                lastSync={lastSync}
                refreshing={refreshing}
                onRefresh={refresh}
              />
            ) : (
              <PushPreviewScreen />
            )}
          </View>

          <View style={styles.tabs}>
            <Pressable
              accessibilityRole="button"
              onPress={() => setTab('health')}
              style={styles.tab}
            >
              <Text style={[styles.tabText, tab === 'health' && styles.tabActive]}>Health</Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              onPress={() => setTab('preview')}
              style={styles.tab}
            >
              <Text style={[styles.tabText, tab === 'preview' && styles.tabActive]}>
                Push preview
              </Text>
            </Pressable>
          </View>
          <StatusBar style="light" />
        </SafeAreaView>
      </LockGate>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: colors.bg,
  },
  body: {
    flex: 1,
  },
  tabs: {
    flexDirection: 'row',
    borderTopColor: colors.line,
    borderTopWidth: 1,
    backgroundColor: colors.bg,
  },
  tab: {
    flex: 1,
    alignItems: 'center',
    paddingVertical: 14,
  },
  tabText: {
    color: colors.muted,
    fontSize: 12,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  tabActive: {
    color: colors.amber,
  },
});
