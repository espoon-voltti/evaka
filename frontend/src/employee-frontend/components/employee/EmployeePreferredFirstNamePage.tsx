// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'

import { wrapResult } from 'lib-common/api'
import { useApiState } from 'lib-common/utils/useRestApi'
import Title from 'lib-components/atoms/Title'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label, P } from 'lib-components/typography'

import {
  getEmployeePreferredFirstName,
  setEmployeePreferredFirstName
} from '../../generated/api-clients/pis'
import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'

const getEmployeePreferredFirstNameResult = wrapResult(
  getEmployeePreferredFirstName
)
const setEmployeePreferredFirstNameResult = wrapResult(
  setEmployeePreferredFirstName
)

export default React.memo(function EmployeePreferredFirstNamePage() {
  const { i18n } = useTranslation()
  const { refreshAuthStatus } = useContext(UserContext)
  const [preferredFirstName, loadPreferredFirstName] = useApiState(
    getEmployeePreferredFirstNameResult,
    []
  )

  const [selectedPreferredFirstName, setSelectedPreferredFirstName] = useState<
    string | null
  >(null)

  useEffect(() => {
    if (
      preferredFirstName.isSuccess &&
      preferredFirstName.value.preferredFirstNameOptions.length > 0
    ) {
      const initialPreferredFirstName =
        preferredFirstName.value.preferredFirstName ||
        preferredFirstName.value.preferredFirstNameOptions[0]
      setSelectedPreferredFirstName(initialPreferredFirstName)
    }
  }, [preferredFirstName])

  const onSave = () =>
    setEmployeePreferredFirstNameResult({
      body: { preferredFirstName: selectedPreferredFirstName }
    }).then(loadPreferredFirstName)

  const disableConfirm = () =>
    preferredFirstName.isSuccess
      ? preferredFirstName.value.preferredFirstName != null &&
        preferredFirstName.value.preferredFirstName ==
          selectedPreferredFirstName
      : false

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
            onSuccess={() => refreshAuthStatus()}
            data-qa="confirm-button"
          />
        </FixedSpaceColumn>
      </ContentArea>
    </Container>
  )
})
