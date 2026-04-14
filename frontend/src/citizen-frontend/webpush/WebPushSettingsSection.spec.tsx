// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import React from 'react'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  TestContextProvider,
  testTranslations
} from 'lib-components/utils/TestContextProvider'

import { WebPushSettingsSection } from './WebPushSettingsSection'
import { useWebPushState, type UseWebPushStateResult } from './webpush-state'

vi.mock('./webpush-state')

vi.mock('./PermissionGuide', () => ({
  PermissionGuide: () => <div data-testid="webpush-permission-guide" />
}))

vi.mock('../pwa/detectPlatform', () => ({
  detectBrowser: () => ({ os: 'other', family: 'other', isStandalone: false })
}))

vi.mock('../localization', () => ({
  useTranslation: () => ({
    personalDetails: {
      webPushSection: {
        title: 'Push notifications',
        info: <p>Enable browser push notifications for new messages.</p>,
        enable: 'Enable push notifications',
        enabling: 'Enabling…',
        enabled: 'Push notifications enabled',
        categoryUrgent: {
          label: 'Urgent messages',
          description: 'Urgent messages from staff'
        },
        categoryMessage: {
          label: 'Normal messages',
          description: 'New messages and replies'
        },
        categoryBulletin: {
          label: 'Bulletins',
          description: 'General bulletins'
        },
        sendTest: 'Send test notification',
        testSent: 'Test notification sent',
        testFailed: 'Test notification failed',
        unsupported: 'Not supported in this browser.',
        denied: 'Push notifications are blocked.',
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

function setState(state: UseWebPushStateResult) {
  vi.mocked(useWebPushState).mockReturnValue(state)
}

const baseActions = {
  subscribe: vi.fn().mockResolvedValue(undefined),
  updateCategories: vi.fn().mockResolvedValue(undefined),
  unsubscribe: vi.fn().mockResolvedValue(undefined),
  sendTest: vi.fn().mockResolvedValue(undefined)
}

describe('WebPushSettingsSection', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('shows the master toggle disabled when unsupported', () => {
    setState({
      ...baseActions,
      status: 'unsupported',
      categories: new Set(['MESSAGE'])
    })
    render(wrap(<WebPushSettingsSection />))
    expect(
      screen.getByRole('checkbox', { name: /push notifications/i })
    ).toBeDisabled()
  })

  it('shows the guide when denied', () => {
    setState({
      ...baseActions,
      status: 'denied',
      categories: new Set(['MESSAGE'])
    })
    render(wrap(<WebPushSettingsSection />))
    expect(screen.getByTestId('webpush-permission-guide')).toBeDefined()
  })

  it('calls subscribe when the master toggle flips on', async () => {
    setState({
      ...baseActions,
      status: 'unregistered',
      categories: new Set(['URGENT_MESSAGE', 'MESSAGE', 'BULLETIN'])
    })
    render(wrap(<WebPushSettingsSection />))
    const toggle = screen.getByRole('checkbox', { name: /push notifications/i })
    await userEvent.click(toggle)
    expect(baseActions.subscribe).toHaveBeenCalledTimes(1)
  })

  it('disables category checkboxes when not subscribed', () => {
    setState({
      ...baseActions,
      status: 'unregistered',
      categories: new Set(['URGENT_MESSAGE', 'MESSAGE', 'BULLETIN'])
    })
    render(wrap(<WebPushSettingsSection />))
    expect(screen.getByRole('checkbox', { name: /urgent/i })).toBeDisabled()
    expect(screen.getByRole('checkbox', { name: /bulletin/i })).toBeDisabled()
  })

  it('enables category checkboxes and the test button when subscribed', () => {
    setState({
      ...baseActions,
      status: 'subscribed',
      categories: new Set(['MESSAGE'])
    })
    render(wrap(<WebPushSettingsSection />))
    expect(screen.getByRole('checkbox', { name: /normal messages/i })).not.toBeDisabled()
    expect(screen.getByRole('button', { name: /send test/i })).not.toBeDisabled()
  })

  it('calls sendTest when the test button is clicked', async () => {
    setState({
      ...baseActions,
      status: 'subscribed',
      categories: new Set(['MESSAGE'])
    })
    render(wrap(<WebPushSettingsSection />))
    await userEvent.click(screen.getByRole('button', { name: /send test/i }))
    expect(baseActions.sendTest).toHaveBeenCalledTimes(1)
  })
})
