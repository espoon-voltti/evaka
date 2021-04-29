// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useState } from 'react'
import { useTranslation } from '../../state/i18n'
import { UUID } from '../../types'
import AssistanceNeed from '../../components/child-information/AssistanceNeed'
import AssistanceAction from '../../components/child-information/AssistanceAction'
import { CollapsibleContentArea } from '../../../lib-components/layout/Container'
import { H2 } from '../../../lib-components/typography'

export interface Props {
  id: UUID
  startOpen: boolean
}

function Assistance({ id, startOpen }: Props) {
  const { i18n } = useTranslation()

  const [open, setOpen] = useState(startOpen)

  return (
    <div>
      <CollapsibleContentArea
        //icon={faHandHolding}
        title={<H2 noMargin>{i18n.childInformation.assistance.title}</H2>}
        open={open}
        toggleOpen={() => setOpen(!open)}
        opaque
        paddingVertical="L"
        data-qa="assistance-collapsible"
      >
        <AssistanceNeed id={id} />
        <div className="separator large" />
        <AssistanceAction id={id} />
      </CollapsibleContentArea>
    </div>
  )
}

export default Assistance
