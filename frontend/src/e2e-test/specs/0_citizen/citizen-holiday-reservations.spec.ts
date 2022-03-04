// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import FiniteDateRange from 'lib-common/finite-date-range'
import LocalDate from 'lib-common/local-date'

import { resetDatabase } from '../../dev-api'
import {
  careAreaFixture,
  daycareFixture,
  daycareGroupFixture,
  enduserChildFixtureJari,
  enduserGuardianFixture,
  Fixture
} from '../../dev-api/fixtures'
import CitizenCalendarPage from '../../pages/citizen/citizen-calendar'
import CitizenHeader from '../../pages/citizen/citizen-header'
import { Page } from '../../utils/page'
import { enduserLogin } from '../../utils/user'

let page: Page

const period = new FiniteDateRange(
  LocalDate.of(2035, 12, 18),
  LocalDate.of(2036, 1, 8)
)
const child = enduserChildFixtureJari
const mockedDate = LocalDate.of(2035, 12, 1)

beforeEach(async () => {
  await resetDatabase()
  page = await Page.open({ mockedTime: mockedDate.toSystemTzDate() })

  const daycare = await Fixture.daycare()
    .with(daycareFixture)
    .careArea(await Fixture.careArea().with(careAreaFixture).save())
    .save()
  await Fixture.daycareGroup().with(daycareGroupFixture).daycare(daycare).save()

  const guardian = await Fixture.person().with(enduserGuardianFixture).save()
  const child1 = await Fixture.person().with(child).save()
  await Fixture.child(child1.data.id).save()
  await Fixture.guardian(child1, guardian).save()

  await Fixture.placement()
    .child(child1)
    .daycare(daycare)
    .with({
      startDate: LocalDate.of(2022, 1, 1).formatIso(),
      endDate: LocalDate.of(2036, 6, 30).formatIso()
    })
    .save()
})

describe('Holiday periods', () => {
  describe('Holiday period questionnaire is active', () => {
    beforeEach(async () => {
      await Fixture.holidayPeriod()
        .with({
          period,
          reservationDeadline: LocalDate.of(2035, 12, 6),
          showReservationBannerFrom: LocalDate.today(),
          description: {
            en: 'Please submit your reservations for 18.12.2035 - 8.1.2036 asap',
            fi: 'Ystävällisesti pyydän tekemään varauksenne ajalle 18.12.2035 - 8.1.2036 heti kun mahdollista, kuitenkin viimeistään 6.12.',
            sv: 'Vänligen samma på svenska för 18.12.2035 - 8.1.2036'
          },
          freePeriod: {
            deadline: LocalDate.of(2035, 12, 6),
            periodOptionLabel: {
              en: 'My child is away for 8 weeks between',
              fi: 'Lapseni on poissa 8 viikkoa aikavälillä',
              sv: 'Mitt barn är borta 8 veckor mellan'
            },
            periodOptions: [
              new FiniteDateRange(
                LocalDate.of(2035, 12, 18),
                LocalDate.of(2035, 12, 25)
              ),
              new FiniteDateRange(
                LocalDate.of(2035, 12, 26),
                LocalDate.of(2036, 1, 1)
              ),
              new FiniteDateRange(
                LocalDate.of(2036, 1, 2),
                LocalDate.of(2036, 1, 8)
              )
            ],
            questionLabel: {
              en: 'Are your children away for 8 weeks during 18.12.2035 - 8.1.2036?',
              fi: 'Ovatko lapsenne 18.12.2035 - 8.1.2036 välillä yhtenäisesti 8 viikkoa poissa?',
              sv: 'Är dina barn borta 8 veckor under tiden 18.12.2035 - 8.1.2036?'
            }
          }
        })
        .save()
    })

    test('A holiday reservations banner is shown on calendar page', async () => {
      await enduserLogin(page)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      expect(await calendar.getHolidayBannerContent()).toEqual(
        'Ilmoita lomat ja tee varaukset 18.12.2035-08.01.2036 välille viimeistään 06.12.2035.'
      )
    })

    test('The calendar page should show a button for reporting holidays', async () => {
      await enduserLogin(page)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')
      await calendar.assertHolidayModalButtonVisible()
    })

    test('Holidays can be reported', async () => {
      await enduserLogin(page)
      await new CitizenHeader(page).selectTab('calendar')
      const calendar = new CitizenCalendarPage(page, 'desktop')

      await calendar.assertNoReservationsOrAbsences(LocalDate.of(2035, 12, 26))

      const holidayModal = await calendar.openHolidayModal()
      await holidayModal.markHoliday(child, '26.12.2035 - 01.01.2036')

      await calendar.assertReservations(
        LocalDate.of(2035, 12, 26),
        [],
        false,
        true
      )
    })
  })
})
