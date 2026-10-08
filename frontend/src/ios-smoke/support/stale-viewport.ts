// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { Point, Session } from './webdriver'
import { sleep, waitUntil } from './webdriver'

export const selectors = {
  header: 'header',
  nav: 'nav:has([data-qa="nav-calendar-mobile"])',
  scrollArea: '[data-qa="scroll-area"]',
  navMessages: '[data-qa="nav-messages-mobile"]',
  navCalendar: '[data-qa="nav-calendar-mobile"]',
  threadListItem: '[data-qa="thread-list-item"]',
  replyButton:
    '[data-qa="message-thread-actions-mobile"] [data-qa="message-reply-editor-btn"]',
  replyContent: '[data-qa="message-reply-content"]',
  sendReply: '[data-qa="message-send-btn"]',
  sentNotification: '[data-qa="message-sent-notification"]'
}

// The step sequence and its timing come from a manual reproduction in the
// simulator, recorded with its input events. Offsets in ms from the tap on
// the messages tab.
const at = {
  thread: 1610,
  reply: 2590,
  landscape: 4320,
  jKey: 6370,
  flick: 8360,
  drag: 9840,
  send: 12250
}
const sendToPortraitMs = 4700
const portraitToCalendarMs = 2610

// Screen points in the orientation of the moment, from the same recording
const points = {
  jKey: [488, 279] as Point,
  flick: { from: [470, 68] as Point, to: [469, 14] as Point, ms: 60 },
  drag: { from: [636, 44] as Point, to: [637, 227] as Point, ms: 1636 },
  send: [744, 142] as Point,
  calendarScroll: {
    from: [200, 600] as Point,
    to: [200, 300] as Point,
    ms: 600
  }
}

// The recording sent the reply with the send button just above the keyboard
const sendButtonScreenY = { min: 60, max: 150 }
const portraitScreenHeight = 874
const navHeight = 66
const orientationTimeoutMs = 6000

export interface Rect {
  top: number
  bottom: number
  position: string
}

export interface ViewportMeasurement {
  ih: number
  vvH: number
  vvTop: number
  header: Rect | null
  nav: Rect | null
  scrollY: number
  orientation: string
  focus: string | null
  path: string
}

export function measureViewport(
  session: Session
): Promise<ViewportMeasurement> {
  return session.execute((sel: typeof selectors) => {
    const rect = (selector: string) => {
      const el = document.querySelector(selector)
      if (!el) return null
      const r = el.getBoundingClientRect()
      return {
        top: Math.round(r.top),
        bottom: Math.round(r.bottom),
        position: getComputedStyle(el).position
      }
    }
    return {
      ih: window.innerHeight,
      vvH: Math.round(window.visualViewport?.height ?? -1),
      vvTop: Math.round(window.visualViewport?.offsetTop ?? -1),
      header: rect(sel.header),
      nav: rect(sel.nav),
      scrollY: Math.round(window.scrollY),
      orientation: screen.orientation.type,
      focus: document.activeElement?.tagName ?? null,
      path: location.pathname
    }
  }, selectors)
}

/**
 * The WebKit bug state: in portrait with no text field focused (so the
 * software keyboard is closed), the visual viewport is shorter than the
 * layout viewport or offset from it
 */
export const isStale = (m: ViewportMeasurement) =>
  m.orientation.startsWith('portrait') &&
  m.focus !== 'INPUT' &&
  m.focus !== 'TEXTAREA' &&
  (Math.abs(m.vvH - m.ih) > 1 || m.vvTop > 0)

export const formatMeasurement = (m: ViewportMeasurement) => {
  const r = (rect: Rect | null) =>
    rect ? `${rect.top}..${rect.bottom} (${rect.position})` : 'missing'
  return `${m.path} ${m.orientation} innerHeight ${m.ih} visualViewport.height ${m.vvH} offsetTop ${m.vvTop} scrollY ${m.scrollY} header ${r(m.header)} nav ${r(m.nav)} focus ${m.focus}`
}

/** Rotates the simulator and waits until the page has the new orientation */
async function rotate(session: Session, orientation: 'LANDSCAPE' | 'PORTRAIT') {
  await session.setOrientation(orientation)
  const landscape = orientation === 'LANDSCAPE'
  await waitUntil(
    () =>
      session.execute(
        (expectLandscape: boolean) =>
          window.innerWidth > window.innerHeight === expectLandscape,
        landscape
      ),
    {
      timeout: orientationTimeoutMs,
      interval: 150,
      message: `The page did not switch to ${orientation.toLowerCase()}`
    }
  )
}

