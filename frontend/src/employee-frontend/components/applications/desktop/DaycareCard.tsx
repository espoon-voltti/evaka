// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React from 'react'
import styled from 'styled-components'

import type { PreferredUnit } from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import { ExpandableList } from 'lib-components/atoms/ExpandableList'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateIconOnlyButton } from 'lib-components/atoms/buttons/MutateIconOnlyButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { Label, LabelLike } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { faEyeSlash } from 'lib-icons'
import { faUndo } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { renderResult } from '../../async-rendering'
import {
  getPlacementDesktopDaycareQuery,
  updateApplicationPlacementDraftMutation
} from '../queries'

export default React.memo(function DaycareCard({
  daycare,
  onUpdateApplicationPlacementSuccess,
  onUpdateApplicationPlacementFailure,
  onRemoveFromShownDaycares
}: {
  daycare: PreferredUnit
  onUpdateApplicationPlacementSuccess: (
    applicationId: ApplicationId,
    unit: PreferredUnit | null
  ) => void
  onUpdateApplicationPlacementFailure: () => void
  onRemoveFromShownDaycares: () => void
}) {
  const { i18n } = useTranslation()
  const unitDetails = useQueryResult(
    getPlacementDesktopDaycareQuery({ unitId: daycare.id })
  )
  return (
    <Card>
      <FixedSpaceColumn spacing="s">
        <FixedSpaceRow justifyContent="space-between">
          <LabelLike>{daycare.name}</LabelLike>
          <IconOnlyButton
            icon={faEyeSlash}
            aria-label="Piilota"
            onClick={onRemoveFromShownDaycares}
          />
        </FixedSpaceRow>

        {renderResult(unitDetails, (unitDetails) => (
          <FixedSpaceColumn spacing="xxs">
            <Label>
              Sijoitushahmotelmat ({unitDetails.placementDrafts.length})
            </Label>
            <ExpandableList rowsToOccupy={5} i18n={i18n.common.expandableList}>
              {orderBy(unitDetails.placementDrafts, (pd) => pd.childName).map(
                (pd) => (
                  <FixedSpaceRow key={pd.applicationId}>
                    <span>{pd.childName}</span>
                    <MutateIconOnlyButton
                      icon={faUndo}
                      aria-label="Palauta"
                      mutation={updateApplicationPlacementDraftMutation}
                      onClick={() => ({
                        applicationId: pd.applicationId,
                        previousUnitId: daycare.id,
                        body: { unitId: null }
                      })}
                      onSuccess={() =>
                        onUpdateApplicationPlacementSuccess(
                          pd.applicationId,
                          null
                        )
                      }
                      onFailure={onUpdateApplicationPlacementFailure}
                    />
                  </FixedSpaceRow>
                )
              )}
            </ExpandableList>
          </FixedSpaceColumn>
        ))}
      </FixedSpaceColumn>
    </Card>
  )
})

const Card = styled.div`
  width: 580px;
  border: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  border-radius: 4px;
  padding: ${defaultMargins.s};
  background-color: ${(p) => p.theme.colors.grayscale.g0};
`
