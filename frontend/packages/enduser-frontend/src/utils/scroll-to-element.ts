// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export const scrollToElement = (element, offset = 30): void => {
  const bClientRect = element.getBoundingClientRect()

  window.scrollTo({
    top: window.scrollY + bClientRect.y - offset,
    behavior: 'smooth'
  })
}

export const scrollToTop = (): void => {
  window.scrollTo({
    top: 0,
    left: 0,
    behavior: 'smooth'
  })
}
