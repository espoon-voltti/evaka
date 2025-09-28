// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'
import { useLocation } from 'wouter'

import type {
  ApplicationSummary,
  PreferredUnit
} from 'lib-common/generated/api-types/application'
import type {
  ApplicationId,
  DaycareId
} from 'lib-common/generated/api-types/shared'
import PlacementCircle from 'lib-components/atoms/PlacementCircle'
import Tooltip from 'lib-components/atoms/Tooltip'
import { Button } from 'lib-components/atoms/buttons/Button'
import { IconOnlyButton } from 'lib-components/atoms/buttons/IconOnlyButton'
import { MutateIconOnlyButton } from 'lib-components/atoms/buttons/MutateIconOnlyButton'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import { faUndo } from 'lib-icons'
import { faCommentAlt, faFile, fasCommentAltLines } from 'lib-icons'

import { useTranslation } from '../../../state/i18n'
import { isPartDayPlacement } from '../../../utils/placements'
import {
  BasisFragment,
  DateOfBirthInfo,
  ServiceWorkerNoteModal
} from '../ApplicationsList'
import { updateApplicationPlacementDraftMutation } from '../queries'

export default React.memo(function ApplicationCardPlaced({
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
  const [, navigate] = useLocation()

  const [editingNote, setEditingNote] = React.useState(false)

  return (
    <SmallCard>
      {editingNote && (
        <ServiceWorkerNoteModal
          application={application}
          onClose={() => setEditingNote(false)}
        />
      )}
      <FixedSpaceColumn spacing="xs">
        <FixedSpaceRow justifyContent="space-between">
          <div>
            {application.lastName} {application.firstName}
          </div>
          <FixedSpaceRow spacing="xs" alignItems="center">
            <MutateIconOnlyButton
              icon={faUndo}
              aria-label={
                i18n.applications.placementDesktop.cancelPlacementDraft
              }
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
            <Tooltip
              tooltip={
                application.serviceWorkerNote ? (
                  <span>{application.serviceWorkerNote}</span>
                ) : (
                  <i>{i18n.applications.list.addNote}</i>
                )
              }
            >
              <IconOnlyButton
                icon={
                  application.serviceWorkerNote
                    ? fasCommentAltLines
                    : faCommentAlt
                }
                onClick={(e) => {
                  e.stopPropagation()
                  setEditingNote(true)
                }}
                aria-label={
                  application.serviceWorkerNote
                    ? i18n.common.edit
                    : i18n.applications.list.addNote
                }
                data-qa="service-worker-note"
              />
            </Tooltip>
            <a
              href={`/employee/applications/${application.id}`}
              target="_blank"
              rel="noreferrer"
            >
              <IconOnlyButton icon={faFile} aria-label={i18n.common.open} />
            </a>
          </FixedSpaceRow>
        </FixedSpaceRow>
        <FixedSpaceRow justifyContent="space-between" alignItems="center">
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
          <FixedSpaceRow alignItems="center" spacing="xs">
            {application.checkedByAdmin && (
              <Button
                appearance="inline"
                text={i18n.applications.placementDesktop.toPlacementPlan}
                onClick={() =>
                  navigate(`/applications/${application.id}/placement`)
                }
              />
            )}
          </FixedSpaceRow>
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
