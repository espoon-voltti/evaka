// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import assert from 'assert'

import FiniteDateRange from 'lib-common/finite-date-range'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { UUID } from 'lib-common/types'

import config from '../../config'
import { insertVasuTemplateFixture, runPendingAsyncJobs } from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareFixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import { PersonDetailWithDependantsAndGuardians } from '../../dev-api/types'
import {
  createDaycareGroups,
  createDaycarePlacements,
  createVasuDocument,
  getSentEmails,
  insertGuardians,
  resetDatabase
} from '../../generated/api-clients'
import { DevEmployee, DevPlacement } from '../../generated/api-types'
import ChildInformationPage, {
  ChildDocumentsSection
} from '../../pages/employee/child-information'
import { VasuEditPage, VasuPage } from '../../pages/employee/vasu/vasu'
import { waitUntilEqual, waitUntilTrue } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let admin: DevEmployee
let unitSupervisor: DevEmployee
let childInformationPage: ChildInformationPage
let child: PersonDetailWithDependantsAndGuardians
let templateId: UUID
let daycarePlacementFixture: DevPlacement

const mockedTime = LocalDate.of(2022, 12, 20)

beforeAll(async () => {
  await resetDatabase()

  admin = (await Fixture.employeeAdmin().save()).data

  const fixtures = await initializeAreaAndPersonData()
  await createDaycareGroups({ body: [daycareGroupFixture] })

  const unitId = fixtures.daycareFixture.id
  child = fixtures.familyWithTwoGuardians.children[0]

  unitSupervisor = (await Fixture.employeeUnitSupervisor(unitId).save()).data

  daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    child.id,
    unitId
  )

  await createDaycarePlacements({ body: [daycarePlacementFixture] })

  await Fixture.groupPlacement()
    .with({
      daycareGroupId: daycareGroupFixture.id,
      daycarePlacementId: daycarePlacementFixture.id,
      startDate: daycarePlacementFixture.startDate,
      endDate: daycarePlacementFixture.endDate
    })
    .save()

  templateId = await insertVasuTemplateFixture()

  const [firstGuardian, secondGuardian] = child.guardians ?? []
  await insertGuardians({
    body: [
      {
        guardianId: firstGuardian.id,
        childId: child.id
      },
      {
        guardianId: secondGuardian.id,
        childId: child.id
      }
    ]
  })
})

const openPage = async (addDays = 0) =>
  await Page.open({
    mockedTime: mockedTime
      .addDays(addDays)
      .toHelsinkiDateTime(LocalTime.of(12, 0))
  })

describe('Child Information - Vasu documents section', () => {
  let section: ChildDocumentsSection
  beforeEach(async () => {
    page = await openPage()
    await employeeLogin(page, admin)
    await page.goto(`${config.employeeUrl}/child-information/${child.id}`)
    childInformationPage = new ChildInformationPage(page)
    section = await childInformationPage.openCollapsible('childDocuments')
  })

  test('Can add a new vasu document', async () => {
    await section.addNewVasu()
  })
})

