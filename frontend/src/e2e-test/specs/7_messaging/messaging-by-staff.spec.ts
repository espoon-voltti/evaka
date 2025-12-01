// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type {
  DaycareId,
  GroupId,
  PersonId
} from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import { formatPersonName } from 'lib-common/names'

import config from '../../config'
import { runPendingAsyncJobs } from '../../dev-api'
import {
  Fixture,
  testAdult,
  testAdult2,
  testCareArea,
  testChild,
  testChild2,
  testDaycare,
  testDaycare2,
  testDaycareGroup,
  testPreschool
} from '../../dev-api/fixtures'
import {
  createDaycareGroups,
  createMessageAccounts,
  insertGuardians,
  resetServiceState,
  upsertWeakCredentials
} from '../../generated/api-clients'
import type { DevEmployee, DevPerson } from '../../generated/api-types'
import CitizenMessagesPage from '../../pages/citizen/citizen-messages'
import MessagesPage from '../../pages/employee/messages/messages-page'
import { waitUntilEqual } from '../../utils'
import { Page } from '../../utils/page'
import { employeeLogin, enduserLogin, enduserLoginWeak } from '../../utils/user'

let staffPage: Page
let unitSupervisorPage: Page
let citizenPage: Page
let childId: PersonId
let staff: DevEmployee
let unitSupervisor: DevEmployee

const mockedDate = LocalDate.of(2020, 5, 21)
const mockedDateAt10 = HelsinkiDateTime.fromLocal(
  mockedDate,
  LocalTime.of(10, 2)
)
const mockedDateAt11 = HelsinkiDateTime.fromLocal(
  mockedDate,
  LocalTime.of(11, 31)
)
const mockedDateAt12 = HelsinkiDateTime.fromLocal(
  mockedDate,
  LocalTime.of(12, 17)
)
const credentials = {
  username: 'test@example.com',
  password: 'TestPassword456!'
}
beforeEach(async () => {
  await resetServiceState()
  await testCareArea.save()
  await testDaycare.save()
  await testPreschool.save()
  await Fixture.family({
    guardian: testAdult,
    children: [testChild, testChild2]
  }).save()
  await createDaycareGroups({ body: [testDaycareGroup] })

  await upsertWeakCredentials({
    id: testAdult.id,
    body: credentials
  })

  staff = await Fixture.employee()
    .staff(testDaycare.id)
    .groupAcl(testDaycareGroup.id, mockedDateAt10, mockedDateAt10)
    .save()

  unitSupervisor = await Fixture.employee()
    .unitSupervisor(testDaycare.id)
    .unitSupervisor(testPreschool.id)
    .save()

  const unitId = testDaycare.id
  childId = testChild.id // born 7.7.2014

  const daycarePlacementFixture1 = await Fixture.placement({
    childId,
    unitId,
    startDate: mockedDate,
    endDate: mockedDate.addYears(1)
  }).save()
  await Fixture.groupPlacement({
    daycarePlacementId: daycarePlacementFixture1.id,
    daycareGroupId: testDaycareGroup.id,
    startDate: mockedDate,
    endDate: mockedDate.addYears(1)
  }).save()

  const daycarePlacementFixture2 = await Fixture.placement({
    childId: testChild2.id,
    unitId,
    startDate: mockedDate,
    endDate: mockedDate.addYears(1)
  }).save()
  await Fixture.groupPlacement({
    daycarePlacementId: daycarePlacementFixture2.id,
    daycareGroupId: testDaycareGroup.id,
    startDate: mockedDate,
    endDate: mockedDate.addYears(1)
  }).save()

  await insertGuardians({
    body: [
      {
        childId: childId,
        guardianId: testAdult.id
      }
    ]
  })

  await insertGuardians({
    body: [
      {
        childId: testChild2.id,
        guardianId: testAdult.id
      }
    ]
  })

  await createMessageAccounts()
})

async function initStaffPage(mockedTime: HelsinkiDateTime) {
  staffPage = await Page.open({ mockedTime })
  await employeeLogin(staffPage, staff)
}

async function initUnitSupervisorPage(mockedTime: HelsinkiDateTime) {
  unitSupervisorPage = await Page.open({
    mockedTime: mockedTime
  })
  await employeeLogin(unitSupervisorPage, unitSupervisor)
}

