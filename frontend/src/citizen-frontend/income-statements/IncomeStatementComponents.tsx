// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React, { useCallback } from 'react'
import styled, { useTheme } from 'styled-components'

import { tabletMin } from 'lib-components/breakpoints'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights, Label } from 'lib-components/typography'
import { defaultMargins } from 'lib-components/white-space'
import { fasExclamationTriangle } from 'lib-icons'

export type SetStateCallback<T> = (fn: (prev: T) => T) => void

export function useFieldSetState<T, K extends keyof T>(
  onChange: SetStateCallback<T>,
  key: K
): SetStateCallback<T[K]> {
  return useCallback<SetStateCallback<T[K]>>(
    (fn) => onChange((prev) => ({ ...prev, [key]: fn(prev[key]) })),
    [onChange, key]
  )
}

export function useFieldDispatch<T, K extends keyof T>(
  onChange: SetStateCallback<T>,
  key: K
): (value: T[K]) => void {
  const setState = useFieldSetState(onChange, key)
  return useCallback((value: T[K]) => setState(() => value), [setState])
}

export const LabelWithError = React.memo(function LabelWithError({
  label,
  showError,
  errorText
}: {
  label: string
  showError: boolean
  errorText: string
}) {
  return (
    <FixedSpaceRow>
      <Label>{label}</Label>
      {showError ? <LabelError text={errorText} /> : null}
    </FixedSpaceRow>
  )
})

export function identity<T>(value: T): T {
  return value
}

export interface IncomeStatementFormAPI {
  scrollToErrors: () => void
}

export const ActionContainer = styled.div`
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: ${defaultMargins.s} 0;
  background-color: ${(p) => p.theme.colors.grayscale.g0};

  > * {
    margin: 0 ${defaultMargins.m};

    &:not(:first-child) {
      margin-top: ${defaultMargins.s};
    }
  }

  @media (min-width: ${tabletMin}) {
    flex-direction: row;

    > * {
      margin: ${defaultMargins.s} ${defaultMargins.m};
    }
  }
`
export const AssureCheckbox = styled.div`
  display: flex;
  align-items: center;
`

export const LabelError = styled(
  React.memo(function LabelError({
    text,
    className
  }: {
    text: string
    className?: string
  }) {
    const { colors } = useTheme()
    return (
      <span className={className}>
        <FontAwesomeIcon
          icon={fasExclamationTriangle}
          color={colors.status.warning}
        />
        {text}
      </span>
    )
  })
)`
  font-size: 14px;
  font-weight: ${fontWeights.semibold};
  color: ${(p) => p.theme.colors.accents.a2orangeDark};

  > :first-child {
    margin-right: ${defaultMargins.xs};
  }
`
