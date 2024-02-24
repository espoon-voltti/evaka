// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo } from 'react'

import { isLoading } from 'lib-common/api'
import { ContentArea } from 'lib-components/layout/Container'
import { FixedSpaceColumn } from 'lib-components/layout/flex-helpers'
import { H2 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'

import UnitAccessControl from '../../components/unit/tab-unit-information/UnitAccessControl'
import UnitInformation from '../../components/unit/tab-unit-information/UnitInformation'
import { UnitContext } from '../../state/unit'
import { renderResult } from '../async-rendering'

import UnitMobileDevices from './tab-unit-information/UnitMobileDevices'

export default React.memo(function TabUnitInformation() {
  const { unitInformation } = useContext(UnitContext)

  const groups = useMemo(
    () =>
      unitInformation
        .map((unitInformation) =>
          Object.fromEntries(
            unitInformation.groups.map((group) => [group.id, group] as const)
          )
        )
        .getOrElse({}),
    [unitInformation]
  )

  return renderResult(unitInformation, ({ daycare, permittedActions }) => (
    <FixedSpaceColumn>
      <ContentArea
        opaque
        data-qa="unit-information"
        data-isloading={isLoading(unitInformation)}
      >
        <H2 data-qa="unit-name">{daycare.name}</H2>
        <Gap size="xxs" />
        <UnitInformation unit={daycare} permittedActions={permittedActions} />
      </ContentArea>

      {permittedActions.includes('READ_ACL') && (
        <UnitAccessControl
          permittedActions={permittedActions}
          groups={groups}
        />
      )}

      {daycare.enabledPilotFeatures.includes('MOBILE') &&
        permittedActions.includes('READ_MOBILE_DEVICES') && (
          <UnitMobileDevices
            canAddNew={permittedActions.includes(
              'CREATE_MOBILE_DEVICE_PAIRING'
            )}
          />
        )}
    </FixedSpaceColumn>
  ))
})
