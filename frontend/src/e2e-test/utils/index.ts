// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

/**
 * Returns a promise that is resolved after the given amount of milliseconds
 *
 * @param ms
 */
export async function delay(ms: number): Promise<void> {
  return new Promise<void>((resolve) => setTimeout(resolve, ms))
}

/**
 * Bounding box of an element.
 *
 * Roughly equivalent to {@type DOMRect}.
 */
export class BoundingBox {
  readonly x: number
  readonly y: number
  readonly width: number
  readonly height: number

  constructor(value: { x: number; y: number; width: number; height: number }) {
    this.x = value.x
    this.y = value.y
    this.width = value.width
    this.height = value.height
  }

  get left(): number {
    return this.x
  }

  get right(): number {
    return this.x + this.width
  }

  get top(): number {
    return this.y
  }

  get bottom(): number {
    return this.y + this.height
  }

  contains(other: BoundingBox): boolean {
    const h = this.left <= other.left && other.right <= this.right
    const v = this.top <= other.top && other.bottom <= this.bottom
    return h && v
  }
}

/**
 * Converts the given string into a valid single-quoted CSS string by escaping problematic characters and wrapping the content in single quotes.
 *
 * See https://developer.mozilla.org/en-US/docs/Web/CSS/string
 */
export function toCssString(text: string): string {
  return [
    "'",
    ...Array.from(text).map((codepoint) => {
      switch (codepoint) {
        case '\\':
          return '\\\\'
        case "'":
          return "\\'"
        case '\n':
          return '\\A'
        default:
          return codepoint
      }
    }),
    "'"
  ].join('')
}
