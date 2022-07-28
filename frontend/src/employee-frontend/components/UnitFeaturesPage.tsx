// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import React, { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import { useTranslation } from 'employee-frontend/state/i18n'
import { Failure, Result, Success } from 'lib-common/api'
import {
  ProviderType,
  UnitFeatures
} from 'lib-common/generated/api-types/daycare'
import {
  PilotFeature,
  pilotFeatures
} from 'lib-common/generated/api-types/shared'
import { JsonOf } from 'lib-common/json'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { ButtonLink } from 'lib-components/atoms/buttons/ButtonLink'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Thead, Tr } from 'lib-components/layout/Table'
import { H1 } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { unitProviderTypes } from 'lib-customizations/employee'

import { client } from '../api/client'

import { renderResult } from './async-rendering'
import { TableScrollable } from './reports/common'

async function getUnitFeatures(): Promise<Result<UnitFeatures[]>> {
  return client
    .get<JsonOf<UnitFeatures[]>>('/daycares/features')
    .then((res) => Success.of(res.data))
    .catch((e) => Failure.fromError(e))
}

async function updateUnitFeatures(
  unitIds: UUID[],
  features: PilotFeature[],
  enable: boolean
): Promise<Result<void>> {
  return client
    .post(`/daycares/unit-features`, { unitIds, features, enable })
    .then(() => Success.of())
    .catch((e) => Failure.fromError(e))
}

export default React.memo(function UnitFeaturesPage() {
  const { i18n } = useTranslation()

  const [units, reloadUnits] = useApiState(getUnitFeatures, [])

  const [submitting, setSubmitting] = useState(false)

  const [undoAction, setUndoAction] =
    useState<[UUID[], PilotFeature[], boolean]>()

  const updateFeatures = useCallback(
    async (
      unitIds: UUID[],
      features: PilotFeature[],
      enable: boolean,
      isUndoAction = false
    ) => {
      setSubmitting(true)

      const result = await updateUnitFeatures(unitIds, features, enable)
      if (!isUndoAction) {
        setUndoAction([unitIds, features, !enable])
      }

      void reloadUnits()
      setSubmitting(false)

      return result
    },
    [reloadUnits]
  )

  const [filteredProviderTypes, setFilteredProviderTypes] =
    useState<ProviderType[]>(unitProviderTypes)

  const filteredUnits = useMemo(
    () =>
      isEqual(unitProviderTypes, filteredProviderTypes)
        ? units
        : units.map((units) =>
            units.filter((unit) =>
              filteredProviderTypes.includes(unit.providerType)
            )
          ),
    [filteredProviderTypes, units]
  )

  const undo = useCallback(() => {
    if (!undoAction)
      return Promise.resolve(
        Failure.of<void>({
          message: 'No undo action'
        })
      )

    const promise = updateFeatures(
      undoAction[0],
      undoAction[1],
      undoAction[2],
      true
    )
    setUndoAction(undefined)
    return promise
  }, [undoAction, updateFeatures])

  return (
    <Container>
      <ContentArea opaque>
        <H1>{i18n.unitFeatures.page.title}</H1>
        <MultiSelect
          options={unitProviderTypes}
          getOptionId={(pt) => pt}
          placeholder={i18n.unitFeatures.page.providerType}
          getOptionLabel={(providerType) =>
            i18n.common.providerType[providerType]
          }
          value={filteredProviderTypes}
          onChange={setFilteredProviderTypes}
        />
        <Gap size="s" />
        <AsyncButton
          disabled={!undoAction}
          onClick={undo}
          onSuccess={reloadUnits}
          text={i18n.unitFeatures.page.undo}
        />
        <Gap size="s" />
        {renderResult(filteredUnits, (units) => (
          <TableScrollable>
            <Thead sticky>
              <Tr>
                <Td>{i18n.unitFeatures.page.unit}</Td>
                {pilotFeatures.map((f) => {
                  const allSelected = units.every(({ features }) =>
                    features.includes(f)
                  )

                  return (
                    <Td key={f}>
                      <div>{i18n.unitFeatures.pilotFeatures[f]}</div>
                      <Gap size="xs" />
                      <div>
                        <ButtonLink
                          onClick={() => {
                            void updateFeatures(
                              units
                                .filter(
                                  ({ features }) =>
                                    features.includes(f) === allSelected
                                )
                                .map(({ id }) => id),
                              [f],
                              !allSelected
                            )
                          }}
                        >
                          {allSelected
                            ? i18n.unitFeatures.page.unselectAll
                            : i18n.unitFeatures.page.selectAll}
                        </ButtonLink>
                      </div>
                    </Td>
                  )
                })}
              </Tr>
            </Thead>
            <Tbody>
              {units.map((unit) => (
                <Tr key={unit.id}>
                  <Td>
                    <Link to={`/units/${unit.id}`}>{unit.name}</Link>
                    <div>{i18n.common.providerType[unit.providerType]}</div>
                  </Td>
                  {pilotFeatures.map((f) => (
                    <Td key={f}>
                      <Checkbox
                        label={f}
                        hiddenLabel
                        checked={unit.features.includes(f)}
                        onChange={(value) =>
                          updateFeatures([unit.id], [f], value)
                        }
                        disabled={submitting}
                      />
                    </Td>
                  ))}
                </Tr>
              ))}
            </Tbody>
          </TableScrollable>
        ))}
      </ContentArea>
    </Container>
  )
})
