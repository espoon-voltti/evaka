// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

function focusElement(id: string): void {
  const element = document.getElementById(id)
  if (element) {
    element.focus()
  }
}

export function focusElementOnNextFrame(id: string): void {
  requestAnimationFrame(() => {
    focusElement(id)
  })
}

export function focusElementAfterDelay(id: string, timeout = 25): void {
  setTimeout(() => {
    focusElement(id)
  }, timeout)
}
