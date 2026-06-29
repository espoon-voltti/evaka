// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export class LoginStatusChangeEvent extends CustomEvent<boolean> {
  static eventName = 'loginstatuschange' as const

  constructor(loginStatus: boolean) {
    super(LoginStatusChangeEvent.eventName, {
      cancelable: true,
      detail: loginStatus
    })
  }
}
// TS typings to allow type safe listening for the custom event, e.g.:
// window.addEventListener(LoginStatusChangeEvent.eventName, (e) => {e.detail /* <- TS knows this is a boolean */})
declare global {
  interface WindowEventMap {
    [LoginStatusChangeEvent.eventName]: CustomEvent<boolean>
  }
}
