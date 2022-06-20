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
  PlacementPlanRejectReason
} from 'lib-common/generated/api-types/placement'
import { UUID } from 'lib-common/types'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import InputField from 'lib-components/atoms/form/InputField'
import Radio from 'lib-components/atoms/form/Radio'
import { Table, Tbody, Th, Thead, Tr } from 'lib-components/layout/Table'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import FormModal from 'lib-components/molecules/modals/FormModal'
import { Label, P } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { placementPlanRejectReasons } from 'lib-customizations/employee'

import {
  acceptPlacementProposal,
  respondToPlacementProposal
} from '../../../api/applications'
import PlacementProposalRow from '../../../components/unit/tab-placement-proposals/PlacementProposalRow'
import { useTranslation } from '../../../state/i18n'
import { UIContext } from '../../../state/ui'
import { DaycarePlacementPlan } from '../../../types/unit'

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
  placementPlans: DaycarePlacementPlan[]
  loadUnitData: () => void
}

export default React.memo(function PlacementProposals({
  unitId,
  placementPlans,
  loadUnitData
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

  const sendConfirmation = async (
    applicationId: UUID,
    confirmation: PlacementPlanConfirmationStatus,
    reason?: PlacementPlanRejectReason,
    otherReason?: string
  ) => {
    setConfirmationStates((state) => ({
      ...state,
      [applicationId]: {
        ...state[applicationId],
        submitting: true
      }
    }))

    try {
      await respondToPlacementProposal(
        applicationId,
        confirmation,
        reason,
        otherReason
      )
      if (!isMounted.current) return
      setConfirmationStates((state) => ({
        ...state,
        [applicationId]: {
          confirmation,
          submitting: false
        }
      }))
    } catch (_err) {
      if (!isMounted.current) return
      setConfirmationStates((state) => ({
        ...state,
        [applicationId]: {
          ...state[applicationId],
          submitting: false
        }
      }))
      void loadUnitData()
    }
  }

  const onAccept = useCallback(() => acceptPlacementProposal(unitId), [unitId])

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
    (p: DaycarePlacementPlan) => p.child.lastName,
    (p: DaycarePlacementPlan) => p.child.firstName,
    (p: DaycarePlacementPlan) => p.period.start
  ])

  return (
    <>
      {modalOpen && (
        <FormModal
          title={i18n.unit.placementProposals.rejectTitle}
          resolveAction={() => {
            if (reason != null && currentApplicationId != null) {
              void sendConfirmation(
                currentApplicationId,
                'REJECTED_NOT_CONFIRMED',
                reason,
                otherReason
              )

              setModalOpen(false)
              loadUnitData()
            }
          }}
          resolveLabel={i18n.common.save}
          resolveDisabled={!reason || (reason === 'OTHER' && !otherReason)}
          rejectAction={() => setModalOpen(false)}
          rejectLabel={i18n.common.cancel}
        >
          <FixedSpaceColumn>
            {placementPlanRejectReasons.map((option) => {
              return (
                <Radio
                  key={option}
                  data-qa="proposal-reject-reason"
                  checked={reason === option}
                  onChange={() => setReason(option)}
                  label={i18n.unit.placementProposals.rejectReasons[option]}
                />
              )
            })}
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
        </FormModal>
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
                key={`${p.id}`}
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
                onChange={(state, reason, otherReason) =>
                  void sendConfirmation(
                    p.applicationId,
                    state,
                    reason,
                    otherReason
                  )
                }
              />
            ))}
          </Tbody>
        </Table>
      </div>
      <Gap />

      {placementPlans.length > 0 && (
        <ButtonRow>
          <AsyncButton
            data-qa="placement-proposals-accept-button"
            onClick={onAccept}
            onSuccess={loadUnitData}
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
