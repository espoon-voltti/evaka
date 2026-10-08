// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import assert from 'node:assert/strict'

import { formatBox, paintedBox } from './reachable'
import type { ScreenBox } from './reachable'

const tolerancePx = 1
const scrollTestDistancePx = 400

const appShell = '[data-qa="app-shell"]'
const scrollArea = '[data-qa="scroll-area"]'
const header = '[data-qa="header"]'
const mobileNav = '[data-qa="mobile-nav"]'

// Floating action buttons that are still fixed to the viewport in the
// installed app. They are not covered by the app shell workaround and may
// drift with a stale visual viewport; remove an entry when the button is
// anchored to the shell.
const knownFixedButtons = [
  'open-calendar-actions-modal',
  'new-message-btn-mobile'
]

const close = (a: number, b: number) => Math.abs(a - b) <= tolerancePx

/**
 * Asserts the app shell layout that the installed app relies on (see the
 * comment above `AppShell` in citizen-frontend/App.tsx): the document never
 * scrolls, the shell fills the viewport, only the scroll area scrolls and
 * nothing in the shell is positioned against the viewport.
 *
 * iOS can leave the visual viewport stale in an installed web app after the
 * software keyboard closes or the device rotates, and it resolves
 * `position: fixed` against it, so viewport anchored elements drift (WebKit
 * bugs 254861 and 297779). The layout avoids viewport anchoring altogether,
 * so these checks guard the workaround without reproducing the bug.
 *
 * With `longPage`, the content must be taller than the scroll area, and the
 * check that scrolling moves only the content is required instead of skipped.
 */
export async function expectAppShellLayout({
  longPage = false
}: { longPage?: boolean } = {}) {
  await $(appShell).waitForExist()
  await $(mobileNav).waitForDisplayed()
  const layout = await measureLayout()

  assert.ok(layout.standalone, 'html[data-standalone] is not set')

  assert.equal(
    layout.document.scrollY,
    0,
    `The document is scrolled (scrollY ${layout.document.scrollY}): only the scroll area may scroll`
  )
  assert.ok(
    layout.document.scrollHeight <= layout.viewport.height + tolerancePx,
    `The document is taller than the viewport (scrollHeight ${layout.document.scrollHeight}, innerHeight ${layout.viewport.height}): it can scroll`
  )
  for (const { name, overflow } of layout.document.overflows) {
    assert.equal(
      overflow,
      'hidden hidden',
      `${name} has overflow-x/y "${overflow}", expected "hidden hidden" so that the document never scrolls`
    )
  }

  const shell = layout.shell
  assert.ok(shell, `${appShell} not found`)
  assert.equal(
    shell.position,
    'relative',
    `${appShell} has position ${shell.position}, expected relative (absolutely positioned menus are anchored to it)`
  )
  assert.equal(
    shell.overflow,
    'hidden hidden',
    `${appShell} has overflow-x/y "${shell.overflow}", expected "hidden hidden"`
  )
  const { width, height } = layout.viewport
  assert.ok(
    close(shell.rect.left, 0) &&
      close(shell.rect.top, 0) &&
      close(shell.rect.right, width) &&
      close(shell.rect.bottom, height),
    `${appShell} does not fill the viewport: rect ${formatRect(shell.rect)}, viewport 0,0 ${width}x${height}`
  )

  const area = layout.scrollArea
  assert.ok(area, `${scrollArea} not found`)
  assert.ok(area.insideShell, `${scrollArea} is not inside ${appShell}`)
  assert.ok(
    area.overflowY === 'scroll' || area.overflowY === 'auto',
    `${scrollArea} has overflow-y ${area.overflowY}, expected scroll or auto: it must be the scrolling element`
  )
  if (longPage) {
    assert.ok(
      area.scrollHeight > area.clientHeight,
      `${scrollArea} does not overflow (scrollHeight ${area.scrollHeight}, clientHeight ${area.clientHeight}) although the page is long: the content is not laid out inside it`
    )
  }

  assert.deepEqual(
    layout.fixed,
    [],
    `Elements with position: fixed inside ${appShell}: ${layout.fixed.join(', ')}. iOS can anchor them to a stale visual viewport; use position: absolute against the shell or put them in #modal-container`
  )
  assert.deepEqual(
    layout.stickyOutsideShell,
    [],
    `Elements with position: sticky whose scroll container is outside ${appShell}: ${layout.stickyOutsideShell.join(', ')}`
  )

  const nav = layout.mobileNav
  assert.ok(nav, `${mobileNav} not found`)
  assert.equal(
    nav.position,
    'static',
    `${mobileNav} has position ${nav.position}, expected static (a flex child at the bottom of ${appShell}, not anchored to the viewport)`
  )
  assert.ok(
    nav.childOfShell,
    `${mobileNav} is not a direct child of ${appShell}`
  )
  assert.deepEqual(
    nav.inFlowAfter,
    [],
    `${mobileNav} is not the last flex child of ${appShell}; after it: ${nav.inFlowAfter.join(', ')}`
  )
  assert.ok(
    close(nav.rect.bottom, shell.rect.bottom),
    `${mobileNav} bottom is at ${nav.rect.bottom}, expected the bottom of ${appShell} at ${shell.rect.bottom}`
  )

  assert.ok(layout.header, `${header} not found`)
  assert.ok(
    close(layout.header.rect.top, 0),
    `${header} top is at ${layout.header.rect.top}, expected 0`
  )

  await expectScrollingMovesOnlyContent(longPage)
}