/**
 * Waits until no text field has focus and the visual viewport is as tall as
 * the layout viewport again. A stale visual viewport never passes this.
 */
export function waitForKeyboardClosed(session: Session) {
  return waitUntil(
    () =>
      session.execute(() => {
        const viewport = window.visualViewport
        const focused = document.activeElement?.tagName
        return (
          focused !== 'INPUT' &&
          focused !== 'TEXTAREA' &&
          viewport !== null &&
          Math.abs(viewport.height - window.innerHeight) <= 1
        )
      }),
    {
      timeout: 5000,
      interval: 200,
      message: 'The software keyboard did not close'
    }
  )
}

/**
 * Screen point of the element's centre. In portrait the web view starts below
 * the status bar, at the screen height minus `innerHeight`. In landscape the
 * status bar is hidden and the web view starts at the top, but a page without
 * `viewport-fit=cover` gets a shorter `innerHeight` (home indicator) and is
 * centred horizontally between the safe areas. The visible part of the
 * document starts at `visualViewport.pageTop`, which moves when the keyboard
 * is open.
 */
async function screenPoint(session: Session, selector: string) {
  const point = await session.execute((sel: string) => {
    const el = document.querySelector(sel)
    const viewport = window.visualViewport
    if (!el || !viewport) return null
    const r = el.getBoundingClientRect()
    const landscape = screen.orientation.type.startsWith('landscape')
    const long = Math.max(screen.width, screen.height)
    const short = Math.min(screen.width, screen.height)
    const screenWidth = landscape ? long : short
    return [
      r.left +
        r.width / 2 +
        window.scrollX -
        viewport.pageLeft +
        (screenWidth - window.innerWidth) / 2,
      r.top +
        r.height / 2 +
        window.scrollY -
        viewport.pageTop +
        (landscape ? 0 : long - window.innerHeight)
    ]
  }, selector)
  return point ? (point.map(Math.round) as Point) : undefined
}

/**
 * Taps the element natively, like a finger. The thread grows by one message
 * per loop, so the element is first dragged above `maxY` (the bottom
 * navigation) when needed. Falls back to the recorded point when the element
 * is not found.
 */
async function tapElement(
  session: Session,
  name: string,
  selector: string,
  { maxY = Infinity, fallback }: { maxY?: number; fallback?: Point } = {}
) {
  let point = await screenPoint(session, selector)
  for (let i = 0; i < 6 && point && point[1] > maxY; i++) {
    if (point[1] - maxY > 500) {
      // A fast flick scrolls with momentum, to the end of a long thread
      await session.drag([200, 700], [200, 300], 80)
      await sleep(2500)
    } else {
      const dy = Math.min(point[1] - (maxY - 150), 400)
      await session.drag([200, 650], [200, 650 - dy], 900)
      await sleep(800)
    }
    point = await screenPoint(session, selector)
  }
  const target = point ?? fallback
  if (!target) throw new Error(`${name}: ${selector} not found`)
  if (target[1] > maxY) {
    throw new Error(`${name} is still under the navigation at y ${target[1]}`)
  }
  await session.tap(target)
  await sleep(150)
}

const replyState = (session: Session) =>
  session.execute(
    (sel: typeof selectors) => ({
      value:
        document.querySelector<HTMLTextAreaElement>(sel.replyContent)?.value ??
        null,
      sent: document.querySelector(sel.sentNotification) !== null
    }),
    selectors
  )

const exists = (session: Session, selector: string) =>
  session.execute(
    (sel: string) => document.querySelector(sel) !== null,
    selector
  )

function startClock() {
  const t0 = Date.now()
  return {
    now: () => Date.now() - t0,
    until: async (offset: number) => {
      const wait = offset - (Date.now() - t0)
      if (wait > 0) await sleep(wait)
    }
  }
}

// The J key of the Finnish keyboard in landscape, by its recorded position
async function typeReply(session: Session) {
  await session.tap(points.jKey)
  await sleep(400)
  let { value } = await replyState(session)
  if (value === '') {
    await session.mobile('keys', { keys: ['J'] })
    await sleep(400)
    ;({ value } = await replyState(session))
  }
  if (value !== 'J') {
    throw new Error(
      `The reply text is ${JSON.stringify(value)} after typing J: the keyboard is not where it was recorded`
    )
  }
}

