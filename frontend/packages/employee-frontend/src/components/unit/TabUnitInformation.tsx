{
  /*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React, { useContext } from 'react'
import { ContentArea } from '~components/shared/layout/Container'
import { FixedSpaceColumn } from '~components/shared/layout/flex-helpers'
import UnitInformation from '~components/unit/tab-unit-information/UnitInformation'
import { UnitContext } from '~state/unit'
import { isFailure, isLoading, isSuccess } from '~api'
import { SpinnerSegment } from '~components/shared/atoms/state/Spinner'
import ErrorSegment from '~components/shared/atoms/state/ErrorSegment'
import { requireRole, RequireRole } from '~utils/roles'
import UnitAccessControl from '~components/unit/tab-unit-information/UnitAccessControl'
import Occupancy from '~components/unit/tab-unit-information/Occupancy'
import { H2, H3 } from '~components/shared/Typography'
import UnitDataFilters from '~components/unit/UnitDataFilters'
import { UserContext } from '~state/user'
import { DataList } from '~components/common/DataList'
import { useTranslation } from '~state/i18n'
import { Gap } from '~components/shared/layout/white-space'

function TabUnitInformation() {
  const { i18n } = useTranslation()
  const { roles } = useContext(UserContext)
  const { unitInformation, unitData, filters, setFilters } = useContext(
    UnitContext
  )

  if (isLoading(unitInformation)) {
    return <SpinnerSegment />
  }

  if (isFailure(unitInformation)) {
    return <ErrorSegment />
  }

  const renderCaretakers = () => {
    if (!isSuccess(unitData)) return null

    const formatNumber = (num: number) =>
      parseFloat(num.toFixed(2)).toLocaleString()

    const min = formatNumber(unitData.data.caretakers.unitCaretakers.minimum)
    const max = formatNumber(unitData.data.caretakers.unitCaretakers.maximum)

    return min === max ? (
      <span>
        {min} {i18n.unit.info.caretakers.unitOfValue}
      </span>
    ) : (
      <span>{`${min} - ${max} ${i18n.unit.info.caretakers.unitOfValue}`}</span>
    )
  }

  return (
    <FixedSpaceColumn>
      <ContentArea opaque>
        <H2 data-qa="unit-name">{unitInformation.data.daycare.name}</H2>
        <Gap size="s" />
        <H3>{i18n.unit.occupancies}</H3>
        <Gap size="s" />
        <DataList>
          <div>
            <label>{i18n.unit.filters.title}</label>
            <span>
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
            </span>
          </div>
        </DataList>
        <Gap size="s" />
        <DataList>
          <div>
            <label>{i18n.unit.info.caretakers.titleLabel}</label>
            <span data-qa={'unit-total-caretaker-count'}>
              {renderCaretakers()}
            </span>
          </div>
        </DataList>
        <Gap />
        {isLoading(unitData) && <SpinnerSegment />}
        {isFailure(unitData) && <ErrorSegment />}
        {isSuccess(unitData) && unitData.data.unitOccupancies && (
          <Occupancy
            filters={filters}
            occupancies={unitData.data.unitOccupancies}
          />
        )}
      </ContentArea>

      <ContentArea opaque>
        <UnitInformation unit={unitInformation.data.daycare} />
      </ContentArea>

      <RequireRole oneOf={['ADMIN', 'UNIT_SUPERVISOR']}>
        <UnitAccessControl unitId={unitInformation.data.daycare.id} />
      </RequireRole>
    </FixedSpaceColumn>
  )
}

export default React.memo(TabUnitInformation)
