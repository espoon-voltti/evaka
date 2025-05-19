// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import orderBy from 'lodash/orderBy'
import React, { useCallback, useContext } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import styled from 'styled-components'

import type { Daycare } from 'lib-common/generated/api-types/daycare'
import { careTypes } from 'lib-common/generated/api-types/daycare'
import LocalDate from 'lib-common/local-date'
import { useQueryResult } from 'lib-common/query'
import { LegacyButton } from 'lib-components/atoms/buttons/LegacyButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import InputField from 'lib-components/atoms/form/InputField'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { Container, ContentArea } from 'lib-components/layout/Container'
import {
  SortableTh,
  Table,
  Tbody,
  Td,
  Thead,
  Tr
} from 'lib-components/layout/Table'
import { Gap } from 'lib-components/white-space'
import { unitProviderTypes } from 'lib-customizations/employee'
import { faSearch } from 'lib-icons'

import { useTranslation } from '../state/i18n'
import type { SearchColumn, UnitsState } from '../state/units'
import { UnitsContext } from '../state/units'
import { UserContext } from '../state/user'
import { RequireRole } from '../utils/roles'

import { renderResult } from './async-rendering'
import { daycaresQuery } from './unit/queries'

const TopBar = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;

  .buttons {
    flex: 0 0 auto;
  }
`

export default React.memo(function Units() {
  const { i18n } = useTranslation()
  const { user } = useContext(UserContext)
  const {
    filter,
    setFilter,
    sortColumn,
    setSortColumn,
    sortDirection,
    setSortDirection,
    includeClosed,
    setIncludeClosed
  } = useContext<UnitsState>(UnitsContext)
  const navigate = useNavigate()
  const units = useQueryResult(daycaresQuery({ includeClosed }))

  const sortBy = (column: SearchColumn) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')
    }
    setSortColumn(column)
  }

  const orderedUnits = useCallback(
    (units: Daycare[]) =>
      sortColumn === 'name'
        ? orderBy(
            units,
            [sortColumn],
            [sortDirection === 'ASC' ? 'asc' : 'desc']
          )
        : orderBy(
            units,
            [sortColumn, 'name'],
            [sortDirection === 'ASC' ? 'asc' : 'desc', 'asc']
          ),
    [sortColumn, sortDirection]
  )

  if (
    units.isSuccess &&
    units.value.length === 1 &&
    !user?.accessibleFeatures.createUnits
  ) {
    return <Navigate to={`/units/${units.value[0].id}`} replace={true} />
  }

  return (
    <Container data-qa="units-page">
      <ContentArea opaque>
        <Gap size="xs" />
        <TopBar>
          <div>
            <InputField
              data-qa="unit-name-filter"
              value={filter.text}
              placeholder={i18n.units.findByName}
              onChange={(value) => {
                setFilter({ ...filter, text: value })
              }}
              icon={faSearch}
              width="L"
            />
            <Gap size="s" />
            <MultiSelect
              value={filter.providerTypes}
              onChange={(value) =>
                setFilter({ ...filter, providerTypes: value })
              }
              options={unitProviderTypes}
              getOptionId={(opt) => opt}
              getOptionLabel={(opt) => i18n.common.providerType[opt]}
              placeholder={i18n.units.selectProviderTypes}
              data-qa="provider-types-select"
            />
            <Gap size="s" />
            <MultiSelect
              value={filter.careTypes}
              onChange={(value) => setFilter({ ...filter, careTypes: value })}
              options={careTypes}
              getOptionId={(opt) => opt}
              getOptionLabel={(opt) => i18n.common.types[opt]}
              placeholder={i18n.units.selectCareTypes}
              data-qa="care-types-select"
            />
            <Gap size="s" />
            <Checkbox
              label={i18n.units.includeClosed}
              checked={includeClosed}
              onChange={() =>
                setIncludeClosed((previousState) => !previousState)
              }
              data-qa="include-closed"
            />
          </div>
          <RequireRole oneOf={['ADMIN']}>
            <div>
              <LegacyButton
                data-qa="create-new-unit"
                className="units-wrapper-create"
                onClick={() => void navigate('/units/new')}
                text={i18n.unit.create}
              />
            </div>
          </RequireRole>
        </TopBar>
        <Gap size="L" />
        <div className="table-of-units">
          {renderResult(units, (units) => (
            <Table data-qa="table-of-units">
              <Thead>
                <Tr>
                  <SortableTh
                    sorted={sortColumn === 'name' ? sortDirection : undefined}
                    onClick={() => sortBy('name')}
                  >
                    {i18n.units.name}
                  </SortableTh>
                  <SortableTh
                    sorted={
                      sortColumn === 'area.name' ? sortDirection : undefined
                    }
                    onClick={() => sortBy('area.name')}
                  >
                    {i18n.units.area}
                  </SortableTh>
                  <SortableTh
                    sorted={
                      sortColumn === 'address' ? sortDirection : undefined
                    }
                    onClick={() => sortBy('visitingAddress.streetAddress')}
                  >
                    {i18n.units.address}
                  </SortableTh>
                  <SortableTh
                    sorted={sortColumn === 'city' ? sortDirection : undefined}
                    onClick={() => sortBy('visitingAddress.postOffice')}
                  >
                    {i18n.units.city}
                  </SortableTh>
                  <SortableTh
                    sorted={sortColumn === 'type' ? sortDirection : undefined}
                    onClick={() => sortBy('type')}
                  >
                    {i18n.units.type}
                  </SortableTh>
                </Tr>
              </Thead>
              <Tbody>
                {orderedUnits(units)
                  .filter(
                    (unit) =>
                      (filter.text.length === 0 ||
                        unit.name
                          .toLowerCase()
                          .includes(filter.text.toLowerCase())) &&
                      (filter.providerTypes.length === 0 ||
                        filter.providerTypes.includes(unit.providerType)) &&
                      (filter.careTypes.length === 0 ||
                        filter.careTypes.some((ct) =>
                          unit.type.includes(ct)
                        )) &&
                      (includeClosed ||
                        !unit.closingDate?.isBefore(
                          LocalDate.todayInSystemTz()
                        ))
                  )
                  .map((unit: Daycare) => (
                    <Tr key={unit.id} data-qa="unit-row" data-id={unit.id}>
                      <Td>
                        <Link to={`/units/${unit.id}`}>{unit.name}</Link>
                      </Td>
                      <Td>{unit.area.name}</Td>
                      <Td>
                        {unit.visitingAddress.streetAddress &&
                        unit.visitingAddress.postalCode
                          ? [
                              unit.visitingAddress.streetAddress,
                              unit.visitingAddress.postalCode
                            ].join(', ')
                          : unit.visitingAddress.streetAddress}
                      </Td>
                      <Td>{unit.visitingAddress.postOffice}</Td>
                      <Td>
                        {unit.type
                          .map((type) => i18n.common.types[type])
                          .join(', ')}
                      </Td>
                    </Tr>
                  ))}
              </Tbody>
            </Table>
          ))}
        </div>
        <Gap size="XXL" />
      </ContentArea>
    </Container>
  )
})
