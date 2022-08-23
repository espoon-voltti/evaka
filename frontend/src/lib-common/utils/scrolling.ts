// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { MutableRefObject } from 'react'

import { isAutomatedTest, isIOS } from './helpers'

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

export function scrollToRef(
  ref: MutableRefObject<HTMLElement | null>,
  timeout = 0
) {
  scrollWithTimeout(
    () =>
      ref.current ? { top: getDocumentOffsetPosition(ref.current) } : undefined,
    timeout
  )
}

export function scrollRefIntoView(
  ref: MutableRefObject<HTMLElement | null>,
  timeout = 0,
  options?: ScrollIntoViewOptions
) {
  scrollIntoViewWithTimeout(() => ref.current ?? undefined, timeout, options)
}

export function scrollIntoViewSoftKeyboard(
  target: Element,
  blockPosition: ScrollLogicalPosition = 'center'
) {
  if (!('visualViewport' in window)) return

  const onResize = () => {
    target.scrollIntoView({
      block: blockPosition
    })
    window.visualViewport.removeEventListener('resize', onResize)
  }

  if (isIOS()) {
    // On iOS, the resize event is triggered very late so
    // the scrolling should happen instantly
    target.scrollIntoView({
      block: blockPosition
    })
  }

  window.visualViewport.addEventListener('resize', onResize)
  setTimeout(() => {
    window.visualViewport.removeEventListener('resize', onResize)
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
      element
        ? element.scrollTo({ behavior: 'smooth', ...opts })
        : window.scrollTo({ behavior: 'smooth', ...opts })
    }
  }, timeout)
}

function scrollIntoViewWithTimeout(
  getElement: () => HTMLElement | undefined,
  timeout = 0,
  options: ScrollIntoViewOptions = {}
) {
  if (isAutomatedTest) return

  withTimeout(() => {
    const elem = getElement()
    if (elem) elem.scrollIntoView({ behavior: 'smooth', ...options })
  }, timeout)
}

function withTimeout(callback: () => void, timeout = 0) {
  if (timeout > 0) {
    window.setTimeout(callback, timeout)
  } else {
    requestAnimationFrame(callback)
  }
}

function getDocumentOffsetPosition(elem: HTMLElement): number {
  let elemTop = elem.offsetTop
  if (elem.offsetParent) {
    const parentOffsetTop = getDocumentOffsetPosition(
      elem.offsetParent as HTMLElement
    )
    elemTop = elemTop + parentOffsetTop
  }
  return elemTop
}