async function initCitizenPage(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({ mockedTime })
  await enduserLogin(citizenPage, testAdult)
}

async function initOtherCitizenPage(
  mockedTime: HelsinkiDateTime,
  citizen: DevPerson
) {
  citizenPage = await Page.open({ mockedTime })
  await enduserLogin(citizenPage, citizen)
}

async function initCitizenPageWeak(mockedTime: HelsinkiDateTime) {
  citizenPage = await Page.open({ mockedTime })
  await enduserLoginWeak(citizenPage, credentials)
}

const defaultReply = 'Testivastaus testiviestiin'

const defaultMessage = {
  title: 'Otsikko',
  content: 'Testiviestin sisältö'
}

describe('Sending and receiving messages', () => {
  const initConfigurations = [
    ['direct login', initCitizenPage] as const,
    ['weak login', initCitizenPageWeak] as const
  ]

  describe.each(initConfigurations)(
    `Interactions with %s`,
    (_name, initCitizen) => {
      test('Staff sends message and citizen replies', async () => {
        await initStaffPage(mockedDateAt10)
        await staffPage.goto(`${config.employeeUrl}/messages`)
        let messagesPage = new MessagesPage(staffPage)
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage(defaultMessage)
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await initCitizen(mockedDateAt11)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)
        await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))

        await initStaffPage(mockedDateAt12)
        await staffPage.goto(`${config.employeeUrl}/messages`)
        messagesPage = new MessagesPage(staffPage)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)
        await messagesPage.receivedMessage.click()
        await messagesPage.assertMessageContent(1, defaultReply)
      })

      test('Staff can archive a message', async () => {
        await initStaffPage(mockedDateAt10)
        await staffPage.goto(`${config.employeeUrl}/messages`)
        let messagesPage = new MessagesPage(staffPage)
        const messageEditor = await messagesPage.openMessageEditor()
        await messageEditor.sendNewMessage(defaultMessage)
        await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

        await initCitizen(mockedDateAt11)
        await citizenPage.goto(config.enduserMessagesUrl)
        const citizenMessagesPage = new CitizenMessagesPage(
          citizenPage,
          'desktop'
        )
        await citizenMessagesPage.assertThreadContent(defaultMessage)
        await citizenMessagesPage.replyToFirstThread(defaultReply)
        await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 2)
        await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))

        await initStaffPage(mockedDateAt12)
        await staffPage.goto(`${config.employeeUrl}/messages`)
        messagesPage = new MessagesPage(staffPage)
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 1)

        await messagesPage.deleteFirstThread()
        await waitUntilEqual(() => messagesPage.getReceivedMessageCount(), 0)
      })
    }
  )

  test('Unit supervisor can send a message to a starter', async () => {
    const daycarePlacementFixture1 = await Fixture.placement({
      childId,
      unitId: testPreschool.id,
      startDate: mockedDate.addYears(1).addDays(1),
      endDate: mockedDate.addYears(2)
    }).save()
    const preschoolGroup = await Fixture.daycareGroup({
      daycareId: testPreschool.id,
      name: 'Esiopetusryhmä'
    }).save()
    await Fixture.groupPlacement({
      daycarePlacementId: daycarePlacementFixture1.id,
      daycareGroupId: preschoolGroup.id,
      startDate: mockedDate.addYears(1).addDays(1),
      endDate: mockedDate.addYears(2)
    }).save()

    // Verify that available recipients contain current placements and starters
    await initUnitSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(unitSupervisorPage)
    const messageEditor = await messagesPage.openMessageEditor()
    const recipientSelector = messageEditor.recipientSelection
    await recipientSelector.open()
    await recipientSelector.expandAll() // open first level
    await recipientSelector.expandAll() // open second level
    const labels = await recipientSelector.labels.allTexts()
    const starterChildLabel = `${testChild.lastName} ${testChild.firstName} (${mockedDate.addYears(1).addDays(1).format()})`
    const expectedRecipientNames = [
      testDaycare.name,
      testDaycareGroup.name,
      `${testChild.lastName} ${testChild.firstName}`,
      `${testChild2.lastName} ${testChild2.firstName}`,
      `${testPreschool.name} (aloittavat lapset)`,
      `${preschoolGroup.name} (aloittavat lapset)`,
      starterChildLabel
    ]
    expect(labels).toEqual(expectedRecipientNames)

    // Send a message to a starter child -> selects the whole unit
    await recipientSelector.optionByLabel(starterChildLabel).click()
    await recipientSelector.close()
    await messageEditor.inputTitle.fill('Aloittavalle otsikko')
    await messageEditor.inputContent.fill('Sisältö')
    await messageEditor.sendButton.click()
    await messageEditor.waitUntilHidden()

    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    // Verify that the message is received by the starter child
    await initCitizenPage(mockedDateAt11)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage, 'desktop')
    await citizenMessagesPage.assertThreadContent({
      title: 'Aloittavalle otsikko',
      content: 'Sisältö'
    })
  })

  test('Staff can send a message to a starter', async () => {
    const secondGroup = await Fixture.daycareGroup({
      daycareId: testDaycare.id,
      name: 'Toinen ryhmä'
    }).save()
    const futureStaff = await Fixture.employee()
      .staff(testDaycare.id)
      .groupAcl(secondGroup.id, mockedDateAt10, mockedDateAt10)
      .save()
    const daycarePlacementFixture1 = await Fixture.placement({
      childId,
      unitId: testDaycare.id,
      startDate: mockedDate.addYears(1).addDays(1),
      endDate: mockedDate.addYears(2)
    }).save()
    const daycarePlacementFixture2 = await Fixture.placement({
      childId: testChild2.id,
      unitId: testDaycare.id,
      startDate: mockedDate.addYears(1).addDays(1),
      endDate: mockedDate.addYears(2)
    }).save()
    await Fixture.groupPlacement({
      daycarePlacementId: daycarePlacementFixture1.id,
      daycareGroupId: secondGroup.id,
      startDate: mockedDate.addYears(1).addDays(1),
      endDate: mockedDate.addYears(2)
    }).save()
    await Fixture.groupPlacement({
      daycarePlacementId: daycarePlacementFixture2.id,
      daycareGroupId: secondGroup.id,
      startDate: mockedDate.addYears(1).addDays(1),
      endDate: mockedDate.addYears(2)
    }).save()
    await createMessageAccounts()

    // Verify that available recipients contain the starters
    staffPage = await Page.open({ mockedTime: mockedDateAt10 })
    await employeeLogin(staffPage, futureStaff)
    await staffPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(staffPage)
    const messageEditor = await messagesPage.openMessageEditor()
    const recipientSelector = messageEditor.recipientSelection
    await recipientSelector.open()
    await recipientSelector.expandAll()
    const labels = await recipientSelector.labels.allTexts()
    const expectedRecipientNames = [
      `${secondGroup.name} (aloittavat lapset)`,
      `${testChild.lastName} ${testChild.firstName} (${mockedDate.addYears(1).addDays(1).format()})`,
      `${testChild2.lastName} ${testChild2.firstName} (${mockedDate.addYears(1).addDays(1).format()})`
    ]
    expect(labels).toEqual(expectedRecipientNames)

    // Send a message to a starter group (contains 2 children)
    await recipientSelector
      .optionByLabel(`${secondGroup.name} (aloittavat lapset)`)
      .click()
    await recipientSelector.close()
    await messageEditor.inputTitle.fill('Aloittavalle otsikko')
    await messageEditor.inputContent.fill('Sisältö')
    await messageEditor.sendButton.click()
    await messageEditor.waitUntilHidden()

    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    // Verify that the message is received by the starter
    await initCitizenPage(mockedDateAt11)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage, 'desktop')
    await citizenMessagesPage.assertThreadContent({
      title: 'Aloittavalle otsikko',
      content: 'Sisältö',
      childNames: [
        formatPersonName(testChild, 'FirstFirst'),
        formatPersonName(testChild2, 'FirstFirst')
      ]
    })

    // Reply to the message
    await citizenMessagesPage.replyToFirstThread('Vastaukseni')

    await runPendingAsyncJobs(mockedDateAt11.addMinutes(1))

    // Verify that the message is received by the staff
    staffPage = await Page.open({ mockedTime: mockedDateAt12 })
    await employeeLogin(staffPage, futureStaff)
    await staffPage.goto(`${config.employeeUrl}/messages`)
    const staffMessagesPage = new MessagesPage(staffPage)
    await waitUntilEqual(() => staffMessagesPage.getReceivedMessageCount(), 1)
    await staffMessagesPage.receivedMessage.click()
    await staffMessagesPage.assertMessageContent(1, 'Vastaukseni')
  })
})

