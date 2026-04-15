// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useRouter } from 'expo-router'
import { useState } from 'react'
import { StyleSheet, View } from 'react-native'
import { Button, List, RadioButton, Text } from 'react-native-paper'

import { useAuth } from '../../src/auth/state'
import { i18n, setLanguage, t } from '../../src/i18n'
import type { Language } from '../../src/i18n'

const LANGUAGES: { code: Language; labelKey: string }[] = [
  { code: 'fi', labelKey: 'settings.languageFi' },
  { code: 'sv', labelKey: 'settings.languageSv' },
  { code: 'en', labelKey: 'settings.languageEn' }
]

export default function SettingsScreen() {
  const { logout } = useAuth()
  const router = useRouter()
  const [current, setCurrent] = useState<Language>(i18n.locale as Language)

  function pickLanguage(lang: Language) {
    setLanguage(lang)
    setCurrent(lang)
  }

  async function handleLogout() {
    await logout()
    router.replace('/login')
  }

  return (
    <View style={styles.container}>
      <List.Section>
        <List.Subheader>{t('settings.language')}</List.Subheader>
        <RadioButton.Group
          onValueChange={(value) => pickLanguage(value as Language)}
          value={current}
        >
          {LANGUAGES.map(({ code, labelKey }) => (
            <RadioButton.Item key={code} label={t(labelKey)} value={code} />
          ))}
        </RadioButton.Group>
      </List.Section>
      <Button
        mode="outlined"
        onPress={handleLogout}
        style={styles.logout}
        testID="logout"
      >
        <Text>{t('settings.logout')}</Text>
      </Button>
    </View>
  )
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16, gap: 16 },
  logout: { marginTop: 16 }
})
