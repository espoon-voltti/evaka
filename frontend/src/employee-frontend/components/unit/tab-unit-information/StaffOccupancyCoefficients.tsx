// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import sortBy from 'lodash/sortBy'
import React, { useCallback, useContext, useState } from 'react'

import { formatPersonName } from 'employee-frontend/utils'
import { combine } from 'lib-common/api'
import type { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Th, Thead, Tr } from 'lib-components/layout/Table'
import { H2 } from 'lib-components/typography'

import {
  getOccupancyCoefficients,
  upsertOccupancyCoefficient
} from '../../../api/staff-occupancy'
import { useTranslation } from '../../../state/i18n'
import { UnitContext } from '../../../state/unit'
import { renderResult } from '../../async-rendering'

interface Props {
  allowEditing: boolean
}

export function StaffOccupancyCoefficients({ allowEditing }: Props) {
  const { i18n } = useTranslation()

  const { unitId, daycareAclRows } = useContext(UnitContext)

  const [occupancyCoefficients, reload] = useApiState(
    () => getOccupancyCoefficients(unitId),
    [unitId]
  )

  const [saving, setSaving] = useState(false)
  const updateCoefficient = useCallback(
    (employeeId: UUID, checked: boolean) => {
      setSaving(true)
      return upsertOccupancyCoefficient({
        unitId,
        employeeId,
        coefficient: checked ? 7 : 0
      }).then(() => {
        setSaving(false)
        void reload()
      })
    },
    [reload, unitId]
  )

  return (
    <ContentArea opaque>
      <H2>{i18n.unit.staffOccupancies.title}</H2>
      {renderResult(
        combine(daycareAclRows, occupancyCoefficients),
        ([aclRows, coefficients]) => {
          const hasPositiveCoefficient = (employeeId: UUID): boolean =>
            (coefficients.find((o) => o.employeeId === employeeId)
              ?.coefficient ?? 0) > 0
          const rows = sortBy(
            aclRows,
            (r) => r.employee.firstName,
            (r) => r.employee.lastName
          )
          return (
            <Table>
              <Thead>
                <Tr>
                  <Th>{i18n.common.form.name}</Th>
                  <Th>
                    {i18n.unit.staffOccupancies.occupancyCoefficientEnabled}
                  </Th>
                </Tr>
              </Thead>
              <Tbody>
                {rows.map((r) => (
                  <Tr key={`${r.employee.id}-${r.role}`}>
                    <Td>{formatPersonName(r.employee, i18n)}</Td>
                    <Td>
                      {allowEditing ? (
                        <Checkbox
                          checked={hasPositiveCoefficient(r.employee.id)}
                          disabled={saving}
                          label={i18n.common.yes}
                          hiddenLabel
                          onChange={(checked) =>
                            updateCoefficient(r.employee.id, checked)
                          }
                        />
                      ) : hasPositiveCoefficient(r.employee.id) ? (
                        i18n.common.yes
                      ) : (
                        i18n.common.no
                      )}
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )
        }
      )}
    </ContentArea>
  )
}
