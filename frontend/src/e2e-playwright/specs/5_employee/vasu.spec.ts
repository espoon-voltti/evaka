// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import config from 'e2e-test-common/config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertVasuDocument,
  insertVasuTemplateFixture,
  resetDatabase
} from 'e2e-test-common/dev-api'
import { initializeAreaAndPersonData } from 'e2e-test-common/dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  uuidv4
} from 'e2e-test-common/dev-api/fixtures'
import ChildInformationPage, {
  VasuAndLeopsSection
} from 'e2e-playwright/pages/employee/child-information'
import { employeeLogin } from 'e2e-playwright/utils/user'
import { UUID } from 'lib-common/types'
import { VasuEditPage, VasuPage } from '../../pages/employee/vasu/vasu'
import LocalDate from 'lib-common/local-date'
import { Page } from '../../utils/page'
import { waitUntilEqual } from '../../utils'

let page: Page
let childInformationPage: ChildInformationPage
let childId: UUID
let templateId: UUID

beforeAll(async () => {
  await resetDatabase()

  const fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const unitId = fixtures.daycareFixture.id
  childId = fixtures.familyWithTwoGuardians.children[0].id

  const daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    childId,
    unitId
  )

  await insertDaycarePlacementFixtures([daycarePlacementFixture])
  templateId = await insertVasuTemplateFixture()
})

describe('Child Information - Vasu documents section', () => {
  let section: VasuAndLeopsSection
  beforeEach(async () => {
    page = await Page.open()
    await employeeLogin(page, 'ADMIN')
    await page.goto(`${config.employeeUrl}/child-information/${childId}`)
    childInformationPage = new ChildInformationPage(page)
    section = await childInformationPage.openCollapsible('vasuAndLeops')
  })

  test('Can add a new vasu document', async () => {
    await section.addNew()
  })
})

