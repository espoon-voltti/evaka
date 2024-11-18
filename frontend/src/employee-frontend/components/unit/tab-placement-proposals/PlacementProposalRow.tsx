// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { Link } from 'react-router-dom'
import styled from 'styled-components'

import {
  PlacementPlanConfirmationStatus,
  PlacementPlanDetails
} from 'lib-common/generated/api-types/placement'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import CheckIconButton from 'lib-components/atoms/buttons/CheckIconButton'
import CrossIconButton from 'lib-components/atoms/buttons/CrossIconButton'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { Td, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { faFileAlt } from 'lib-icons'

import { getEmployeeUrlPrefix } from '../../../constants'
import { useTranslation } from '../../../state/i18n'
import { formatName } from '../../../utils'
import { isPartDayPlacement } from '../../../utils/placements'
import { CareTypeChip } from '../../common/CareTypeLabel'

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
  placementPlan: PlacementPlanDetails
  confirmationState: PlacementPlanConfirmationStatus
  submitting: boolean
  onChange: (state: PlacementPlanConfirmationStatus) => undefined | void
  openModal: () => void
}

export default React.memo(function PlacementProposalRow({
  placementPlan,
  confirmationState,
  submitting,
  onChange,
  openModal
}: Props) {
  const { i18n } = useTranslation()

  const childName = formatName(
    placementPlan.child.firstName,
    placementPlan.child.lastName,
    i18n,
    true
  )

  return (
    <>
      <CenteredRow
        data-qa={`placement-proposal-row-${placementPlan.applicationId}`}
      >
        <Td data-qa="child-name">
          <Link to={`/child-information/${placementPlan.child.id}`}>
            {childName}
          </Link>
        </Td>
        <Td data-qa="child-dob">{placementPlan.child.dateOfBirth.format()}</Td>
        <Td data-qa="placement-duration">{placementPlan.period.format()}</Td>
        <Td data-qa="placement-type">
          <CareTypeChip type={placementPlan.type} />
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
              data-qa="open-application"
            >
              <IconOnlyButton
                onClick={() => undefined}
                icon={faFileAlt}
                aria-label={i18n.personProfile.application.open}
              />
            </a>
          </CenteredDiv>
        </Td>
        <Td>
          {!submitting && (
            <FixedSpaceRow spacing="m">
              {!placementPlan.unitAcceptDisabled && (
                <div>
                  <CheckIconButton
                    data-qa="accept-button"
                    onClick={() =>
                      confirmationState === 'ACCEPTED'
                        ? onChange('PENDING')
                        : onChange('ACCEPTED')
                    }
                    active={confirmationState === 'ACCEPTED'}
                  />
                </div>
              )}
              <div>
                <CrossIconButton
                  data-qa="reject-button"
                  onClick={() =>
                    confirmationState === 'REJECTED_NOT_CONFIRMED'
                      ? onChange('PENDING')
                      : openModal()
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
