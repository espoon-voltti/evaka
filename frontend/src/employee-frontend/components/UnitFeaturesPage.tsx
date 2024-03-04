// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import React, { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import { useTranslation } from 'employee-frontend/state/i18n'
import { Failure, wrapResult } from 'lib-common/api'
import {
  CareType,
  careTypes,
  ProviderType
} from 'lib-common/generated/api-types/daycare'
import {
  PilotFeature,
  pilotFeatures
} from 'lib-common/generated/api-types/shared'
import { UUID } from 'lib-common/types'
import { useApiState } from 'lib-common/utils/useRestApi'
import AsyncButton from 'lib-components/atoms/buttons/AsyncButton'
import { ButtonLink } from 'lib-components/atoms/buttons/ButtonLink'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { Container, ContentArea } from 'lib-components/layout/Container'
import { Tbody, Td, Thead, Tr } from 'lib-components/layout/Table'
import { H1, LabelLike } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { unitProviderTypes } from 'lib-customizations/employee'

import {
  getUnitFeatures,
  updateUnitFeatures
} from '../generated/api-clients/daycare'

import { renderResult } from './async-rendering'
import { TableScrollable } from './reports/common'

const getUnitFeaturesResult = wrapResult(getUnitFeatures)
const updateUnitFeaturesResult = wrapResult(updateUnitFeatures)

export default React.memo(function UnitFeaturesPage() {
  const { i18n } = useTranslation()

  const [units, reloadUnits] = useApiState(getUnitFeaturesResult, [])

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

      const result = await updateUnitFeaturesResult({
        body: { unitIds, features, enable }
      })
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
  const [filteredCareTypes, setFilteredCareTypes] = useState<CareType[]>(
    Array.from(careTypes)
  )

  const filteredUnits = useMemo(
    () =>
      isEqual(unitProviderTypes, filteredProviderTypes) &&
      isEqual(careTypes, filteredCareTypes)
        ? units
        : units.map((units) =>
            units.filter(
              (unit) =>
                filteredProviderTypes.includes(unit.providerType) &&
                unit.type.some((type) => filteredCareTypes.includes(type))
            )
          ),
    [filteredProviderTypes, filteredCareTypes, units]
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

        <LabelLike>{i18n.unitFeatures.page.providerType}</LabelLike>
        <Gap size="xs" />
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

        <Gap size="xs" />

        <LabelLike>{i18n.unitFeatures.page.careType}</LabelLike>
        <Gap size="xs" />
        <MultiSelect
          options={careTypes}
          getOptionId={(pt) => pt}
          placeholder={i18n.unitFeatures.page.careType}
          getOptionLabel={(providerType) => i18n.common.types[providerType]}
          value={filteredCareTypes}
          onChange={setFilteredCareTypes}
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
