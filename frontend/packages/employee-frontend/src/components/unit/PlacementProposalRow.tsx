// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { Table } from '~components/shared/alpha'
import { useTranslation } from '~state/i18n'
import {
  DaycarePlacementPlan,
  PlacementPlanConfirmationStatus,
  PlacementPlanRejectReason
} from '~types/unit'
import { Link } from 'react-router-dom'
import { careTypesFromPlacementType } from '~components/common/CareTypeLabel'
import { formatName } from '~utils'
import styled from 'styled-components'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'components/shared/layout/flex-helpers'
import IconButton from 'components/shared/atoms/buttons/IconButton'
import { faFileAlt } from 'icon-set'
import { getEmployeeUrlPrefix } from '~constants'
import CheckIconButton from '~components/shared/atoms/buttons/CheckIconButton'
import CrossIconButton from '~components/shared/atoms/buttons/CrossIconButton'
import FormModal from '~components/common/FormModal'
import Radio from '~components/shared/atoms/form/Radio'
import InputField from '~components/shared/atoms/form/InputField'
import { Gap } from '~components/shared/layout/white-space'

const CenteredDiv = styled.div`
  display: flex;
  justify-content: center;
`

const CenteredRow = styled(Table.Row)`
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
}

export default React.memo(function PlacementProposalRow({
  placementPlan,
  confirmationState,
  submitting,
  onChange
}: Props) {
  const { i18n } = useTranslation()
  const [modalOpen, setModalOpen] = useState(false)
  const [reason, setReason] = useState<PlacementPlanRejectReason | null>(null)
  const [otherReason, setOtherReason] = useState<string>('')

  return (
    <>
      {modalOpen && (
        <FormModal
          title={i18n.unit.placementProposals.rejectTitle}
          resolve={() => {
            if (reason != null) {
              onChange('REJECTED', reason, otherReason)
              setModalOpen(false)
            }
          }}
          reject={() => setModalOpen(false)}
          resolveLabel={i18n.common.confirm}
          rejectLabel={i18n.common.cancel}
          resolveDisabled={!reason || (reason === 'OTHER' && !otherReason)}
        >
          <FixedSpaceColumn>
            <Radio
              dataQa="proposal-reject-reason"
              checked={reason === 'REASON_1'}
              onChange={() => setReason('REASON_1')}
              label={i18n.unit.placementProposals.rejectReasons.REASON_1}
            />
            <Radio
              dataQa="proposal-reject-reason"
              checked={reason === 'REASON_2'}
              onChange={() => setReason('REASON_2')}
              label={i18n.unit.placementProposals.rejectReasons.REASON_2}
            />
            <Radio
              dataQa="proposal-reject-reason"
              checked={reason === 'OTHER'}
              onChange={() => setReason('OTHER')}
              label={i18n.unit.placementProposals.rejectReasons.OTHER}
            />
            <InputField
              dataQa="proposal-reject-reason-input"
              value={otherReason}
              onChange={setOtherReason}
              placeholder={i18n.unit.placementProposals.rejectReasons.OTHER}
            />
          </FixedSpaceColumn>
          <Gap />
        </FormModal>
      )}

      <CenteredRow
        dataQa={`placement-proposal-row-${placementPlan.applicationId}`}
      >
        <Table.Td dataQa="child-name">
          <Link to={`/child-information/${placementPlan.child.id}`}>
            {formatName(
              placementPlan.child.firstName,
              placementPlan.child.lastName,
              i18n,
              true
            )}
          </Link>
        </Table.Td>
        <Table.Td dataQa="child-dob">
          {placementPlan.child.dateOfBirth.format()}
        </Table.Td>
        <Table.Td dataQa="placement-duration">
          {`${placementPlan.period.start.format()} - ${placementPlan.period.end.format()}`}
        </Table.Td>
        <Table.Td dataQa="placement-type">
          {careTypesFromPlacementType(placementPlan.type)}
        </Table.Td>
        <Table.Td dataQa="application-link">
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
        </Table.Td>
        <Table.Td>
          {!submitting && (
            <FixedSpaceRow spacing="m">
              <div>
                <CheckIconButton
                  dataQa={'accept-button'}
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
                  dataQa={'reject-button'}
                  onClick={() =>
                    confirmationState === 'REJECTED'
                      ? onChange('PENDING')
                      : setModalOpen(true)
                  }
                  active={confirmationState === 'REJECTED'}
                />
              </div>
            </FixedSpaceRow>
          )}
        </Table.Td>
      </CenteredRow>
    </>
  )
})