/**
 * Scrolls the scroll area and checks from screenshots that the header and the
 * bottom navigation stay where they are painted while the content moves, and
 * that the document itself does not scroll.
 */
async function expectScrollingMovesOnlyContent(longPage: boolean) {
  const headerBefore = await paintedBox(header)
  const navBefore = await paintedBox(mobileNav)
  assert.ok(headerBefore, `${header} is not painted on screen`)
  assert.ok(navBefore, `${mobileNav} is not painted on screen`)

  const scrolled = await browser.execute(
    (areaSelector: string, distance: number) => {
      const area = document.querySelector(areaSelector)
      const content = area?.firstElementChild
      if (!area || !content) return undefined
      const originalScrollTop = area.scrollTop
      const contentTopBefore = content.getBoundingClientRect().top
      const canScrollDown =
        originalScrollTop + area.clientHeight + distance <= area.scrollHeight
      area.scrollTop =
        originalScrollTop + (canScrollDown ? distance : -distance)
      return {
        originalScrollTop,
        scrolledBy: area.scrollTop - originalScrollTop,
        contentMoved: content.getBoundingClientRect().top - contentTopBefore,
        scrollY: window.scrollY
      }
    },
    scrollArea,
    scrollTestDistancePx
  )
  assert.ok(scrolled, `${scrollArea} or its content not found`)
  try {
    if (scrolled.scrolledBy === 0) {
      assert.ok(
        !longPage,
        `Scrolling ${scrollArea} by ${scrollTestDistancePx} px had no effect`
      )
      console.warn(
        `Skipping the scroll check: ${scrollArea} is not taller than its box`
      )
      return
    }
    assert.equal(
      scrolled.scrollY,
      0,
      `Scrolling ${scrollArea} scrolled the document (scrollY ${scrolled.scrollY})`
    )
    assert.ok(
      close(scrolled.contentMoved, -scrolled.scrolledBy),
      `The content moved by ${scrolled.contentMoved} px when ${scrollArea} was scrolled by ${scrolled.scrolledBy} px`
    )
    await expectPaintedUnchanged(header, headerBefore, scrolled.scrolledBy)
    await expectPaintedUnchanged(mobileNav, navBefore, scrolled.scrolledBy)
  } finally {
    await browser.execute(
      (areaSelector: string, scrollTop: number) => {
        const area = document.querySelector(areaSelector)
        if (area) area.scrollTop = scrollTop
      },
      scrollArea,
      scrolled.originalScrollTop
    )
  }
}

