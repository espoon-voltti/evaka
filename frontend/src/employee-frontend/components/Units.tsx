// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'
import styled from 'styled-components'
import * as _ from 'lodash'
import { Link } from 'react-router-dom'
import LocalDate from '@evaka/lib-common/local-date'
import { SearchColumn, UnitsContext, UnitsState } from '../state/units'
import Button from '@evaka/lib-components/atoms/buttons/Button'
import Checkbox from '@evaka/lib-components/atoms/form/Checkbox'
import InputField from '@evaka/lib-components/atoms/form/InputField'
import {
  Container,
  ContentArea
} from '@evaka/lib-components/layout/Container'
import {
  Table,
  Tr,
  Td,
  Thead,
  Tbody,
  SortableTh
} from '@evaka/lib-components/layout/Table'
import { Gap } from '@evaka/lib-components/white-space'
import { useTranslation } from '../state/i18n'
import { faSearch } from '@evaka/lib-icons'
import { getDaycares } from '../api/unit'
import { Unit } from '../types/unit'
import { RequireRole } from '../utils/roles'
import Loader from '@evaka/lib-components/atoms/Loader'

const TopBar = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: flex-start;

  .buttons {
    flex: 0 0 auto;
  }
`

function Units() {
  const { i18n } = useTranslation()
  const {
    units,
    setUnits,
    filter,
    setFilter,
    sortColumn,
    setSortColumn,
    sortDirection,
    setSortDirection,
    includeClosed,
    setIncludeClosed
  } = useContext<UnitsState>(UnitsContext)

  const sortBy = (column: SearchColumn) => {
    if (sortColumn === column) {
      setSortDirection(sortDirection === 'ASC' ? 'DESC' : 'ASC')
    }
    setSortColumn(column)
  }

  useEffect(() => {
    void getDaycares().then((units) => {
      setUnits(units)
    })
  }, [setUnits])

  const renderUnits = () =>
    units
      .map((us) =>
        _.orderBy(us, [sortColumn], [sortDirection === 'ASC' ? 'asc' : 'desc'])
          .filter((unit: Unit) =>
            unit.name.toLowerCase().includes(filter.toLowerCase())
          )
          .filter(
            (unit: Unit) =>
              includeClosed || !unit.closingDate?.isBefore(LocalDate.today())
          )
          .map((unit: Unit) => {
            return (
              <Tr key={unit.id} data-qa="unit-row">
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
                <Td>
                  {unit.type.map((type) => i18n.common.types[type]).join(', ')}
                </Td>
              </Tr>
            )
          })
      )
      .getOrElse(null)

  return (
    <Container data-qa="units-page">
      <Gap size={'L'} />
      <ContentArea opaque>
        <Gap size={'XXL'} />
        <TopBar>
          <div>
            <InputField
              dataQa="unit-name-filter"
              value={filter}
              placeholder={i18n.units.findByName}
              onChange={(value) => {
                setFilter(value)
              }}
              icon={faSearch}
              width={'L'}
            />
            <Gap size="s" />
            <Checkbox
              label={i18n.units.includeClosed}
              checked={includeClosed}
              onChange={() =>
                setIncludeClosed((previousState) => !previousState)
              }
              dataQa="include-closed"
            />
          </div>
          <RequireRole oneOf={['ADMIN']}>
            <div>
              <Button
                data-qa="create-new-unit"
                className="units-wrapper-create"
                onClick={() => {
                  window.location.href = '/employee/units/new'
                }}
                text={i18n.unit.create}
              />
            </div>
          </RequireRole>
        </TopBar>
        <Gap size="L" />
        <div className="table-of-units">
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
                  sorted={sortColumn === 'address' ? sortDirection : undefined}
                  onClick={() => sortBy('address')}
                >
                  {i18n.units.address}
                </SortableTh>
                <SortableTh
                  sorted={sortColumn === 'type' ? sortDirection : undefined}
                  onClick={() => sortBy('type')}
                >
                  {i18n.units.type}
                </SortableTh>
              </Tr>
            </Thead>
            <Tbody>{renderUnits()}</Tbody>
          </Table>
        </div>
        {units.isLoading && <Loader />}
        {units.isFailure && <div>{i18n.common.loadingFailed}</div>}
        <Gap size={'XXL'} />
      </ContentArea>
    </Container>
  )
}

export default Units
