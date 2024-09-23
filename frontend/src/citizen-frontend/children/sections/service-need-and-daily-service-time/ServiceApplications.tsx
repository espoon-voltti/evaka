// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useContext, useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'

import { Lang, useLang, useTranslation } from 'citizen-frontend/localization'
import {
  CitizenServiceApplication,
  ServiceApplication,
  ServiceNeedOptionBasics
} from 'lib-common/generated/api-types/serviceneed'
import { UUID } from 'lib-common/types'
import { StaticChip } from 'lib-components/atoms/Chip'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import AddButton from 'lib-components/atoms/buttons/AddButton'
import { Button } from 'lib-components/atoms/buttons/Button'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import {
  MobileOnly,
  TabletAndDesktop
} from 'lib-components/layout/responsive-layout'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import {
  ModalHeader,
  PlainModal
} from 'lib-components/molecules/modals/BaseModal'
import { H1, H2, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { faLockAlt } from 'lib-icons'
import { faTrash, faFile } from 'lib-icons'
import { faTimes } from 'lib-icons'

import ModalAccessibilityWrapper from '../../../ModalAccessibilityWrapper'
import { AuthContext } from '../../../auth/state'
import {
  CalendarModalBackground,
  CalendarModalCloseButton,
  CalendarModalSection
} from '../../../calendar/CalendarModal'
import { deleteServiceApplicationsMutation } from '../../queries'

export default React.memo(function ServiceApplications({
  childId,
  applications,
  canCreate
}: {
  childId: UUID
  applications: CitizenServiceApplication[]
  canCreate: boolean
}) {
  const i18n = useTranslation()
  const navigate = useNavigate()
  const { user } = useContext(AuthContext)
  const weakAuth = user.map((u) => u?.authLevel === 'WEAK').getOrElse(false)
  const [detailsView, setDetailsView] = useState<ServiceApplication | null>(
    null
  )
  const hasOpenApplication = useMemo(
    () => applications.some((a) => a.data.decision === null),
    [applications]
  )

  return (
    <FixedSpaceColumn>
      {hasOpenApplication && (
        <>
          <InfoBox
            message={i18n.children.serviceApplication.openApplicationInfo}
            noMargin
          />
          <Gap size="s" />
        </>
      )}
      {detailsView && (
        <ServiceApplicationsDetails
          application={detailsView}
          onClose={() => setDetailsView(null)}
        />
      )}
      {applications.length > 0 ? (
        <div>
          <TabletAndDesktop>
            <ServiceApplicationsTable
              applications={applications}
              onOpenDetails={setDetailsView}
            />
          </TabletAndDesktop>
          <MobileOnly>
            <ServiceApplicationsList
              applications={applications}
              onOpenDetails={setDetailsView}
            />
          </MobileOnly>
        </div>
      ) : (
        <div>{i18n.children.serviceApplication.empty}</div>
      )}
      {!hasOpenApplication &&
        (canCreate ? (
          <AddButton
            icon={weakAuth ? faLockAlt : undefined}
            text={i18n.children.serviceApplication.createButton}
            onClick={() => navigate(`/children/${childId}/service-application`)}
          />
        ) : (
          <InfoBox
            message={
              i18n.children.serviceApplication.noSuitablePlacementMessage
            }
            noMargin
          />
        ))}
    </FixedSpaceColumn>
  )
})

const ServiceApplicationsTable = React.memo(function ServiceApplicationsTable({
  applications,
  onOpenDetails
}: {
  applications: CitizenServiceApplication[]
  onOpenDetails: (application: ServiceApplication) => void
}) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const sortedApplications = useMemo(
    () => orderBy(applications, [(a) => a.data.sentAt.timestamp], ['desc']),
    [applications]
  )

  return (
    <Table data-qa="service-applications">
      <Thead>
        <Tr>
          <Th minimalWidth>{i18n.children.serviceApplication.sentAt}</Th>
          <Th>{i18n.children.serviceApplication.startDate}</Th>
          <Th>{i18n.children.serviceApplication.serviceNeed}</Th>
          <Th />
          <Th minimalWidth>{i18n.children.serviceApplication.status}</Th>
        </Tr>
      </Thead>
      <Tbody>
        {sortedApplications.map(({ data: application, permittedActions }) => (
          <Tr key={application.id} data-qa="service-application">
            <Td minimalWidth data-qa="application-sent-date">
              {application.sentAt.toLocalDate().format()}
            </Td>
            <Td minimalWidth data-qa="application-start-date">
              {application.startDate.format()}
            </Td>
            <Td data-qa="application-service-need-description">
              {getServiceNeedName(application.serviceNeedOption, lang)}
            </Td>
            <Td>
              <FixedSpaceRow justifyContent="flex-end">
                <Button
                  text={i18n.children.serviceApplication.additionalInfo}
                  icon={faFile}
                  appearance="inline"
                  onClick={() => onOpenDetails(application)}
                  data-qa="open-details"
                />
                {permittedActions.includes('DELETE') && (
                  <MutateButton
                    text={i18n.children.serviceApplication.cancelApplication}
                    icon={faTrash}
                    appearance="inline"
                    mutation={deleteServiceApplicationsMutation}
                    onClick={() => ({
                      id: application.id,
                      childId: application.childId
                    })}
                    data-qa="cancel-application"
                  />
                )}
              </FixedSpaceRow>
            </Td>
            <Td minimalWidth>
              <StaticChip
                color={
                  application.decision?.status === 'REJECTED'
                    ? colors.accents.a5orangeLight
                    : colors.main.m1
                }
              >
                {
                  i18n.children.serviceApplication.decision.statuses[
                    application.decision === null
                      ? 'undecided'
                      : application.decision.status
                  ]
                }
              </StaticChip>
            </Td>
          </Tr>
        ))}
      </Tbody>
    </Table>
  )
})

