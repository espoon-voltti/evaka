// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useContext, useMemo, useState } from 'react'
import styled from 'styled-components'

import {
  PlacementPlanConfirmationStatus,
  PlacementPlanDetails,
  PlacementPlanRejectReason
} from 'lib-common/generated/api-types/placement'
import { ApplicationId, DaycareId } from 'lib-common/generated/api-types/shared'
import { useMutationResult } from 'lib-common/query'
import {
  MutateButton,
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Bold, Italic, Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import colors from 'lib-customizations/common'
import { placementPlanRejectReasons } from 'lib-customizations/employee'

import PlacementProposalRow from '../../../components/unit/tab-placement-proposals/PlacementProposalRow'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import {
  acceptPlacementProposalMutation,
  respondToPlacementProposalMutation
} from '../queries'

interface Summary {
  confirmationStates: Record<ApplicationId, PlacementPlanConfirmationStatus>
  accepted: number
  rejected: number
}

type Props = {
  unitId: DaycareId
  placementPlans: PlacementPlanDetails[]
}

export default React.memo(function PlacementProposals({
  unitId,
  placementPlans
}: Props) {
  const { i18n } = useTranslation()
  const { setErrorMessage } = useContext(UIContext)

  const [modalOpen, setModalOpen] = useState(false)
  const [reason, setReason] = useState<PlacementPlanRejectReason | null>(null)
  const [otherReason, setOtherReason] = useState<string>('')
  const [currentApplicationId, setCurrentApplicationId] =
    useState<ApplicationId | null>(null)

  const summary = useMemo(
    () =>
      placementPlans.reduce<Summary>(
        (map, plan) => ({
          confirmationStates: {
            ...map.confirmationStates,
            [plan.applicationId]: plan.unitConfirmationStatus
          },
          accepted:
            map.accepted + (plan.unitConfirmationStatus === 'ACCEPTED' ? 1 : 0),
          rejected:
            map.rejected +
            (plan.unitConfirmationStatus === 'REJECTED_NOT_CONFIRMED' ? 1 : 0)
        }),
        { confirmationStates: {}, accepted: 0, rejected: 0 }
      ),
    [placementPlans]
  )

  const {
    mutateAsync: respondToPlacementProposal,
    isPending: respondToPlacementProposalIsPending
  } = useMutationResult(respondToPlacementProposalMutation)

  const sendConfirmation = async (
    applicationId: ApplicationId,
    status: PlacementPlanConfirmationStatus
  ) => {
    await respondToPlacementProposal({
      unitId,
      applicationId,
      body: {
        status,
        reason: null,
        otherReason: null
      }
    })
  }

  const onAcceptFailure = useCallback(() => {
    setErrorMessage({
      type: 'error',
      title: i18n.common.error.unknown,
      resolveLabel: i18n.common.ok
    })
  }, [i18n, setErrorMessage])

  const acceptDisabled = useMemo(
    () =>
      respondToPlacementProposalIsPending ||
      (summary.accepted === 0 && summary.rejected === 0),
    [summary, respondToPlacementProposalIsPending]
  )

  const sortedRows = sortBy(placementPlans, [
    (p: PlacementPlanDetails) => p.child.lastName,
    (p: PlacementPlanDetails) => p.child.firstName,
    (p: PlacementPlanDetails) => p.period.start
  ])

  const closeModal = useCallback(() => {
    setModalOpen(false)
  }, [])

  return (
    <>
      {modalOpen && (
        <MutateFormModal
          title={i18n.unit.placementProposals.rejectTitle}
          resolveMutation={respondToPlacementProposalMutation}
          resolveAction={() => {
            if (reason == null || currentApplicationId == null) {
              return cancelMutation
            }
            return {
              unitId,
              applicationId: currentApplicationId,
              body: {
                status: 'REJECTED_NOT_CONFIRMED' as const,
                reason,
                otherReason
              }
            }
          }}
          onSuccess={() => {
            if (currentApplicationId === null) return
            closeModal()
          }}
          onFailure={() => {
            if (currentApplicationId === null) return
          }}
          resolveLabel={i18n.common.save}
          resolveDisabled={!reason || (reason === 'OTHER' && !otherReason)}
          rejectAction={closeModal}
          rejectLabel={i18n.common.cancel}
        >
          <FixedSpaceColumn>
            {placementPlanRejectReasons.map((option) => (
              <Radio
                key={option}
                data-qa="proposal-reject-reason"
                checked={reason === option}
                onChange={() => setReason(option)}
                label={i18n.unit.placementProposals.rejectReasons[option]}
              />
            ))}
            {reason === 'OTHER' && (
              <InputField
                data-qa="proposal-reject-reason-input"
                value={otherReason}
                onChange={setOtherReason}
                placeholder={i18n.unit.placementProposals.describeOtherReason}
              />
            )}
          </FixedSpaceColumn>
          <Gap />
        </MutateFormModal>
      )}

      {placementPlans.length > 0 && (
        <>
          <Label>{i18n.unit.placementProposals.infoTitle}</Label>
          <Gap size="xxs" />
          <P noMargin width="960px">
            {i18n.unit.placementProposals.infoText}
          </P>
        </>
      )}

      <div>
        <Table data-qa="placement-proposal-table">
          <Thead>
            <Tr>
              <Th>{i18n.unit.placementProposals.name}</Th>
              <Th>{i18n.unit.placementProposals.birthday}</Th>
              <Th>{i18n.unit.placementProposals.placementDuration}</Th>
              <Th>{i18n.unit.placementProposals.type}</Th>
              <Th>{i18n.unit.placementProposals.subtype}</Th>
              <Th>{i18n.unit.placementProposals.application}</Th>
              <Th>{i18n.unit.placementProposals.confirmation}</Th>
            </Tr>
          </Thead>
          <Tbody>
            {sortedRows.map((p) => (
              <PlacementProposalRow
                key={p.id}
                placementPlan={p}
                confirmationState={
                  summary.confirmationStates[p.applicationId] ?? null
                }
                submitting={respondToPlacementProposalIsPending}
                openModal={() => {
                  setCurrentApplicationId(p.applicationId)
                  setModalOpen(true)
                }}
                onChange={(status) =>
                  void sendConfirmation(p.applicationId, status)
                }
              />
            ))}
          </Tbody>
        </Table>
      </div>

      {placementPlans.length > 0 && (
        <PlacementProposalsActionBar sticky={!acceptDisabled}>
          <FixedSpaceRow alignItems="center" justifyContent="space-between">
            {(summary.accepted > 0 || summary.rejected > 0) && (
              <>
                <div>
                  <Bold>{i18n.unit.placementProposals.acceptAllTitle}</Bold>
                </div>
                <div>
                  <Italic>
                    {i18n.unit.placementProposals.acceptAllSummary(summary)}
                  </Italic>
                </div>
              </>
            )}
            <MutateButton
              data-qa="placement-proposals-accept-button"
              mutation={acceptPlacementProposalMutation}
              onClick={() => ({
                unitId,
                body: {
                  rejectReasonTranslations:
                    i18n.unit.placementProposals.rejectReasons
                }
              })}
              onSuccess={() => undefined}
              onFailure={onAcceptFailure}
              disabled={acceptDisabled}
              text={i18n.unit.placementProposals.acceptAllButton}
              primary
            />
          </FixedSpaceRow>
        </PlacementProposalsActionBar>
      )}
    </>
  )
})

const PlacementProposalsActionBar = styled.div<{ sticky: boolean }>`
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: right;
  position: ${(p) => (p.sticky ? 'sticky' : 'relative')};
  bottom: 0;
  background: ${colors.grayscale.g0};
  padding: 16px 0;
`
