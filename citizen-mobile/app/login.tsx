// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useRouter } from 'expo-router'
import { useState } from 'react'
import { KeyboardAvoidingView, Platform, StyleSheet, View } from 'react-native'
import { Button, HelperText, Text, TextInput } from 'react-native-paper'

import { useAuth } from '../src/auth/state'
import { t } from '../src/i18n'

export default function LoginScreen() {
  const router = useRouter()
  const { login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [pending, setPending] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function submit() {
    setPending(true)
    setError(null)
    const result = await login(username, password)
    setPending(false)
    if (result === 'ok') {
      router.replace('/(authed)')
    } else if (result === 'invalid') {
      setError(t('login.invalidCredentials'))
    } else if (result === 'rate-limited') {
      setError(t('login.rateLimited'))
    } else {
      setError(t('common.networkError'))
    }
  }

  return (
    <KeyboardAvoidingView
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      style={styles.container}
    >
      <View style={styles.form}>
        <Text variant="headlineMedium" style={styles.title}>
          {t('login.title')}
        </Text>
        <TextInput
          label={t('login.username')}
          value={username}
          onChangeText={setUsername}
          autoCapitalize="none"
          autoCorrect={false}
          keyboardType="email-address"
          style={styles.input}
          testID="username"
        />
        <TextInput
          label={t('login.password')}
          value={password}
          onChangeText={setPassword}
          secureTextEntry
          style={styles.input}
          testID="password"
        />
        {error ? (
          <HelperText type="error" visible>
            {error}
          </HelperText>
        ) : null}
        <Button
          mode="contained"
          onPress={submit}
          loading={pending}
          disabled={pending || !username || !password}
          style={styles.submit}
          testID="login-submit"
        >
          {t('login.submit')}
        </Button>
      </View>
    </KeyboardAvoidingView>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', padding: 24 },
  form: { gap: 12 },
  title: { marginBottom: 24, textAlign: 'center' },
  input: { marginBottom: 8 },
  submit: { marginTop: 16 }
})