const ServiceApplicationsList = React.memo(function ServiceApplicationsList({
  applications,
  onOpenDetails
}: {
  applications: CitizenServiceApplication[]
  onOpenDetails: (application: ServiceApplication) => void
}) {
  const i18n = useTranslation()
  const [lang] = useLang()

  const sortedApplications = useMemo(
    () => orderBy(applications, [(a) => a.data.sentAt.timestamp], ['desc']),
    [applications]
  )

  return (
    <FixedSpaceColumn data-qa="service-applications">
      {sortedApplications.map(({ data: application, permittedActions }) => (
        <FixedSpaceColumn
          key={application.id}
          data-qa="service-application"
          spacing="xs"
        >
          <FixedSpaceRow justifyContent="space-between">
            <Label>
              <span data-qa="application-sent-date">
                {application.startDate.format()}
              </span>{' '}
              -{' '}
            </Label>
            <StaticChip
              color={
                application.decision?.status === 'REJECTED'
                  ? colors.accents.a5orangeLight
                  : colors.main.m1
              }
            >
              {
                i18n.children.serviceApplication.decision.statuses[
                  application.decision === null
                    ? 'undecided'
                    : application.decision.status
                ]
              }
            </StaticChip>
          </FixedSpaceRow>

          <div data-qa="application-service-need-description">
            {getServiceNeedName(application.serviceNeedOption, lang)}
          </div>

          <div data-qa="application-sent-date">
            {i18n.children.serviceApplication.sentAt}{' '}
            {application.sentAt.toLocalDate().format()}
          </div>

          <FixedSpaceRow>
            <Button
              text={i18n.children.serviceApplication.additionalInfo}
              icon={faFile}
              appearance="inline"
              onClick={() => onOpenDetails(application)}
              data-qa="open-details"
            />
            {permittedActions.includes('DELETE') && (
              <MutateButton
                text={i18n.children.serviceApplication.cancelApplication}
                icon={faTrash}
                appearance="inline"
                mutation={deleteServiceApplicationsMutation}
                onClick={() => ({
                  id: application.id,
                  childId: application.childId
                })}
                data-qa="cancel-application"
              />
            )}
          </FixedSpaceRow>

          <HorizontalLine slim />
        </FixedSpaceColumn>
      ))}
    </FixedSpaceColumn>
  )
})

