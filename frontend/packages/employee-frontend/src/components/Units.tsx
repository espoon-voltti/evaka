// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'
import styled from 'styled-components'
import * as _ from 'lodash'
import { Link } from 'react-router-dom'

import { isFailure, isLoading, isSuccess } from '~api'
import { SearchColumn, UnitsContext, UnitsState } from '~state/units'
import Button from 'components/shared/atoms/buttons/Button'
import InputField from 'components/shared/atoms/form/InputField'
import { Container, ContentArea } from 'components/shared/layout/Container'
import {
  Table,
  Tr,
  Td,
  Thead,
  Tbody,
  SortableTh
} from '~components/shared/layout/Table'
import { Gap } from 'components/shared/layout/white-space'
import { useTranslation } from '~state/i18n'
import { faSearch } from 'icon-set'
import { getDaycares } from '~api/unit'
import { Unit } from '~types/unit'
import { RequireRole } from '~utils/roles'
import '~components/Units.scss'
import Loader from './shared/atoms/Loader'

const TopBar = styled.div`
  display: flex;
  justify-content: space-between;
  align-items: baseline;

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
    setSortDirection
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
    isSuccess(units)
      ? _.orderBy(
          units.data,
          [sortColumn],
          [sortDirection === 'ASC' ? 'asc' : 'desc']
        )
          .filter((unit: Unit) =>
            unit.name.toLowerCase().includes(filter.toLowerCase())
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
      : null

  return (
    <>
      <div className="units-wrapper" data-qa="units-page">
        <Container>
          <Gap size={'L'} />
          <ContentArea opaque>
            <Gap size={'XXL'} />
            <TopBar>
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
            <Gap size={'XXL'} />
            <>
              <div className="table-of-units">
                <Table data-qa="table-of-units">
                  <Thead>
                    <Tr>
                      <SortableTh
                        sorted={
                          sortColumn === 'name' ? sortDirection : undefined
                        }
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
                        onClick={() => sortBy('address')}
                      >
                        {i18n.units.address}
                      </SortableTh>
                      <SortableTh
                        sorted={
                          sortColumn === 'type' ? sortDirection : undefined
                        }
                        onClick={() => sortBy('type')}
                      >
                        {i18n.units.type}
                      </SortableTh>
                    </Tr>
                  </Thead>
                  <Tbody>{renderUnits()}</Tbody>
                </Table>
              </div>
            </>
            {isLoading(units) && <Loader />}
            {isFailure(units) && <div>{i18n.common.loadingFailed}</div>}
            <Gap size={'XXL'} />
          </ContentArea>
        </Container>
      </div>
    </>
  )
}

export default Units
