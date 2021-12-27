// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { Link } from 'react-router-dom'

import { Td, Tr } from 'lib-components/layout/Table'
import { useTranslation } from '../../../state/i18n'
import {
  DaycarePlacementPlan,
  PlacementPlanConfirmationStatus
} from '../../../types/unit'
import { careTypesFromPlacementType } from '../../common/CareTypeLabel'
import { formatName } from '../../../utils'
import styled from 'styled-components'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import IconButton from 'lib-components/atoms/buttons/IconButton'
import { faFileAlt } from 'lib-icons'
import { getEmployeeUrlPrefix } from '../../../constants'
import CheckIconButton from 'lib-components/atoms/buttons/CheckIconButton'
import CrossIconButton from 'lib-components/atoms/buttons/CrossIconButton'
import FormModal from 'lib-components/molecules/modals/FormModal'
import Radio from 'lib-components/atoms/form/Radio'
import InputField from 'lib-components/atoms/form/InputField'
import { Gap } from 'lib-components/white-space'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import { isPartDayPlacement } from '../../../utils/placements'
import { PlacementPlanRejectReason } from 'lib-customizations/types'
import { placementPlanRejectReasons } from 'lib-customizations/employee'

const CenteredDiv = styled.div`
  display: flex;
  justify-content: center;
`

const CenteredRow = styled(Tr)`
  height: 52px;

  td {
    vertical-align: middle;
    padding-top: 8px;
    padding-bottom: 8px;
  }
`

type Props = {
  placementPlan: DaycarePlacementPlan
  confirmationState: PlacementPlanConfirmationStatus
  submitting: boolean
  onChange: (
    state: PlacementPlanConfirmationStatus,
    reason?: PlacementPlanRejectReason,
    otherReason?: string
  ) => undefined | void
  loadUnitData: () => void
}

export default React.memo(function PlacementProposalRow({
  placementPlan,
  confirmationState,
  submitting,
  onChange,
  loadUnitData
}: Props) {
  const { i18n } = useTranslation()
  const [modalOpen, setModalOpen] = useState(false)
  const [reason, setReason] = useState<PlacementPlanRejectReason | null>(null)
  const [otherReason, setOtherReason] = useState<string>('')

  const childName = formatName(
    placementPlan.child.firstName,
    placementPlan.child.lastName,
    i18n,
    true
  )

  return (
    <>
      {modalOpen && (
        <FormModal
          title={i18n.unit.placementProposals.rejectTitle}
          resolveAction={() => {
            if (reason != null) {
              onChange('REJECTED_NOT_CONFIRMED', reason, otherReason)
              setModalOpen(false)
              void loadUnitData()
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

      <CenteredRow
        data-qa={`placement-proposal-row-${placementPlan.applicationId}`}
      >
        <Td data-qa="child-name">
          <Link to={`/child-information/${placementPlan.child.id}`}>
            {childName}
          </Link>
        </Td>
        <Td data-qa="child-dob">{placementPlan.child.dateOfBirth.format()}</Td>
        <Td data-qa="placement-duration">
          {placementPlan.period.start.format()} - $
          {placementPlan.period.end.format()}
        </Td>
        <Td data-qa="placement-type">
          {careTypesFromPlacementType(placementPlan.type)}
        </Td>
        <Td data-qa="placement-subtype">
          <PlacementCircle
            type={isPartDayPlacement(placementPlan.type) ? 'half' : 'full'}
            label={i18n.placement.type[placementPlan.type]}
          />
        </Td>
        <Td data-qa="application-link">
          <CenteredDiv>
            <a
              href={`${getEmployeeUrlPrefix()}/employee/applications/${
                placementPlan.applicationId
              }`}
              target="_blank"
              rel="noreferrer"
            >
              <IconButton
                onClick={() => undefined}
                icon={faFileAlt}
                altText={i18n.personProfile.application.open}
              />
            </a>
          </CenteredDiv>
        </Td>
        <Td>
          {!submitting && (
            <FixedSpaceRow spacing="m">
              <div>
                <CheckIconButton
                  data-qa={'accept-button'}
                  onClick={() =>
                    confirmationState === 'ACCEPTED'
                      ? onChange('PENDING')
                      : onChange('ACCEPTED')
                  }
                  active={confirmationState === 'ACCEPTED'}
                />
              </div>
              <div>
                <CrossIconButton
                  data-qa={'reject-button'}
                  onClick={() =>
                    confirmationState === 'REJECTED_NOT_CONFIRMED'
                      ? onChange('PENDING')
                      : setModalOpen(true)
                  }
                  active={confirmationState === 'REJECTED_NOT_CONFIRMED'}
                />
              </div>
            </FixedSpaceRow>
          )}
        </Td>
      </CenteredRow>
    </>
  )
})