const ServiceApplicationsDetails = React.memo(
  function ServiceApplicationsDetails({
    application,
    onClose
  }: {
    application: ServiceApplication
    onClose: () => void
  }) {
    const i18n = useTranslation()
    const [lang] = useLang()

    return (
      <ModalAccessibilityWrapper>
        <PlainModal
          mobileFullScreen
          margin="auto"
          data-qa="service-application-modal"
        >
          <CalendarModalBackground>
            <CalendarModalSection>
              <Gap size="L" sizeOnMobile="zero" />
              <ModalHeader
                headingComponent={(props) => (
                  <H1 noMargin data-qa="title" {...props}>
                    {props.children}
                  </H1>
                )}
              >
                {i18n.children.serviceApplication.createTitle}
              </ModalHeader>
            </CalendarModalSection>

            <Gap size="zero" sizeOnMobile="s" />

            <CalendarModalSection>
              <H2>{application.childName}</H2>
              <FixedSpaceColumn>
                <FixedSpaceColumn spacing="xxs">
                  <Label>{i18n.children.serviceApplication.sentAt}</Label>
                  <div>{application.sentAt.format()}</div>
                  <div>{application.personName}</div>
                </FixedSpaceColumn>
                <FixedSpaceColumn spacing="xxs">
                  <Label>{i18n.children.serviceApplication.startDate}</Label>
                  <div>{application.startDate.format()}</div>
                </FixedSpaceColumn>
                <FixedSpaceColumn spacing="xxs">
                  <Label>{i18n.children.serviceApplication.serviceNeed}</Label>
                  <div>
                    {getServiceNeedName(application.serviceNeedOption, lang)}
                  </div>
                </FixedSpaceColumn>
                <FixedSpaceColumn spacing="xxs">
                  <Label>
                    {i18n.children.serviceApplication.additionalInfo}
                  </Label>
                  <div>{application.additionalInfo}</div>
                </FixedSpaceColumn>
                <FixedSpaceColumn spacing="xxs">
                  <Label>{i18n.children.serviceApplication.status}</Label>
                  {application.decision === null ? (
                    <div>
                      {
                        i18n.children.serviceApplication.decision.statuses
                          .undecided
                      }
                    </div>
                  ) : (
                    <div>
                      {
                        i18n.children.serviceApplication.decision.statuses[
                          application.decision.status
                        ]
                      }{' '}
                      ({application.decision.decidedByName},{' '}
                      {application.decision.decidedAt.toLocalDate().format()})
                    </div>
                  )}
                </FixedSpaceColumn>
                {!!application.decision?.rejectedReason && (
                  <FixedSpaceColumn spacing="xxs">
                    <Label>
                      {i18n.children.serviceApplication.decision.rejectedReason}
                    </Label>
                    <div>{application.decision.rejectedReason}</div>
                  </FixedSpaceColumn>
                )}
                <FixedSpaceRow justifyContent="space-evenly">
                  <Button text={i18n.common.close} onClick={onClose} />
                </FixedSpaceRow>
              </FixedSpaceColumn>
              <Gap sizeOnMobile="zero" />
            </CalendarModalSection>
          </CalendarModalBackground>
          <CalendarModalCloseButton
            onClick={onClose}
            aria-label={i18n.common.closeModal}
            icon={faTimes}
          />
        </PlainModal>
      </ModalAccessibilityWrapper>
    )
  }
)

const getServiceNeedName = (
  option: ServiceNeedOptionBasics,
  lang: Lang
): string => {
  switch (lang) {
    case 'fi':
      return option.nameFi
    case 'sv':
      return option.nameSv
    case 'en':
      return option.nameEn
  }
}
