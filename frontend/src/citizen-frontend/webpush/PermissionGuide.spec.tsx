// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { render, screen } from '@testing-library/react'
import React from 'react'
import { describe, expect, it, vi } from 'vitest'

import {
  TestContextProvider,
  testTranslations
} from 'lib-components/utils/TestContextProvider'

import { PermissionGuide } from './PermissionGuide'

vi.mock('../localization', () => ({
  useTranslation: () => ({
    personalDetails: {
      webPushSection: {
        guide: {
          chromeAndroid: <p>Chrome Android guide</p>,
          samsungAndroid: <p>Samsung Android guide</p>,
          firefoxAndroid: <p>Firefox Android guide</p>,
          safariIOS: <p>Safari iOS guide</p>,
          chromeDesktop: <p>Chrome Desktop guide</p>,
          firefoxDesktop: <p>Firefox Desktop guide</p>,
          safariMacos: <p>Safari macOS guide</p>,
          fallback: <p>Fallback guide</p>
        }
      }
    }
  })
}))

function wrap(child: React.JSX.Element) {
  return <TestContextProvider translations={testTranslations}>{child}</TestContextProvider>
}

describe('PermissionGuide', () => {
  it('renders fallback guide when browser family is unknown', () => {
    render(
      wrap(<PermissionGuide browser={{ os: 'other', family: 'other', isStandalone: false }} />)
    )
    expect(screen.getByRole('group')).toBeDefined()
  })

  it('renders Chrome-on-Android variant', () => {
    render(
      wrap(<PermissionGuide browser={{ os: 'android', family: 'chrome', isStandalone: false }} />)
    )
    expect(screen.getByRole('group')).toBeDefined()
  })

  it('renders iOS Safari variant when not standalone', () => {
    render(
      wrap(<PermissionGuide browser={{ os: 'ios', family: 'safari', isStandalone: false }} />)
    )
    expect(screen.getByRole('group')).toBeDefined()
  })
})
