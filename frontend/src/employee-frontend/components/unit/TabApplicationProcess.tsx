// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import Title from 'lib-components/atoms/Title'
import { ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import { useTranslation } from '../../state/i18n'
import TabApplications from './TabApplications'
import TabPlacementProposals from './TabPlacementProposals'
import TabWaitingConfirmation from './TabWaitingConfirmation'

interface Props {
  isLoading: boolean
  reloadUnitData: () => void
}

export default React.memo(function TabApplicationProcess({
  isLoading,
  reloadUnitData
}: Props) {
  const { i18n } = useTranslation()
  return (
    <div data-qa="application-process-page" data-isloading={isLoading}>
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
})
