// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { GuardianApplications } from '~applications/types'
import { useTranslation } from '~localization'
import { H2, H3, Label } from '@evaka/lib-components/src/typography'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import { defaultMargins, Gap } from '@evaka/lib-components/src/white-space'
import styled from 'styled-components'
import ListGrid from '@evaka/lib-components/src/layout/ListGrid'
import { DATE_FORMAT_DATE_TIME, formatDate } from '~util'
import { ApplicationLink } from '~decisions/ApplicationLink'

const MobileFriendlyListGrid = styled(ListGrid)`
  @media (max-width: 600px) {
    grid-template-columns: auto;
    row-gap: ${defaultMargins.xxs};

    *:nth-child(2n) {
      margin-bottom: ${defaultMargins.s};
    }
  }
`

const LineBreak = styled.div`
  width: 100%;
  border: 1px solid #d8d8d8;
  margin: 40px 0 20px 0;
`

export default React.memo(function ChildApplicationsBlock({
  childId,
  childName,
  applicationSummaries
}: GuardianApplications) {
  const t = useTranslation()

  return (
    <ContentArea opaque paddingVertical="L" data-qa={`child-${childId}`}>
      <H2 noMargin data-qa={`title-applications-child-name-${childId}`}>
        {childName}
      </H2>
      {applicationSummaries.map(
        (
          {
            applicationId,
            type,
            preferredUnitName,
            startDate,
            createdDate,
            modifiedDate,
            applicationStatus
          },
          index
        ) => (
          <React.Fragment key={applicationId}>
            <Gap size="L" />
            <H3 noMargin data-qa={`title-application-type-${applicationId}`}>
              {t.applicationsList.type[type]}
            </H3>
            <Gap size="m" />
            <MobileFriendlyListGrid
              labelWidth="max-content"
              rowGap="s"
              columnGap="L"
            >
              <Label>{t.applicationsList.unit}</Label>
              <span data-qa={`application-unit-${applicationId}`}>
                {preferredUnitName}
              </span>

              <Label>{t.applicationsList.period}</Label>
              <span data-qa={`application-period-${applicationId}`}>
                {startDate?.format()}
              </span>

              <Label>{t.applicationsList.created}</Label>
              <span data-qa={`application-created-${applicationId}`}>
                {formatDate(createdDate, DATE_FORMAT_DATE_TIME)}
              </span>

              <Label>{t.applicationsList.modified}</Label>
              <span data-qa={`application-modified-${applicationId}`}>
                {formatDate(modifiedDate, DATE_FORMAT_DATE_TIME)}
              </span>

              <Label>{t.applicationsList.status.title}</Label>
              <span data-qa={`application-status-${applicationId}`}>
                {t.applicationsList.status[applicationStatus]}
              </span>
            </MobileFriendlyListGrid>
            <Gap size="m" />
            <ApplicationLink applicationId={applicationId} />

            {index != applicationSummaries.length - 1 && <LineBreak />}
          </React.Fragment>
        )
      )}
    </ContentArea>
  )
})
