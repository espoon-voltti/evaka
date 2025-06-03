// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useMemo } from 'react'
import styled, { useTheme } from 'styled-components'

import type { BoundFormState } from 'lib-common/form/hooks'
import { useBoolean } from 'lib-common/form/hooks'
import type { ReservationChild } from 'lib-common/generated/api-types/reservations'
import { formatPersonName } from 'lib-common/names'
import { SelectionChip } from 'lib-components/atoms/Chip'
import { StatusIcon } from 'lib-components/atoms/StatusIcon'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap
} from 'lib-components/layout/flex-helpers'
import { defaultMargins } from 'lib-components/white-space'
import { fasExclamationTriangle } from 'lib-icons'

import { useTranslation } from '../localization'
import { getDuplicateChildInfo } from '../utils/duplicated-child-utils'

interface ChildSelectorProps {
  childItems: ReservationChild[]
  bind: BoundFormState<ReservationChild[]>
}

export default React.memo(function ChildSelector({
  childItems,
  bind
}: ChildSelectorProps) {
  const t = useTranslation()

  const [isChildSelectionTouched, setChildSelectionTouched] = useBoolean(false)
  const duplicateChildInfo = useMemo(
    () => getDuplicateChildInfo(childItems, t),
    [childItems, t]
  )

  const chipGroupContainerRef = React.useRef<HTMLDivElement>(null)

  return (
    <FixedSpaceColumn>
      <FixedSpaceFlexWrap ref={chipGroupContainerRef}>
        {childItems.map((child) => (
          <div key={child.id} data-qa="relevant-child">
            <SelectionChip
              key={child.id}
              text={`${formatPersonName(child, 'FirstFirst')}${
                duplicateChildInfo[child.id] !== undefined
                  ? ` ${duplicateChildInfo[child.id]}`
                  : ''
              }`}
              translate="no"
              selected={bind.state.some(({ id }) => id === child.id)}
              onChange={(selected) => {
                setChildSelectionTouched.on()
                bind.update((prev) =>
                  selected
                    ? [...prev, child]
                    : prev.filter(({ id }) => id !== child.id)
                )
              }}
              onBlur={(e) => {
                const focusTargetOutsideThisSelector =
                  chipGroupContainerRef.current &&
                  !chipGroupContainerRef.current.contains(e.relatedTarget)

                if (focusTargetOutsideThisSelector) {
                  setChildSelectionTouched.on()
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