async function bringSendButtonAboveKeyboard(session: Session) {
  for (let i = 0; i < 5; i++) {
    const point = await screenPoint(session, selectors.sendReply)
    if (!point) throw new Error(`${selectors.sendReply} not found`)
    const y = point[1]
    if (y >= sendButtonScreenY.min && y <= sendButtonScreenY.max) return
    const dy =
      y > sendButtonScreenY.max
        ? -Math.min(y - 130, 100)
        : Math.min(130 - y, 100)
    const fromY = dy < 0 ? 130 : 30
    await session.drag([636, fromY], [636, fromY + dy], 800)
    await sleep(300)
  }
}

/**
 * One loop of the reproduction, starting from the calendar in portrait:
 * open a message thread, start a reply (the editor focuses its text field and
 * the software keyboard opens), rotate to landscape, type, scroll, send (the
 * keyboard closes), rotate back to portrait and open the calendar. Returns the
 * measurement on the calendar.
 */
async function runLoop(session: Session): Promise<ViewportMeasurement> {
  const clock = startClock()
  await tapElement(session, 'messages tab', selectors.navMessages)
  await clock.until(at.thread)
  await waitUntil(
    () =>
      session.execute(
        (sel: string) =>
          document.querySelector(sel) !== null &&
          document.querySelector('[data-qa="spinner"]') === null,
        selectors.threadListItem
      ),
    { timeout: 10000, message: 'The thread list did not load' }
  )
  const maxY = portraitScreenHeight - navHeight - 20
  await tapElement(session, 'thread', selectors.threadListItem, { maxY })
  await clock.until(at.reply)
  await waitUntil(() => exists(session, selectors.replyButton), {
    timeout: 10000,
    message: 'The reply button did not appear'
  })
  await tapElement(session, 'reply', selectors.replyButton, { maxY })
  await sleep(600)

  await clock.until(at.landscape)
  await rotate(session, 'LANDSCAPE')
  await sleep(500)

  await clock.until(at.jKey)
  await typeReply(session)

  await clock.until(at.flick)
  await session.drag(points.flick.from, points.flick.to, points.flick.ms)
  await clock.until(at.drag)
  await session.drag(points.drag.from, points.drag.to, points.drag.ms)
  await sleep(300)
  await bringSendButtonAboveKeyboard(session)

  await clock.until(at.send)
  await tapElement(session, 'send', selectors.sendReply, {
    fallback: points.send
  })
  const sentAt = clock.now()
  await sleep(500)
  await waitUntil(async () => (await replyState(session)).sent, {
    timeout: 3000,
    interval: 200,
    message: 'The reply sent notification did not appear'
  }).catch((e) => console.warn(String(e)))

  await clock.until(sentAt + sendToPortraitMs)
  await rotate(session, 'PORTRAIT')
  const portraitAt = clock.now()
  await clock.until(portraitAt + portraitToCalendarMs)
  await tapElement(session, 'calendar tab', selectors.navCalendar)
  await sleep(2000)
  return measureViewport(session)
}

/** Scrolls the calendar with a native drag, like a user */
export async function scrollCalendar(session: Session) {
  const { from, to, ms } = points.calendarScroll
  await session.drag(from, to, ms)
  await sleep(1200)
}

/**
 * Drives the app into the iOS WebKit stale visual viewport state (WebKit bugs
 * 254861 and 297779) by repeating a recorded user sequence. It has never
 * triggered on the first loop after the app was launched, but did on the
 * second loop in the same app process. Starts on the calendar in portrait
 * and stops on the calendar as soon as the state is reached.
 */
export async function triggerStaleViewport(session: Session, maxLoops: number) {
  const loops: ViewportMeasurement[] = []
  for (let n = 1; n <= maxLoops; n++) {
    const start = await measureViewport(session)
    if (isStale(start) || start.orientation.startsWith('landscape')) {
      throw new Error(
        `The app is not in a clean state before loop ${n}: ${formatMeasurement(start)}`
      )
    }
    await sleep(1000)
    const measured = await runLoop(session)
    loops.push(measured)
    console.warn(`Stale viewport loop ${n}: ${formatMeasurement(measured)}`)
    if (isStale(measured)) return { reached: true, loops }
    await scrollCalendar(session)
  }
  return { reached: false, loops }
}
