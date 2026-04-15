// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useRouter } from 'expo-router'
import { FlatList, RefreshControl, StyleSheet, View } from 'react-native'
import {
  ActivityIndicator,
  Avatar,
  Badge,
  Button,
  Divider,
  List,
  Text
} from 'react-native-paper'

import { t } from '../../src/i18n'
import { useThreadsQuery } from '../../src/messages/queries'

export default function InboxScreen() {
  const router = useRouter()
  const { data, isLoading, isRefetching, refetch, error } = useThreadsQuery()

  if (isLoading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    )
  }
  if (error) {
    return (
      <View style={styles.center}>
        <Text>{t('common.error')}</Text>
        <Button onPress={() => void refetch()}>{t('common.retry')}</Button>
      </View>
    )
  }

  return (
    <FlatList
      data={data?.data ?? []}
      keyExtractor={(item) => item.id}
      ItemSeparatorComponent={Divider}
      refreshControl={
        <RefreshControl
          refreshing={isRefetching}
          onRefresh={() => void refetch()}
        />
      }
      ListEmptyComponent={
        <View style={styles.center}>
          <Text>{t('inbox.empty')}</Text>
        </View>
      }
      renderItem={({ item }) => (
        <List.Item
          title={item.title}
          description={`${item.senderName} — ${item.lastMessagePreview}`}
          left={(p) => (
            <Avatar.Text
              {...p}
              size={40}
              label={item.senderName.slice(0, 2).toUpperCase()}
            />
          )}
          right={(p) =>
            item.unreadCount > 0 ? (
              <Badge {...p} style={{ alignSelf: 'center' }}>
                {item.unreadCount}
              </Badge>
            ) : null
          }
          onPress={() => router.push(`/(authed)/thread/${item.id}`)}
        />
      )}
    />
  )
}

const styles = StyleSheet.create({
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
    gap: 12
  }
})
