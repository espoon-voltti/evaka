// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import React from 'react'
import { describe, expect, it, vi } from 'vitest'

import { PwaInstallButton } from './PwaInstallButton'
import { usePwaInstall, type PwaInstallState } from './usePwaInstall'

vi.mock('./usePwaInstall', async () => {
  const actual =
    await vi.importActual<typeof import('./usePwaInstall')>('./usePwaInstall')
  return {
    ...actual,
    usePwaInstall: vi.fn()
  }
})

vi.mock('../localization', () => ({
  useTranslation: () => ({
    common: { openExpandingInfo: 'open' },
    loginPage: {
      pwaInstall: {
        button: 'Add to home screen',
        iosUseSafariNote: 'Open in Safari',
        notSupported: 'Not supported',
        instructions: {
          ios: <div data-qa="ios-instructions">iOS steps</div>,
          android: <div data-qa="android-instructions">Android steps</div>
        }
      }
    }
  })
}))

const mockedUsePwaInstall = vi.mocked(usePwaInstall)

const setState = (state: PwaInstallState) => {
  mockedUsePwaInstall.mockReturnValue(state)
}

describe('PwaInstallButton', () => {
  it('renders nothing when running standalone', () => {
    setState({ kind: 'standalone' })
    const { container } = render(<PwaInstallButton />)
    expect(container).toBeEmptyDOMElement()
  })

  it('renders a button that calls promptInstall in native state', async () => {
    const promptInstall = vi.fn().mockResolvedValue(undefined)
    setState({ kind: 'native', promptInstall })

    render(<PwaInstallButton />)

    const button = screen.getByRole('button', { name: 'Add to home screen' })
    await userEvent.click(button)

    expect(promptInstall).toHaveBeenCalledOnce()
  })

  it('shows only iOS instructions in fallback state on iOS Safari', async () => {
    setState({
      kind: 'fallback',
      platform: { os: 'ios', isSafari: true }
    })

    render(<PwaInstallButton />)
    await userEvent.click(
      screen.getByRole('button', { name: 'Add to home screen' })
    )

    expect(screen.getByTestId('ios-instructions')).toBeInTheDocument()
    expect(screen.queryByTestId('android-instructions')).not.toBeInTheDocument()
    expect(screen.queryByText('Open in Safari')).not.toBeInTheDocument()
  })

  it('shows iOS instructions plus a "use Safari" note on iOS non-Safari', async () => {
    setState({
      kind: 'fallback',
      platform: { os: 'ios', isSafari: false }
    })

    render(<PwaInstallButton />)
    await userEvent.click(
      screen.getByRole('button', { name: 'Add to home screen' })
    )

    expect(screen.getByTestId('ios-instructions')).toBeInTheDocument()
    expect(screen.getByText('Open in Safari')).toBeInTheDocument()
  })

  it('shows only Android instructions in fallback state on Android', async () => {
    setState({ kind: 'fallback', platform: { os: 'android' } })

    render(<PwaInstallButton />)
    await userEvent.click(
      screen.getByRole('button', { name: 'Add to home screen' })
    )

    expect(screen.getByTestId('android-instructions')).toBeInTheDocument()
    expect(screen.queryByTestId('ios-instructions')).not.toBeInTheDocument()
  })

  it('shows the not-supported message in fallback state on other platforms', async () => {
    setState({ kind: 'fallback', platform: { os: 'other' } })

    render(<PwaInstallButton />)
    await userEvent.click(
      screen.getByRole('button', { name: 'Add to home screen' })
    )

    expect(screen.getByText('Not supported')).toBeInTheDocument()
    expect(screen.queryByTestId('ios-instructions')).not.toBeInTheDocument()
    expect(screen.queryByTestId('android-instructions')).not.toBeInTheDocument()
  })
})
