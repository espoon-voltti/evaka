// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React, { useCallback, useMemo } from 'react'
import styled, { useTheme } from 'styled-components'

import type { BoundFormState } from 'lib-common/form/hooks'
import { useBoolean } from 'lib-common/form/hooks'
import type { ReservationChild } from 'lib-common/generated/api-types/reservations'
import type LocalDate from 'lib-common/local-date'
import { formatPersonName } from 'lib-common/names'
import { SelectionChip } from 'lib-components/atoms/Chip'
import { StatusIcon } from 'lib-components/atoms/StatusIcon'
import {
  FixedSpaceColumn,
  FixedSpaceFlexWrap
} from 'lib-components/layout/flex-helpers'
import { InfoBox } from 'lib-components/molecules/MessageBoxes'
import { defaultMargins } from 'lib-components/white-space'
import { fasExclamationTriangle } from 'lib-icons'

import { useTranslation } from '../localization'
import { getDuplicateChildInfo } from '../utils/duplicated-child-utils'

interface ChildSelectorProps {
  childItems: ReservationChild[]
  bind: BoundFormState<ReservationChild[]>
  rangeEnd?: LocalDate
}

const useChildPlacementValidation = (rangeEnd?: LocalDate) => {
  return useCallback(
    (child: ReservationChild) => {
      return (
        (rangeEnd &&
          child.upcomingPlacementStartDate !== null &&
          child.upcomingPlacementStartDate.isAfter(rangeEnd)) ??
        false
      )
    },
    [rangeEnd]
  )
}

export default React.memo(function ChildSelector({
  childItems,
  bind,
  rangeEnd
}: ChildSelectorProps) {
  const t = useTranslation()

  const [isChildSelectionTouched, setChildSelectionTouched] = useBoolean(false)
  const duplicateChildInfo = useMemo(
    () => getDuplicateChildInfo(childItems, t),
    [childItems, t]
  )

  const chipGroupContainerRef = React.useRef<HTMLDivElement>(null)

  const isPlacementNotStarted = useChildPlacementValidation(rangeEnd)

  return (
    <FixedSpaceColumn>
      <FixedSpaceFlexWrap ref={chipGroupContainerRef}>
        {childItems.map((child) => {
          const isDisabled = isPlacementNotStarted(child)

          return (
            <div key={child.id} data-qa="relevant-child">
              <SelectionChip
                disabled={isDisabled}
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
          )
        })}
      </FixedSpaceFlexWrap>
      {isChildSelectionTouched && !bind.isValid() && (
        <ErrorMessageBox text={t.calendar.childSelectionMissingError} />
      )}
      {childItems.map((child) =>
        isPlacementNotStarted(child) ? (
          <InfoBoxWrapper
            key={child.id}
            data-qa={`child-not-started-infobox-${child.id}`}
            className="child-selector-infobox"
          >
            <InfoBox
              noMargin
              key={child.id}
              message={t.calendar.infoToast.childStartsInfo(
                formatPersonName(child, 'FirstFirst'),
                child.upcomingPlacementStartDate
                  ? child.upcomingPlacementStartDate.format()
                  : '',
                child.upcomingPlacementUnitName ?? '',
                child.upcomingPlacementCalendarOpenDate
                  ? child.upcomingPlacementCalendarOpenDate.format()
                  : ''
              )}
            />
          </InfoBoxWrapper>
        ) : null
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
  margin-bottom: ${defaultMargins.zero};
`
const InfoBoxWrapper = styled.div`
  margin-top: ${defaultMargins.s};
  margin-bottom: ${defaultMargins.zero};
`
