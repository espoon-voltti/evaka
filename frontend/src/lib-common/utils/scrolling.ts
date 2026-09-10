// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { useEffect, useRef } from 'react'
import type { RefObject } from 'react'

import { isAutomatedTest } from './helpers'

type ScrollContainerResolver = () => HTMLElement | null

/**
 * Registers how to find the element that scrolls the page in place of the
 * window. The page-level scroll helpers below ask the resolver on every call,
 * because which element scrolls can depend on the viewport width. Without a
 * resolver, or when it returns null, they scroll the window.
 */
let resolveScrollContainer: ScrollContainerResolver = () => null

const scrollsItsContent = (el: HTMLElement) =>
  /auto|scroll/.test(getComputedStyle(el).overflowY)

// Which of the two scrolls depends on the viewport width, so the choice is
// made on every call rather than once
export function useRegisterScrollContainer() {
  const shellRef = useRef<HTMLDivElement>(null)
  const scrollAreaRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    resolveScrollContainer = () =>
      [scrollAreaRef.current, shellRef.current].find(
        (el): el is HTMLDivElement => !!el && scrollsItsContent(el)
      ) ?? null
    return () => {
      resolveScrollContainer = () => null
    }
  }, [])
  return { shellRef, scrollAreaRef }
}

export function scrollToPos(options: ScrollToOptions, timeout = 0) {
  scrollWithTimeout(() => options, timeout)
}

export function scrollElementToPos(
  element: HTMLElement | null,
  options: ScrollToOptions,
  timeout = 0
) {
  scrollWithTimeout(() => options, timeout, element)
}

export function scrollToTop(timeout = 0) {
  scrollWithTimeout(() => ({ top: 0, left: 0 }), timeout)
}

export function scrollToRef(ref: RefObject<HTMLElement | null>, timeout = 0) {
  scrollWithTimeout(
    () =>
      ref.current ? { top: getScrollOffsetPosition(ref.current) } : undefined,
    timeout
  )
}

/** Scrolls the page so that the element's top edge lands `offset` px below the top of the scrolling area */
export function scrollToElementTop(
  element: HTMLElement,
  offset: number,
  behavior: ScrollBehavior,
  timeout = 0
) {
  scrollWithTimeout(
    () => ({ top: getScrollOffsetPosition(element) - offset, behavior }),
    timeout
  )
}

export function scrollToElement(
  element: HTMLElement,
  timeout = 0,
  blockPosition: ScrollLogicalPosition = 'start'
) {
  scrollIntoViewWithTimeout(() => element, timeout, blockPosition)
}

export function scrollRefIntoView(
  ref: RefObject<HTMLElement | null>,
  timeout = 0,
  blockPosition: ScrollLogicalPosition = 'start'
) {
  scrollIntoViewWithTimeout(
    () => ref.current ?? undefined,
    timeout,
    blockPosition
  )
}

export function scrollIntoViewSoftKeyboard(
  target: Element,
  blockPosition: ScrollLogicalPosition = 'center'
) {
  const onResize = () => {
    target.scrollIntoView({
      block: blockPosition
    })
    window.visualViewport?.removeEventListener('resize', onResize)
  }

  window.visualViewport?.addEventListener('resize', onResize)
  setTimeout(() => {
    window.visualViewport?.removeEventListener('resize', onResize)
  }, 1000)
}

function scrollWithTimeout(
  getOptions: () => ScrollToOptions | undefined,
  timeout = 0,
  element: HTMLElement | null = null
) {
  if (isAutomatedTest) return

  withTimeout(() => {
    const opts = getOptions()
    if (opts) {
      const target = element ?? resolveScrollContainer()
      if (target) {
        target.scrollTo({ behavior: 'smooth', ...opts })
      } else {
        window.scrollTo({ behavior: 'smooth', ...opts })
      }
    }
  }, timeout)
}

function scrollIntoViewWithTimeout(
  getElement: () => HTMLElement | undefined,
  timeout = 0,
  blockPosition: ScrollLogicalPosition
) {
  if (isAutomatedTest) return

  withTimeout(() => {
    const elem = getElement()
    if (elem) elem.scrollIntoView({ behavior: 'smooth', block: blockPosition })
  }, timeout)
}

function withTimeout(callback: () => void, timeout = 0) {
  if (timeout > 0) {
    window.setTimeout(callback, timeout)
  } else {
    requestAnimationFrame(callback)
  }
}

function getScrollOffsetPosition(elem: HTMLElement): number {
  const top = elem.getBoundingClientRect().top
  const container = resolveScrollContainer()
  return container
    ? top - container.getBoundingClientRect().top + container.scrollTop
    : top + window.scrollY
}
