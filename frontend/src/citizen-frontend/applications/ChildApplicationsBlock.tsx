// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import noop from 'lodash/noop'
import React, { useContext } from 'react'
import { Link, useNavigate } from 'react-router'
import styled from 'styled-components'

import type {
  ApplicationsOfChild,
  ApplicationStatus
} from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { useMutation } from 'lib-common/query'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import { ContentArea } from 'lib-components/layout/Container'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceFlexWrap } from 'lib-components/layout/flex-helpers'
import { H2, H3, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import {
  faArrowRight,
  faCheck,
  faEnvelope,
  faExclamation,
  faFile,
  faFileAlt,
  faGavel,
  faPen,
  faPlay,
  faTimes,
  faTrash
} from 'lib-icons'

import { Status } from '../decisions/shared'
import { useTranslation } from '../localization'
import { OverlayContext } from '../overlay/state'

import { removeUnprocessableApplicationMutation } from './queries'

const StyledLink = styled(Link)`
  color: ${colors.main.m2};
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

const StatusContainer = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
`

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

interface ChildApplicationsBlockProps {
  data: ApplicationsOfChild
}

const ChildHeading = styled(H2)`
  margin: 0;
  margin-bottom: ${defaultMargins.s};
`

const applicationStatusIcon = {
  PROCESSING: {
    icon: faPlay,
    color: colors.main.m1
  },
  PENDING: {
    icon: faGavel,
    color: colors.status.warning
  },
  ACCEPTED: {
    icon: faCheck,
    color: colors.status.success
  },
  REJECTED: {
    icon: faTimes,
    color: colors.status.danger
  },
  CREATED: {
    icon: faFile,
    color: colors.grayscale.g70
  },
  SENT: {
    icon: faEnvelope,
    color: colors.main.m1
  }
}

const applicationStatusToIcon = (
  applicationStatus: ApplicationStatus,
  decidable: boolean
): keyof typeof applicationStatusIcon => {
  switch (applicationStatus) {
    case 'ACTIVE':
      return decidable ? 'PENDING' : 'ACCEPTED'
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

const ApplicationStatusIcon = React.memo(function ApplicationStatusIcon({
  status,
  decidable
}: {
  status: ApplicationStatus
  decidable: boolean
}) {
  const icon = applicationStatusToIcon(status, decidable)
  return (
    <RoundIcon
      content={applicationStatusIcon[icon].icon}
      color={applicationStatusIcon[icon].color}
      size="m"
    />
  )
})

export default React.memo(function ChildApplicationsBlock({
  data: {
    childId,
    childName,
    applicationSummaries,
    permittedActions,
    decidableApplications
  }
}: ChildApplicationsBlockProps) {
  const navigate = useNavigate()
  const t = useTranslation()
  const { setErrorMessage, setInfoMessage, clearInfoMessage } =
    useContext(OverlayContext)

  const { mutateAsync: removeUnprocessedApplication } = useMutation(
    removeUnprocessableApplicationMutation
  )

  const onDeleteApplication = (
    applicationId: ApplicationId,
    applicationStatus: ApplicationStatus
  ) => {
    setInfoMessage({
      title:
        applicationStatus === 'CREATED'
          ? t.applications.deleteDraftTitle
          : t.applications.deleteSentTitle,
      text:
        applicationStatus === 'CREATED'
          ? t.applications.deleteDraftText
          : t.applications.deleteSentText,
      type: applicationStatus === 'CREATED' ? 'warning' : 'danger',
      icon: applicationStatus === 'CREATED' ? faExclamation : faTimes,
      resolve: {
        action: () => {
          removeUnprocessedApplication({ applicationId })
            .catch(() => {
              setErrorMessage({
                title: t.applications.deleteUnprocessedApplicationError,
                type: 'error',
                resolveLabel: t.common.ok
              })
            })
            .finally(() => {
              clearInfoMessage()
            })
        },
        label:
          applicationStatus === 'CREATED'
            ? t.applications.deleteDraftOk
            : t.applications.deleteSentOk
      },
      reject: {
        action: () => clearInfoMessage(),
        label:
          applicationStatus === 'CREATED'
            ? t.applications.deleteDraftCancel
            : t.applications.deleteSentCancel
      },
      'data-qa': 'info-message-draft-saved'
    })
  }

  const hasName = childName?.trim().length > 0

  return (
    <ContentArea opaque paddingVertical="L" data-qa={`child-${childId}`}>
      <TitleContainer>
        <ChildHeading
          translate={hasName ? 'no' : undefined}
          data-qa={`title-applications-child-name-${childId}`}
        >
          {hasName ? childName : t.applicationsList.namelessChild}
        </ChildHeading>
        <AddButton
          text={t.applicationsList.newApplicationLink}
          onClick={() => navigate(`/applications/new/${childId}`)}
          data-qa={`new-application-${childId}`}
        />
      </TitleContainer>

      {applicationSummaries.length > 0 &&
        applicationSummaries.map(
          (
            {
              applicationId,
              type,
              startDate,
              createdDate,
              modifiedDate,
              applicationStatus,
              transferApplication
            },
            index
          ) => (
            <React.Fragment key={applicationId}>
              <Gap size="L" />
              <H3 noMargin data-qa={`title-application-type-${applicationId}`}>
                {t.applicationsList.type[type]}
                {transferApplication &&
                  ` (${t.applicationsList.transferApplication})`}
              </H3>
              <Gap size="m" />
              <ListGrid labelWidth="max-content" rowGap="s" columnGap="L">
                {startDate !== null && (
                  <>
                    <Label>{t.applicationsList.period}</Label>
                    <span data-qa={`application-period-${applicationId}`}>
                      {startDate.format()}
                    </span>
                  </>
                )}

                <Label>{t.applicationsList.created}</Label>
                <span data-qa={`application-created-${applicationId}`}>
                  {createdDate.toLocalDate().format()}
                </span>

                {!modifiedDate.isEqual(createdDate) && (
                  <>
                    <Label>{t.applicationsList.modified}</Label>
                    <span data-qa={`application-modified-${applicationId}`}>
                      {modifiedDate.toLocalDate().format()}
                    </span>
                  </>
                )}

                <Label>{t.applicationsList.status.title}</Label>
                <StatusContainer>
                  <div>
                    <ApplicationStatusIcon
                      status={applicationStatus}
                      decidable={decidableApplications.includes(applicationId)}
                    />
                    <Gap size="xs" horizontal={true} />
                    <Status data-qa={`application-status-${applicationId}`}>
                      {
                        t.applicationsList.status[
                          applicationStatus === 'ACTIVE' &&
                          decidableApplications.includes(applicationId)
                            ? 'WAITING_CONFIRMATION'
                            : applicationStatus
                        ]
                      }
                    </Status>
                  </div>

                  {(applicationStatus === 'WAITING_CONFIRMATION' ||
                    applicationStatus === 'ACTIVE') &&
                    permittedActions[applicationId]?.includes(
                      'READ_DECISIONS'
                    ) &&
                    decidableApplications.includes(applicationId) && (
                      <ConfirmationContainer>
                        <div color={colors.main.m2}>
                          {t.applicationsList.confirmationLinkInstructions}
                        </div>
                        <StyledLink
                          to={`/decisions/by-application/${applicationId}`}
                        >
                          {t.applicationsList.confirmationLink}{' '}
                          <Icon icon={faArrowRight} color={colors.main.m2} />
                        </StyledLink>
                      </ConfirmationContainer>
                    )}
                </StatusContainer>

                <Gap size="xs" horizontal />
              </ListGrid>
              <Gap size="s" />
              <FixedSpaceFlexWrap>
                {applicationStatus === 'CREATED' ||
                (applicationStatus === 'SENT' &&
                  permittedActions[applicationId]?.includes('UPDATE')) ? (
                  <Link to={`/applications/${applicationId}/edit`}>
                    <Button
                      appearance="inline"
                      icon={faPen}
                      text={t.applicationsList.editApplicationLink}
                      onClick={noop}
                      data-qa={`button-open-application-${applicationId}`}
                    />
                  </Link>
                ) : permittedActions[applicationId]?.includes('READ') ? (
                  <Link to={`/applications/${applicationId}`}>
                    <Button
                      appearance="inline"
                      icon={faFileAlt}
                      text={t.applicationsList.openApplicationLink}
                      onClick={noop}
                      data-qa={`button-open-application-${applicationId}`}
                    />
                  </Link>
                ) : undefined}
                {(applicationStatus === 'CREATED' ||
                  applicationStatus === 'SENT') &&
                  permittedActions[applicationId]?.includes('DELETE') && (
                    <Button
                      appearance="inline"
                      icon={faTrash}
                      text={
                        applicationStatus === 'CREATED'
                          ? t.applicationsList.removeApplicationBtn
                          : t.applicationsList.cancelApplicationBtn
                      }
                      onClick={() =>
                        onDeleteApplication(applicationId, applicationStatus)
                      }
                      data-qa={`button-remove-application-${applicationId}`}
                    />
                  )}
              </FixedSpaceFlexWrap>

              {index !== applicationSummaries.length - 1 && <LineBreak />}
            </React.Fragment>
          )
        )}
    </ContentArea>
  )
})