describe('Sending and receiving sensitive messages', () => {
  test('VEO sends sensitive message, citizen needs strong auth and after strong auth sees message', async () => {
    staff = await Fixture.employee()
      .specialEducationTeacher(testDaycare.id)
      .groupAcl(testDaycareGroup.id)
      .save()
    // create messaging account for newly created VEO account
    await createMessageAccounts()

    const sensitiveMessage = {
      ...defaultMessage,
      sensitive: true,
      recipientKeys: [`${testChild2.id}+false`]
    }

    await initStaffPage(mockedDateAt10)
    await staffPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(staffPage)
    const messageEditor = await messagesPage.openMessageEditor()
    await messageEditor.sendNewMessage(sensitiveMessage)

    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    await initCitizenPageWeak(mockedDateAt11)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage, 'desktop')
    await citizenMessagesPage.assertThreadIsRedacted()

    const authPage = await citizenMessagesPage.openStrongAuthPage()
    const strongAuthCitizenMessagePage = await authPage.login(
      testAdult.ssn!,
      (page) => new CitizenMessagesPage(page, 'desktop')
    )

    await strongAuthCitizenMessagePage.assertThreadContent(sensitiveMessage)
  })
})

describe('Staff copies', () => {
  test('Bulletin sent by supervisor to the whole unit creates a copy for the staff', async () => {
    await initUnitSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const message = {
      title: 'Ilmoitus',
      content: 'Ilmoituksen sisältö',
      recipientKeys: [`${testDaycare.id}+false`],
      type: 'BULLETIN' as const
    }
    const messageEditor = await new MessagesPage(
      unitSupervisorPage
    ).openMessageEditor()
    await messageEditor.sendNewMessage(message)
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    await initStaffPage(mockedDateAt11)
    await staffPage.goto(`${config.employeeUrl}/messages`)
    await new MessagesPage(staffPage).assertCopyContent(
      message.title,
      message.content
    )
  })

  test('Message sent by supervisor to a single child does not create a copy for the staff', async () => {
    await initUnitSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const message = {
      title: 'Ilmoitus',
      content: 'Ilmoituksen sisältö',
      recipientKeys: [`${testChild2.id}+false`]
    }
    const bulletin = {
      title: 'Ilmoitus',
      content: 'Ilmoituksen sisältö',
      recipientKeys: [`${testChild2.id}+false`],
      type: 'BULLETIN' as const
    }
    const messagesPage = new MessagesPage(unitSupervisorPage)
    const messageEditor = await messagesPage.openMessageEditor()
    await messageEditor.sendNewMessage(message)
    await messagesPage.openMessageEditor()
    await messageEditor.sendNewMessage(bulletin)
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    await initStaffPage(mockedDateAt11)
    await staffPage.goto(`${config.employeeUrl}/messages`)
    await new MessagesPage(staffPage).assertNoCopies()
  })

  test('Message sent by supervisor from a group account to a single child does not create a copy for the staff', async () => {
    await initUnitSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const message = {
      title: 'Ilmoitus',
      content: 'Ilmoituksen sisältö',
      sender: `${testDaycare.name} - ${testDaycareGroup.name}`,
      recipientKeys: [`${testDaycareGroup.id}+false`]
    }
    const messageEditor = await new MessagesPage(
      unitSupervisorPage
    ).openMessageEditor()
    await messageEditor.sendNewMessage(message)
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    await initStaffPage(mockedDateAt11)
    await staffPage.goto(`${config.employeeUrl}/messages`)
    await new MessagesPage(staffPage).assertNoCopies()
  })

  test('Bulletin staff copy shows only accessible group names as recipients', async () => {
    // This test verifies the real production scenario where:
    // 1. A bulletin is sent to cherry-picked groups from TWO different units
    // 2. Each unit has MULTIPLE groups with MULTIPLE children (realistic scenario)
    // 3. The bulletin is sent to only ONE group per unit (not all groups)
    // 4. Staff from each unit only sees the recipient name that is the "root recipient" causing the message copy to exist in the group mailbox

    // Helper function to create a family where each child is placed in a different group
    // Creates one child per group ID provided and returns the children
    async function createFamilyWithChildrenInGroups(
      guardianSsnSeed: number,
      daycareId: DaycareId,
      daycareGroupIds: GroupId[]
    ) {
      const guardian = Fixture.person({
        ssn: `${String(guardianSsnSeed).padStart(6, '0')}-${String(1000 + guardianSsnSeed).slice(-3)}A`,
        firstName: 'Guardian',
        lastName: `Family${guardianSsnSeed}`,
        dateOfBirth: LocalDate.of(1980, 1, 1)
      })

      const children = daycareGroupIds.map((_, childIndex) =>
        Fixture.person({
          ssn: `${String(guardianSsnSeed + childIndex + 1).padStart(6, '0')}-${String(2000 + guardianSsnSeed + childIndex).slice(-3)}B`,
          firstName: `Child${childIndex + 1}`,
          lastName: `Family${guardianSsnSeed}`,
          dateOfBirth: LocalDate.of(2017, (childIndex % 12) + 1, 1)
        })
      )

      await Fixture.family({
        guardian,
        children
      }).save()

      await insertGuardians({
        body: children.map((child) => ({
          childId: child.id,
          guardianId: guardian.id
        }))
      })

      for (const [childIndex, child] of children.entries()) {
        const placement = await Fixture.placement({
          childId: child.id,
          unitId: daycareId,
          startDate: mockedDate,
          endDate: mockedDate.addYears(1)
        }).save()
        await Fixture.groupPlacement({
          daycarePlacementId: placement.id,
          daycareGroupId: daycareGroupIds[childIndex],
          startDate: mockedDate,
          endDate: mockedDate.addYears(1)
        }).save()
      }

      return children
    }

    // Setup second daycare with messaging enabled
    await Fixture.daycare({
      ...testDaycare2,
      areaId: testCareArea.id,
      enabledPilotFeatures: ['MESSAGING', 'MOBILE']
    }).save()

    // Create TWO groups for the second daycare (only one will receive the bulletin)
    const daycare2Group = await Fixture.daycareGroup({
      name: 'Korholan ryhmä',
      daycareId: testDaycare2.id
    }).save()

    const daycare2GroupB = await Fixture.daycareGroup({
      name: 'Korholan toinen ryhmä',
      daycareId: testDaycare2.id
    }).save()

    // Create multi-unit supervisor with access to BOTH daycares
    const multiUnitSupervisor = await Fixture.employee()
      .unitSupervisor(testDaycare.id)
      .unitSupervisor(testDaycare2.id)
      .save()

    // Create a SECOND group for the first daycare (this one won't receive the bulletin)
    const daycareGroupB = await Fixture.daycareGroup({
      name: 'Alkuryhmän toinen ryhmä',
      daycareId: testDaycare.id
    }).save()

    // Add 2 more children to first daycare - each in a different group
    const family1Children = await createFamilyWithChildrenInGroups(
      100,
      testDaycare.id,
      [testDaycareGroup.id, daycareGroupB.id]
    )

    // Add 2 children to first daycare - each in a different group
    await createFamilyWithChildrenInGroups(200, testDaycare.id, [
      testDaycareGroup.id,
      daycareGroupB.id
    ])
    // Create 2 children for second daycare - each in a different group
    const family3Children = await createFamilyWithChildrenInGroups(
      300,
      testDaycare2.id,
      [daycare2Group.id, daycare2GroupB.id]
    )

    // Add 2 children to second daycare - each in a different group
    await createFamilyWithChildrenInGroups(400, testDaycare2.id, [
      daycare2Group.id,
      daycare2GroupB.id
    ])
    // Create staff member with access ONLY to second daycare
    const staff2 = await Fixture.employee()
      .staff(testDaycare2.id)
      .groupAcl(daycare2Group.id, mockedDateAt10, mockedDateAt10)
      .save()

    await createMessageAccounts()

    // Multi-unit supervisor sends bulletin to ONE cherry-picked group from EACH unit
    const multiUnitSupervisorPage = await Page.open({
      mockedTime: mockedDateAt10
    })
    await employeeLogin(multiUnitSupervisorPage, multiUnitSupervisor)
    await multiUnitSupervisorPage.goto(`${config.employeeUrl}/messages`)

    // Select a combination of whole groups and single children
    const message = {
      title: 'Multi-Unit Bulletin',
      content: 'Important announcement for selected children',
      recipientKeys: [
        `${testDaycareGroup.id}+false`, // Whole group from first unit
        `${daycare2Group.id}+false`, // Whole group from second unit
        `${family1Children[1].id}+false`, // Single child from another group of first unit
        `${family3Children[1].id}+false` // Single child from another group of second unit
      ],
      type: 'BULLETIN' as const,
      confirmManyRecipients: true
    }

    const messageEditor = await new MessagesPage(
      multiUnitSupervisorPage
    ).openMessageEditor()
    await messageEditor.sendNewMessage(message)
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    // Verify: Staff from first unit sees group names from their accessible unit
    await initStaffPage(mockedDateAt11)
    await staffPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(staffPage)

    await messagesPage.assertCopyContent(message.title, message.content)
    const copyPage1 = await messagesPage.openCopyThread()

    const expectedGroupName1 = 'Kosmiset vakiot'
    await copyPage1.assertMessageRecipients(expectedGroupName1)

    // Verify: Staff from second unit sees unit names only
    const staff2Page = await Page.open({ mockedTime: mockedDateAt11 })
    await employeeLogin(staff2Page, staff2)
    await staff2Page.goto(`${config.employeeUrl}/messages`)
    const messagesPage2 = new MessagesPage(staff2Page)

    await messagesPage2.assertCopyContent(message.title, message.content)
    const copyPage2 = await messagesPage2.openCopyThread()

    const expectedGroupName2 = 'Korholan ryhmä'
    await copyPage2.assertMessageRecipients(expectedGroupName2)
  })
})

