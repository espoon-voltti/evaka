// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'

import { Loading, Result } from 'lib-common/api'
import { EmployeePreferredFirstName } from 'lib-common/generated/api-types/pis'
import Title from 'lib-components/atoms/Title'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label, P } from 'lib-components/typography'

import {
  getEmployeePreferredFirstName,
  setEmployeePreferredFirstName
} from '../../api/employees'
import { useTranslation } from '../../state/i18n'

export default React.memo(function EmployeePreferredFirstNamePage() {
  const { i18n } = useTranslation()
  const [preferredFirstName, setPreferredFirstName] = useState<
    Result<EmployeePreferredFirstName>
  >(Loading.of())
  const [selectedPreferredFirstName, setSelectedPreferredFirstName] = useState<
    string | null
  >(null)

  useEffect(() => {
    setPreferredFirstName(Loading.of())
    void getEmployeePreferredFirstName().then(setPreferredFirstName)
  }, [])

  useEffect(() => {
    if (preferredFirstName.isSuccess) {
      setSelectedPreferredFirstName(preferredFirstName.value.preferredFirstName)
    }
  }, [preferredFirstName])

  const onSave = () => {
    return setEmployeePreferredFirstName({
      preferredFirstName: selectedPreferredFirstName
    })
  }

  const disableConfirm = () => {
    return preferredFirstName.isSuccess
      ? preferredFirstName.value.preferredFirstName != null &&
          preferredFirstName.value.preferredFirstName ==
            selectedPreferredFirstName
      : false
  }

  return (
    <Container>
      <ContentArea opaque>
        <Title>{i18n.preferredFirstName.popupLink}</Title>
        <P>{i18n.preferredFirstName.description}</P>
        <FixedSpaceColumn spacing="xs">
          <Label>{i18n.preferredFirstName.select}</Label>
          <Select
            items={
              preferredFirstName.isSuccess
                ? preferredFirstName.value.preferredFirstNameOptions
                : []
            }
            selectedItem={
              preferredFirstName.isSuccess
                ? selectedPreferredFirstName ||
                  preferredFirstName.value.preferredFirstName
                : null
            }
            onChange={(value) => setSelectedPreferredFirstName(value)}
            data-qa="select-preferred-first-name"
          />
          <AsyncButton
            primary
            disabled={disableConfirm()}
            text={i18n.preferredFirstName.confirm}
            onClick={onSave}
            onSuccess={() => location.reload()}
            data-qa="confirm-button"
          />
        </FixedSpaceColumn>
      </ContentArea>
    </Container>
  )
})
