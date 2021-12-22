// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useRef, useState } from 'react'
import _ from 'lodash'
import styled from 'styled-components'

import { Table, Th, Tr, Thead, Tbody } from 'lib-components/layout/Table'
import { useTranslation } from '../../../state/i18n'
import {
  DaycarePlacementPlan,
  PlacementPlanConfirmationStatus
} from '../../../types/unit'
import { Gap } from 'lib-components/white-space'
import Button from 'lib-components/atoms/buttons/Button'
import {
  acceptPlacementProposal,
  respondToPlacementProposal
} from '../../../api/applications'
import { UIContext } from '../../../state/ui'
import PlacementProposalRow from '../../../components/unit/tab-placement-proposals/PlacementProposalRow'
import { PlacementPlanRejectReason } from 'lib-customizations/types'
import { UUID } from 'lib-common/types'
import { Label, P } from 'lib-components/typography'

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

  const sortedRows = _.sortBy(placementPlans, [
    (p: DaycarePlacementPlan) => p.child.lastName,
    (p: DaycarePlacementPlan) => p.child.firstName,
    (p: DaycarePlacementPlan) => p.period.start
  ])

  return (
    <>
      {placementPlans.length > 0 && (
        <>
          <Label>{i18n.unit.placementProposals.infoTitle}</Label>
          <Gap size={'xxs'} />
          <P noMargin width={'960px'}>
            {i18n.unit.placementProposals.infoText}
          </P>
        </>
      )}

      <div>
        <Table>
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
                onChange={(state, reason, otherReason) =>
                  void sendConfirmation(
                    p.applicationId,
                    state,
                    reason,
                    otherReason
                  )
                }
                loadUnitData={loadUnitData}
              />
            ))}
          </Tbody>
        </Table>
      </div>
      <Gap />

      {placementPlans.length > 0 && (
        <ButtonRow>
          <Button
            data-qa={'placement-proposals-accept-button'}
            onClick={() => {
              acceptPlacementProposal(unitId)
                .then(loadUnitData)
                .catch(() =>
                  setErrorMessage({
                    type: 'error',
                    title: i18n.common.error.unknown,
                    resolveLabel: i18n.common.ok
                  })
                )
            }}
            text={i18n.unit.placementProposals.acceptAllButton}
            primary
            disabled={
              Object.values(confirmationStates).some(
                (state) => state.submitting
              ) ||
              !Object.values(confirmationStates).some(
                (state) =>
                  state.confirmation === 'ACCEPTED' ||
                  state.confirmation === 'REJECTED_NOT_CONFIRMED'
              )
            }
          />
        </ButtonRow>
      )}
    </>
  )
})
