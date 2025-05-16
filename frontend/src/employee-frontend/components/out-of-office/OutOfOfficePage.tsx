// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useState } from 'react'
import styled from 'styled-components'

import type { OutOfOfficePeriod } from 'lib-common/generated/api-types/outofoffice'
import { useMutationResult, useQueryResult } from 'lib-common/query'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faPen, faTrash } from 'lib-icons'

import { useTranslation } from '../../state/i18n'

import OutOfOfficeEditor from './OutOfOfficeEditor'
import {
  deleteOutOfOfficePeriodMutation,
  outOfOfficePeriodsQuery
} from './queries'

export default React.memo(function OutOfOfficePage() {
  const { i18n } = useTranslation()
  const [isEditing, setIsEditing] = useState(false)
  const [selectedPeriod, setSelectedPeriod] =
    useState<OutOfOfficePeriod | null>(null)

  const getPeriodsResult = useQueryResult(outOfOfficePeriodsQuery())
  const periods = getPeriodsResult.getOrElse([])

  const { mutateAsync: deletePeriod } = useMutationResult(
    deleteOutOfOfficePeriodMutation
  )

  function startEdit(period: OutOfOfficePeriod) {
    setSelectedPeriod(period)
    setIsEditing(true)
  }

  function onCloseEditor() {
    setSelectedPeriod(null)
    setIsEditing(false)
  }

  return (
    <Container>
      <ContentArea opaque data-qa="out-of-office-page">
        <H1>{i18n.outOfOffice.title}</H1>
        <P>{i18n.outOfOffice.description}</P>
        <Gap size="m" />
        <Label>{i18n.outOfOffice.header}</Label>
        <Gap size="s" />
        {!isEditing && periods.length > 0 ? (
          <PeriodListContainer>
            {periods.map((period) => (
              <li key={period.id}>
                <PeriodItemContainer>
                  <div>{period.period.format()}</div>
                  <Button
                    text={i18n.common.edit}
                    appearance="inline"
                    icon={faPen}
                    onClick={() => startEdit(period)}
                    data-qa="edit-out-of-office"
                  />
                  <AsyncButton
                    text={i18n.common.remove}
                    onClick={() => deletePeriod({ id: period.id })}
                    onSuccess={() => void {}}
                    appearance="inline"
                    icon={faTrash}
                    data-qa="remove-out-of-office"
                  />
                </PeriodItemContainer>
              </li>
            ))}
          </PeriodListContainer>
        ) : null}
        {isEditing ? (
          <OutOfOfficeEditor
            onClose={onCloseEditor}
            editedPeriod={selectedPeriod}
          />
        ) : null}
        {periods.length === 0 && !isEditing ? (
          <Fragment>
            <div>{i18n.outOfOffice.noFutureOutOfOffice}</div>
            <Gap size="m" />
            <Button
              text={i18n.outOfOffice.addOutOfOffice}
              primary
              onClick={() => setIsEditing(true)}
              data-qa="add-out-of-office"
            />
          </Fragment>
        ) : null}
      </ContentArea>
    </Container>
  )
})

const PeriodListContainer = styled.ul`
  margin-block-start: 0;
  padding-inline-start: ${defaultMargins.m};
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.m};
`
const PeriodItemContainer = styled.div`
  display: flex;
  align-items: center;
  gap: ${defaultMargins.m};

  & > :first-child {
    margin-right: ${defaultMargins.s};
  }
`
