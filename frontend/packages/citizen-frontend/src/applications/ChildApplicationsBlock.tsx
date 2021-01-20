// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { ApplicationStatus, GuardianApplications } from '~applications/types'
import { useTranslation } from '~localization'
import { H2, H3, Label } from '@evaka/lib-components/src/typography'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import { Gap } from '@evaka/lib-components/src/white-space'
import styled from 'styled-components'
import ListGrid from '@evaka/lib-components/src/layout/ListGrid'
import { formatDate } from '~util'
import { ApplicationLink } from '~decisions/ApplicationLink'
import { Status, applicationStatusIcon } from '~decisions/shared'
import RoundIcon from '@evaka/lib-components/src/atoms/RoundIcon'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faArrowRight } from '@evaka/lib-icons'
import colors from '@evaka/lib-components/src/colors'
import AddButton from '@evaka/lib-components/src/atoms/buttons/AddButton'
import { Link } from 'react-router-dom'

const StyledLink = styled(Link)`
  color: ${colors.blues.primary};
  text-decoration: none;
`

const LineBreak = styled.div`
  width: 100%;
  border: 1px solid #d8d8d8;
  margin: 40px 0 20px 0;
`

const TitleContainer = styled.div`
  display: flex;
  justify-content: space-between;
  flex-flow: row wrap;
`

const StatusContainer = styled.div``

const ConfirmationContainer = styled.div`
  display: flex;
  flex-direction: column;

  & div {
    margin-top: 7px;
  }
`

const Icon = styled(FontAwesomeIcon)`
  height: 1rem !important;
  width: 1rem !important;
  margin-right: 10px;
`

const NoApplications = styled.p`
  color: ${colors.greyscale.dark};
`

export default React.memo(function ChildApplicationsBlock({
  childId,
  childName,
  applicationSummaries
}: GuardianApplications) {
  const t = useTranslation()

  const applicationStatusToIcon = (
    applicationStatus: ApplicationStatus
  ): string => {
    switch (applicationStatus) {
      case 'ACTIVE':
        return 'ACCEPTED'
      case 'WAITING_PLACEMENT':
      case 'WAITING_DECISION':
      case 'WAITING_UNIT_CONFIRMATION':
      case 'WAITING_MAILING':
        return 'PROCESSING'
      case 'WAITING_CONFIRMATION':
        return 'PENDING'
      case 'CANCELLED':
        return 'REJECTED'
      default:
        return applicationStatus
    }
  }

  return (
    <ContentArea opaque paddingVertical="L" data-qa={`child-${childId}`}>
      <TitleContainer>
        <H2 noMargin data-qa={`title-applications-child-name-${childId}`}>
          {childName}
        </H2>
        <AddButton
          text={t.applicationsList.newApplicationLink}
          onClick={() =>
            (window.location.href = `/citizen/applications/${childId}/create`)
          }
        />
      </TitleContainer>

      {applicationSummaries.length ? (
        applicationSummaries.map(
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
              <ListGrid labelWidth="max-content" rowGap="s" columnGap="L">
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
                  {formatDate(createdDate)}
                </span>

                <Label>{t.applicationsList.modified}</Label>
                <span data-qa={`application-modified-${applicationId}`}>
                  {formatDate(modifiedDate)}
                </span>

                <Label>{t.applicationsList.status.title}</Label>
                <StatusContainer
                  data-qa={`application-status-${applicationId}`}
                >
                  <RoundIcon
                    content={
                      applicationStatusIcon[
                        applicationStatusToIcon(applicationStatus)
                      ].icon
                    }
                    color={
                      applicationStatusIcon[
                        applicationStatusToIcon(applicationStatus)
                      ].color
                    }
                    size="s"
                  />
                  <Status data-qa={`application-status-${applicationId}`}>
                    {t.applicationsList.status[applicationStatus]}
                  </Status>

                  {applicationStatus === 'WAITING_CONFIRMATION' && (
                    <ConfirmationContainer>
                      <div color={colors.blues.primary}>
                        {t.applicationsList.confirmationLinkInstructions}
                      </div>
                      <StyledLink
                        to={`/decisions/by-application/${applicationId}`}
                      >
                        {t.applicationsList.confirmationLink}{' '}
                        <Icon
                          icon={faArrowRight}
                          color={colors.blues.primary}
                        />
                      </StyledLink>
                    </ConfirmationContainer>
                  )}
                </StatusContainer>

                <Gap size="xs" horizontal />
              </ListGrid>
              <Gap size="s" />
              <ApplicationLink applicationId={applicationId} />

              {index != applicationSummaries.length - 1 && <LineBreak />}
            </React.Fragment>
          )
        )
      ) : (
        <NoApplications>{t.applicationsList.noApplications}</NoApplications>
      )}
    </ContentArea>
  )
})
