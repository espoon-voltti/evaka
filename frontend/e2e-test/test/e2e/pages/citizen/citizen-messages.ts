import { Selector } from 'testcafe'

export default class CitizenMessagesPage {
  readonly message = (index: number) =>
    Selector('[data-qa="bulletin-list-item"]').nth(index)

  readonly messageReaderTitle = Selector('[data-qa="message-reader-title"]')
  readonly messageReaderSender = Selector('[data-qa="message-reader-sender"]')
  readonly messageReaderContent = Selector('[data-qa="message-reader-content"]')
}
