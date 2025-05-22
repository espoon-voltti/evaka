// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { FontAwesomeIcon } from '@fortawesome/react-fontawesome'
import React from 'react'
import styled, { useTheme } from 'styled-components'

import { tabletMin } from 'lib-components/breakpoints'
import ListGrid from 'lib-components/layout/ListGrid'
import { FixedSpaceRow } from 'lib-components/layout/flex-helpers'
import { fontWeights, Label } from 'lib-components/typography'
import { defaultMargins, Gap } from 'lib-components/white-space'
import { fasExclamationTriangle } from 'lib-icons'

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
    <FixedSpaceRow aria-invalid={showError}>
      <Label>{label}</Label>
      {showError ? <LabelError text={errorText} /> : null}
    </FixedSpaceRow>
  )
})

export interface IncomeStatementFormAPI {
  scrollToErrors: () => void
}

export const ActionContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  flex-wrap: wrap;
  padding: ${defaultMargins.s} 0;
  background-color: ${(p) => p.theme.colors.grayscale.g0};

  > * {
    margin: 0 ${defaultMargins.m};

    &:not(:first-child) {
      margin-top: ${defaultMargins.s};
    }
  }

  @media (min-width: ${tabletMin}) {
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

export const Row = React.memo(function Row({
  label,
  light,
  value,
  translate,
  'data-qa': dataQa
}: {
  label: string
  light?: boolean
  value: React.ReactNode
  translate?: 'yes' | 'no'
  'data-qa'?: string
}) {
  return (
    <>
      <ListGrid>
        <LabelColumn light={light}>{label}</LabelColumn>
        <div translate={translate} data-qa={dataQa}>
          {value}
        </div>
      </ListGrid>
      <Gap size="s" />
    </>
  )
})

const LabelColumn = styled(Label)<{ light?: boolean }>`
  flex: 0 0 auto;
  width: 250px;
  ${(p) => (p.light ? 'font-weight: 400;' : '')}
`
