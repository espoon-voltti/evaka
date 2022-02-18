// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

import { Failure, Loading, Result, Success } from 'lib-common/api'
import { UnitFeatures } from 'lib-common/generated/api-types/daycare'
import {
  PilotFeature,
  pilotFeatures
} from 'lib-common/generated/api-types/shared'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { useRestApi } from 'lib-common/utils/useRestApi'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Thead, Tr } from 'lib-components/layout/Table'
import { H1 } from 'lib-components/typography'

import { client } from '../api/client'

import { renderResult } from './async-rendering'

async function getUnitFeatures(): Promise<Result<UnitFeatures[]>> {
  return client
    .get<JsonOf<UnitFeatures[]>>('/daycares/features')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

async function putUnitFeatures(
  unitId: UUID,
  features: PilotFeature[]
): Promise<Result<void>> {
  return client
    .put<JsonOf<UnitFeatures[]>>(`/daycares/${unitId}/features`, features)
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export default React.memo(function UnitFeaturesPage() {
  const [unitsResult, setUnitsResult] = useState<Result<UnitFeatures[]>>(
    Loading.of()
  )

  const load = useRestApi(getUnitFeatures, setUnitsResult)
  useEffect(load, [load])

  // editable live copy of data
  const [units, setUnits] = useState<Result<UnitFeatures[]>>(Loading.of())
  useEffect(() => setUnits(unitsResult), [unitsResult])

  const [submitting, setSubmitting] = useState(false)

  const updateFlag = (unitId: UUID, feature: PilotFeature, value: boolean) => {
    if (!units.isSuccess) return

    const currentFeatures = units.value.find((u) => u.id === unitId)?.features
    if (!currentFeatures) return

    const newFeatures = value
      ? [...currentFeatures, feature]
      : currentFeatures.filter((f) => f !== feature)

    setSubmitting(true)
    void putUnitFeatures(unitId, newFeatures).then((res) => {
      setSubmitting(false)
      if (res.isSuccess) {
        setUnits((prev) =>
          prev.map((rows) =>
            rows.map((row) =>
              row.id === unitId ? { ...row, features: newFeatures } : row
            )
          )
        )
      } else {
        load()
      }
    })
  }

  return (
    <Container>
      <ContentArea opaque>
        <H1>Yksiköille avatut toiminnot</H1>
        {renderResult(units, (units) => (
          <Table>
            <Thead>
              <Td>Yksikkö</Td>
              {pilotFeatures.map((f) => (
                <Td key={f}>{f}</Td>
              ))}
            </Thead>
            <Tbody>
              {units.map((unit) => (
                <Tr key={unit.id}>
                  <Td>
                    <Link to={`/units/${unit.id}`}>{unit.name}</Link>
                  </Td>
                  {pilotFeatures.map((f) => (
                    <Td key={f}>
                      <Checkbox
                        label={f}
                        hiddenLabel
                        checked={unit.features.includes(f)}
                        onChange={(value) => updateFlag(unit.id, f, value)}
                        disabled={submitting}
                      />
                    </Td>
                  ))}
                </Tr>
              ))}
            </Tbody>
          </Table>
        ))}
      </ContentArea>
    </Container>
  )
})
