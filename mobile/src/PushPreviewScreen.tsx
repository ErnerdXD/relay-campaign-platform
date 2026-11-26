import { useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Switch, Text, TextInput, View } from 'react-native';
import { colors } from './theme';

type Width = 'narrow' | 'wide';

export default function PushPreviewScreen() {
  const [title, setTitle] = useState('Statement ready');
  const [body, setBody] = useState(
    'Your September statement is ready. Open Northstar to review the balance and recent transactions.',
  );
  const [width, setWidth] = useState<Width>('wide');
  const [expanded, setExpanded] = useState(false);
  const [rich, setRich] = useState(true);

  return (
    <ScrollView contentContainerStyle={styles.content}>
      <Text style={styles.eyebrow}>PREVIEW</Text>
      <Text style={styles.title}>Push notification</Text>
      <Text style={styles.subtitle}>
        Check how the message collapses and adapts across device widths before it ships.
      </Text>

      <View style={styles.controls}>
        <Text style={styles.controlLabel}>Device width</Text>
        <View style={styles.segment}>
          <Pressable
            onPress={() => setWidth('narrow')}
            style={[styles.segmentItem, width === 'narrow' && styles.segmentActive]}
          >
            <Text style={[styles.segmentText, width === 'narrow' && styles.segmentTextActive]}>
              Small
            </Text>
          </Pressable>
          <Pressable
            onPress={() => setWidth('wide')}
            style={[styles.segmentItem, width === 'wide' && styles.segmentActive]}
          >
            <Text style={[styles.segmentText, width === 'wide' && styles.segmentTextActive]}>
              Large
            </Text>
          </Pressable>
        </View>
      </View>

      <View style={styles.toggleRow}>
        <Text style={styles.controlLabel}>Collapsed</Text>
        <Switch
          value={!expanded}
          onValueChange={(value) => setExpanded(!value)}
          trackColor={{ true: colors.amber, false: colors.line }}
          thumbColor={colors.text}
        />
      </View>

      <View style={styles.toggleRow}>
        <Text style={styles.controlLabel}>Rich media</Text>
        <Switch
          value={rich}
          onValueChange={setRich}
          trackColor={{ true: colors.amber, false: colors.line }}
          thumbColor={colors.text}
        />
      </View>

      <View style={[styles.device, width === 'narrow' ? styles.narrow : styles.wide]}>
        <View style={styles.notification}>
          <View style={styles.notificationHead}>
            <View style={styles.appIcon}>
              <Text style={styles.appIconText}>R</Text>
            </View>
            <Text style={styles.appName}>RELAY OPS</Text>
            <Text style={styles.time}>now</Text>
          </View>
          <Text style={styles.notificationTitle}>{title || 'Notification title'}</Text>
          <Text style={styles.notificationBody} numberOfLines={expanded ? undefined : 2}>
            {body || 'Notification body'}
          </Text>
          {rich && (
            <View style={styles.media}>
              <Text style={styles.mediaLabel}>rich media</Text>
            </View>
          )}
        </View>
      </View>

      <View style={styles.form}>
        <Text style={styles.controlLabel}>Title</Text>
        <TextInput
          value={title}
          onChangeText={setTitle}
          placeholder="Notification title"
          placeholderTextColor={colors.muted}
          style={styles.input}
        />
        <Text style={styles.controlLabel}>Body</Text>
        <TextInput
          value={body}
          onChangeText={setBody}
          multiline
          placeholder="Notification body"
          placeholderTextColor={colors.muted}
          style={[styles.input, styles.bodyInput]}
        />
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  content: {
    padding: 20,
    paddingBottom: 48,
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
    lineHeight: 20,
    marginTop: 8,
    marginBottom: 22,
  },
  controls: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 14,
  },
  controlLabel: {
    color: colors.muted,
    fontSize: 12,
    letterSpacing: 1,
    textTransform: 'uppercase',
    marginBottom: 6,
  },
  segment: {
    flexDirection: 'row',
    backgroundColor: colors.panel,
    borderRadius: 9,
    borderColor: colors.line,
    borderWidth: 1,
    padding: 3,
  },
  segmentItem: {
    paddingVertical: 7,
    paddingHorizontal: 14,
    borderRadius: 6,
  },
  segmentActive: {
    backgroundColor: colors.panel2,
  },
  segmentText: {
    color: colors.muted,
    fontSize: 12,
  },
  segmentTextActive: {
    color: colors.text,
    fontWeight: '700',
  },
  toggleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 10,
  },
  device: {
    alignSelf: 'center',
    backgroundColor: '#05080c',
    borderRadius: 28,
    borderColor: colors.line,
    borderWidth: 1,
    padding: 12,
    marginVertical: 22,
  },
  narrow: {
    width: 300,
  },
  wide: {
    width: 360,
  },
  notification: {
    backgroundColor: 'rgba(22,32,43,0.96)',
    borderRadius: 18,
    padding: 14,
    borderColor: 'rgba(147,161,177,0.18)',
    borderWidth: 1,
  },
  notificationHead: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 7,
  },
  appIcon: {
    width: 18,
    height: 18,
    borderRadius: 5,
    backgroundColor: colors.amber,
    alignItems: 'center',
    justifyContent: 'center',
  },
  appIconText: {
    color: '#1a1208',
    fontSize: 11,
    fontWeight: '900',
  },
  appName: {
    color: colors.muted,
    fontSize: 10,
    letterSpacing: 1,
    flex: 1,
  },
  time: {
    color: colors.muted,
    fontSize: 10,
  },
  notificationTitle: {
    color: colors.text,
    fontSize: 15,
    fontWeight: '700',
    marginTop: 9,
  },
  notificationBody: {
    color: colors.muted,
    fontSize: 13,
    lineHeight: 18,
    marginTop: 4,
  },
  media: {
    height: 96,
    borderRadius: 10,
    marginTop: 10,
    backgroundColor: 'rgba(240,163,94,0.16)',
    borderColor: 'rgba(240,163,94,0.3)',
    borderWidth: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  mediaLabel: {
    color: colors.amber,
    fontSize: 10,
    letterSpacing: 2,
    textTransform: 'uppercase',
  },
  form: {
    gap: 8,
  },
  input: {
    backgroundColor: colors.panel,
    borderColor: colors.line,
    borderWidth: 1,
    borderRadius: 9,
    color: colors.text,
    padding: 12,
    marginBottom: 8,
  },
  bodyInput: {
    minHeight: 90,
    textAlignVertical: 'top',
  },
});
