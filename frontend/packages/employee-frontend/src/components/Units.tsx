// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useContext, useEffect } from 'react'
import { isFailure, isLoading, isSuccess } from '~api'
import { SearchColumn, UnitsContext, UnitsState } from '~state/units'
import {
  Button,
  Buttons,
  Container,
  ContentArea,
  Input,
  Loader,
  Table
} from '~components/shared/alpha'
import { useTranslation } from '~state/i18n'
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import { faSearch } from '@evaka/icons'
import { getDaycares } from '~api/unit'
import { Unit } from '~types/unit'
import * as _ from 'lodash'
import { Link } from 'react-router-dom'
import { RequireRole } from '~utils/roles'
import '~components/Units.scss'
import styled from 'styled-components'

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
              <Table.Row key={unit.id} dataQa="unit-row">
                <Table.Td>
                  <Link to={`/units/${unit.id}`}>{unit.name}</Link>
                </Table.Td>
                <Table.Td>{unit.area.name}</Table.Td>
                <Table.Td>
                  {unit.visitingAddress.streetAddress &&
                  unit.visitingAddress.postalCode
                    ? [
                        unit.visitingAddress.streetAddress,
                        unit.visitingAddress.postalCode
                      ].join(', ')
                    : unit.visitingAddress.streetAddress}
                </Table.Td>
                <Table.Td>
                  {unit.type.map((type) => i18n.common.types[type]).join(', ')}
                </Table.Td>
              </Table.Row>
            )
          })
      : null

  return (
    <>
      <div className="units-wrapper" data-qa="units-page">
        <Container>
          <ContentArea opaque>
            <TopBar>
              <div className="input-search">
                <Input
                  dataQa="unit-name-filter"
                  value={filter}
                  placeholder={i18n.units.findByName}
                  onChange={(event: React.ChangeEvent<HTMLInputElement>) => {
                    setFilter(event.target.value)
                  }}
                />
                <Button plain disabled>
                  <FontAwesomeIcon icon={faSearch} size="lg" />
                </Button>
              </div>
              <RequireRole oneOf={['ADMIN']}>
                <Buttons>
                  <Button
                    dataQa="create-new-unit"
                    className="units-wrapper-create"
                    onClick={() => {
                      window.location.href = '/employee/units/new'
                    }}
                  >
                    {i18n.unit.create}
                  </Button>
                </Buttons>
              </RequireRole>
            </TopBar>
            <>
              <div className="table-of-units">
                <Table.Table dataQa="table-of-units">
                  <Table.Head>
                    <Table.Row>
                      <Table.HeadButton
                        sortable
                        sorted={
                          sortColumn === 'name' ? sortDirection : undefined
                        }
                        onClick={() => sortBy('name')}
                      >
                        {i18n.units.name}
                      </Table.HeadButton>
                      <Table.HeadButton
                        sortable
                        sorted={
                          sortColumn === 'area.name' ? sortDirection : undefined
                        }
                        onClick={() => sortBy('area.name')}
                      >
                        {i18n.units.area}
                      </Table.HeadButton>
                      <Table.HeadButton
                        sortable
                        sorted={
                          sortColumn === 'address' ? sortDirection : undefined
                        }
                        onClick={() => sortBy('address')}
                      >
                        {i18n.units.address}
                      </Table.HeadButton>
                      <Table.HeadButton
                        sortable
                        sorted={
                          sortColumn === 'type' ? sortDirection : undefined
                        }
                        onClick={() => sortBy('type')}
                      >
                        {i18n.units.type}
                      </Table.HeadButton>
                    </Table.Row>
                  </Table.Head>
                  <Table.Body>{renderUnits()}</Table.Body>
                </Table.Table>
              </div>
            </>
            {isLoading(units) && <Loader />}
            {isFailure(units) && <div>{i18n.common.loadingFailed}</div>}
          </ContentArea>
        </Container>
      </div>
    </>
  )
}

export default Units
