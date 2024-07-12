import { Element } from '../../utils/page'

export class DiscussionReservationModal extends Element {
  #modalSendButton: Element

  constructor(el: Element) {
    super(el)
    this.#modalSendButton = this.findByDataQa('modal-okBtn')
  }

  async save() {
    await this.#modalSendButton.click()
  }
}

export class DiscussionSurveyModal extends Element {
  constructor(el: Element) {
    super(el)
  }

  assertChildSurvey = async (surveyId: string, childId: string) => {
    const childElement = this.findByDataQa(`child-element-${childId}`)
    await childElement.waitUntilVisible()

    const surveyElement = childElement.findByDataQa(
      `survey-element-${surveyId}`
    )
    await surveyElement.waitUntilVisible()
  }

  assertChildReservation = async (
    surveyId: string,
    childId: string,
    _: string
  ) => {
    const childElement = this.findByDataQa(`child-element-${childId}`)
    await childElement.waitUntilVisible()

    const surveyElement = childElement.findByDataQa(
      `survey-element-${surveyId}`
    )
    await surveyElement.waitUntilVisible()

    surveyElement.findAllByDataQa(`reservation-element`)
  }

  openReservationModal = async (surveyId: string, childId: string) => {
    const childElement = this.findByDataQa(`child-element-${childId}`)
    await childElement.waitUntilVisible()

    const surveyElement = childElement.findByDataQa(
      `survey-element-${surveyId}`
    )
    await surveyElement.waitUntilVisible()

    const surveyReservationButton =
      surveyElement.findByDataQa(`reservation-button`)
    await surveyReservationButton.click()
  }
}
