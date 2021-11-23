// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { ContentArea } from 'lib-components/layout/Container'
import Title from 'lib-components/atoms/Title'
import { useTranslation } from '../../state/i18n'
import TabPlacementProposals from './TabPlacementProposals'
import TabWaitingConfirmation from './TabWaitingConfirmation'
import TabApplications from './TabApplications'
import { Gap } from 'lib-components/white-space'

interface Props {
  reloadUnitData: () => void
}

function TabApplicationProcess({ reloadUnitData }: Props) {
  const { i18n } = useTranslation()
  return (
    <div>
      <ContentArea opaque>
        <Title size={2}>{i18n.unit.applicationProcess.title}</Title>
      </ContentArea>
      <Gap size={'m'} />
      <TabWaitingConfirmation />
      <Gap size={'m'} />
      <TabPlacementProposals reloadUnitData={reloadUnitData} />
      <Gap size={'m'} />
      <TabApplications />
    </div>
  )
}

export default React.memo(TabApplicationProcess)
