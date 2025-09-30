// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled, { css } from 'styled-components'

import type {
  ApplicationSummary,
  PlacementDraft,
  PreferredUnit
} from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateIconOnlyButton } from 'lib-components/atoms/buttons/MutateIconOnlyButton'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { defaultMargins } from 'lib-components/white-space'
import { faUndo } from 'lib-icons'
import { faFile } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { updateApplicationPlacementDraftMutation } from '../queries'

export default React.memo(function ApplicationCardPlaced({
  placementDraft,
  application,
  onUpdateApplicationPlacementSuccess,
  onUpdateApplicationPlacementFailure
}: {
  placementDraft: PlacementDraft
  application: ApplicationSummary | undefined
  onUpdateApplicationPlacementSuccess: (
    applicationId: ApplicationId,
    unit: PreferredUnit | null
  ) => void
  onUpdateApplicationPlacementFailure: () => void
}) {
  const { i18n } = useTranslation()

  const applicationInSearchResults = application !== undefined

  return (
    <SmallCard>
      <FixedSpaceRow justifyContent="space-between">
        <ChildName $applicationInSearchResults={applicationInSearchResults}>
          {placementDraft.childName}
        </ChildName>

        <FixedSpaceRow spacing="xs" alignItems="center">
          {applicationInSearchResults ? (
            <div>
              <MutateIconOnlyButton
                icon={faUndo}
                aria-label={
                  i18n.applications.placementDesktop.cancelPlacementDraft
                }
                mutation={updateApplicationPlacementDraftMutation}
                onClick={() => ({
                  applicationId: placementDraft.applicationId,
                  previousUnitId: placementDraft.unitId,
                  body: { unitId: null }
                })}
                onSuccess={() =>
                  onUpdateApplicationPlacementSuccess(
                    placementDraft.applicationId,
                    null
                  )
                }
                onFailure={onUpdateApplicationPlacementFailure}
              />
            </div>
          ) : (
            <ConfirmedMutation
              buttonStyle="ICON"
              icon={faUndo}
              buttonAltText={
                i18n.applications.placementDesktop.cancelPlacementDraft
              }
              confirmationTitle={
                i18n.applications.placementDesktop
                  .cancelPlacementDraftConfirmationTitle
              }
              confirmationText={
                i18n.applications.placementDesktop
                  .cancelPlacementDraftConfirmationMessage
              }
              mutation={updateApplicationPlacementDraftMutation}
              onClick={() => ({
                applicationId: placementDraft.applicationId,
                previousUnitId: placementDraft.unitId,
                body: { unitId: null }
              })}
              onSuccess={() =>
                onUpdateApplicationPlacementSuccess(
                  placementDraft.applicationId,
                  null
                )
              }
            />
          )}
          <a
            href={`/employee/applications/${placementDraft.applicationId}`}
            target="_blank"
            rel="noreferrer"
          >
            <IconOnlyButton icon={faFile} aria-label={i18n.common.open} />
          </a>
        </FixedSpaceRow>
      </FixedSpaceRow>
    </SmallCard>
  )
})

const SmallCard = styled.div`
  border: 1px solid ${(p) => p.theme.colors.grayscale.g35};
  border-radius: 4px;
  padding: ${defaultMargins.xs};
`

const ChildName = styled.div<{ $applicationInSearchResults: boolean }>`
  ${(p) =>
    p.$applicationInSearchResults
      ? ''
      : css`
          font-style: italic;
          color: ${p.theme.colors.grayscale.g70};
        `}
`
