// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'

import { UUID } from 'lib-common/types'
import HorizontalLine from 'lib-components/atoms/HorizontalLine'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'
import { featureFlags } from 'lib-customizations/employee'

import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'

import ChildDiscussion from './ChildDiscussions'
import ChildDocuments from './ChildDocuments'
import VasuAndLeops from './VasuAndLeops'

interface Props {
  id: UUID
  startOpen: boolean
}

export default React.memo(function ChildDocumentsSection({
  id: childId,
  startOpen
}: Props) {
  const { i18n } = useTranslation()
  const { permittedActions, placements } = useContext(ChildContext)

  const [open, setOpen] = useState(startOpen)

  const hasVasuPermission = useMemo(
    () =>
      permittedActions.has('READ_VASU_DOCUMENT') &&
      placements
        .map((ps) =>
          ps.placements.some((placement) =>
            placement.daycare.enabledPilotFeatures.includes('VASU_AND_PEDADOC')
          )
        )
        .getOrElse(false),
    [permittedActions, placements]
  )

  const hasChildDiscussionPermission = useMemo(
    () =>
      permittedActions.has('READ_CHILD_DISCUSSION') &&
      featureFlags.childDiscussion &&
      placements
        .map((ps) =>
          ps.placements.some(
            (placement) => placement.groupPlacements.length > 0
          )
        )
        .getOrElse(false),
    [permittedActions, placements]
  )

  const hasChildDocumentsPermission = useMemo(
    () =>
      permittedActions.has('READ_CHILD_DOCUMENT') &&
      featureFlags.experimental?.assistanceNeedDecisions,
    [permittedActions]
  )

  if (
    !hasVasuPermission &&
    !hasChildDocumentsPermission &&
    !hasChildDiscussionPermission
  ) {
    return null
  }

  return (
    <div>
      <CollapsibleContentArea
        title={
          <H2 noMargin>{i18n.childInformation.childDocumentsSectionTitle}</H2>
        }
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="child-documents-collapsible"
      >
        {hasVasuPermission && <VasuAndLeops id={childId} />}
        {hasChildDiscussionPermission && (
          <>
            {hasVasuPermission && <HorizontalLine dashed slim />}
            <ChildDiscussion childId={childId} />
            {hasChildDocumentsPermission && <HorizontalLine dashed slim />}
          </>
        )}
        {hasChildDocumentsPermission && <ChildDocuments childId={childId} />}
      </CollapsibleContentArea>
    </div>
  )
})
