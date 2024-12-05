// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

export class LoginStatusChangeEvent extends Event {
  constructor(public readonly loginStatus: boolean) {
    super(LoginStatusChangeEvent.name)
  }
}
export class LoginStatusEventManager extends EventTarget {}

export const loginStatusEventBus = new LoginStatusEventManager()
window.evaka = { ...window.evaka, loginStatusEventBus }
