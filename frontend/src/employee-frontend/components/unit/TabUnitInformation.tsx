{
  /*
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
*/
}

import React, { useContext } from 'react'
import { ContentArea } from '@evaka/lib-components/src/layout/Container'
import { FixedSpaceColumn } from '@evaka/lib-components/src/layout/flex-helpers'
import UnitInformation from '../../components/unit/tab-unit-information/UnitInformation'
import { UnitContext } from '../../state/unit'
import { SpinnerSegment } from '@evaka/lib-components/src/atoms/state/Spinner'
import ErrorSegment from '@evaka/lib-components/src/atoms/state/ErrorSegment'
import { requireRole, RequireRole } from '../../utils/roles'
import UnitAccessControl from '../../components/unit/tab-unit-information/UnitAccessControl'
import Occupancy from '../../components/unit/tab-unit-information/Occupancy'
import { H2, H3 } from '@evaka/lib-components/src/typography'
import UnitDataFilters from '../../components/unit/UnitDataFilters'
import { UserContext } from '../../state/user'
import { DataList } from '../../components/common/DataList'
import { useTranslation } from '../../state/i18n'
import { Gap } from '@evaka/lib-components/src/white-space'

function TabUnitInformation() {
  const { i18n } = useTranslation()
  const { roles } = useContext(UserContext)
  const { unitInformation, unitData, filters, setFilters } = useContext(
    UnitContext
  )

  if (unitInformation.isLoading) {
    return <SpinnerSegment />
  }

  if (unitInformation.isFailure) {
    return <ErrorSegment />
  }

  const renderCaretakers = () => {
    if (!unitData.isSuccess) return null

    const formatNumber = (num: number) =>
      parseFloat(num.toFixed(2)).toLocaleString()

    const min = formatNumber(unitData.value.caretakers.unitCaretakers.minimum)
    const max = formatNumber(unitData.value.caretakers.unitCaretakers.maximum)

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
        <H2 data-qa="unit-name">{unitInformation.value.daycare.name}</H2>
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
        {unitData.isLoading && <SpinnerSegment />}
        {unitData.isFailure && <ErrorSegment />}
        {unitData.isSuccess && unitData.value.unitOccupancies && (
          <Occupancy
            filters={filters}
            occupancies={unitData.value.unitOccupancies}
          />
        )}
      </ContentArea>

      <ContentArea opaque>
        <UnitInformation unit={unitInformation.value.daycare} />
      </ContentArea>

      <RequireRole oneOf={['ADMIN', 'UNIT_SUPERVISOR']}>
        <UnitAccessControl unitId={unitInformation.value.daycare.id} />
      </RequireRole>
    </FixedSpaceColumn>
  )
}

export default React.memo(TabUnitInformation)
