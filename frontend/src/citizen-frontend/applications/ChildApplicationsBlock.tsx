// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'
import { useTranslation } from '../localization'
import { H2, H3, Label } from 'lib-components/typography'
import { ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import styled from 'styled-components'
import ListGrid from 'lib-components/layout/ListGrid'
import { Status, applicationStatusIcon } from '../decisions/shared'
import RoundIcon from 'lib-components/atoms/RoundIcon'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import {
  faArrowRight,
  faExclamation,
  faFileAlt,
  faPen,
  faTimes,
  faTrash
} from 'lib-icons'
import colors from 'lib-customizations/common'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Link, useHistory } from 'react-router-dom'
import { CitizenApplicationSummary } from 'lib-common/api-types/application/ApplicationsOfChild'
import { ApplicationStatus } from 'lib-common/api-types/application/enums'
import { FixedSpaceFlexWrap } from 'lib-components/layout/flex-helpers'
import InlineButton from 'lib-components/atoms/buttons/InlineButton'
import { noop } from 'lodash'
import { removeUnprocessedApplication } from '../applications/api'
import { OverlayContext } from '../overlay/state'
import { isEqual } from 'date-fns'
import { formatDate } from 'lib-common/date'

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

const StatusContainer = styled.div`
  display: flex;
  align-items: center;
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

const NoApplications = styled.p`
  color: ${colors.greyscale.dark};
`

interface ChildApplicationsBlockProps {
  childId: string
  childName: string
  applicationSummaries: CitizenApplicationSummary[]
  reload: () => void
}

export default React.memo(function ChildApplicationsBlock({
  childId,
  childName,
  applicationSummaries,
  reload
}: ChildApplicationsBlockProps) {
  const history = useHistory()
  const t = useTranslation()
  const { setErrorMessage, setInfoMessage, clearInfoMessage } =
    useContext(OverlayContext)

  const onDeleteApplication = (
    applicationId: string,
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
      iconColour: applicationStatus === 'CREATED' ? 'orange' : 'red',
      icon: applicationStatus === 'CREATED' ? faExclamation : faTimes,
      resolve: {
        action: () => {
          void removeUnprocessedApplication(applicationId).then((res) => {
            if (res.isFailure) {
              setErrorMessage({
                title: t.applications.deleteUnprocessedApplicationError,
                type: 'error',
                resolveLabel: t.common.ok
              })
            }

            clearInfoMessage()
            reload()
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
          onClick={() => history.push(`/applications/new/${childId}`)}
          data-qa={`new-application-${childId}`}
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
                {preferredUnitName !== null && (
                  <>
                    <Label>{t.applicationsList.unit}</Label>
                    <span data-qa={`application-unit-${applicationId}`}>
                      {preferredUnitName}
                    </span>
                  </>
                )}

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
                  {formatDate(createdDate)}
                </span>

                {!isEqual(modifiedDate, createdDate) && (
                  <>
                    <Label>{t.applicationsList.modified}</Label>
                    <span data-qa={`application-modified-${applicationId}`}>
                      {formatDate(modifiedDate)}
                    </span>
                  </>
                )}

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
                    size="m"
                  />

                  <Gap size={'xs'} horizontal={true} />

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
              <FixedSpaceFlexWrap>
                {applicationStatus === 'CREATED' ||
                applicationStatus === 'SENT' ? (
                  <Link to={`/applications/${applicationId}/edit`}>
                    <InlineButton
                      icon={faPen}
                      text={t.applicationsList.editApplicationLink}
                      onClick={noop}
                      data-qa={`button-open-application-${applicationId}`}
                    />
                  </Link>
                ) : (
                  <Link to={`/applications/${applicationId}`}>
                    <InlineButton
                      icon={faFileAlt}
                      text={t.applicationsList.openApplicationLink}
                      onClick={noop}
                      data-qa={`button-open-application-${applicationId}`}
                    />
                  </Link>
                )}
                {(applicationStatus === 'CREATED' ||
                  applicationStatus === 'SENT') && (
                  <InlineButton
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
