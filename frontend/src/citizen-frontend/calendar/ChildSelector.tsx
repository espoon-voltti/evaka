// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import styled, { useTheme } from 'styled-components'

import { getDuplicateChildInfo } from 'citizen-frontend/utils/duplicated-child-utils'
import { BoundFormState, useBoolean } from 'lib-common/form/hooks'
import { ReservationChild } from 'lib-common/generated/api-types/reservations'
import { formatFirstName } from 'lib-common/names'
import { UUID } from 'lib-common/types'
import { SelectionChip } from 'lib-components/atoms/Chip'
import { StatusIcon } from 'lib-components/atoms/StatusIcon'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap
} from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import { fasExclamationTriangle } from 'lib-icons'

import { useTranslation } from '../localization'

interface ChildSelectorProps {
  childItems: ReservationChild[]
  bind: BoundFormState<UUID[]>
}

export default React.memo(function ChildSelector({
  childItems,
  bind
}: ChildSelectorProps) {
  const t = useTranslation()

  const [isChildSelectionTouched, childSelectionMarker] = useBoolean(false)
  const duplicateChildInfo = useMemo(
    () => getDuplicateChildInfo(childItems, t),
    [childItems, t]
  )

  return (
    <FixedSpaceColumn>
      <FixedSpaceFlexWrap id="child-chip-container">
        {childItems.map((child) => (
          <div key={child.id} data-qa="relevant-child">
            <SelectionChip
              key={child.id}
              text={`${formatFirstName(child)}${
                duplicateChildInfo[child.id] !== undefined
                  ? ` ${duplicateChildInfo[child.id]}`
                  : ''
              }`}
              translate="no"
              selected={bind.state.includes(child.id)}
              onChange={(selected) => {
                childSelectionMarker.on()
                bind.update((prev) =>
                  selected
                    ? [...prev, child.id]
                    : prev.filter((id) => id !== child.id)
                )
              }}
              onBlur={(e) => {
                const chipGroupContainer = document.getElementById(
                  'child-chip-container'
                )
                if (
                  chipGroupContainer &&
                  !chipGroupContainer.contains(e.relatedTarget)
                ) {
                  childSelectionMarker.on()
                }
              }}
              data-qa={`child-${child.id}`}
            />
          </div>
        ))}
      </FixedSpaceFlexWrap>
      {isChildSelectionTouched && !bind.isValid() && (
        <ErrorMessageBox text={t.calendar.childSelectionMissingError} />
      )}
    </FixedSpaceColumn>
  )
})

export const ErrorMessageBox = React.memo(function ErrorMessageBox({
  text
}: {
  text: string
}) {
  const { colors } = useTheme()
  return (
    <ErrorBox role="alert">
      {text}
      <StatusIcon icon={fasExclamationTriangle} color={colors.status.warning} />
    </ErrorBox>
  )
})
export const ErrorBox = styled.div`
  color: ${(p) => p.theme.colors.status.warning};
  margin-top: ${defaultMargins.s};
`
