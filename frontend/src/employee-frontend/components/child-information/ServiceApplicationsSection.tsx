// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import styled from 'styled-components'

import FiniteDateRange from 'lib-common/finite-date-range'
import { boolean } from 'lib-common/form/fields'
import { object, oneOf, required, validated, value } from 'lib-common/form/form'
import { useBoolean, useForm, useFormFields } from 'lib-common/form/hooks'
import { nonBlank } from 'lib-common/form/validators'
import {
  EmployeeServiceApplication,
  ServiceApplication,
  shiftCareType,
  ShiftCareType
} from 'lib-common/generated/api-types/serviceneed'
import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import { Button } from 'lib-components/atoms/buttons/Button'
import Checkbox, { CheckboxF } from 'lib-components/atoms/form/Checkbox'
import { InputFieldF } from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import InfoModal from 'lib-components/molecules/modals/InfoModal'
import { H2, H3, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'
import { faFile } from 'lib-icons'

import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import {
  acceptServiceApplicationsMutation,
  childServiceApplicationsQuery,
  rejectServiceApplicationsMutation
} from './queries'

const DetailsModal = React.memo(function DetailsModal({
  application,
  onClose
}: {
  application: ServiceApplication
  onClose: () => void
}) {
  const { i18n } = useTranslation()

  return (
    <InfoModal
      title={i18n.childInformation.serviceApplications.applicationTitle}
      close={onClose}
      closeLabel={i18n.common.close}
    >
      <FixedSpaceColumn spacing="L">
        <FixedSpaceColumn>
          <H2 noMargin>{application.childName}</H2>
          <FixedSpaceColumn spacing="xxs">
            <Label>{i18n.childInformation.serviceApplications.sentAt}</Label>
            <div>{application.sentAt.format()}</div>
            <div>{application.personName}</div>
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xxs">
            <Label>{i18n.childInformation.serviceApplications.startDate}</Label>
            <div>{application.startDate.format()}</div>
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xxs">
            <Label>
              {i18n.childInformation.serviceApplications.serviceNeed}
            </Label>
            <div>{application.serviceNeedOption.nameFi}</div>
          </FixedSpaceColumn>
          <FixedSpaceColumn spacing="xxs">
            <Label>
              {i18n.childInformation.serviceApplications.additionalInfo}
            </Label>
            <div data-qa="additional-info">{application.additionalInfo}</div>
          </FixedSpaceColumn>
          {application.decision && (
            <>
              <FixedSpaceColumn spacing="xxs">
                <Label>
                  {i18n.childInformation.serviceApplications.status}
                </Label>
                <div data-qa="decision-status">
                  {
                    i18n.childInformation.serviceApplications.decision.statuses[
                      application.decision.status
                    ]
                  }{' '}
                  ({application.decision.decidedByName},{' '}
                  {application.decision.decidedAt.toLocalDate().format()})
                </div>
              </FixedSpaceColumn>
              {!!application.decision.rejectedReason && (
                <FixedSpaceColumn spacing="xxs">
                  <Label>
                    {
                      i18n.childInformation.serviceApplications.decision
                        .rejectedReason
                    }
                  </Label>
                  <div data-qa="rejected-reason">
                    {application.decision.rejectedReason}
                  </div>
                </FixedSpaceColumn>
              )}
            </>
          )}
        </FixedSpaceColumn>
      </FixedSpaceColumn>
    </InfoModal>
  )
})

const acceptanceForm = object({
  shiftCare: required(oneOf<ShiftCareType>()),
  partWeek: required(boolean())
})

const AcceptanceModal = React.memo(function AcceptanceModal({
  application,
  onClose
}: {
  application: ServiceApplication
  onClose: () => void
}) {
  const { i18n } = useTranslation()

  const form = useForm(
    acceptanceForm,
    () => ({
      shiftCare: {
        options: shiftCareType.map((type) => ({
          label:
            i18n.childInformation.placements.serviceNeeds.shiftCareTypes[type],
          value: type,
          domValue: type
        })),
        domValue: 'NONE'
      },
      partWeek: application.serviceNeedOption.partWeek ?? false
    }),
    i18n.validationErrors
  )

  const { shiftCare, partWeek } = useFormFields(form)
  const partWeekEditable = application.serviceNeedOption.partWeek === null

  const currentPlacementEnd = application.currentPlacement?.endDate
  if (!currentPlacementEnd) return null // cannot accept
  const optionValidityEnd = application.serviceNeedOption.validity.end
  const endDate =
    optionValidityEnd === null ||
    optionValidityEnd.isEqualOrAfter(currentPlacementEnd)
      ? currentPlacementEnd
      : optionValidityEnd
  const range = new FiniteDateRange(application.startDate, endDate)

  return (
    <MutateFormModal
      title={
        i18n.childInformation.serviceApplications.decision.confirmAcceptTitle
      }
      resolveLabel={i18n.childInformation.serviceApplications.decision.accept}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!form.isValid()}
      resolveMutation={acceptServiceApplicationsMutation}
      resolveAction={() => ({
        id: application.id,
        childId: application.childId,
        body: {
          shiftCareType: shiftCare.value(),
          partWeek: partWeek.value()
        }
      })}
      rejectAction={onClose}
      onSuccess={onClose}
    >
      <FixedSpaceColumn spacing="L">
        <div>
          <InfoBox
            noMargin
            message={i18n.childInformation.serviceApplications.decision.confirmAcceptText(
              range
            )}
          />
        </div>
        <FixedSpaceColumn spacing="s">
          <Label>
            {i18n.childInformation.serviceApplications.decision.shiftCareLabel}
          </Label>
          {featureFlags.intermittentShiftCare ? (
            <FixedSpaceColumn spacing="xs">
              {shiftCareType.map((type) => (
                <Radio
                  key={type}
                  data-qa={`shift-care-type-radio-${type}`}
                  label={
                    i18n.childInformation.placements.serviceNeeds
                      .shiftCareTypes[type]
                  }
                  checked={shiftCare.value() === type}
                  onChange={() =>
                    shiftCare.update((s) => ({
                      ...s,
                      domValue: type
                    }))
                  }
                />
              ))}
            </FixedSpaceColumn>
          ) : (
            <Checkbox
              label={
                i18n.childInformation.serviceApplications.decision
                  .shiftCareCheckbox
              }
              data-qa="shift-care-toggle"
              checked={shiftCare.value() === 'FULL'}
              onChange={(checked) =>
                shiftCare.update((s) => ({
                  ...s,
                  domValue: checked ? 'FULL' : 'NONE'
                }))
              }
            />
          )}
        </FixedSpaceColumn>
        <FixedSpaceColumn spacing="s">
          <Label>
            {i18n.childInformation.serviceApplications.decision.partWeekLabel}
          </Label>
          {partWeekEditable ? (
            <CheckboxF
              label={
                i18n.childInformation.serviceApplications.decision
                  .partWeekCheckbox
              }
              bind={partWeek}
              data-qa="part-week-checkbox"
            />
          ) : (
            <div>{partWeek.value() ? i18n.common.yes : i18n.common.no}</div>
          )}
        </FixedSpaceColumn>
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})

const rejectionForm = object({
  reason: validated(required(value<string>()), nonBlank)
})

const RejectionModal = React.memo(function RejectionModal({
  application,
  onClose
}: {
  application: ServiceApplication
  onClose: () => void
}) {
  const { i18n } = useTranslation()

  const form = useForm(
    rejectionForm,
    () => ({
      reason: ''
    }),
    i18n.validationErrors
  )
  const { reason } = useFormFields(form)

  return (
    <MutateFormModal
      title={
        i18n.childInformation.serviceApplications.decision.confirmRejectTitle
      }
      resolveLabel={i18n.childInformation.serviceApplications.decision.reject}
      rejectLabel={i18n.common.cancel}
      resolveDisabled={!form.isValid()}
      resolveMutation={rejectServiceApplicationsMutation}
      resolveAction={() => ({
        id: application.id,
        childId: application.childId,
        body: { reason: reason.value().trim() }
      })}
      rejectAction={onClose}
      onSuccess={onClose}
    >
      <FixedSpaceColumn spacing="s">
        <Label>
          {i18n.childInformation.serviceApplications.decision.rejectedReason} *
        </Label>
        <InputFieldF
          bind={reason}
          hideErrorsBeforeTouched
          data-qa="reject-reason"
        />
      </FixedSpaceColumn>
    </MutateFormModal>
  )
})

const UndecidedServiceApplicationContainer = styled.div`
  border: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  padding: ${defaultMargins.L};
`

const UndecidedServiceApplication = React.memo(
  function UndecidedServiceApplication({
    application: { data: application, permittedActions }
  }: {
    application: EmployeeServiceApplication
  }) {
    const { i18n } = useTranslation()
    const [
      rejectionModalOpen,
      { on: openRejectionModal, off: closeRejectionModal }
    ] = useBoolean(false)
    const [
      acceptanceModalOpen,
      { on: openAcceptanceModal, off: closeAcceptanceModal }
    ] = useBoolean(false)

    return (
      <UndecidedServiceApplicationContainer data-qa="undecided-application">
        {rejectionModalOpen && (
          <RejectionModal
            application={application}
            onClose={closeRejectionModal}
          />
        )}
        {acceptanceModalOpen && (
          <AcceptanceModal
            application={application}
            onClose={closeAcceptanceModal}
          />
        )}
        <FixedSpaceColumn spacing="L">
          <FixedSpaceColumn spacing="xs">
            <H3 noMargin>
              {i18n.childInformation.serviceApplications.applicationTitle},{' '}
            </H3>
            <H3 noMargin>{application.sentAt.toLocalDate().format()}</H3>
          </FixedSpaceColumn>
          <FixedSpaceRow spacing="XL">
            <FixedSpaceColumn spacing="xs">
              <Label>
                {i18n.childInformation.serviceApplications.startDate}
              </Label>
              <div data-qa="start-date">{application.startDate.format()}</div>
            </FixedSpaceColumn>
            <FixedSpaceColumn spacing="xs">
              <Label>
                {i18n.childInformation.serviceApplications.serviceNeed}
              </Label>
              <div data-qa="service-need">
                {application.serviceNeedOption.nameFi}
              </div>
            </FixedSpaceColumn>
            <FixedSpaceColumn spacing="xs">
              <Label>{i18n.childInformation.serviceApplications.sentBy}</Label>
              <div>{application.personName}</div>
            </FixedSpaceColumn>
          </FixedSpaceRow>
          <FixedSpaceColumn spacing="xs">
            <Label>
              {i18n.childInformation.serviceApplications.additionalInfo}
            </Label>
            <div data-qa="additional-info">
              {application.additionalInfo.trim() || '-'}
            </div>
          </FixedSpaceColumn>
          <FixedSpaceRow justifyContent="flex-end">
            {permittedActions.includes('REJECT') && (
              <Button
                text={i18n.childInformation.serviceApplications.decision.reject}
                onClick={openRejectionModal}
                data-qa="reject-application-button"
              />
            )}
            {permittedActions.includes('ACCEPT') && (
              <Button
                primary
                text={i18n.childInformation.serviceApplications.decision.accept}
                onClick={openAcceptanceModal}
                data-qa="accept-application-button"
              />
            )}
          </FixedSpaceRow>
        </FixedSpaceColumn>
      </UndecidedServiceApplicationContainer>
    )
  }
)

const ServiceApplications = React.memo(function ServiceApplications({
  childId
}: {
  childId: UUID
}) {
  const { i18n } = useTranslation()
  const applications = useQueryResult(
    childServiceApplicationsQuery({ childId })
  )
  const [detailsModal, setDetailsModal] = useState<ServiceApplication | null>(
    null
  )

  return renderResult(applications, (applications) => {
    const undecidedApplication = applications.find(
      (a) => a.data.decision === null
    )
    const decidedApplications = applications.filter(
      (a) => a.data.decision !== null
    )
    return (
      <div>
        {detailsModal && (
          <DetailsModal
            application={detailsModal}
            onClose={() => setDetailsModal(null)}
          />
        )}
        {undecidedApplication && (
          <>
            <UndecidedServiceApplication application={undecidedApplication} />
            <Gap size="L" />
          </>
        )}
        <H3 noMargin>
          {i18n.childInformation.serviceApplications.decidedApplications}
        </H3>
        <Gap size="s" />
        {decidedApplications.length === 0 ? (
          <div>{i18n.childInformation.serviceApplications.noApplications}</div>
        ) : (
          <Table>
            <Thead>
              <Tr>
                <Th>{i18n.childInformation.serviceApplications.sentAt}</Th>
                <Th>{i18n.childInformation.serviceApplications.startDate}</Th>
                <Th>{i18n.childInformation.serviceApplications.serviceNeed}</Th>
                <Th>{i18n.childInformation.serviceApplications.sentBy}</Th>
                <Th>
                  {i18n.childInformation.serviceApplications.additionalInfo}
                </Th>
                <Th>{i18n.childInformation.serviceApplications.status}</Th>
              </Tr>
            </Thead>
            <Tbody>
              {decidedApplications.map(({ data: application }) => (
                <Tr key={application.id} data-qa="decided-application-row">
                  <Td>{application.sentAt.toLocalDate().format()}</Td>
                  <Td data-qa="start-date">{application.startDate.format()}</Td>
                  <Td data-qa="service-need">
                    {application.serviceNeedOption.nameFi}
                  </Td>
                  <Td>{application.personName}</Td>
                  <Td>
                    <Button
                      appearance="inline"
                      icon={faFile}
                      text={
                        i18n.childInformation.serviceApplications.additionalInfo
                      }
                      onClick={() => setDetailsModal(application)}
                    />
                  </Td>
                  <Td data-qa="decision-status">
                    {
                      i18n.childInformation.serviceApplications.decision
                        .statuses[application.decision!.status]
                    }
                    , {application.decision!.decidedAt.toLocalDate().format()}
                  </Td>
                </Tr>
              ))}
            </Tbody>
          </Table>
        )}
      </div>
    )
  })
})

export default React.memo(function ServiceApplicationsSection({
  childId,
  startOpen
}: {
  childId: UUID
  startOpen: boolean
}) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext(ChildContext)

  const [open, setOpen] = useState(startOpen)

  if (
    !featureFlags.serviceApplications ||
    !permittedActions.has('READ_SERVICE_APPLICATIONS')
  ) {
    return null
  }

  return (
    <div>
      <CollapsibleContentArea
        title={
          <H2 noMargin>{i18n.childInformation.serviceApplications.title}</H2>
        }
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="service-applications-collapsible"
      >
        <ServiceApplications childId={childId} />
      </CollapsibleContentArea>
    </div>
  )
})
