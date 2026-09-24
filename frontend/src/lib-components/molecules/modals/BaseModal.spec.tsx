// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { render, screen } from '@testing-library/react'
import React from 'react'
import { ThemeProvider } from 'styled-components'
import { describe, expect, it } from 'vitest'

import { theme } from 'lib-customizations/common'

import BaseModal from './BaseModal'

const wrap = (el: React.ReactElement) =>
  render(<ThemeProvider theme={theme}>{el}</ThemeProvider>)

describe('BaseModal accessible name', () => {
  it('uses the title when there is one', () => {
    wrap(
      <BaseModal title="Poissaolo" close={() => undefined} closeLabel="Sulje">
        <div>body</div>
      </BaseModal>
    )
    expect(screen.getByRole('dialog', { name: 'Poissaolo' })).toBeTruthy()
  })

  it('uses aria-label when there is no title', () => {
    wrap(
      <BaseModal
        aria-label="Poissaolon merkintä"
        close={() => undefined}
        closeLabel="Sulje"
      >
        <div>body</div>
      </BaseModal>
    )
    expect(
      screen.getByRole('dialog', { name: 'Poissaolon merkintä' })
    ).toBeTruthy()
  })
})
