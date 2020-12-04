import { Selector } from 'testcafe'

export default class PairingFlow {
  readonly mobileStartPairingBtn = Selector('[data-qa="start-pairing-btn"]')
  readonly mobilePairingTitle1 = Selector(
    '[data-qa="mobile-pairing-wizard-title-1"]'
  )
  readonly mobilePairingTitle3 = Selector(
    '[data-qa="mobile-pairing-wizard-title-3"]'
  )
  readonly challengeKeyInput = Selector('[data-qa="challenge-key-input"]')
  readonly submitChallengeKeyBtn = Selector(
    '[data-qa="submit-challenge-key-btn"]'
  )
  readonly responseKey = Selector('[data-qa="response-key"]')
}