async function expectPaintedUnchanged(
  selector: string,
  before: ScreenBox,
  scrolledBy: number
) {
  const after = await paintedBox(selector)
  assert.ok(
    after &&
      close(after.top, before.top) &&
      close(after.left, before.left) &&
      close(after.width, before.width) &&
      close(after.height, before.height),
    `${selector} moved on screen when ${scrollArea} was scrolled by ${scrolledBy} px: painted at ${formatBox(before)} before, ${formatBox(after)} after`
  )
}

/**
 * Asserts that every open modal dialog is rendered in `#modal-container`,
 * outside the app shell. Overlays there may use `position: fixed`, because the
 * shell does not clip them. Returns the number of open dialogs.
 */
export async function expectModalsOutsideAppShell() {
  const dialogs = await browser.execute(
    (shellSelector: string) =>
      Array.from(document.querySelectorAll('[role="dialog"]')).map(
        (dialog) => ({
          label:
            dialog.getAttribute('aria-label') ??
            dialog.closest('[data-qa]')?.getAttribute('data-qa') ??
            dialog.tagName.toLowerCase(),
          insideShell: dialog.closest(shellSelector) !== null,
          inModalContainer: dialog.closest('#modal-container') !== null
        })
      ),
    appShell
  )
  for (const dialog of dialogs) {
    assert.ok(
      !dialog.insideShell && dialog.inModalContainer,
      `Dialog "${dialog.label}" is ${dialog.insideShell ? `inside ${appShell}` : 'not in #modal-container'}: modals must be portaled to #modal-container, outside the shell`
    )
  }
  return dialogs.length
}

/**
 * Asserts that the mobile menu is anchored to the app shell with
 * `position: absolute`, not to the viewport
 */
export async function expectMobileMenuInsideAppShell() {
  await $('[data-qa="mobile-menu"]').waitForDisplayed()
  const menu = await browser.execute((shellSelector: string) => {
    const el = document.querySelector('[data-qa="mobile-menu"]')
    if (!el) return undefined
    return {
      position: getComputedStyle(el).position,
      insideShell: el.closest(shellSelector) !== null
    }
  }, appShell)
  assert.ok(menu, '[data-qa="mobile-menu"] not found')
  assert.ok(
    menu.insideShell,
    `[data-qa="mobile-menu"] is not inside ${appShell}`
  )
  assert.equal(
    menu.position,
    'absolute',
    `[data-qa="mobile-menu"] has position ${menu.position}, expected absolute (anchored to ${appShell}, not to the viewport)`
  )
}

interface Rect {
  left: number
  top: number
  right: number
  bottom: number
}

interface Layout {
  standalone: boolean
  viewport: { width: number; height: number }
  document: {
    scrollY: number
    scrollHeight: number
    overflows: { name: string; overflow: string }[]
  }
  shell?: { position: string; overflow: string; rect: Rect }
  scrollArea?: {
    overflowY: string
    insideShell: boolean
    scrollHeight: number
    clientHeight: number
  }
  mobileNav?: {
    position: string
    childOfShell: boolean
    inFlowAfter: string[]
    rect: Rect
  }
  header?: { rect: Rect }
  fixed: string[]
  stickyOutsideShell: string[]
}

const formatRect = (rect: Rect) =>
  `${rect.left},${rect.top} ${rect.right - rect.left}x${rect.bottom - rect.top}`

