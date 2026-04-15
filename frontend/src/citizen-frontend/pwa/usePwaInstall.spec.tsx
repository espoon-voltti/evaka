// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { act, renderHook } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

type UsePwaInstall = typeof import('./usePwaInstall').usePwaInstall

class MockBeforeInstallPromptEvent extends Event {
  prompt = vi.fn().mockResolvedValue(undefined)
  userChoice: Promise<{ outcome: 'accepted' | 'dismissed'; platform: string }>

  constructor(outcome: 'accepted' | 'dismissed' = 'accepted') {
    super('beforeinstallprompt', { cancelable: true })
    this.userChoice = Promise.resolve({ outcome, platform: 'web' })
  }
}

const setMatchMediaStandalone = (matches: boolean) => {
  Object.defineProperty(window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: vi.fn().mockImplementation((query: string) => ({
      matches: query === '(display-mode: standalone)' ? matches : false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn()
    }))
  })
}

describe('usePwaInstall', () => {
  let usePwaInstall: UsePwaInstall

  beforeEach(async () => {
    // The hook keeps `appInstalled` / `deferredPrompt` / `captured` at module
    // scope so that a `beforeinstallprompt` event fired before any consumer
    // mounts still reaches the first hook that shows up. That shared state
    // also leaks between test cases (e.g. the 'on accept' test sets
    // appInstalled=true), so reset the module between tests and re-import.
    vi.resetModules()
    setMatchMediaStandalone(false)
    Object.defineProperty(navigator, 'userAgent', {
      writable: true,
      configurable: true,
      value:
        'Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1'
    })
    usePwaInstall = (await import('./usePwaInstall')).usePwaInstall
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('starts in fallback state with the detected platform', () => {
    const { result } = renderHook(() => usePwaInstall())
    expect(result.current.kind).toBe('fallback')
    if (result.current.kind === 'fallback') {
      expect(result.current.platform.os).toBe('ios')
    }
  })

  it('starts in standalone state when display-mode: standalone matches at mount', () => {
    setMatchMediaStandalone(true)
    const { result } = renderHook(() => usePwaInstall())
    expect(result.current.kind).toBe('standalone')
  })

  it('transitions to native state when beforeinstallprompt fires', () => {
    const { result } = renderHook(() => usePwaInstall())
    expect(result.current.kind).toBe('fallback')

    const event = new MockBeforeInstallPromptEvent()
    act(() => {
      window.dispatchEvent(event)
    })

    expect(result.current.kind).toBe('native')
  })

  it('promptInstall on accept transitions to standalone', async () => {
    const { result } = renderHook(() => usePwaInstall())

    const event = new MockBeforeInstallPromptEvent('accepted')
    act(() => {
      window.dispatchEvent(event)
    })

    expect(result.current.kind).toBe('native')

    await act(async () => {
      if (result.current.kind === 'native') {
        await result.current.promptInstall()
      }
    })

    expect(event.prompt).toHaveBeenCalledOnce()
    expect(result.current.kind).toBe('standalone')
  })

  it('promptInstall on dismiss transitions back to fallback', async () => {
    const { result } = renderHook(() => usePwaInstall())

    const event = new MockBeforeInstallPromptEvent('dismissed')
    act(() => {
      window.dispatchEvent(event)
    })

    expect(result.current.kind).toBe('native')

    await act(async () => {
      if (result.current.kind === 'native') {
        await result.current.promptInstall()
      }
    })

    expect(result.current.kind).toBe('fallback')
    if (result.current.kind === 'fallback') {
      expect(result.current.platform.os).toBe('ios')
    }
  })

  it('transitions to standalone when appinstalled fires', () => {
    const { result } = renderHook(() => usePwaInstall())

    act(() => {
      window.dispatchEvent(new Event('appinstalled'))
    })

    expect(result.current.kind).toBe('standalone')
  })
})
