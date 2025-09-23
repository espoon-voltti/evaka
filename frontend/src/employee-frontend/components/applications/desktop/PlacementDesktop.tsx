// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { PagedApplicationSummaries } from 'lib-common/generated/api-types/application'
import type { ApplicationId } from 'lib-common/generated/api-types/shared'
import { AlertBox } from 'lib-components/molecules/MessageBoxes'

export default React.memo(function PlacementDesktop({
  applicationSummaries
}: {
  applicationSummaries: PagedApplicationSummaries
}) {
  if (applicationSummaries.pages > 1 || applicationSummaries.total > 50) {
    return (
      <AlertBox
        title={`Liikaa hakemuksia (${applicationSummaries.total})`}
        message="Tarkenna hakuehtoja niin, että hakemuksia on enintään 50 kpl."
      />
    )
  }

  const primaryUnits = [
    ...new Set(applicationSummaries.data.map((a) => a.preferredUnits[0].id))
  ]

  if (primaryUnits.length > 10) {
    return (
      <AlertBox
        title={`Liikaa ensisijaisia hakuyksiköitä (${primaryUnits.length})`}
        message="Tarkenna hakuehtoja niin, että hakemuksien ykköstoiveissa on enintään 10 eri yksikköä."
      />
    )
  }

  return (
    <PlacementDesktopValidated
      applicationIds={applicationSummaries.data.map((a) => a.id)}
    />
  )
})

const PlacementDesktopValidated = React.memo(
  function PlacementDesktopValidated({
    applicationIds
  }: {
    applicationIds: ApplicationId[]
  }) {
    return <div>applications: {applicationIds.length}</div>
  }
)
