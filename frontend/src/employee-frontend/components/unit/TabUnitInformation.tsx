// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'

import type { DaycareResponse } from 'lib-common/generated/api-types/daycare'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import UnitAccessControl from './tab-unit-information/UnitAccessControl'
import UnitInformation from './tab-unit-information/UnitInformation'
import UnitMobileDevices from './tab-unit-information/UnitMobileDevices'

export default React.memo(function TabUnitInformation({
  unitInformation
}: {
  unitInformation: DaycareResponse
}) {
  const { daycare, permittedActions } = unitInformation

  return (
    <FixedSpaceColumn>
      <ContentArea opaque data-qa="unit-information">
        <H2 data-qa="unit-name">{daycare.name}</H2>
        <Gap size="xxs" />
        <UnitInformation unit={daycare} permittedActions={permittedActions} />
      </ContentArea>

      {permittedActions.includes('READ_ACL') && (
        <UnitAccessControl unitInformation={unitInformation} />
      )}

      {daycare.enabledPilotFeatures.includes('MOBILE') &&
        permittedActions.includes('READ_MOBILE_DEVICES') && (
          <UnitMobileDevices
            unitId={daycare.id}
            canAddNew={permittedActions.includes(
              'CREATE_MOBILE_DEVICE_PAIRING'
            )}
          />
        )}
    </FixedSpaceColumn>
  )
})
