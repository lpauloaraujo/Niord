import * as Device from 'expo-device';
import { Platform, StyleSheet, Button } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { AnimatedIcon } from '@/components/animated-icon';
import { HintRow } from '@/components/hint-row';
import { ThemedText } from '@/components/themed-text';
import { ThemedView } from '@/components/themed-view';
import { WebBadge } from '@/components/web-badge';
import { BottomTabInset, MaxContentWidth, Spacing } from '@/constants/theme';

import { FloatingBubbleHelper } from 'tylo-floating-bubble';

function getDevMenuHint() {
  if (Platform.OS === 'web') {
    return <ThemedText type="small">use browser devtools</ThemedText>;
  }
  if (Device.isDevice) {
    return (
      <ThemedText type="small">
        shake device or press <ThemedText type="code">m</ThemedText> in terminal
      </ThemedText>
    );
  }
  const shortcut = Platform.OS === 'android' ? 'cmd+m (or ctrl+m)' : 'cmd+d';
  return (
    <ThemedText type="small">
      press <ThemedText type="code">{shortcut}</ThemedText>
    </ThemedText>
  );
}

export default function HomeScreen() {
  const showBubble = async () => {
    const bubbleData = {
      // Bubble params
      title: "New Order #1234",
      subtitle: "Tap to view order details",
      showBadge: true,
      badgeCount: 0,
      
      // Popup params
      popupTitle: "New Ride Request",
      popupPrice: "$24.50",
      popupDuration: "15 min",
      popupDistance: "8.2 km",
      popupPickupAddress: "123 Main Street, Downtown Area",
      popupDestinationAddress: "456 Business Center, Tech District",
      popupPaymentMethod: "Credit Card",
      popupAcceptText: "Accept",
      popupRejectText: "Reject",
    };
    let perm = false; 
    if(await FloatingBubbleHelper.checkPermission()){
      perm = await FloatingBubbleHelper.requestPermission();
    }
    if(perm){
      if(!await FloatingBubbleHelper.isVisible()){
        await FloatingBubbleHelper.showBubble(bubbleData);
      }else{
        await FloatingBubbleHelper.hideBubble();
      }
    }
  };
  return (
    <ThemedView style={styles.container}>
      <SafeAreaView style={styles.safeArea}>
        <ThemedView style={styles.heroSection}>
          <AnimatedIcon />
          <ThemedText type="title" style={styles.title}>
            Hello, world!{'\n'}
            Niord{'\n'}
            The{'\n'}
            Best
          </ThemedText>
        </ThemedView>
        <Button
        title="Mostrar Overlay"
        onPress={showBubble}
        />

        <ThemedText type="code" style={styles.code}>
          get started
        </ThemedText>

        <ThemedView type="backgroundElement" style={styles.stepContainer}>
          <HintRow
            title="Try editing"
            hint={<ThemedText type="code">src/app/index.tsx</ThemedText>}
          />
          <HintRow title="Dev tools" hint={getDevMenuHint()} />
          <HintRow
            title="Fresh start"
            hint={<ThemedText type="code">npm run reset-project</ThemedText>}
          />
        </ThemedView>

        {Platform.OS === 'web' && <WebBadge />}
      </SafeAreaView>
    </ThemedView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    flexDirection: 'row',
  },
  safeArea: {
    flex: 1,
    paddingHorizontal: Spacing.four,
    alignItems: 'center',
    gap: Spacing.three,
    paddingBottom: BottomTabInset + Spacing.three,
    maxWidth: MaxContentWidth,
  },
  heroSection: {
    alignItems: 'center',
    justifyContent: 'center',
    flex: 1,
    paddingHorizontal: Spacing.four,
    gap: Spacing.four,
  },
  title: {
    textAlign: 'center',
  },
  code: {
    textTransform: 'uppercase',
  },
  stepContainer: {
    gap: Spacing.three,
    alignSelf: 'stretch',
    paddingHorizontal: Spacing.three,
    paddingVertical: Spacing.four,
    borderRadius: Spacing.four,
  },
});