describe('Vasu document page', () => {
  let vasuDocId: UUID

  const openDocument = async () => {
    await page.goto(`${config.employeeUrl}/vasu/${vasuDocId}`)
    return new VasuPage(page)
  }

  const editDocument = async () => {
    await page.goto(`${config.employeeUrl}/vasu/${vasuDocId}/edit`)
    return new VasuEditPage(page)
  }

  const finalizeDocument = async () => {
    let vasuPage = await openDocument()
    await vasuPage.finalizeButton.click()
    await vasuPage.modalOkButton.click()
    await vasuPage.modalOkButton.click()
    vasuPage = await openDocument()
    await vasuPage.assertDocumentVisible()
  }

  beforeAll(async () => {
    vasuDocId = await insertVasuDocument(childId, templateId)
  })

  beforeEach(async () => {
    page = await Page.open()
    await employeeLogin(page, 'ADMIN')
  })

  describe('Fill out document', () => {
    test('Fill the authors section', async () => {
      const vasuEditPage = await editDocument()
      const authors = vasuEditPage.authorsSection
      await authors.primaryFirstNameInput.fill('Leena')
      await authors.primaryLastNameInput.fill('Virtanen')
      await authors.primaryTitleInput.fill('Johtaja')
      await authors.primaryPhoneNumberInput.fill('1234565')

      await waitUntilEqual(() => authors.otherFieldsCount, 1)
      await authors.otherFirstNameInput(0).fill('Veena')
      await authors.otherLastNameInput(0).fill('Lirtanen')
      await authors.otherTitleInput(0).fill('Hoitaja')
      await authors.otherPhoneNumberInput(0).fill('1234565')

      await waitUntilEqual(() => authors.otherFieldsCount, 2)
      await authors.otherFirstNameInput(1).fill('Aneev')
      await authors.otherLastNameInput(1).fill('Nenatril')
      await authors.otherTitleInput(1).fill('Hoitaja')
      await authors.otherPhoneNumberInput(1).fill('5654321')

      await waitUntilEqual(() => authors.otherFieldsCount, 3)
      await vasuEditPage.waitUntilSaved()

      const vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.authorsSection.primaryValue,
        'Leena Virtanen Johtaja 1234565'
      )
      await waitUntilEqual(
        () => vasuPage.authorsSection.otherValues,
        'Veena Lirtanen Hoitaja 1234565,\nAneev Nenatril Hoitaja 5654321'
      )
    })

    test('Fill the consideration of views section', async () => {
      const vasuEditPage = await editDocument()
      const considerations = vasuEditPage.considerationsSection

      await considerations.childsViewInput.fill("This is the child's view")
      await considerations.guardiansViewInput.fill(
        "This is the guardian's view"
      )

      await vasuEditPage.waitUntilSaved()
      const vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.considerationsSection.childsView,
        "This is the child's view"
      )
      await waitUntilEqual(
        () => vasuPage.considerationsSection.guardiansView,
        "This is the guardian's view"
      )
    })

    test('Fill the previous vasu goals section', async () => {
      const vasuEditPage = await editDocument()
      const previousGoals = vasuEditPage.previousVasuGoalsSection

      await previousGoals.goalsRealizedInput.fill(
        'The goals were realized by ...'
      )
      await previousGoals.otherObservationsInput.fill(
        'Other observations include ...'
      )

      await vasuEditPage.waitUntilSaved()
      const vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.previousVasuGoalsSection.goalsRealized,
        'The goals were realized by ...'
      )
      await waitUntilEqual(
        () => vasuPage.previousVasuGoalsSection.otherObservations,
        'Other observations include ...'
      )
    })

    test('Fill the goals section', async () => {
      const vasuEditPage = await editDocument()
      const goals = vasuEditPage.goalsSection

      await goals.childsStrengthsInput.fill('Super helpful with chores')
      await goals.goalsForTeachersInput.fill('Child needs help drawing squares')
      await goals.otherInput.fill(
        'Child snores heavily, waking all the other kids up'
      )

      await vasuEditPage.waitUntilSaved()
      const vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.goalsSection.childsStrengths,
        'Super helpful with chores'
      )
      await waitUntilEqual(
        () => vasuPage.goalsSection.goalsForTeachers,
        'Child needs help drawing squares'
      )
      await waitUntilEqual(
        () => vasuPage.goalsSection.other,
        'Child snores heavily, waking all the other kids up'
      )
    })

    test('Fill the wellness support section', async () => {
      const vasuEditPage = await editDocument()
      const wellnessSupport = vasuEditPage.wellnessSupportSection

      await wellnessSupport.wellnessInput.fill(
        'Only sleeps for 0.2 seconds at a time'
      )

      await vasuEditPage.waitUntilSaved()
      const vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.wellnessSupportSection.wellness,
        'Only sleeps for 0.2 seconds at a time'
      )
    })

    test('Fill the other documents and plans section', async () => {
      const vasuEditPage = await editDocument()
      const otherDocsAndPlans = vasuEditPage.otherDocsAndPlansSection

      await otherDocsAndPlans.otherDocsInput.fill(
        'Drawings made by child and parents'
      )

      await vasuEditPage.waitUntilSaved()
      const vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.otherDocsAndPlansSection.otherDocs,
        'Drawings made by child and parents'
      )
    })

    test('Fill the info shared to section', async () => {
      const vasuEditPage = await editDocument()
      const sharedTo = vasuEditPage.infoSharedToSection

      await sharedTo.recipientsOptions.nth(1).click()
      await sharedTo.recipientsOptions.nth(2).click()
      await sharedTo.otherInput.fill('Police')

      await vasuEditPage.waitUntilSaved()
      const vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.infoSharedToSection.recipients,
        'Neuvolaan, Lasten terapiapalveluihin'
      )
      await waitUntilEqual(() => vasuPage.infoSharedToSection.other, 'Police')
    })

    test('Fill the additional info section', async () => {
      const vasuEditPage = await editDocument()
      const additionalInfo = vasuEditPage.additionalInfoSection

      await additionalInfo.infoInput.fill('I love icecream')

      await vasuEditPage.waitUntilSaved()
      const vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.additionalInfoSection.info,
        'I love icecream'
      )
    })

    test('Fill the discussion section', async () => {
      const vasuEditPage = await editDocument()
      const discussion = vasuEditPage.discussionSection

      await discussion.dateInput.fill('1.12.2021')
      await discussion.presentInput.fill('Mom, dad, and teacher')
      await discussion.collaborationAndOpinionInput.fill(
        'Everything is awesome'
      )

      await vasuEditPage.waitUntilSaved()
      const vasuPage = await openDocument()
      await waitUntilEqual(() => vasuPage.discussionSection.date, '01.12.2021')
      await waitUntilEqual(
        () => vasuPage.discussionSection.present,
        'Mom, dad, and teacher'
      )
      await waitUntilEqual(
        () => vasuPage.discussionSection.collaborationAndOpinion,
        'Everything is awesome'
      )
    })
  })

  describe('Followup questions', () => {
    test('An unpublished vasu document has no followup questions', async () => {
      const vasuPage = await openDocument()
      await waitUntilEqual(() => vasuPage.followupQuestionCount, 0)
    })

    describe('With a finalized document', () => {
      beforeAll(async () => {
        page = await Page.open()
        await employeeLogin(page, 'ADMIN')
        await finalizeDocument()
      })

      test('A published vasu document has one followup question', async () => {
        const vasuPage = await openDocument()
        await waitUntilEqual(() => vasuPage.followupQuestionCount, 1)
      })

      test('Adding a followup comment renders it on the page', async () => {
        const vasuEditPage = await editDocument()
        await vasuEditPage.inputFollowupComment('This is a followup')
        await waitUntilEqual(
          () => vasuEditPage.followupEntryTexts,
          ['This is a followup']
        )
        await vasuEditPage.inputFollowupComment('A second one')
        await waitUntilEqual(
          () => vasuEditPage.followupEntryTexts,
          ['This is a followup', 'A second one']
        )

        const expectedMetadataStr = `${LocalDate.today().format()} Seppo Sorsa`
        await waitUntilEqual(
          () => vasuEditPage.followupEntryMetadata,
          [expectedMetadataStr, expectedMetadataStr]
        )
      })

      const lastElement = <T>(arr: Array<T>): T => arr[arr.length - 1]

      test('A user can edit his own followup comment', async () => {
        const vasuEditPage = await editDocument()
        await vasuEditPage.inputFollowupComment('This will be edited')
        await waitUntilEqual(
          () => vasuEditPage.followupEntryTexts.then(lastElement),
          'This will be edited'
        )

        const entryCount = (await vasuEditPage.followupEntryTexts).length
        await vasuEditPage.editFollowupComment(entryCount - 1, 'now edited: ')

        await waitUntilEqual(
          () => vasuEditPage.followupEntryTexts.then(lastElement),
          'now edited: This will be edited'
        )

        const date = LocalDate.today().format()
        await waitUntilEqual(
          () => vasuEditPage.followupEntryMetadata.then(lastElement),
          `${date} Seppo Sorsa, muokattu ${date} Seppo Sorsa`
        )
      })
    })
  })

  describe('Publishing of vasu documents', () => {
    beforeEach(async () => {
      vasuDocId = await insertVasuDocument(childId, templateId)
    })

    test('Finalize a document', async () => {
      let vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.publishedToGuardiansDate(),
        'Ei merkintää'
      )

      await finalizeDocument()
      vasuPage = await openDocument()

      await waitUntilEqual(() => vasuPage.documentState(), 'Laadittu')
      await waitUntilEqual(
        () => vasuPage.publishedDate(),
        LocalDate.today().format()
      )
    })

    const markDocumentReviewed = async () => {
      const vasuPage = await openDocument()
      await vasuPage.markReviewedButton.click()
      await vasuPage.modalOkButton.click()
      await vasuPage.modalOkButton.click()
    }

    test('Publish a document as reviewed', async () => {
      let vasuPage = await openDocument()

      await finalizeDocument()
      await markDocumentReviewed()

      vasuPage = await openDocument()
      await vasuPage.assertDocumentVisible()

      await waitUntilEqual(() => vasuPage.documentState(), 'Arvioitu')
      await waitUntilEqual(
        () => vasuPage.reviewedDate(),
        LocalDate.today().format()
      )
    })
  })
})
