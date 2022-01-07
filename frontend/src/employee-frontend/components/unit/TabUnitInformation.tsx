// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useMemo } from 'react'
import { isLoading, Result } from 'lib-common/api'
import { ContentArea } from 'lib-components/layout/Container'
import {
  FixedSpaceColumn,
  FixedSpaceRow
} from 'lib-components/layout/flex-helpers'
import { H2, H3, Label } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { UnitData } from '../../api/unit'
import UnitDataFilters from '../../components/unit/UnitDataFilters'
import Occupancy from '../../components/unit/tab-unit-information/Occupancy'
import UnitAccessControl from '../../components/unit/tab-unit-information/UnitAccessControl'
import UnitInformation from '../../components/unit/tab-unit-information/UnitInformation'
import { useTranslation } from '../../state/i18n'
import { UnitContext } from '../../state/unit'
import { UserContext } from '../../state/user'
import { requireRole } from '../../utils/roles'
import { renderResult } from '../async-rendering'
import { DataList } from '../common/DataList'

export default React.memo(function TabUnitInformation() {
  const { i18n } = useTranslation()
  const { roles } = useContext(UserContext)
  const { unitInformation, unitData, filters, setFilters } =
    useContext(UnitContext)

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

  return renderResult(unitInformation, (unitInformation) => (
    <FixedSpaceColumn>
      <ContentArea
        opaque
        data-qa="unit-information"
        data-isloading={isLoading(unitData)}
      >
        <H2 data-qa="unit-name">{unitInformation.daycare.name}</H2>
        <Gap size="xxs" />
        <H3>{i18n.unit.occupancies}</H3>
        <Gap size="s" />
        <FixedSpaceRow alignItems="center">
          <Label>{i18n.unit.filters.title}</Label>
          <UnitDataFilters
            canEdit={requireRole(
              roles,
              'ADMIN',
              'SERVICE_WORKER',
              'UNIT_SUPERVISOR',
              'FINANCE_ADMIN'
            )}
            filters={filters}
            setFilters={setFilters}
          />
        </FixedSpaceRow>
        <Gap size="s" />
        <DataList>
          <div>
            <label>{i18n.unit.info.caretakers.titleLabel}</label>
            <span data-qa={'unit-total-caretaker-count'}>
              <Caretakers unitData={unitData} />
            </span>
          </div>
        </DataList>
        <Gap />
        {renderResult(unitData, (unitData) =>
          unitData.unitOccupancies ? (
            <Occupancy
              filters={filters}
              occupancies={unitData.unitOccupancies}
            />
          ) : null
        )}
      </ContentArea>

      <ContentArea opaque>
        <UnitInformation
          unit={unitInformation.daycare}
          permittedActions={unitInformation.permittedActions}
        />
      </ContentArea>

      {unitInformation.permittedActions.has('READ_ACL') && (
        <UnitAccessControl
          unitId={unitInformation.daycare.id}
          permittedActions={unitInformation.permittedActions}
          groups={groups}
          mobileEnabled={unitInformation.daycare.enabledPilotFeatures.includes(
            'MOBILE'
          )}
        />
      )}
    </FixedSpaceColumn>
  ))
})

const Caretakers = React.memo(function Caretakers({
  unitData
}: {
  unitData: Result<UnitData>
}) {
  const { i18n } = useTranslation()

  const formatNumber = (num: number) =>
    parseFloat(num.toFixed(2)).toLocaleString()

  return unitData
    .map((unitData) => {
      const min = formatNumber(unitData.caretakers.unitCaretakers.minimum)
      const max = formatNumber(unitData.caretakers.unitCaretakers.maximum)

      return min === max ? (
        <span>
          {min} {i18n.unit.info.caretakers.unitOfValue}
        </span>
      ) : (
        <span>{`${min} - ${max} ${i18n.unit.info.caretakers.unitOfValue}`}</span>
      )
    })
    .getOrElse(null)
})
