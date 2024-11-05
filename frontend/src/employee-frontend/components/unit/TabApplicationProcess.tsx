// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext } from 'react'

import { useQueryResult } from 'lib-common/query'
import { UUID } from 'lib-common/types'
import Title from 'lib-components/atoms/Title'
import { ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'

import { useTranslation } from '../../state/i18n'
import { UnitContext } from '../../state/unit'
import { renderResult } from '../async-rendering'

import TabApplications from './TabApplications'
import TabPlacementProposals from './TabPlacementProposals'
import TabServiceApplications from './TabServiceApplications'
import TabWaitingConfirmation from './TabWaitingConfirmation'
import { unitApplicationsQuery } from './queries'

interface Props {
  unitId: UUID
}

export default React.memo(function TabApplicationProcess({ unitId }: Props) {
  const { i18n } = useTranslation()
  const { unitInformation } = useContext(UnitContext)

  return renderResult(unitInformation, ({ permittedActions }) => (
    <div>
      <ContentArea opaque>
        <Title size={2}>{i18n.unit.applicationProcess.title}</Title>
      </ContentArea>
      {featureFlags.serviceApplications &&
        permittedActions.includes('READ_SERVICE_APPLICATIONS') && (
          <>
            <Gap size="m" />
            <TabServiceApplications unitId={unitId} />
          </>
        )}
      {permittedActions.includes('READ_APPLICATIONS_AND_PLACEMENT_PLANS') && (
        <DaycareApplications unitId={unitId} />
      )}
    </div>
  ))
})

const DaycareApplications = React.memo(function DaycareApplications({
  unitId
}: Props) {
  const applications = useQueryResult(unitApplicationsQuery({ unitId }))
  return renderResult(
    applications,
    ({ placementPlans, placementProposals, applications }, isReloading) => (
      <div data-qa="daycare-applications" data-isloading={isReloading}>
        <Gap size="m" />
        <TabWaitingConfirmation placementPlans={placementPlans} />
        <Gap size="m" />
        <TabPlacementProposals
          unitId={unitId}
          placementProposals={placementProposals}
        />
        <Gap size="m" />
        <TabApplications applications={applications} />
      </div>
    )
  )
})
