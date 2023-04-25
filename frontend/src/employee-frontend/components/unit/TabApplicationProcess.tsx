// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useContext } from 'react'

import { getUnitApplications } from 'employee-frontend/api/unit'
import { UnitContext } from 'employee-frontend/state/unit'
import type { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import Title from 'lib-components/atoms/Title'
import { ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import TabApplications from './TabApplications'
import TabPlacementProposals from './TabPlacementProposals'
import TabWaitingConfirmation from './TabWaitingConfirmation'

interface Props {
  unitId: UUID
}

export default React.memo(function TabApplicationProcess({ unitId }: Props) {
  const { i18n } = useTranslation()
  const { reloadUnitNotifications } = useContext(UnitContext)
  const [applications, reloadApplications] = useApiState(
    () => getUnitApplications(unitId),
    [unitId]
  )
  const reloadData = useCallback(() => {
    void reloadApplications()
    reloadUnitNotifications()
  }, [reloadApplications, reloadUnitNotifications])

  return renderResult(
    applications,
    ({ placementPlans, placementProposals, applications }, isReloading) => (
      <div data-qa="application-process-page" data-isloading={isReloading}>
        <ContentArea opaque>
          <Title size={2}>{i18n.unit.applicationProcess.title}</Title>
        </ContentArea>
        <Gap size="m" />
        <TabWaitingConfirmation placementPlans={placementPlans} />
        <Gap size="m" />
        <TabPlacementProposals
          unitId={unitId}
          placementProposals={placementProposals}
          reloadApplications={reloadData}
        />
        <Gap size="m" />
        <TabApplications applications={applications} />
      </div>
    )
  )
})
