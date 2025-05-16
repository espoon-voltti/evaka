// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { DaycareResponse } from 'lib-common/generated/api-types/daycare'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import { useQueryResult } from 'lib-common/query'
import Title from 'lib-components/atoms/Title'
import { ContentArea } from 'lib-components/layout/Container'
import { Gap } from 'lib-components/white-space'
import { featureFlags } from 'lib-customizations/employee'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { TabAbsenceApplications } from './TabAbsenceApplications'
import TabApplications from './TabApplications'
import TabPlacementProposals from './TabPlacementProposals'
import TabServiceApplications from './TabServiceApplications'
import TabTransferApplications from './TabTransferApplications'
import TabWaitingConfirmation from './TabWaitingConfirmation'
import { unitApplicationsQuery } from './queries'

export default React.memo(function TabApplicationProcess({
  unitInformation
}: {
  unitInformation: DaycareResponse
}) {
  const { i18n } = useTranslation()
  const {
    daycare: { id: unitId },
    permittedActions
  } = unitInformation

  return (
    <div>
      <ContentArea opaque>
        <Title size={2}>{i18n.unit.applicationProcess.title}</Title>
      </ContentArea>
      {featureFlags.absenceApplications &&
        permittedActions.includes('READ_ABSENCE_APPLICATIONS') && (
          <>
            <Gap size="m" />
            <TabAbsenceApplications unitId={unitId} />
          </>
        )}
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
  )
})

const DaycareApplications = React.memo(function DaycareApplications({
  unitId
}: {
  unitId: DaycareId
}) {
  const applications = useQueryResult(unitApplicationsQuery({ unitId }))
  return renderResult(
    applications,
    (
      {
        placementPlans,
        placementProposals,
        applications,
        transferApplications
      },
      isReloading
    ) => (
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
        {transferApplications !== null && (
          <>
            <Gap size="m" />
            <TabTransferApplications
              transferApplications={transferApplications}
            />
          </>
        )}
      </div>
    )
  )
})
