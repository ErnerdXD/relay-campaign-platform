import { FlatList, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { colors } from './theme';
import type { Campaign } from './types';
import type { SyncState } from './useCampaigns';

interface Props {
  campaigns: Campaign[];
  state: SyncState;
  lastSync: Date | null;
  refreshing: boolean;
  onRefresh: () => void;
}

function health(campaign: Campaign) {
  const total = campaign.totalRecipients;
  const rate = total > 0 ? campaign.failed / total : 0;
  const pending = Math.max(0, total - campaign.sent - campaign.failed - campaign.skipped);
  return { total, rate, pending, high: rate > 0.2 };
}

function CampaignCard({ campaign }: { campaign: Campaign }) {
  const stats = health(campaign);
  const tone = stats.total === 0 ? colors.muted : stats.high ? colors.red : colors.green;

  return (
    <View style={[styles.card, { borderLeftColor: tone }]}>
      <View style={styles.cardTop}>
        <Text style={styles.cardName} numberOfLines={1}>
          {campaign.name}
        </Text>
        <Text style={[styles.health, { color: tone }]}>
          {stats.total === 0 ? 'NO DATA' : stats.high ? 'HIGH FAILURE' : 'HEALTHY'}
        </Text>
      </View>

      <Text style={styles.meta}>
        {campaign.channel} · {campaign.status} · {stats.total.toLocaleString()} recipients
      </Text>

      <View style={styles.numbers}>
        <View>
          <Text style={styles.number}>{campaign.sent.toLocaleString()}</Text>
          <Text style={styles.numberLabel}>SENT</Text>
        </View>
        <View>
          <Text style={[styles.number, { color: campaign.failed > 0 ? colors.red : colors.text }]}>
            {campaign.failed.toLocaleString()}
          </Text>
          <Text style={styles.numberLabel}>FAILED</Text>
        </View>
        <View>
          <Text style={styles.number}>{stats.pending.toLocaleString()}</Text>
          <Text style={styles.numberLabel}>PENDING</Text>
        </View>
        <View>
          <Text style={[styles.number, { color: stats.high ? colors.red : colors.amber }]}>
            {(stats.rate * 100).toFixed(0)}%
          </Text>
          <Text style={styles.numberLabel}>FAIL RATE</Text>
        </View>
      </View>

      <View style={styles.track}>
        <View
          style={[
            styles.fill,
            { width: `${Math.min(100, stats.rate * 100)}%`, backgroundColor: tone },
          ]}
        />
      </View>
    </View>
  );
}

export default function HealthScreen({ campaigns, state, lastSync, refreshing, onRefresh }: Props) {
  const highFailure = campaigns.filter((campaign) => health(campaign).high).length;
  const healthy = campaigns.filter((campaign) => {
    const stats = health(campaign);
    return stats.total > 0 && !stats.high;
  }).length;

  return (
    <FlatList
      data={campaigns}
      keyExtractor={(item) => item.id}
      contentContainerStyle={styles.content}
      refreshControl={
        <RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.amber} />
      }
      ListHeaderComponent={
        <View>
          <Text style={styles.eyebrow}>RELAY OPS</Text>
          <Text style={styles.title}>Campaign health</Text>
          <Text style={styles.subtitle}>
            {state === 'offline'
              ? 'Showing the last synced snapshot.'
              : lastSync
                ? `Synced ${lastSync.toLocaleTimeString()}`
                : 'Connecting to the delivery API…'}
          </Text>

          <View style={styles.summary}>
            <View style={styles.summaryItem}>
              <Text style={[styles.summaryNumber, { color: colors.green }]}>{healthy}</Text>
              <Text style={styles.summaryLabel}>HEALTHY</Text>
            </View>
            <View style={styles.summaryItem}>
              <Text style={[styles.summaryNumber, { color: colors.red }]}>{highFailure}</Text>
              <Text style={styles.summaryLabel}>NEEDS ATTENTION</Text>
            </View>
            <View style={styles.summaryItem}>
              <Text style={styles.summaryNumber}>{campaigns.length}</Text>
              <Text style={styles.summaryLabel}>CAMPAIGNS</Text>
            </View>
          </View>

          {state === 'offline' && (
            <View style={styles.offline}>
              <Text style={styles.offlineText}>Offline — cached campaign data</Text>
            </View>
          )}
          {state === 'error' && (
            <View style={styles.offline}>
              <Text style={styles.offlineText}>
                API unreachable. Start the Spring service on port 8081, then pull to refresh.
              </Text>
            </View>
          )}
        </View>
      }
      ListEmptyComponent={
        state === 'loading' ? (
          <Text style={styles.empty}>Loading campaigns…</Text>
        ) : (
          <Text style={styles.empty}>No campaigns yet. Create one from the web dashboard.</Text>
        )
      }
      renderItem={({ item }) => <CampaignCard campaign={item} />}
    />
  );
}

const styles = StyleSheet.create({
  content: {
    padding: 20,
    paddingBottom: 40,
    gap: 12,
  },
  eyebrow: {
    color: colors.amber,
    fontSize: 11,
    letterSpacing: 2,
    fontWeight: '700',
  },
  title: {
    color: colors.text,
    fontSize: 34,
    lineHeight: 40,
    fontWeight: '700',
    letterSpacing: -1,
    marginTop: 8,
  },
  subtitle: {
    color: colors.muted,
    fontSize: 13,
    marginTop: 6,
    marginBottom: 18,
  },
  summary: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: 16,
  },
  summaryItem: {
    flex: 1,
    backgroundColor: colors.panel,
    borderColor: colors.line,
    borderWidth: 1,
    borderRadius: 12,
    padding: 12,
  },
  summaryNumber: {
    color: colors.text,
    fontSize: 24,
    fontWeight: '700',
  },
  summaryLabel: {
    color: colors.muted,
    fontSize: 9,
    letterSpacing: 1,
    marginTop: 4,
  },
  offline: {
    backgroundColor: 'rgba(240,163,94,0.1)',
    borderColor: 'rgba(240,163,94,0.35)',
    borderWidth: 1,
    borderRadius: 10,
    padding: 12,
    marginBottom: 12,
  },
  offlineText: {
    color: colors.amber,
    fontSize: 12,
  },
  card: {
    backgroundColor: colors.panel,
    borderColor: colors.line,
    borderWidth: 1,
    borderLeftWidth: 4,
    borderRadius: 12,
    padding: 16,
  },
  cardTop: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 10,
  },
  cardName: {
    color: colors.text,
    fontSize: 17,
    fontWeight: '700',
    flex: 1,
  },
  health: {
    fontSize: 9,
    fontWeight: '800',
    letterSpacing: 1,
  },
  meta: {
    color: colors.muted,
    fontSize: 12,
    marginTop: 6,
  },
  numbers: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 16,
  },
  number: {
    color: colors.text,
    fontSize: 18,
    fontWeight: '700',
  },
  numberLabel: {
    color: colors.muted,
    fontSize: 8,
    letterSpacing: 1,
    marginTop: 3,
  },
  track: {
    height: 5,
    borderRadius: 999,
    backgroundColor: colors.panel2,
    overflow: 'hidden',
    marginTop: 14,
  },
  fill: {
    height: '100%',
  },
  empty: {
    color: colors.muted,
    textAlign: 'center',
    padding: 24,
  },
});
