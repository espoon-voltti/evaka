// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useState } from 'react'
import { useTranslation } from '../../state/i18n'
import { UUID } from '../../types'
import AssistanceNeed from '../../components/child-information/AssistanceNeed'
import AssistanceAction from '../../components/child-information/AssistanceAction'
import { CollapsibleContentArea } from 'lib-components/layout/Container'
import { H2 } from 'lib-components/typography'
import { ChildContext, ChildState } from '../../state/child'

export interface Props {
  id: UUID
  startOpen: boolean
}

function Assistance({ id, startOpen }: Props) {
  const { i18n } = useTranslation()
  const { permittedActions } = useContext<ChildState>(ChildContext)

  const [open, setOpen] = useState(startOpen)

  return (
    <div>
      <CollapsibleContentArea
        title={<H2 noMargin>{i18n.childInformation.assistance.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="assistance-collapsible"
      >
        {permittedActions.has('READ_ASSISTANCE_NEED') && (
          <AssistanceNeed id={id} />
        )}
        <div className="separator large" />
        {permittedActions.has('READ_ASSISTANCE_ACTION') && (
          <AssistanceAction id={id} />
        )}
      </CollapsibleContentArea>
    </div>
  )
}

export default Assistance
