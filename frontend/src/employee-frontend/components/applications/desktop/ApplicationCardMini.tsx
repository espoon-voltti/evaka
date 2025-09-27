// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type {
  ApplicationSummary,
  PreferredUnit
} from 'lib-common/generated/api-types/application'
import type {
  ApplicationId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import { MutateIconOnlyButton } from 'lib-components/atoms/buttons/MutateIconOnlyButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import { faUndo } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { isPartDayPlacement } from '../../../utils/placements'
import { BasisFragment, DateOfBirthInfo } from '../ApplicationsList'
import { updateApplicationPlacementDraftMutation } from '../queries'

export default React.memo(function ApplicationCardMini({
  application,
  unitId,
  onUpdateApplicationPlacementSuccess,
  onUpdateApplicationPlacementFailure
}: {
  application: ApplicationSummary
  unitId: DaycareId
  onUpdateApplicationPlacementSuccess: (
    applicationId: ApplicationId,
    unit: PreferredUnit | null
  ) => void
  onUpdateApplicationPlacementFailure: () => void
}) {
  const { i18n } = useTranslation()

  return (
    <SmallCard>
      <FixedSpaceColumn spacing="xs">
        <FixedSpaceRow justifyContent="space-between">
          <div>
            {application.lastName} {application.firstName}
          </div>
          <MutateIconOnlyButton
            icon={faUndo}
            aria-label="Palauta"
            mutation={updateApplicationPlacementDraftMutation}
            onClick={() => ({
              applicationId: application.id,
              previousUnitId: unitId,
              body: { unitId: null }
            })}
            onSuccess={() =>
              onUpdateApplicationPlacementSuccess(application.id, null)
            }
            onFailure={onUpdateApplicationPlacementFailure}
          />
        </FixedSpaceRow>
        <FixedSpaceRow spacing="xs" alignItems="center">
          <PlacementCircle
            type={
              isPartDayPlacement(application.placementType) ? 'half' : 'full'
            }
            label={
              application.serviceNeed !== null
                ? application.serviceNeed.nameFi
                : i18n.placement.type[application.placementType]
            }
            size={24}
          />
          <div>
            <DateOfBirthInfo application={application} chipOnly />
          </div>
          <BasisFragment application={application} />
        </FixedSpaceRow>
      </FixedSpaceColumn>
    </SmallCard>
  )
})

const SmallCard = styled.div`
  border: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  border-radius: 4px;
  padding: ${defaultMargins.xs};
`
