// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { useTranslation } from '~/state/i18n'
import { faHandHolding } from '@evaka/icons'
import { UUID } from '~/types'
import AssistanceNeed from 'components/child-information/AssistanceNeed'
import AssistanceAction from 'components/child-information/AssistanceAction'
import CollapsibleSection from 'components/shared/molecules/CollapsibleSection'

export interface Props {
  id: UUID
  open: boolean
}

function Assistance({ id, open }: Props) {
  const { i18n } = useTranslation()

  return (
    <div>
      <CollapsibleSection
        icon={faHandHolding}
        title={i18n.childInformation.assistance.title}
        startCollapsed={!open}
        dataQa="assistance-collapsible"
      >
        <AssistanceNeed id={id} />
        <div className="separator large" />
        <AssistanceAction id={id} />
      </CollapsibleSection>
    </div>
  )
}

export default Assistance
