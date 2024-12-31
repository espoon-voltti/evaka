// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { Fragment, useState } from 'react'
import styled from 'styled-components'

import { useTranslation } from 'employee-frontend/state/i18n'
import { Result } from 'lib-common/api'
import { useQueryResult } from 'lib-common/query'
import { AsyncButton } from 'lib-components/atoms/buttons/AsyncButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import Container, { ContentArea } from 'lib-components/layout/Container'
import { H1, Label, P } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { faPen, faTrash } from 'lib-icons'

import OutOfOfficeEditor from './OutOfOfficeEditor'
import { outOfOfficePeriodsQuery } from './queries'

export default React.memo(function OutOfOfficePage() {
  const { i18n } = useTranslation()
  const [isEditing, setIsEditing] = useState(false)

  const getPeriodsResult = useQueryResult(outOfOfficePeriodsQuery())
  const periods = getPeriodsResult.getOrElse([])

  return (
    <Container>
      <ContentArea opaque>
        <H1>{i18n.outOfOffice.title}</H1>
        <P>{i18n.outOfOffice.description}</P>
        <Gap size="m" />
        <Label>{i18n.outOfOffice.header}</Label>
        <Gap size="s" />
        {periods.length > 0 ? (
          <PeriodListContainer>
            {periods.map((period) => (
              <li key={period.id}>
                <PeriodItemContainer>
                  <div>{period.period.format()}</div>
                  <Button
                    text={i18n.common.edit}
                    appearance="inline"
                    icon={faPen}
                  />
                  <AsyncButton
                    text={i18n.common.remove}
                    onClick={function (): void | Promise<Result<unknown>> {
                      throw new Error('Function not implemented.')
                    }}
                    onSuccess={function (value: unknown): void {
                      throw new Error('Function not implemented.')
                    }}
                    appearance="inline"
                    icon={faTrash}
                  />
                </PeriodItemContainer>
              </li>
            ))}
          </PeriodListContainer>
        ) : isEditing ? (
          <OutOfOfficeEditor onClose={() => setIsEditing(false)} />
        ) : (
          <Fragment>
            <div>{i18n.outOfOffice.noFutureOutOfOffice}</div>
            <Gap size="m" />
            <Button
              text={i18n.outOfOffice.addOutOfOffice}
              primary
              onClick={() => setIsEditing(true)}
            />
          </Fragment>
        )}
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
