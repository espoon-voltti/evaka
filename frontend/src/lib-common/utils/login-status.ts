export class LoginStatusChangeEvent extends Event {
  constructor(public readonly loginStatus: boolean) {
    super(LoginStatusChangeEvent.name)
  }
}
export class LoginStatusEventManager extends EventTarget {}

export const loginStatusEventBus = new LoginStatusEventManager()
window.evaka = { ...window.evaka, loginStatusEventBus }
