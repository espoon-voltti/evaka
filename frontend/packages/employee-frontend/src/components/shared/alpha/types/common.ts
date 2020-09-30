// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export interface Children {
  children: React.ReactNode
}

export interface ClassName {
  className?: string
}

export type StatusType =
  | 'ready'
  | 'alert'
  | 'visited'
  | 'new'
  | 'not-in-focus'
  | 'error'

export type States = 'success' | 'warning' | 'error'