describe('Child Information - Vasu language', () => {
  let section: ChildDocumentsSection
  beforeEach(async () => {
    const child = await Fixture.person().save()
    await Fixture.child(child.data.id).save()
    const swedishUnit = await Fixture.daycare()
      .careArea(await Fixture.careArea().save())
      .with({ language: 'sv', enabledPilotFeatures: ['VASU_AND_PEDADOC'] })
      .save()
    const placementDateRange = new FiniteDateRange(
      mockedTime.subMonths(1),
      mockedTime.addMonths(5)
    )
    await Fixture.placement()
      .daycare(swedishUnit)
      .with({
        childId: child.data.id,
        startDate: placementDateRange.start,
        endDate: placementDateRange.end
      })
      .save()
    await insertVasuTemplateFixture({
      language: 'SV',
      valid: placementDateRange
    })

    page = await openPage()
    await employeeLogin(page, admin)
    await page.goto(`${config.employeeUrl}/child-information/${child.data.id}`)
    childInformationPage = new ChildInformationPage(page)
    section = await childInformationPage.openCollapsible('childDocuments')
  })

  test('Child placed in a Swedish unit can only use Swedish templates', async () => {
    await section.addNewVasu()
    await page
      .findAllByDataQa('vasu-state-chip')
      .nth(1)
      .assertTextEquals('Utkast')
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
    vasuDocId = await createVasuDocument({
      body: { childId: child.id, templateId }
    })
  })

  describe('Fill out document', () => {
    test('Fill the basic info section', async () => {
      page = await openPage()
      await employeeLogin(page, admin)

      const vasuEditPage = await editDocument()
      const basicInfo = vasuEditPage.basicInfoSection
      await waitUntilEqual(
        () => basicInfo.childName,
        `${child.firstName} ${child.lastName}`
      )
      await waitUntilEqual(
        () => basicInfo.childDateOfBirth,
        child.dateOfBirth.format()
      )
      await waitUntilEqual(
        () => basicInfo.placement(0),
        `${daycareFixture.name} (${
          daycareGroupFixture.name
        }) ${daycarePlacementFixture.startDate.format()} - ${daycarePlacementFixture.endDate.format()}`
      )
      const [firstGuardian, secondGuardian] = child.guardians ?? []
      await waitUntilTrue(async () => {
        const guardian1 = await basicInfo.guardian(0)
        const guardian2 = await basicInfo.guardian(1)
        return (
          (guardian1 ===
            `${firstGuardian.firstName} ${firstGuardian.lastName}` &&
            guardian2 ===
              `${secondGuardian.firstName} ${secondGuardian.lastName}`) ||
          (guardian1 ===
            `${secondGuardian.firstName} ${secondGuardian.lastName}` &&
            guardian2 ===
              `${firstGuardian.firstName} ${firstGuardian.lastName}`)
        )
      })
      await basicInfo.additionalContactInfoInput.fill(
        'Only contact during 8-12'
      )

      await vasuEditPage.waitUntilSaved()

      const vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.basicInfoSection.additionalContactInfo,
        'Only contact during 8-12'
      )
    })

    test('Fill the authoring section', async () => {
      page = await openPage()
      await employeeLogin(page, admin)

      const vasuEditPage = await editDocument()
      const authoring = vasuEditPage.authoringSection
      await authoring.primaryFirstNameInput.fill('Leena')
      await authoring.primaryLastNameInput.fill('Virtanen')
      await authoring.primaryTitleInput.fill('Johtaja')
      await authoring.primaryPhoneNumberInput.fill('1234565')

      await waitUntilEqual(() => authoring.otherFieldsCount, 1)
      await authoring.otherFirstNameInput(0).fill('Veena')
      await authoring.otherLastNameInput(0).fill('Lirtanen')
      await authoring.otherTitleInput(0).fill('Hoitaja')
      await authoring.otherPhoneNumberInput(0).fill('1234565')

      await waitUntilEqual(() => authoring.otherFieldsCount, 2)
      await authoring.otherFirstNameInput(1).fill('Aneev')
      await authoring.otherLastNameInput(1).fill('Nenatril')
      await authoring.otherTitleInput(1).fill('Hoitaja')
      await authoring.otherPhoneNumberInput(1).fill('5654321')

      await waitUntilEqual(() => authoring.otherFieldsCount, 3)

      await authoring.childPOVInput.fill('Hearing')
      await authoring.guardianPOVInput.fill('Listening')

      await vasuEditPage.waitUntilSaved()

      const vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.authoringSection.primaryValue,
        'Leena Virtanen Johtaja 1234565'
      )
      await waitUntilEqual(
        () => vasuPage.authoringSection.otherValues,
        'Veena Lirtanen Hoitaja 1234565,\nAneev Nenatril Hoitaja 5654321'
      )
      await waitUntilEqual(() => vasuPage.authoringSection.childPOV, 'Hearing')
      await waitUntilEqual(
        () => vasuPage.authoringSection.guardianPOV,
        'Listening'
      )
    })

    test('Fill the multidisciplinary cooperation section', async () => {
      page = await openPage()
      await employeeLogin(page, admin)

      const vasuEditPage = await editDocument()
      const cooperation = vasuEditPage.cooperationSection

      await cooperation.collaboratorsInput.type(
        'John Doe, child health centre director'
      )
      await cooperation.methodsOfCooperationInput.type('Visit once a week')

      await vasuEditPage.waitUntilSaved()
      const vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.cooperationSection.collaborators,
        'John Doe, child health centre director'
      )
      await waitUntilEqual(
        () => vasuPage.cooperationSection.methodsOfCooperation,
        'Visit once a week'
      )
    })

    test('Fill the vasu goals section', async () => {
      page = await openPage()
      await employeeLogin(page, admin)

      const vasuEditPage = await editDocument()
      const goals = vasuEditPage.vasuGoalsSection

      await goals.goalsRealizationInput.type(
        'The goals are going to be realized by ...'
      )
      await goals.specialNeedsEstimationInput.type('Some special needs ...')
      await goals.otherObservationsInput.type('Other observations include ...')

      await vasuEditPage.waitUntilSaved()
      const vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.vasuGoalsSection.goalsRealization,
        'The goals are going to be realized by ...'
      )
      await waitUntilEqual(
        () => vasuPage.vasuGoalsSection.specialNeedsEstimation,
        'Some special needs ...'
      )
      await waitUntilEqual(
        () => vasuPage.vasuGoalsSection.otherObservations,
        'Other observations include ...'
      )
    })

    test('Fill the goals section', async () => {
      page = await openPage()
      await employeeLogin(page, admin)

      const vasuEditPage = await editDocument()
      const goals = vasuEditPage.goalsSection

      await goals.childsStrengthsInput.type('Super helpful with chores')
      await goals.languageViewsInput.type('Daily reminder')
      await goals.pedagogicalSupportInput.type('Flash cards')
      await goals.structuralSupportInput.type('Small groups')
      await goals.therapeuticSupportInput.type('Asthma')
      await goals.staffGoalsInput.type('Child needs help drawing squares')
      await goals.actionsInput.type('Show flash cards')
      await goals.supportLevelOptions('during_range').click()
      await goals
        .supportLevelOptionRangeStart('during_range')
        .fill('02.03.2020')
      await goals.supportLevelOptionRangeEnd('during_range').fill('11.05.2021')
      await goals.supportLevelOptions('intensified').click()
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
        () => vasuPage.goalsSection.languageViews,
        'Daily reminder'
      )
      await waitUntilEqual(
        () => vasuPage.goalsSection.pedagogicalSupport,
        'Flash cards'
      )
      await waitUntilEqual(
        () => vasuPage.goalsSection.structuralSupport,
        'Small groups'
      )
      await waitUntilEqual(
        () => vasuPage.goalsSection.therapeuticSupport,
        'Asthma'
      )
      await waitUntilEqual(
        () => vasuPage.goalsSection.staffGoals,
        'Child needs help drawing squares'
      )
      await waitUntilEqual(
        () => vasuPage.goalsSection.actions,
        'Show flash cards'
      )
      await waitUntilEqual(
        () => vasuPage.goalsSection.supportLevel,
        'Tukipalvelut ajalla 02.03.2020–11.05.2021, Tehostettu tuki'
      )
      await waitUntilEqual(
        () => vasuPage.goalsSection.other,
        'Child snores heavily, waking all the other kids up'
      )
    })

    test('Fill the other info section', async () => {
      page = await openPage()
      await employeeLogin(page, admin)

      const vasuEditPage = await editDocument()
      const section = vasuEditPage.otherSection

      await section.otherInput.type('Something else...')

      await vasuEditPage.waitUntilSaved()

      const vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.otherSection.other,
        'Something else...'
      )
    })

    test('Fill the other documents and plans section', async () => {
      page = await openPage()
      await employeeLogin(page, admin)

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
      page = await openPage()
      await employeeLogin(page, admin)

      const vasuEditPage = await editDocument()
      const sharedTo = vasuEditPage.infoSharedToSection

      await sharedTo.recipientsOptions('Neuvolaan').click()
      await sharedTo.recipientsOptions('Lasten terapiapalveluihin').click()
      await sharedTo.otherInput.type('Police')

      await vasuEditPage.waitUntilSaved()
      const vasuPage = await openDocument()
      await vasuPage.infoSharedToSection.recipients.assertTextEquals(
        'Neuvolaan, Lasten terapiapalveluihin'
      )
      await vasuPage.infoSharedToSection.other.assertTextEquals('Police')
    })

    test('Fill the discussion section', async () => {
      page = await openPage()
      await employeeLogin(page, admin)

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
      page = await openPage()
      await employeeLogin(page, admin)

      const vasuEditPage = await editDocument()
      const evaluation = vasuEditPage.evaluationSection

      await evaluation.descriptionInput.type(
        '1.12.2021, Collaboration is very good'
      )

      await vasuEditPage.waitUntilSaved()
      const vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.evaluationSection.description,
        '1.12.2021, Collaboration is very good'
      )
    })
  })

  describe('Followup questions', () => {
    beforeAll(async () => {
      vasuDocId = await createVasuDocument({
        body: { childId: child.id, templateId }
      })
    })

    test('An unpublished vasu document has no followup questions', async () => {
      page = await openPage()
      await employeeLogin(page, admin)

      const vasuPage = await openDocument()
      await waitUntilEqual(() => vasuPage.followupQuestionCount, 0)
    })

    describe('With a finalized document', () => {
      beforeAll(async () => {
        page = await openPage()
        await employeeLogin(page, admin)
        await finalizeDocument()
      })

      test('A published vasu document has one followup question', async () => {
        page = await openPage()
        await employeeLogin(page, admin)

        const vasuPage = await openDocument()
        await waitUntilEqual(() => vasuPage.followupQuestionCount, 1)
      })

      test('Adding a followup comment renders it on the page', async () => {
        page = await openPage()
        await employeeLogin(page, admin)

        const vasuEditPage = await editDocument()
        await vasuEditPage.addFollowup()
        await vasuEditPage.inputFollowupWithDateComment(
          'This is a followup',
          '01.04.2020',
          0,
          0
        )

        await vasuEditPage.addFollowup()
        await vasuEditPage.inputFollowupWithDateComment(
          'A second one',
          '09.10.2021',
          0,
          1
        )

        await vasuEditPage.waitUntilSaved()
        const refreshedVasuEditPage = await editDocument()

        const expectedMetadataStr = `${LocalDate.todayInSystemTz().format()} Seppo Sorsa`
        await refreshedVasuEditPage
          .followupEntryMetadata(0, 0)
          .assertTextEquals(expectedMetadataStr)
        await refreshedVasuEditPage
          .followupEntryMetadata(0, 1)
          .assertTextEquals(expectedMetadataStr)

        await refreshedVasuEditPage.waitUntilSaved()
        const vasuPage = await openDocument()
        await waitUntilEqual(() => vasuPage.followupEntry(0, 0), {
          date: '01.04.2020:',
          text: 'This is a followup'
        })
        await waitUntilEqual(() => vasuPage.followupEntry(0, 1), {
          date: '09.10.2021:',
          text: 'A second one'
        })
      })

      test('Followup comments are editable', async () => {
        page = await openPage()
        await employeeLogin(page, admin)

        page = await openPage()
        await employeeLogin(page, unitSupervisor)

        const vasuEditPage = await editDocument()
        await vasuEditPage.inputFollowupComment('This will be edited', 0, 0)
        await vasuEditPage.inputFollowupWithDateComment(
          'Edited with date too',
          '01.08.2021',
          0,
          1
        )

        await vasuEditPage.waitUntilSaved()
        const refreshedVasuEditPage = await editDocument()

        const expectedMetadataStr = `${LocalDate.todayInHelsinkiTz().format()} Seppo Sorsa, muokattu ${mockedTime.format()} Essi Esimies`
        await refreshedVasuEditPage
          .followupEntryMetadata(0, 0)
          .assertTextEquals(expectedMetadataStr)
        await refreshedVasuEditPage
          .followupEntryMetadata(0, 1)
          .assertTextEquals(expectedMetadataStr)

        await refreshedVasuEditPage.waitUntilSaved()
        const vasuPage = await openDocument()
        await waitUntilEqual(() => vasuPage.followupEntry(0, 0), {
          date: '01.04.2020:',
          text: 'This will be edited'
        })
        await waitUntilEqual(() => vasuPage.followupEntry(0, 1), {
          date: '01.08.2021:',
          text: 'Edited with date too'
        })
      })
    })
  })

  describe('Publishing of vasu documents', () => {
    beforeEach(async () => {
      vasuDocId = await createVasuDocument({
        body: { childId: child.id, templateId }
      })
    })

    test('Finalize a document', async () => {
      page = await openPage()
      await employeeLogin(page, admin)

      let vasuPage = await openDocument()
      await waitUntilEqual(
        () => vasuPage.publishedToGuardiansDate(),
        'Ei merkintää'
      )

      await finalizeDocument()
      vasuPage = await openDocument()

      await vasuPage.documentState.assertTextEquals('Laadittu')
      await waitUntilEqual(
        () => vasuPage.publishedDate(),
        LocalDate.todayInSystemTz().format()
      )

      await runPendingAsyncJobs(
        HelsinkiDateTime.now() // TODO: use mock clock
      )
      const emails = await getSentEmails()

      // eslint-disable-next-line
      const guardianEmails = child.guardians!.map((guardian) => guardian.email)
      assert(emails.every((email) => guardianEmails.includes(email.toAddress)))
      assert(
        guardianEmails.every((guardianEmailAddress) =>
          emails.some(
            (sentEmail) => sentEmail.toAddress === guardianEmailAddress
          )
        )
      )
    })

    const markDocumentReviewed = async () => {
      const vasuPage = await openDocument()
      await vasuPage.markReviewedButton.click()
      await vasuPage.modalOkButton.click()
      await vasuPage.modalOkButton.click()
    }

    test('Publish a document as reviewed', async () => {
      page = await openPage()
      await employeeLogin(page, admin)

      let vasuPage = await openDocument()
      await finalizeDocument()

      page = await openPage(1)
      await employeeLogin(page, admin)
      await markDocumentReviewed()

      vasuPage = await openDocument()
      await vasuPage.assertDocumentVisible()

      await vasuPage.documentState.assertTextEquals('Arvioitu')
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
      page = await openPage()
      await employeeLogin(page, unitSupervisor)

      let vasuPage = await openDocument()

      await finalizeDocument()

      page = await openPage(1)
      await employeeLogin(page, unitSupervisor)
      await markDocumentReviewed()

      await markDocumentClosed()

      vasuPage = await openDocument()
      await vasuPage.assertDocumentVisible()

      await vasuPage.documentState.assertTextEquals('Päättynyt')
      await waitUntilEqual(
        () => vasuPage.closedDate(),
        LocalDate.todayInSystemTz().format()
      )
    })
  })
})
