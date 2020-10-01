// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect, useState } from 'react'
import _ from 'lodash'
import styled from 'styled-components'

import { Table, Th, Tr, Thead, Tbody } from '~components/shared/layout/Table'
import Title from '~components/shared/atoms/Title'
import { useTranslation } from '~state/i18n'
import {
  DaycarePlacementPlan,
  PlacementPlanConfirmationStatus,
  PlacementPlanRejectReason
} from '~types/unit'
import { Gap } from 'components/shared/layout/white-space'
import Button from 'components/shared/atoms/buttons/Button'
import {
  acceptPlacementProposal,
  respondToPlacementProposal
} from 'api/applications'
import { UIContext } from 'state/ui'
import PlacementProposalRow from '~components/unit/PlacementProposalRow'
import { UUID } from '~types'
import { InfoBox } from '~components/common/MessageBoxes'

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

  const sendConfirmation = (
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

    void respondToPlacementProposal(
      applicationId,
      confirmation,
      reason,
      otherReason
    )
      .then(() =>
        setConfirmationStates((state) => ({
          ...state,
          [applicationId]: {
            confirmation,
            submitting: false
          }
        }))
      )
      .catch(() => {
        setConfirmationStates((state) => ({
          ...state,
          [applicationId]: {
            ...state[applicationId],
            submitting: false
          }
        }))
        void loadUnitData()
      })
  }

  const sortedRows = _.sortBy(placementPlans, [
    (p: DaycarePlacementPlan) => p.child.lastName,
    (p: DaycarePlacementPlan) => p.child.firstName,
    (p: DaycarePlacementPlan) => p.period.start
  ])

  return (
    <>
      <Title size={2}>{i18n.unit.placementProposals.title}</Title>

      {placementPlans.length > 0 && (
        <InfoBox
          title={i18n.unit.placementProposals.infoBoxTitle}
          message={i18n.unit.placementProposals.infoBoxText}
          wide
        />
      )}

      <div>
        <Table>
          <Thead>
            <Tr>
              <Th>{i18n.unit.placementProposals.name}</Th>
              <Th>{i18n.unit.placementProposals.birthday}</Th>
              <Th>{i18n.unit.placementProposals.placementDuration}</Th>
              <Th>{i18n.unit.placementProposals.type}</Th>
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
                  sendConfirmation(p.applicationId, state, reason, otherReason)
                }
              />
            ))}
          </Tbody>
        </Table>
      </div>
      <Gap />

      {placementPlans.length > 0 && (
        <ButtonRow>
          <Button
            dataQa={'placement-proposals-accept-button'}
            onClick={() => {
              acceptPlacementProposal(unitId)
                .then(loadUnitData)
                .catch(() =>
                  setErrorMessage({
                    type: 'error',
                    title: i18n.common.error.unknown
                  })
                )
            }}
            text={i18n.unit.placementProposals.acceptAllButton}
            primary
            disabled={
              !!Object.values(confirmationStates).find(
                (state) => state.confirmation != 'ACCEPTED' || state.submitting
              )
            }
          />
        </ButtonRow>
      )}
    </>
  )
})
