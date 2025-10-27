// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import styled from 'styled-components'

import type {
  ApplicationSummary,
  PlacementDraft
} from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import Tooltip from 'lib-components/atoms/Tooltip'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateIconOnlyButton } from 'lib-components/atoms/buttons/MutateIconOnlyButton'
import { Td, Tr } from 'lib-components/layout/Table'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { ConfirmedMutation } from 'lib-components/molecules/ConfirmedMutation'
import { Gap } from 'lib-components/white-space'
import { faCommentAlt, fasCommentAltLines } from 'lib-icons'
import { faUndo } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { ServiceWorkerNoteModal } from '../ApplicationsList'
import { deleteApplicationPlacementDraftMutation } from '../queries'

export default React.memo(function DraftPlacementRow({
  placementDraft,
  application,
  onMutateApplicationPlacementFailure,
  onDeleteApplicationPlacementSuccess
}: {
  placementDraft: PlacementDraft
  application: ApplicationSummary | undefined
  onDeleteApplicationPlacementSuccess: (applicationId: ApplicationId) => void
  onMutateApplicationPlacementFailure: () => void
}) {
  const { i18n } = useTranslation()

  const applicationInSearchResults = application !== undefined

  const [editingNote, setEditingNote] = useState(false)

  return (
    <Tr>
      {editingNote && (
        <ServiceWorkerNoteModal
          applicationId={placementDraft.applicationId}
          serviceWorkerNote={placementDraft.serviceWorkerNote}
          onClose={() => setEditingNote(false)}
        />
      )}
      <NameTd>
        <Tooltip
          tooltip={
            <div>
              {!applicationInSearchResults && (
                <>
                  <strong>
                    * {i18n.applications.placementDesktop.notInSearchResults}
                  </strong>
                  <Gap size="xs" />
                </>
              )}
              <div>{i18n.applications.placementDesktop.draftedBy}:</div>
              <strong>{placementDraft.modifiedBy.name}</strong>
              <div>{placementDraft.modifiedAt.format()}</div>
            </div>
          }
        >
          <a
            href={`/employee/applications/${placementDraft.applicationId}`}
            target="_blank"
            rel="noreferrer"
          >
            {placementDraft.childName} {!applicationInSearchResults && '*'}
          </a>
        </Tooltip>
      </NameTd>
      <DateTd>{placementDraft.startDate.format()} –</DateTd>
      <ActionsTd>
        <FixedSpaceRow
          spacing="s"
          alignItems="center"
          justifyContent="flex-end"
        >
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
          <Tooltip
            tooltip={i18n.applications.placementDesktop.cancelPlacementDraft}
            delayed
          >
            {applicationInSearchResults ? (
              <div>
                <MutateIconOnlyButton
                  icon={faUndo}
                  aria-label={
                    i18n.applications.placementDesktop.cancelPlacementDraft
                  }
                  mutation={deleteApplicationPlacementDraftMutation}
                  onClick={() => ({
                    applicationId: placementDraft.applicationId,
                    previousUnitId: placementDraft.unitId
                  })}
                  onSuccess={() =>
                    onDeleteApplicationPlacementSuccess(
                      placementDraft.applicationId
                    )
                  }
                  onFailure={onMutateApplicationPlacementFailure}
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
                mutation={deleteApplicationPlacementDraftMutation}
                onClick={() => ({
                  applicationId: placementDraft.applicationId,
                  previousUnitId: placementDraft.unitId
                })}
                onSuccess={() =>
                  onDeleteApplicationPlacementSuccess(
                    placementDraft.applicationId
                  )
                }
              />
            )}
          </Tooltip>
        </FixedSpaceRow>
      </ActionsTd>
    </Tr>
  )
})

const SlimTd = styled(Td)`
  padding: 8px 8px;
`

const NameTd = styled(SlimTd)`
  width: 80%;

  * {
    text-wrap: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 100%;
  }
`

const DateTd = styled(SlimTd)`
  text-wrap: nowrap;
  width: 110px;
  text-align: right;
`

const ActionsTd = styled(SlimTd)`
  width: 80px;
  text-align: right;
`
