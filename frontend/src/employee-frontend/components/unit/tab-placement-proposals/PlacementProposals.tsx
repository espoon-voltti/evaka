// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState
} from 'react'
import styled from 'styled-components'

import {
  PlacementPlanConfirmationStatus,
  PlacementPlanDetails,
  PlacementPlanRejectReason
} from 'lib-common/generated/api-types/placement'
import { useMutationResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import {
  MutateButton,
  cancelMutation
} from 'lib-components/atoms/buttons/MutateButton'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { MutateFormModal } from 'lib-components/molecules/modals/FormModal'
import { Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { placementPlanRejectReasons } from 'lib-customizations/employee'

import PlacementProposalRow from '../../../components/unit/tab-placement-proposals/PlacementProposalRow'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import {
  acceptPlacementProposalMutation,
  respondToPlacementProposalMutation
} from '../queries'

const ButtonRow = styled.div`
  display: flex;
  width: 100%;
  justify-content: flex-end;
`

interface DynamicState {
  confirmation: PlacementPlanConfirmationStatus
  submitting: boolean
}

type Props = {
  unitId: UUID
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
  const [currentApplicationId, setCurrentApplicationId] = useState<
    string | null
  >(null)

  const isMounted = useRef(true)
  useEffect(
    () => () => {
      isMounted.current = false
    },
    []
  )

  const [confirmationStates, setConfirmationStates] = useState<
    Record<UUID, DynamicState>
  >({})

  useEffect(() => {
    setConfirmationStates(
      placementPlans.reduce(
        (map, plan) => ({
          ...map,
          [plan.applicationId]: {
            confirmation: plan.unitConfirmationStatus,
            submitting: false
          }
        }),
        {}
      )
    )
  }, [placementPlans, setConfirmationStates])

  const { mutateAsync: respondToPlacementProposal } = useMutationResult(
    respondToPlacementProposalMutation
  )

  const sendConfirmation = async (
    applicationId: UUID,
    status: PlacementPlanConfirmationStatus
  ) => {
    setConfirmationStates((state) => ({
      ...state,
      [applicationId]: {
        ...state[applicationId],
        submitting: true
      }
    }))
    const result = await respondToPlacementProposal({
      unitId,
      applicationId,
      body: {
        status,
        reason: null,
        otherReason: null
      }
    })
    if (!isMounted.current) return
    if (result.isSuccess) {
      setConfirmationStates((state) => ({
        ...state,
        [applicationId]: {
          confirmation: status,
          submitting: false
        }
      }))
    } else {
      setConfirmationStates((state) => ({
        ...state,
        [applicationId]: {
          ...state[applicationId],
          submitting: false
        }
      }))
    }
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
      Object.values(confirmationStates).some((state) => state.submitting) ||
      !Object.values(confirmationStates).some(
        (state) =>
          state.confirmation === 'ACCEPTED' ||
          state.confirmation === 'REJECTED_NOT_CONFIRMED'
      ),
    [confirmationStates]
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
            setConfirmationStates((state) => ({
              ...state,
              [currentApplicationId]: {
                ...state[currentApplicationId],
                submitting: true
              }
            }))
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
            setConfirmationStates((state) => ({
              ...state,
              [currentApplicationId]: {
                confirmation: 'REJECTED_NOT_CONFIRMED',
                submitting: false
              }
            }))
            closeModal()
          }}
          onFailure={() => {
            if (currentApplicationId === null) return
            setConfirmationStates((state) => ({
              ...state,
              [currentApplicationId]: {
                ...state[currentApplicationId],
                submitting: false
              }
            }))
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
                  confirmationStates[p.applicationId]?.confirmation ?? null
                }
                submitting={
                  confirmationStates[p.applicationId]?.submitting ?? false
                }
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
      <Gap />

      {placementPlans.length > 0 && (
        <ButtonRow>
          <MutateButton
            data-qa="placement-proposals-accept-button"
            mutation={acceptPlacementProposalMutation}
            onClick={() => ({
              unitId,
              body: {
                rejectReasons: i18n.unit.placementProposals.rejectReasons
              }
            })}
            onSuccess={() => undefined}
            onFailure={onAcceptFailure}
            disabled={acceptDisabled}
            text={i18n.unit.placementProposals.acceptAllButton}
            primary
          />
        </ButtonRow>
      )}
    </>
  )
})
