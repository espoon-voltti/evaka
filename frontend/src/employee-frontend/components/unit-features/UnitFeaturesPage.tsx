// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import isEqual from 'lodash/isEqual'
import React, { useMemo, useState } from 'react'
import styled from 'styled-components'
import { Link } from 'wouter'

import type {
  CareType,
  ProviderType,
  UpdateFeaturesRequest
} from 'lib-common/generated/api-types/daycare'
import { careTypes } from 'lib-common/generated/api-types/daycare'
import { pilotFeatures } from 'lib-common/generated/api-types/shared'
import { cancelMutation, useMutation, useQueryResult } from 'lib-common/query'
import { MutateButton } from 'lib-components/atoms/buttons/MutateButton'
import Checkbox from 'lib-components/atoms/form/Checkbox'
import MultiSelect from 'lib-components/atoms/form/MultiSelect'
import { ContentArea } from 'lib-components/layout/Container'
import { Table, Tbody, Td, Thead, Tr } from 'lib-components/layout/Table'
import { H1, LabelLike } from 'lib-components/typography'
import { Gap } from 'lib-components/white-space'
import { unitProviderTypes } from 'lib-customizations/employee'

import { useTranslation } from '../../state/i18n'
import { renderResult } from '../async-rendering'

import { unitFeaturesQuery, updateUnitFeaturesMutation } from './queries'

const StyledContentArea = styled(ContentArea)`
  display: flex;
  flex-direction: column;
  max-height: calc(100vh - 88px + 32px);
`
// 88 px = header height, 32 px = ContentArea paddingBottom -> horizontal scroll bar always at the bottom of the screen

const TableContainer = styled.div`
  overflow: auto;
`

export default React.memo(function UnitFeaturesPage() {
  const { i18n } = useTranslation()

  const units = useQueryResult(unitFeaturesQuery())
  const { mutateAsync: updateUnitFeatures, isPending } = useMutation(
    updateUnitFeaturesMutation
  )

  const [undoAction, setUndoAction] = useState<UpdateFeaturesRequest>()

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

  return (
    <div>
      <StyledContentArea opaque>
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

        <MutateButton
          disabled={!undoAction}
          mutation={updateUnitFeaturesMutation}
          onClick={() =>
            undoAction
              ? {
                  body: undoAction
                }
              : cancelMutation
          }
          onSuccess={() => setUndoAction(undefined)}
          text={i18n.unitFeatures.page.undo}
        />
        <Gap size="s" />
        <TableContainer>
          {renderResult(filteredUnits, (units) => (
            <Table>
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
                          <MutateButton
                            appearance="link"
                            mutation={updateUnitFeaturesMutation}
                            onClick={() => {
                              const request = {
                                unitIds: units
                                  .filter(
                                    ({ features }) =>
                                      features.includes(f) === allSelected
                                  )
                                  .map(({ id }) => id),
                                features: [f],
                                enable: !allSelected
                              }
                              setUndoAction({
                                ...request,
                                enable: !request.enable
                              })
                              return {
                                body: request
                              }
                            }}
                            text={
                              allSelected
                                ? i18n.unitFeatures.page.unselectAll
                                : i18n.unitFeatures.page.selectAll
                            }
                          />
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
                          onChange={(value) => {
                            const request = {
                              unitIds: [unit.id],
                              features: [f],
                              enable: value
                            }
                            setUndoAction({
                              ...request,
                              enable: !request.enable
                            })
                            void updateUnitFeatures({ body: request })
                          }}
                          disabled={isPending}
                        />
                      </Td>
                    ))}
                  </Tr>
                ))}
              </Tbody>
            </Table>
          ))}
        </TableContainer>
      </StyledContentArea>
    </div>
  )
})
