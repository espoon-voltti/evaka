// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useState } from 'react'

import type { SettingType } from 'lib-common/generated/api-types/setting'
import { settings as options } from 'lib-common/generated/api-types/setting'
import { useQueryResult } from 'lib-common/query'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import InputField from 'lib-components/atoms/form/InputField'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import ExpandingInfo from 'lib-components/molecules/ExpandingInfo'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { putSettingsMutation, settingsQuery } from './queries'

const defaultValues = options.reduce(
  (prev, curr) => ({ ...prev, [curr]: '' }),
  {}
)

export default React.memo(function SettingsPageWrapper() {
  const { i18n } = useTranslation()
  const settings = useQueryResult(settingsQuery()).map((s) => ({
    ...defaultValues,
    ...s
  }))

  return (
    <Container>
      <ContentArea opaque>
        <H1>{i18n.titles.settings}</H1>
        {renderResult(settings, (settings) => (
          <SettingsPage initialSettings={settings} />
        ))}
      </ContentArea>
    </Container>
  )
})

const SettingsPage = React.memo(function SettingsPage({
  initialSettings
}: {
  initialSettings: Partial<Record<SettingType, string>>
}) {
  const { i18n } = useTranslation()
  const [settings, setSettings] = useState(initialSettings)

  const onChange = useCallback(
    (option: SettingType, value: string) =>
      setSettings((prevState) => ({
        ...prevState,
        [option]: value
      })),
    []
  )

  return (
    <>
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
              value={settings[option] ?? ''}
              onChange={onChange}
            />
          ))}
        </Tbody>
      </Table>
      <Gap size="s" />
      <MutateButton
        primary
        text={i18n.common.save}
        mutation={putSettingsMutation}
        onClick={() => ({ body: settings })}
      />
    </>
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
