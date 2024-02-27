// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useEffect, useState } from 'react'

import { useTranslation } from 'employee-frontend/state/i18n'
import { Loading, Result, wrapResult } from 'lib-common/api'
import {
  settings as options,
  SettingType
} from 'lib-common/generated/api-types/setting'
import { useRestApi } from 'lib-common/utils/useRestApi'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import InputField from 'lib-components/atoms/form/InputField'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { getSettings, setSettings } from '../generated/api-clients/setting'

import { renderResult } from './async-rendering'

const getSettingsResult = wrapResult(getSettings)
const setSettingsResult = wrapResult(setSettings)

type Settings = Record<SettingType, string>

const defaultValues = options.reduce(
  (prev, curr) => ({ ...prev, [curr]: '' }),
  {}
)

export default React.memo(function SettingsPage() {
  const { i18n } = useTranslation()

  const [settings, setSettings] = useState<Result<Settings>>(Loading.of())
  const loadSettings = useRestApi(
    () =>
      getSettingsResult().then((res) =>
        res.map((s) => ({ ...defaultValues, ...s }))
      ),
    setSettings
  )
  useEffect(() => {
    void loadSettings()
  }, [loadSettings])

  const submit = useCallback(() => {
    if (!settings.isSuccess) return

    return setSettingsResult(
      // eslint-disable-next-line @typescript-eslint/no-unsafe-argument
      Object.fromEntries(
        Object.entries(settings.value)
          .map(([key, value]) => [key, value.trim()])
          .filter(([, value]) => value !== '')
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
        <H1>{i18n.titles.settings}</H1>
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
          width="full"
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
