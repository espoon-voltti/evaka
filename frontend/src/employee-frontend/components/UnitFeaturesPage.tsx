// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { H1 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Failure, Loading, Result, Success } from 'lib-common/api'
import { UnitFeatures } from 'lib-common/generated/api-types/daycare'
import { useRestApi } from 'lib-common/utils/useRestApi'
import { Table, Tbody, Td, Thead, Tr } from 'lib-components/layout/Table'
import { pilotFeatures } from 'lib-common/generated/api-types/shared'
import { UnwrapResult } from './async-rendering'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import { client } from '../api/client'
import { JsonOf } from 'lib-common/json'

async function getUnitFeatures(): Promise<Result<UnitFeatures[]>> {
  return client
    .get<JsonOf<UnitFeatures[]>>('/daycares/features')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

export default React.memo(function UnitFeaturesPage() {
  const [units, setUnits] = useState<Result<UnitFeatures[]>>(Loading.of())

  const load = useRestApi(getUnitFeatures, setUnits)
  useEffect(load, [load])

  return (
    <Container verticalMargin={defaultMargins.L}>
      <ContentArea opaque>
        <H1>Yksiköille avatut toiminnot</H1>
        <UnwrapResult result={units}>
          {(units) => (
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
                          checked={unit.features.includes(f)}
                          hiddenLabel
                          disabled
                        />
                      </Td>
                    ))}
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )}
        </UnwrapResult>
      </ContentArea>
    </Container>
  )
})
