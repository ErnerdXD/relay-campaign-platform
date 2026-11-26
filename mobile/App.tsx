import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';

export default function App() {
  return (
    <View style={styles.container}>
      <Text style={styles.eyebrow}>RELAY OPS</Text>
      <Text style={styles.title}>Campaign health, in your pocket.</Text>
      <Text style={styles.lede}>
        Live delivery status, push previews, and alerts for the campaigns that
        need attention.
      </Text>
      <StatusBar style="light" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0d1117',
    padding: 28,
    justifyContent: 'center',
  },
  eyebrow: {
    color: '#f0a35e',
    fontSize: 12,
    letterSpacing: 2,
    marginBottom: 14,
  },
  title: {
    color: '#f5f2ea',
    fontSize: 34,
    lineHeight: 40,
    fontWeight: '700',
    marginBottom: 14,
  },
  lede: {
    color: '#98a2b3',
    fontSize: 16,
    lineHeight: 24,
  },
});
