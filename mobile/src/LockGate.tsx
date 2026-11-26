import { useCallback, useEffect, useState, type ReactNode } from 'react';
import * as LocalAuthentication from 'expo-local-authentication';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { colors } from './theme';

export default function LockGate({ children }: { children: ReactNode }) {
  const [locked, setLocked] = useState(true);
  const [message, setMessage] = useState('');

  const unlock = useCallback(async () => {
    setMessage('');

    const hasHardware = await LocalAuthentication.hasHardwareAsync();
    const enrolled = await LocalAuthentication.isEnrolledAsync();
    if (!hasHardware || !enrolled) {
      setLocked(false);
      return;
    }

    const result = await LocalAuthentication.authenticateAsync({
      promptMessage: 'Unlock Relay Ops',
      fallbackLabel: 'Use device passcode',
      cancelLabel: 'Cancel',
    });

    if (result.success) {
      setLocked(false);
    } else {
      setMessage('Authentication cancelled.');
    }
  }, []);

  useEffect(() => {
    void unlock();
  }, [unlock]);

  if (!locked) {
    return <>{children}</>;
  }

  return (
    <View style={styles.lock}>
      <Text style={styles.eyebrow}>RELAY OPS</Text>
      <Text style={styles.title}>Locked</Text>
      <Text style={styles.lede}>
        Campaign delivery data is tenant-sensitive. Unlock with Face ID, Touch ID, or your device
        passcode.
      </Text>
      <Pressable accessibilityRole="button" onPress={() => void unlock()} style={styles.button}>
        <Text style={styles.buttonText}>Unlock</Text>
      </Pressable>
      {message ? <Text style={styles.message}>{message}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  lock: {
    flex: 1,
    backgroundColor: colors.bg,
    justifyContent: 'center',
    padding: 28,
  },
  eyebrow: {
    color: colors.amber,
    fontSize: 11,
    letterSpacing: 2,
    fontWeight: '700',
  },
  title: {
    color: colors.text,
    fontSize: 44,
    fontWeight: '700',
    letterSpacing: -1,
    marginTop: 10,
  },
  lede: {
    color: colors.muted,
    fontSize: 14,
    lineHeight: 22,
    marginTop: 10,
    maxWidth: 340,
  },
  button: {
    marginTop: 24,
    alignSelf: 'flex-start',
    backgroundColor: colors.amber,
    borderRadius: 9,
    paddingVertical: 12,
    paddingHorizontal: 22,
  },
  buttonText: {
    color: '#1a1208',
    fontSize: 14,
    fontWeight: '800',
  },
  message: {
    color: colors.red,
    fontSize: 12,
    marginTop: 12,
  },
});
