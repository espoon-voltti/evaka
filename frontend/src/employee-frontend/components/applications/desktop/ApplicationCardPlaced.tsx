// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled, { css } from 'styled-components'

import type {
  ApplicationSummary,
  PlacementDraft,
  PreferredUnit
} from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import Tooltip from 'lib-components/atoms/Tooltip'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateIconOnlyButton } from 'lib-components/atoms/buttons/MutateIconOnlyButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { defaultMargins } from 'lib-components/white-space'
import { faCommentAlt, fasCommentAltLines } from 'lib-icons'
import { faUndo } from 'lib-icons'
import { faFile } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { ServiceWorkerNoteModal } from '../ApplicationsList'
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

  const [editingNote, setEditingNote] = useState(false)

  return (
    <SmallCard>
      {editingNote && (
        <ServiceWorkerNoteModal
          applicationId={placementDraft.applicationId}
          serviceWorkerNote={placementDraft.serviceWorkerNote}
          onClose={() => setEditingNote(false)}
        />
      )}
      <FixedSpaceRow justifyContent="space-between">
        <Tooltip
          tooltip={
            <FixedSpaceColumn spacing="xxs">
              <div>{placementDraft.modifiedBy.name}</div>
              <div>{placementDraft.modifiedAt.format()}</div>
            </FixedSpaceColumn>
          }
        >
          <ChildName $applicationInSearchResults={applicationInSearchResults}>
            {placementDraft.childName}
          </ChildName>
        </Tooltip>

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
          <Tooltip
            tooltip={
              placementDraft.serviceWorkerNote ? (
                <span>{placementDraft.serviceWorkerNote}</span>
              ) : (
                <i>{i18n.applications.list.addNote}</i>
              )
            }
          >
            <IconOnlyButton
              icon={
                placementDraft.serviceWorkerNote
                  ? fasCommentAltLines
                  : faCommentAlt
              }
              onClick={() => setEditingNote(true)}
              aria-label={
                placementDraft.serviceWorkerNote
                  ? i18n.common.edit
                  : i18n.applications.list.addNote
              }
              data-qa="service-worker-note"
            />
          </Tooltip>
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
