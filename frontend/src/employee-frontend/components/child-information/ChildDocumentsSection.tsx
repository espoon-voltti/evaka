// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo, useState } from 'react'

import { UUID } from 'lib-common/types'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'

import { ChildContext } from '../../state'
import { useTranslation } from '../../state/i18n'

import ChildDocuments from './ChildDocuments'
import VasuAndLeops from './VasuAndLeops'

interface Props {
  childId: UUID
  startOpen: boolean
}

export default React.memo(function ChildDocumentsSection({
  childId,
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

  const hasChildDocumentsPermission = useMemo(
    () => permittedActions.has('READ_CHILD_DOCUMENT'),
    [permittedActions]
  )

  if (!hasVasuPermission && !hasChildDocumentsPermission) {
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
        {hasChildDocumentsPermission && <ChildDocuments childId={childId} />}
      </CollapsibleContentArea>
    </div>
  )
})
