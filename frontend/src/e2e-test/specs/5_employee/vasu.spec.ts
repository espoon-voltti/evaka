// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertVasuDocument,
  insertVasuTemplateFixture,
  resetDatabase
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { EmployeeDetail } from '../../dev-api/types'
import ChildInformationPage, {
  VasuAndLeopsSection
} from '../../pages/employee/child-information'
import { VasuEditPage, VasuPage } from '../../pages/employee/vasu/vasu'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let admin: EmployeeDetail
let childInformationPage: ChildInformationPage
let childId: UUID
let templateId: UUID

beforeAll(async () => {
  await resetDatabase()

  admin = (await Fixture.employeeAdmin().save()).data

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
    await employeeLogin(page, admin)
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
    await employeeLogin(page, admin)
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

      await considerations.childsViewInput.type("This is the child's view")
      await considerations.guardiansViewInput.type(
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

      await previousGoals.goalsRealizedInput.type(
        'The goals were realized by ...'
      )
      await previousGoals.otherObservationsInput.type(
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

      await goals.childsStrengthsInput.type('Super helpful with chores')
      await goals.goalsForTeachersInput.type('Child needs help drawing squares')
      await goals.otherInput.type(
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

      await wellnessSupport.wellnessInput.type(
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

      await otherDocsAndPlans.otherDocsInput.type(
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

      await sharedTo.recipientsOptions('Neuvolaan').click()
      await sharedTo.recipientsOptions('Lasten terapiapalveluihin').click()
      await sharedTo.otherInput.type('Police')

      await vasuEditPage.waitUntilSaved()
      const vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.infoSharedToSection.recipients.innerText,
        'Neuvolaan, Lasten terapiapalveluihin'
      )
      await waitUntilEqual(
        () => vasuPage.infoSharedToSection.other.innerText,
        'Police'
      )
    })

    test('Fill the additional info section', async () => {
      const vasuEditPage = await editDocument()
      const additionalInfo = vasuEditPage.additionalInfoSection

      await additionalInfo.infoInput.type('I love icecream')

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

      await discussion.dateInput.type('1.12.2021')
      await discussion.presentInput.type('Mom, dad, and teacher')
      await discussion.collaborationAndOpinionInput.type(
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

    test('Fill the evaluation section', async () => {
      await finalizeDocument()
      const vasuEditPage = await editDocument()
      const evaluation = vasuEditPage.evaluationSection

      await evaluation.dateInput.type('1.12.2021')
      await evaluation.participantsInput.type('Mom, dad, and teacher')
      await evaluation.collaborationWithGuardiansInput.type(
        'Collaboration is very good'
      )
      await evaluation.evaluationOfGoalsInput.type(
        'All goals reached well ahead of time'
      )

      await vasuEditPage.waitUntilSaved()
      const vasuPage = await openDocument()
      await waitUntilEqual(() => vasuPage.evaluationSection.date, '01.12.2021')
      await waitUntilEqual(
        () => vasuPage.evaluationSection.participants,
        'Mom, dad, and teacher'
      )
      await waitUntilEqual(
        () => vasuPage.evaluationSection.collaborationWithGuardians,
        'Collaboration is very good'
      )
      await waitUntilEqual(
        () => vasuPage.evaluationSection.evaluationOfGoals,
        'All goals reached well ahead of time'
      )
    })
  })

  describe('Followup questions', () => {
    beforeAll(async () => {
      vasuDocId = await insertVasuDocument(childId, templateId)
    })

    test('An unpublished vasu document has no followup questions', async () => {
      const vasuPage = await openDocument()
      await waitUntilEqual(() => vasuPage.followupQuestionCount, 0)
    })

    describe('With a finalized document', () => {
      beforeAll(async () => {
        page = await Page.open()
        await employeeLogin(page, admin)
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

        const expectedMetadataStr = `${LocalDate.todayInSystemTz().format()} Seppo Sorsa`
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

        const date = LocalDate.todayInSystemTz().format()
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
        LocalDate.todayInSystemTz().format()
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
        LocalDate.todayInSystemTz().format()
      )
    })

    const markDocumentClosed = async () => {
      const vasuPage = await openDocument()
      await vasuPage.markClosedButton.click()
      await vasuPage.modalOkButton.click()
      await vasuPage.modalOkButton.click()
    }

    test('Mark a document closed', async () => {
      let vasuPage = await openDocument()

      await finalizeDocument()
      await markDocumentReviewed()
      await markDocumentClosed()

      vasuPage = await openDocument()
      await vasuPage.assertDocumentVisible()

      await waitUntilEqual(() => vasuPage.documentState(), 'Päättynyt')
      await waitUntilEqual(
        () => vasuPage.closedDate(),
        LocalDate.todayInSystemTz().format()
      )
    })
  })
})
