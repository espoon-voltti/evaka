// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import styled from 'styled-components'

import type {
  ChildInfo,
  GuardianInfo
} from 'lib-common/generated/api-types/application'
import type { DecisionUnit } from 'lib-common/generated/api-types/decision'
import type { DaycareId } from 'lib-common/generated/api-types/shared'
import { formatPersonName } from 'lib-common/names'
import Combobox from 'lib-components/atoms/dropdowns/Combobox'
import { fontWeights, H2 } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import colors from 'lib-customizations/common'

import { useTranslation } from '../../state/i18n'

const Card = styled.div`
  background: ${colors.grayscale.g4};
  padding: ${defaultMargins.m};
  border-radius: 4px;
`

const ChildHeading = styled.div`
  margin: 0 0 ${defaultMargins.m} 0;
`

const Grid = styled.div`
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: ${defaultMargins.m} ${defaultMargins.L};
`

const Field = styled.div`
  display: flex;
  flex-direction: column;
  gap: ${defaultMargins.xs};
`

const Label = styled.span`
  font-weight: ${fontWeights.semibold};
`

const InlineSummary = styled.div`
  color: ${colors.grayscale.g70};
  font-size: 0.85em;
  margin-top: ${defaultMargins.xxs};
`

interface Props {
  child: ChildInfo
  guardian: GuardianInfo
  otherGuardian: GuardianInfo | null
  placementUnitName: string
  units: DecisionUnit[]
  selectedUnitId: DaycareId | null
  onSelectUnit: (unitId: DaycareId) => void
}

export default React.memo(function ApplicationHeaderCard({
  child,
  guardian,
  otherGuardian,
  placementUnitName,
  units,
  selectedUnitId,
  onSelectUnit
}: Props) {
  const { i18n } = useTranslation()
  const selectedUnit = units.find((u) => u.id === selectedUnitId) ?? null
  const recipients = [
    formatPersonName(guardian, 'First Last'),
    otherGuardian ? formatPersonName(otherGuardian, 'First Last') : ''
  ]
    .filter((s) => s.length > 0)
    .join(', ')

  return (
    <Card>
      <ChildHeading>
        <H2 $noMargin>{formatPersonName(child, 'First Last')}</H2>
      </ChildHeading>
      <Grid>
        <Field>
          <Label>{i18n.decisionDraft.receiver}</Label>
          <span>{recipients}</span>
        </Field>
        <Field>
          <Label>{i18n.decisionDraft.placementUnit}</Label>
          <span>{placementUnitName}</span>
        </Field>
        <Field>
          {selectedUnit && (
            <>
              <Label>{i18n.decisionDraft.handler}</Label>
              <span>{selectedUnit.decisionHandler}</span>
              <span>{selectedUnit.decisionHandlerAddress}</span>
            </>
          )}
        </Field>
        <Field>
          <Label>{i18n.decisionDraft.selectedUnit}</Label>
          <Combobox
            items={units}
            selectedItem={selectedUnit}
            onChange={(u) => u && onSelectUnit(u.id)}
            getItemLabel={(u) => u?.name ?? ''}
            getItemDataQa={(u) => u?.id ?? ''}
            data-qa="header-unit-selector"
          />
          {selectedUnit && (
            <InlineSummary>
              {i18n.decisionDraft.unitInlineSummary(
                `${selectedUnit.streetAddress}, ${selectedUnit.postalCode} ${selectedUnit.postOffice}`,
                selectedUnit.manager ?? ''
              )}
            </InlineSummary>
          )}
        </Field>
      </Grid>
    </Card>
  )
})
