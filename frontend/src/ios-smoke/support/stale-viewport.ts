// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { rotate } from './orientation'

export const selectors = {
  header: 'header',
  nav: 'nav:has([data-qa="nav-calendar-mobile"])',
  fab: '[data-qa="open-calendar-actions-modal"]',
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

type Point = [number, number]

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

export interface Rect {
  top: number
  bottom: number
  left: number
  right: number
  position: string
}

export interface ViewportMeasurement {
  ih: number
  iw: number
  vvH: number
  vvTop: number
  header: Rect | null
  nav: Rect | null
  fab: Rect | null
  scrollAreaTop: number | null
  scrollY: number
  orientation: string
  focus: string | null
  path: string
}

export async function measureViewport(): Promise<ViewportMeasurement> {
  const json = await browser.execute((sel: typeof selectors) => {
    const rect = (selector: string) => {
      const el = document.querySelector(selector)
      if (!el) return null
      const r = el.getBoundingClientRect()
      return {
        top: Math.round(r.top),
        bottom: Math.round(r.bottom),
        left: Math.round(r.left),
        right: Math.round(r.right),
        position: getComputedStyle(el).position
      }
    }
    const area = document.querySelector(sel.scrollArea)
    return JSON.stringify({
      ih: window.innerHeight,
      iw: window.innerWidth,
      vvH: Math.round(window.visualViewport?.height ?? -1),
      vvTop: Math.round(window.visualViewport?.offsetTop ?? -1),
      header: rect(sel.header),
      nav: rect(sel.nav),
      fab: rect(sel.fab),
      scrollAreaTop: area ? Math.round(area.getBoundingClientRect().top) : null,
      scrollY: Math.round(window.scrollY),
      orientation: screen.orientation.type,
      focus: document.activeElement?.tagName ?? null,
      path: location.pathname
    })
  }, selectors)
  return JSON.parse(json) as ViewportMeasurement
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
  return `${m.path} ${m.orientation} innerHeight ${m.ih} visualViewport.height ${m.vvH} offsetTop ${m.vvTop} scrollY ${m.scrollY} header ${r(m.header)} nav ${r(m.nav)} floating button ${r(m.fab)} scroll area top ${m.scrollAreaTop ?? 'missing'} focus ${m.focus}`
}

const sleep = (ms: number) => browser.pause(ms)

const tapAt = ([x, y]: Point) => browser.execute('mobile: tap', { x, y })

async function drag(from: Point, to: Point, ms: number) {
  await browser.performActions([
    {
      type: 'pointer',
      id: 'finger',
      parameters: { pointerType: 'touch' },
      actions: [
        { type: 'pointerMove', duration: 0, x: from[0], y: from[1] },
        { type: 'pointerDown', button: 0 },
        { type: 'pointerMove', duration: ms, x: to[0], y: to[1] },
        { type: 'pointerUp', button: 0 }
      ]
    }
  ])
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
async function screenPoint(selector: string): Promise<Point | undefined> {
  const json = await browser.execute((sel: string) => {
    const el = document.querySelector(sel)
    const viewport = window.visualViewport
    if (!el || !viewport) return null
    const r = el.getBoundingClientRect()
    const landscape = screen.orientation.type.startsWith('landscape')
    const long = Math.max(screen.width, screen.height)
    const short = Math.min(screen.width, screen.height)
    const screenWidth = landscape ? long : short
    return JSON.stringify([
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
    ])
  }, selector)
  if (!json) return undefined
  const [x, y] = JSON.parse(json) as Point
  return [Math.round(x), Math.round(y)]
}

/**
 * Taps the element natively, like a finger. The thread grows by one message
 * per loop, so the element is first dragged above `maxY` (the bottom
 * navigation) when needed. Falls back to the recorded point when the element
 * is not found.
 */
async function tapElement(
  name: string,
  selector: string,
  { maxY = Infinity, fallback }: { maxY?: number; fallback?: Point } = {}
) {
  let point = await screenPoint(selector)
  for (let i = 0; i < 6 && point && point[1] > maxY; i++) {
    if (point[1] - maxY > 500) {
      // A fast flick scrolls with momentum, to the end of a long thread
      await drag([200, 700], [200, 300], 80)
      await sleep(2500)
    } else {
      const dy = Math.min(point[1] - (maxY - 150), 400)
      await drag([200, 650], [200, 650 - dy], 900)
      await sleep(800)
    }
    point = await screenPoint(selector)
  }
  const target = point ?? fallback
  if (!target) throw new Error(`${name}: ${selector} not found`)
  if (target[1] > maxY) {
    throw new Error(`${name} is still under the navigation at y ${target[1]}`)
  }
  await tapAt(target)
  await sleep(150)
}

const replyState = () =>
  browser
    .execute(
      (sel: typeof selectors) =>
        JSON.stringify({
          value:
            document.querySelector<HTMLTextAreaElement>(sel.replyContent)
              ?.value ?? null,
          sent: document.querySelector(sel.sentNotification) !== null
        }),
      selectors
    )
    .then((json) => JSON.parse(json) as { value: string | null; sent: boolean })

const exists = (selector: string) =>
  browser.execute(
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

async function typeReply() {
  await tapAt(points.jKey)
  await sleep(400)
  let { value } = await replyState()
  if (value === '') {
    await browser.execute('mobile: keys', { keys: ['J'] })
    await sleep(400)
    ;({ value } = await replyState())
  }
  if (value !== 'J') {
    throw new Error(
      `The reply text is ${JSON.stringify(value)} after typing J: the keyboard is not where it was recorded`
    )
  }
}

async function bringSendButtonAboveKeyboard() {
  for (let i = 0; i < 5; i++) {
    const point = await screenPoint(selectors.sendReply)
    if (!point) throw new Error(`${selectors.sendReply} not found`)
    const y = point[1]
    if (y >= sendButtonScreenY.min && y <= sendButtonScreenY.max) return
    const dy =
      y > sendButtonScreenY.max
        ? -Math.min(y - 130, 100)
        : Math.min(130 - y, 100)
    const fromY = dy < 0 ? 130 : 30
    await drag([636, fromY], [636, fromY + dy], 800)
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
async function runLoop(): Promise<ViewportMeasurement> {
  const clock = startClock()
  await tapElement('messages tab', selectors.navMessages)
  await clock.until(at.thread)
  await browser.waitUntil(
    () =>
      browser.execute(
        (sel: string) =>
          document.querySelector(sel) !== null &&
          document.querySelector('[data-qa="spinner"]') === null,
        selectors.threadListItem
      ),
    { timeout: 10000, timeoutMsg: 'The thread list did not load' }
  )
  const maxY = portraitScreenHeight - navHeight - 20
  await tapElement('thread', selectors.threadListItem, { maxY })
  await clock.until(at.reply)
  await browser.waitUntil(() => exists(selectors.replyButton), {
    timeout: 10000,
    timeoutMsg: 'The reply button did not appear'
  })
  await tapElement('reply', selectors.replyButton, { maxY })
  await sleep(600)

  await clock.until(at.landscape)
  await rotate('LANDSCAPE')
  await sleep(500)

  await clock.until(at.jKey)
  await typeReply()

  await clock.until(at.flick)
  await drag(points.flick.from, points.flick.to, points.flick.ms)
  await clock.until(at.drag)
  await drag(points.drag.from, points.drag.to, points.drag.ms)
  await sleep(300)
  await bringSendButtonAboveKeyboard()

  await clock.until(at.send)
  await tapElement('send', selectors.sendReply, { fallback: points.send })
  const sentAt = clock.now()
  await sleep(500)
  await browser
    .waitUntil(async () => (await replyState()).sent, {
      timeout: 3000,
      interval: 200
    })
    .catch(() => {
      console.warn('The reply sent notification did not appear')
    })

  await clock.until(sentAt + sendToPortraitMs)
  await rotate('PORTRAIT')
  const portraitAt = clock.now()
  await clock.until(portraitAt + portraitToCalendarMs)
  await tapElement('calendar tab', selectors.navCalendar)
  await sleep(2000)
  return measureViewport()
}

/** Scrolls the calendar with a native drag, like a user */
export async function scrollCalendar() {
  const { from, to, ms } = points.calendarScroll
  await drag(from, to, ms)
  await sleep(1200)
}

export interface StaleViewportResult {
  reached: boolean
  loops: ViewportMeasurement[]
}

/**
 * Drives the app into the iOS WebKit stale visual viewport state (WebKit bugs
 * 254861 and 297779) by repeating a recorded user sequence. It has never
 * triggered on the first loop after the app was launched, but did on the
 * second loop in the same app process. Starts on the calendar in portrait
 * and stops on the calendar as soon as the state is reached.
 */
export async function triggerStaleViewport(
  maxLoops = 3
): Promise<StaleViewportResult> {
  const loops: ViewportMeasurement[] = []
  for (let n = 1; n <= maxLoops; n++) {
    const start = await measureViewport()
    if (isStale(start) || start.orientation.startsWith('landscape')) {
      throw new Error(
        `The app is not in a clean state before loop ${n}: ${formatMeasurement(start)}`
      )
    }
    await sleep(1000)
    const measured = await runLoop()
    loops.push(measured)
    console.warn(`Stale viewport loop ${n}: ${formatMeasurement(measured)}`)
    if (isStale(measured)) return { reached: true, loops }
    await scrollCalendar()
  }
  return { reached: false, loops }
}
