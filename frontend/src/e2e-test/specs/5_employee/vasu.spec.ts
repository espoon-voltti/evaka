// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import LocalDate from 'lib-common/local-date'
import { UUID } from 'lib-common/types'

import config from '../../config'
import {
  insertDaycareGroupFixtures,
  insertDaycarePlacementFixtures,
  insertGuardianFixtures,
  insertVasuDocument,
  insertVasuTemplateFixture,
  resetDatabase
} from '../../dev-api'
import { initializeAreaAndPersonData } from '../../dev-api/data-init'
import {
  createDaycarePlacementFixture,
  daycareFixture,
  daycareGroupFixture,
  Fixture,
  uuidv4
} from '../../dev-api/fixtures'
import {
  DaycarePlacement,
  EmployeeDetail,
  PersonDetailWithDependantsAndGuardians
} from '../../dev-api/types'
import ChildInformationPage, {
  VasuAndLeopsSection
} from '../../pages/employee/child-information'
import { VasuEditPage, VasuPage } from '../../pages/employee/vasu/vasu'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin } from '../../utils/user'

let page: Page
let admin: EmployeeDetail
let unitSupervisor: EmployeeDetail
let childInformationPage: ChildInformationPage
let child: PersonDetailWithDependantsAndGuardians
let templateId: UUID
let daycarePlacementFixture: DaycarePlacement

beforeAll(async () => {
  await resetDatabase()

  admin = (await Fixture.employeeAdmin().save()).data

  const fixtures = await initializeAreaAndPersonData()
  await insertDaycareGroupFixtures([daycareGroupFixture])

  const unitId = fixtures.daycareFixture.id
  child = fixtures.familyWithTwoGuardians.children[0]

  unitSupervisor = (await Fixture.employeeUnitSupervisor(unitId).save()).data

  daycarePlacementFixture = createDaycarePlacementFixture(
    uuidv4(),
    child.id,
    unitId
  )

  await insertDaycarePlacementFixtures([daycarePlacementFixture])

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
  await insertGuardianFixtures([
    {
      guardianId: firstGuardian.id,
      childId: child.id
    },
    {
      guardianId: secondGuardian.id,
      childId: child.id
    }
  ])
})

describe('Child Information - Vasu documents section', () => {
  let section: VasuAndLeopsSection
  beforeEach(async () => {
    page = await Page.open()
    await employeeLogin(page, admin)
    await page.goto(`${config.employeeUrl}/child-information/${child.id}`)
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
    vasuDocId = await insertVasuDocument(child.id, templateId)
  })

  beforeEach(async () => {
    page = await Page.open()
    await employeeLogin(page, admin)
  })

  describe('Fill out document', () => {
    test('Fill the basic info section', async () => {
      const vasuEditPage = await editDocument()
      const basicInfo = vasuEditPage.basicInfoSection
      expect(await basicInfo.childName).toEqual(
        `${child.firstName} ${child.lastName}`
      )
      expect(await basicInfo.childDateOfBirth).toEqual(
        LocalDate.parseIso(child.dateOfBirth).format()
      )
      expect(await basicInfo.placement(0)).toEqual(
        `${daycareFixture.name} (${
          daycareGroupFixture.name
        }) ${LocalDate.parseIso(
          daycarePlacementFixture.startDate
        ).format()} - ${LocalDate.parseIso(
          daycarePlacementFixture.endDate
        ).format()}`
      )
      const [firstGuardian, secondGuardian] = child.guardians ?? []
      expect(await basicInfo.guardian(0)).toEqual(
        `${firstGuardian.firstName} ${firstGuardian.lastName}`
      )
      expect(await basicInfo.guardian(1)).toEqual(
        `${secondGuardian.firstName} ${secondGuardian.lastName}`
      )
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
        'Tukipalvelut ajalla 02.03.2020–11.05.2021'
      )
      await waitUntilEqual(
        () => vasuPage.goalsSection.other,
        'Child snores heavily, waking all the other kids up'
      )
    })

    test('Fill the other info section', async () => {
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
      vasuDocId = await insertVasuDocument(child.id, templateId)
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
        await vasuEditPage.inputFollowupWithDateComment(
          'This is a followup',
          '01.04.2020',
          0,
          0
        )
        await vasuEditPage.inputFollowupWithDateComment(
          'A second one',
          '09.10.2021',
          0,
          1
        )

        await vasuEditPage.waitUntilSaved()
        const refreshedVasuEditPage = await editDocument()

        const expectedMetadataStr = `${LocalDate.todayInSystemTz().format()} Seppo Sorsa`
        await waitUntilEqual(
          () => refreshedVasuEditPage.followupEntryMetadata(0, 0),
          expectedMetadataStr
        )
        await waitUntilEqual(
          () => refreshedVasuEditPage.followupEntryMetadata(0, 1),
          expectedMetadataStr
        )

        await refreshedVasuEditPage.waitUntilSaved()
        const vasuPage = await openDocument()
        await waitUntilEqual(
          () => vasuPage.followupEntry(0, 0),
          '01.04.2020: This is a followup'
        )
        await waitUntilEqual(
          () => vasuPage.followupEntry(0, 1),
          '09.10.2021: A second one'
        )
      })

      test('Followup comments are editable', async () => {
        page = await Page.open()
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

        const expectedMetadataStr = `${LocalDate.todayInSystemTz().format()} Seppo Sorsa, muokattu ${LocalDate.todayInSystemTz().format()} Essi Esimies`
        await waitUntilEqual(
          () => refreshedVasuEditPage.followupEntryMetadata(0, 0),
          expectedMetadataStr
        )
        await waitUntilEqual(
          () => refreshedVasuEditPage.followupEntryMetadata(0, 1),
          expectedMetadataStr
        )

        await refreshedVasuEditPage.waitUntilSaved()
        const vasuPage = await openDocument()
        await waitUntilEqual(
          () => vasuPage.followupEntry(0, 0),
          '01.04.2020: This will be edited'
        )
        await waitUntilEqual(
          () => vasuPage.followupEntry(0, 1),
          '01.08.2021: Edited with date too'
        )
      })
    })
  })

  describe('Publishing of vasu documents', () => {
    beforeEach(async () => {
      vasuDocId = await insertVasuDocument(child.id, templateId)
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
