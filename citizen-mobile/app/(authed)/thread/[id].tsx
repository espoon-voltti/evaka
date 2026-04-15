// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useLocalSearchParams } from 'expo-router'
import { useEffect, useMemo, useState } from 'react'
import {
  FlatList,
  KeyboardAvoidingView,
  Platform,
  StyleSheet,
  View
} from 'react-native'
import {
  ActivityIndicator,
  Button,
  Divider,
  HelperText,
  Text,
  TextInput
} from 'react-native-paper'

import { ApiError } from '../../../src/api/client'
import { t } from '../../../src/i18n'
import {
  useMarkReadMutation,
  useMyAccountQuery,
  useReplyMutation,
  useThreadQuery
} from '../../../src/messages/queries'

export default function ThreadScreen() {
  const { id } = useLocalSearchParams<{ id: string }>()
  const thread = useThreadQuery(id)
  const myAccount = useMyAccountQuery()
  const reply = useReplyMutation()
  const markRead = useMarkReadMutation()

  const [draft, setDraft] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [forbidden, setForbidden] = useState(false)

  useEffect(() => {
    if (thread.data && id) markRead.mutate(id)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [thread.data?.id])

  const recipientAccountIds = useMemo(() => {
    if (!thread.data || !myAccount.data) return []
    const myId = myAccount.data.accountId
    const ids = new Set<string>()
    for (const m of thread.data.messages) {
      if (m.senderAccountId !== myId) ids.add(m.senderAccountId)
    }
    return [...ids]
  }, [thread.data, myAccount.data])

  if (thread.isLoading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" />
      </View>
    )
  }
  if (thread.error || !thread.data) {
    return (
      <View style={styles.center}>
        <Text>{t('thread.notFound')}</Text>
      </View>
    )
  }

  async function submit() {
    setError(null)
    if (!id || !draft.trim()) return
    try {
      await reply.mutateAsync({
        threadId: id,
        content: draft.trim(),
        recipientAccountIds
      })
      setDraft('')
    } catch (e) {
      if (e instanceof ApiError && e.status === 403) {
        setForbidden(true)
        setError(t('thread.replyForbidden'))
      } else {
        setError(t('thread.replyError'))
      }
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <FlatList
        data={thread.data.messages}
        keyExtractor={(m) => m.id}
        ItemSeparatorComponent={Divider}
        ListHeaderComponent={
          <Text variant="titleLarge" style={styles.title}>
            {thread.data.title}
          </Text>
        }
        renderItem={({ item }) => (
          <View style={styles.message}>
            <Text variant="labelMedium">{item.senderName}</Text>
            <Text style={styles.body}>{item.content}</Text>
            <Text variant="bodySmall" style={styles.timestamp}>
              {new Date(item.sentAt).toLocaleString()}
            </Text>
          </View>
        )}
      />
      <View style={styles.composer}>
        <TextInput
          multiline
          mode="outlined"
          placeholder={t('thread.replyPlaceholder')}
          value={draft}
          onChangeText={setDraft}
          editable={!forbidden}
          style={styles.input}
          testID="reply-input"
        />
        {error ? (
          <HelperText type="error" visible>
            {error}
          </HelperText>
        ) : null}
        <Button
          mode="contained"
          onPress={submit}
          disabled={
            !draft.trim() ||
            reply.isPending ||
            forbidden ||
            recipientAccountIds.length === 0
          }
          loading={reply.isPending}
          testID="reply-send"
        >
          {t('thread.replySend')}
        </Button>
      </View>
    </KeyboardAvoidingView>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24
  },
  title: { padding: 16 },
  message: { padding: 16, gap: 4 },
  body: { lineHeight: 22 },
  timestamp: { color: '#666' },
  composer: {
    padding: 16,
    gap: 8,
    borderTopWidth: 1,
    borderTopColor: '#eee'
  },
  input: { maxHeight: 140 }
})
