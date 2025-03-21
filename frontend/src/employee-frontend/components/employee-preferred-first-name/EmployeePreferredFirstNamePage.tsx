// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'

import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import Select from 'lib-components/atoms/dropdowns/Select'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { Label, P } from 'lib-components/typography'

import { useTranslation } from '../../state/i18n'
import { UserContext } from '../../state/user'

import {
  employeePreferredFirstNameQuery,
  setEmployeePreferredFirstNameMutation
} from './queries'

export default React.memo(function EmployeePreferredFirstNamePage() {
  const { i18n } = useTranslation()
  const { refreshAuthStatus } = useContext(UserContext)
  const preferredFirstName = useQueryResult(employeePreferredFirstNameQuery())

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

  const disableConfirm = () =>
    preferredFirstName.isSuccess
      ? preferredFirstName.value.preferredFirstName != null &&
        preferredFirstName.value.preferredFirstName ===
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
          <MutateButton
            primary
            disabled={disableConfirm()}
            text={i18n.preferredFirstName.confirm}
            mutation={setEmployeePreferredFirstNameMutation}
            onClick={() => ({
              body: {
                preferredFirstName: selectedPreferredFirstName
              }
            })}
            onSuccess={() => refreshAuthStatus()}
            data-qa="confirm-button"
          />
        </FixedSpaceColumn>
      </ContentArea>
    </Container>
  )
})