// Serialised to JSON: the Safari adapter does not return the nested object
// intact
async function measureLayout() {
  const json = await browser.execute(
    (
      shellSelector: string,
      areaSelector: string,
      navSelector: string,
      headerSelector: string,
      knownFixed: string[]
    ) => {
      const rectOf = (el: Element) => {
        const r = el.getBoundingClientRect()
        return { left: r.left, top: r.top, right: r.right, bottom: r.bottom }
      }
      const overflowOf = (el: Element) => {
        const style = getComputedStyle(el)
        return `${style.overflowX} ${style.overflowY}`
      }
      const describe = (el: Element) => {
        const dataQa = el.closest('[data-qa]')?.getAttribute('data-qa')
        return `${el.tagName.toLowerCase()}${dataQa ? ` in [data-qa="${dataQa}"]` : ''}`
      }
      const scrollContainerOf = (el: Element) => {
        for (let p = el.parentElement; p; p = p.parentElement) {
          if (overflowOf(p) !== 'visible visible') return p
        }
        return document.documentElement
      }
      // Development and staging only, never shown in production
      const environmentLabel = '[data-qa="environment-label"]'
      // react-focus-lock's invisible focus guards
      const focusGuard = '[data-focus-guard]'

      const shell = document.querySelector(shellSelector)
      const area = document.querySelector(areaSelector)
      const nav = document.querySelector(navSelector)
      const headerEl = document.querySelector(headerSelector)
      const scroller = document.scrollingElement ?? document.documentElement
      const appRoot = document.getElementById('app')

      const inShell = shell ? Array.from(shell.querySelectorAll('*')) : []
      const fixed = inShell
        .filter(
          (el) =>
            getComputedStyle(el).position === 'fixed' &&
            !el.matches(environmentLabel) &&
            !el.matches(focusGuard) &&
            !knownFixed.some((dataQa) => el.matches(`[data-qa="${dataQa}"]`))
        )
        .map(describe)
      // A sticky element sticks to its nearest scroll container. Inside the
      // shell that is the shell itself, which never scrolls, or the scroll
      // area, so it cannot drift with the visual viewport. The header is
      // sticky only because iOS 27 blurs the top of the screen unless a fixed
      // or sticky element is there.
      const stickyOutsideShell = inShell
        .filter(
          (el) =>
            getComputedStyle(el).position === 'sticky' &&
            !(shell?.contains(scrollContainerOf(el)) ?? false)
        )
        .map(describe)

      const inFlowAfter: string[] = []
      if (nav && shell && nav.parentElement === shell) {
        for (let s = nav.nextElementSibling; s; s = s.nextElementSibling) {
          const style = getComputedStyle(s)
          const r = s.getBoundingClientRect()
          if (
            style.display !== 'none' &&
            (style.position === 'static' ||
              style.position === 'relative' ||
              style.position === 'sticky') &&
            r.width * r.height > 0
          ) {
            inFlowAfter.push(describe(s))
          }
        }
      }

      return JSON.stringify({
        standalone: document.documentElement.hasAttribute('data-standalone'),
        viewport: { width: window.innerWidth, height: window.innerHeight },
        document: {
          scrollY: window.scrollY,
          scrollHeight: scroller.scrollHeight,
          overflows: [
            { name: 'html', overflow: overflowOf(document.documentElement) },
            { name: 'body', overflow: overflowOf(document.body) },
            {
              name: '#app',
              overflow: appRoot ? overflowOf(appRoot) : 'missing'
            }
          ]
        },
        shell: shell
          ? {
              position: getComputedStyle(shell).position,
              overflow: overflowOf(shell),
              rect: rectOf(shell)
            }
          : undefined,
        scrollArea: area
          ? {
              overflowY: getComputedStyle(area).overflowY,
              insideShell: shell?.contains(area) ?? false,
              scrollHeight: area.scrollHeight,
              clientHeight: area.clientHeight
            }
          : undefined,
        mobileNav: nav
          ? {
              position: getComputedStyle(nav).position,
              childOfShell: nav.parentElement === shell,
              inFlowAfter,
              rect: rectOf(nav)
            }
          : undefined,
        header: headerEl ? { rect: rectOf(headerEl) } : undefined,
        fixed,
        stickyOutsideShell
      })
    },
    appShell,
    scrollArea,
    mobileNav,
    header,
    knownFixedButtons
  )
  return JSON.parse(json) as Layout
}