describe('Additional filters', () => {
  test('Additional filters are visible to unit supervisor on personal account', async () => {
    await initUnitSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(unitSupervisorPage)
    const messageEditor = await messagesPage.openMessageEditor()
    await messageEditor.filtersButton.waitUntilVisible()
    await messageEditor.filtersButton.click()
    await messageEditor.assertFiltersVisible()
  })

  test('Additional filters are not visible to unit supervisor on group account', async () => {
    await initUnitSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(unitSupervisorPage)
    await messagesPage.unitReceived.click()
    const messageEditor = await messagesPage.openMessageEditor()
    await messageEditor.sendButton.waitUntilVisible()
    expect(await messageEditor.filtersButtonCount).toBe(0)
  })

  test('Citizen receives a message when recipient filter matches', async () => {
    await testAdult2.saveAdult({
      updateMockVtjWithDependants: [testChild]
    })
    await insertGuardians({
      body: [
        {
          childId: childId,
          guardianId: testAdult2.id
        }
      ]
    })
    await initUnitSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const message = {
      title: 'Ilmoitus rajatulle joukolle',
      content: 'Ilmoituksen sisältö rajatulle joukolle',
      recipientKeys: [`${testDaycare.id}+false`],
      yearsOfBirth: [2014]
    }
    const messageEditor = await new MessagesPage(
      unitSupervisorPage
    ).openMessageEditor()
    await messageEditor.sendNewMessage(message)
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    await initOtherCitizenPage(mockedDateAt11, testAdult2)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage, 'desktop')
    await citizenMessagesPage.assertThreadContent(message)
    await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 1)
  })

  test(`Citizen doesn't receive a message when recipient filter doesn't match`, async () => {
    await testAdult2.saveAdult({
      updateMockVtjWithDependants: [testChild]
    })
    await insertGuardians({
      body: [
        {
          childId: childId,
          guardianId: testAdult2.id
        }
      ]
    })
    await initUnitSupervisorPage(mockedDateAt10)
    await unitSupervisorPage.goto(`${config.employeeUrl}/messages`)
    const message = {
      title: 'Ilmoitus rajatulle joukolle',
      content: 'Ilmoituksen sisältö rajatulle joukolle',
      recipientKeys: [`${testDaycare.id}+false`]
    }
    let messageEditor = await new MessagesPage(
      unitSupervisorPage
    ).openMessageEditor()
    await messageEditor.sendNewMessage({
      ...message,
      yearsOfBirth: [2017]
    })
    messageEditor = await new MessagesPage(
      unitSupervisorPage
    ).openMessageEditor()
    await messageEditor.sendNewMessage({
      ...message,
      shiftcare: true
    })
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    await initOtherCitizenPage(mockedDateAt11, testAdult2)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage, 'desktop')
    await citizenMessagesPage.assertInboxIsEmpty()
  })
})
