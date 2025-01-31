// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { PersonId } from 'lib-common/generated/api-types/shared'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'

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
import { DevEmployee, DevPerson } from '../../generated/api-types'
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
  await Fixture.careArea(testCareArea).save()
  await Fixture.daycare(testDaycare).save()
  await Fixture.daycare(testPreschool).save()
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
    .withGroupAcl(testDaycareGroup.id, mockedDateAt10, mockedDateAt10)
    .save()

  unitSupervisor = await Fixture.employee()
    .withDaycareAcl(testDaycare.id, 'UNIT_SUPERVISOR')
    .withDaycareAcl(testPreschool.id, 'UNIT_SUPERVISOR')
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
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
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
        const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
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

  test('Staff can select starters as receivers', async () => {
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
    const receiverSelector = messageEditor.receiverSelection
    await receiverSelector.open()
    await receiverSelector.expandAll() // open first level
    await receiverSelector.expandAll() // open second level
    const labels = await receiverSelector.labels.allTexts()
    const starterChildLabel = `${testChild.lastName} ${testChild.firstName} (${mockedDate.addYears(1).addDays(1).format()})`
    const expectedReceiverNames = [
      testDaycare.name,
      testDaycareGroup.name,
      `${testChild.lastName} ${testChild.firstName}`,
      `${testChild2.lastName} ${testChild2.firstName}`,
      `${testPreschool.name} (aloittavat)`,
      `${preschoolGroup.name} (aloittavat)`,
      starterChildLabel
    ]
    expect(labels.sort()).toEqual(expectedReceiverNames.sort())

    // Send a message to a starter child
    await receiverSelector.optionByLabel(starterChildLabel).click()
    await messageEditor.inputTitle.fill('Aloittavalle otsikko')
    await messageEditor.inputContent.fill('Sisältö')
    await messageEditor.sendButton.click()
    await messageEditor.waitUntilHidden()

    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    // Verify that the message is received by the starter child
    await initCitizenPage(mockedDateAt11)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
    await citizenMessagesPage.assertThreadContent({
      title: 'Aloittavalle otsikko',
      content: 'Sisältö'
    })
  })
})

describe('Sending and receiving sensitive messages', () => {
  test('VEO sends sensitive message, citizen needs strong auth and after strong auth sees message', async () => {
    staff = await Fixture.employee()
      .specialEducationTeacher(testDaycare.id)
      .withGroupAcl(testDaycareGroup.id)
      .save()
    // create messaging account for newly created VEO account
    await createMessageAccounts()

    const sensitiveMessage = {
      ...defaultMessage,
      sensitive: true,
      receiverKeys: [`${testChild2.id}+false`]
    }

    await initStaffPage(mockedDateAt10)
    await staffPage.goto(`${config.employeeUrl}/messages`)
    const messagesPage = new MessagesPage(staffPage)
    const messageEditor = await messagesPage.openMessageEditor()
    await messageEditor.sendNewMessage(sensitiveMessage)

    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    await initCitizenPageWeak(mockedDateAt11)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
    await citizenMessagesPage.assertThreadIsRedacted()

    const authPage = await citizenMessagesPage.openStrongAuthPage()
    const strongAuthCitizenMessagePage = await authPage.login(testAdult.ssn!)

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
      receiverKeys: [`${testDaycare.id}+false`],
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
      receiverKeys: [`${testChild2.id}+false`]
    }
    const bulletin = {
      title: 'Ilmoitus',
      content: 'Ilmoituksen sisältö',
      receiverKeys: [`${testChild2.id}+false`],
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
      receiverKeys: [`${testDaycareGroup.id}+false`]
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
    await Fixture.person(testAdult2).saveAdult({
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
      receiverKeys: [`${testDaycare.id}+false`],
      yearsOfBirth: [2014]
    }
    const messageEditor = await new MessagesPage(
      unitSupervisorPage
    ).openMessageEditor()
    await messageEditor.sendNewMessage(message)
    await runPendingAsyncJobs(mockedDateAt10.addMinutes(1))

    await initOtherCitizenPage(mockedDateAt11, testAdult2)
    await citizenPage.goto(config.enduserMessagesUrl)
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
    await citizenMessagesPage.assertThreadContent(message)
    await waitUntilEqual(() => citizenMessagesPage.getMessageCount(), 1)
  })

  test(`Citizen doesn't receive a message when recipient filter doesn't match`, async () => {
    await Fixture.person(testAdult2).saveAdult({
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
      receiverKeys: [`${testDaycare.id}+false`]
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
    const citizenMessagesPage = new CitizenMessagesPage(citizenPage)
    await citizenMessagesPage.assertInboxIsEmpty()
  })
})
