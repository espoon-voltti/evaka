// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useTranslation } from 'employee-frontend/state/i18n'
import { Failure, Loading, Result, Success } from 'lib-common/api'
import {
  settings as options,
  SettingType
} from 'lib-common/generated/api-types/setting'
import { JsonOf } from 'lib-common/json'
import { useRestApi } from 'lib-common/utils/useRestApi'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import InputField from 'lib-components/atoms/form/InputField'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import React, { useCallback, useEffect, useState } from 'react'
import { client } from '../api/client'
import { renderResult } from './async-rendering'

type Settings = Record<SettingType, string>

const defaultValues = options.reduce(
  (prev, curr) => ({ ...prev, [curr]: '' }),
  {}
)

async function getSettings(): Promise<Result<Settings>> {
  return client
    .get<JsonOf<Settings>>('/settings')
    .then((res) => Success.of({ ...defaultValues, ...res.data }))
    .catch((e) => Failure.fromError(e))
}

async function putSettings(settings: Partial<Settings>): Promise<Result<void>> {
  return client
    .put<JsonOf<Settings>>(`/settings`, settings)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export default React.memo(function SettingsPage() {
  const { i18n } = useTranslation()

  const [settings, setSettings] = useState<Result<Settings>>(Loading.of())
  const loadSettings = useRestApi(getSettings, setSettings)
  useEffect(loadSettings, [loadSettings])

  const submit = useCallback(async () => {
    if (!settings.isSuccess) return

    return await putSettings(
      Object.fromEntries(
        Object.entries(settings.value)
          .map(([key, value]) => [key, value.trim()])
          .filter(([_, value]) => value !== '')
      )
    )
  }, [settings])

  const onChange = useCallback(
    (option: SettingType, value: string) =>
      setSettings((prevState) =>
        prevState.map((prev) => ({
          ...prev,
          [option]: value
        }))
      ),
    []
  )

  return (
    <Container>
      <ContentArea opaque>
        <H1>{i18n.settings.title}</H1>
        {renderResult(settings, (settings) => (
          <Table>
            <Thead>
              <Tr>
                <Th>{i18n.settings.key}</Th>
                <Th>{i18n.settings.value}</Th>
              </Tr>
            </Thead>
            <Tbody>
              {options.map((option) => (
                <SettingRow
                  key={option}
                  option={option}
                  value={settings[option]}
                  onChange={onChange}
                />
              ))}
            </Tbody>
          </Table>
        ))}
        <Gap size="s" />
        <AsyncButton
          primary
          text={i18n.common.save}
          onClick={submit}
          // eslint-disable-next-line @typescript-eslint/no-empty-function
          onSuccess={() => {}}
        />
      </ContentArea>
    </Container>
  )
})

const SettingRow = React.memo(function SettingRow({
  option,
  value,
  onChange
}: {
  option: SettingType
  value: string
  onChange: (option: SettingType, value: string) => void
}) {
  const { i18n } = useTranslation()
  const handleChange = useCallback(
    (value: string) => onChange(option, value),
    [onChange, option]
  )
  return (
    <Tr>
      <Td>
        <ExpandingInfo
          info={i18n.settings.options[option].description}
          ariaLabel={i18n.common.openExpandingInfo}
          fullWidth={true}
        >
          {i18n.settings.options[option].title}
        </ExpandingInfo>
      </Td>
      <Td>
        <InputField value={value} onChange={handleChange} />
      </Td>
    </Tr>
  )
})
