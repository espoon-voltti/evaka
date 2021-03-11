// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from '@evaka/citizen-frontend/applications/editor/ApplicationFormData'
import { ApplicationDetails } from '@evaka/lib-common/api-types/application/ApplicationDetails'
import { clubFixture, daycareFixture } from '../dev-api/fixtures'

function assertEquals<T>(expected: T, actual: T) {
  // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
  if (actual !== expected) throw Error(`expected ${expected}, got ${actual}`)
}

function assertBlank(actual: string | undefined) {
  // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
  if (actual !== '') throw Error(`expected empty string, got ${actual}`)
}

function assertEmpty<T>(actual: T[]) {
  // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
  if (actual.length !== 0) throw Error(`expected empty array, got ${actual}`)
}

function assertTrue(actual: boolean | undefined) {
  // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
  if (actual !== true) throw Error(`expected true, got ${actual}`)
}

function assertFalse(actual: boolean | undefined) {
  // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
  if (actual !== false) throw Error(`expected false, got ${actual}`)
}

function assertNull<T>(actual: T | undefined) {
  // eslint-disable-next-line @typescript-eslint/restrict-template-expressions
  if (actual !== null) throw Error(`expected null, got ${actual}`)
}

type WithOptionalFields<T> = {
  [key in keyof T]?: T[key]
}

export type FormInput = {
  [section in keyof ApplicationFormData]?: WithOptionalFields<
    ApplicationFormData[section]
  >
}

export const minimalDaycareForm: {
  form: FormInput
  validateResult: (result: ApplicationDetails) => void
} = {
  form: {
    serviceNeed: {
      preferredStartDate: '13.8.2021',
      startTime: '09:00',
      endTime: '17:00'
    },
    unitPreference: {
      preferredUnits: [
        {
          id: daycareFixture.id,
          name: daycareFixture.name
        }
      ]
    },
    contactInfo: {
      guardianPhone: '(+358) 50-1234567'
    }
  },
  validateResult: (res) => {
    assertEquals('SENT', res.status)

    assertNull(res.form.child.futureAddress)
    assertBlank(res.form.child.allergies)
    assertBlank(res.form.child.diet)
    assertFalse(res.form.child.assistanceNeeded)
    assertBlank(res.form.child.assistanceDescription)

    assertEquals(null, res.form.guardian.futureAddress)
    assertEquals('(+358) 50-1234567', res.form.guardian.phoneNumber)
    assertBlank(res.form.guardian.email)

    assertNull(res.form.secondGuardian)

    assertNull(res.form.otherPartner)

    assertEmpty(res.form.otherChildren)

    assertEquals(1, res.form.preferences.preferredUnits.length)
    assertEquals(daycareFixture.id, res.form.preferences.preferredUnits[0].id)
    assertEquals(
      '13.08.2021',
      res.form.preferences.preferredStartDate?.format()
    )
    assertEquals('09:00', res.form.preferences.serviceNeed?.startTime)
    assertEquals('17:00', res.form.preferences.serviceNeed?.endTime)
    assertFalse(res.form.preferences.serviceNeed?.shiftCare)
    assertFalse(res.form.preferences.serviceNeed?.partTime)
    assertNull(res.form.preferences.siblingBasis)
    assertFalse(res.form.preferences.preparatory)
    assertFalse(res.form.preferences.urgent)

    assertFalse(res.form.maxFeeAccepted)

    assertBlank(res.form.otherInfo)

    assertNull(res.form.clubDetails)
  }
}

export const fullDaycareForm: {
  form: FormInput
  validateResult: (result: ApplicationDetails) => void
} = {
  form: {
    serviceNeed: {
      preferredStartDate: '13.8.2021',
      urgent: true,
      startTime: '09:00',
      endTime: '17:00',
      partTime: true,
      shiftCare: true,
      assistanceNeeded: true,
      assistanceDescription: 'Keskittymisvaikeus'
    },
    unitPreference: {
      siblingBasis: true,
      siblingName: 'Anna Karhula',
      siblingSsn: '110814A812B',
      preferredUnits: [
        {
          id: daycareFixture.id,
          name: daycareFixture.name
        }
      ]
    },
    contactInfo: {
      childFutureAddressExists: true,
      childMoveDate: '11.10.2022',
      childFutureStreet: 'Katu 1',
      childFuturePostalCode: '00200',
      childFuturePostOffice: 'Espoo',
      guardianFutureAddressExists: true,
      guardianFutureAddressEqualsChild: true,
      guardianPhone: '(+358) 50-1234567',
      guardianEmail: 'johannes.karhula@example.com',
      otherPartnerExists: true,
      otherPartnerFirstName: 'Heikki',
      otherPartnerLastName: 'Heppuli',
      otherPartnerSSN: '210188-389L',
      otherChildrenExists: true,
      otherChildren: [
        {
          firstName: 'Tupu',
          lastName: 'Ankka',
          socialSecurityNumber: '250718A809H'
        },
        {
          firstName: 'Tauno',
          lastName: 'Hanhi',
          socialSecurityNumber: '101017A479B'
        }
      ]
    },
    fee: {
      maxFeeAccepted: true
    },
    additionalDetails: {
      otherInfo: 'Olipa hakeminen helppoa!',
      diet: 'Vegaani',
      allergies: 'Pähkinät, chili'
    }
  },
  validateResult: (res) => {
    assertEquals('SENT', res.status)

    assertEquals(
      '11.10.2022',
      res.form.child.futureAddress?.movingDate?.format()
    )
    assertEquals('Katu 1', res.form.child.futureAddress?.street)
    assertEquals('00200', res.form.child.futureAddress?.postalCode)
    assertEquals('Espoo', res.form.child.futureAddress?.postOffice)

    assertEquals('Pähkinät, chili', res.form.child.allergies)
    assertEquals('Vegaani', res.form.child.diet)
    assertTrue(res.form.child.assistanceNeeded)
    assertEquals('Keskittymisvaikeus', res.form.child.assistanceDescription)

    assertEquals(
      '11.10.2022',
      res.form.guardian.futureAddress?.movingDate?.format()
    )
    assertEquals('Katu 1', res.form.guardian.futureAddress?.street)
    assertEquals('00200', res.form.guardian.futureAddress?.postalCode)
    assertEquals('Espoo', res.form.guardian.futureAddress?.postOffice)
    assertEquals('(+358) 50-1234567', res.form.guardian.phoneNumber)
    assertEquals('johannes.karhula@example.com', res.form.guardian.email)

    assertNull(res.form.secondGuardian)

    assertEquals('Heikki', res.form.otherPartner?.firstName)
    assertEquals('Heppuli', res.form.otherPartner?.lastName)
    assertEquals('210188-389L', res.form.otherPartner?.socialSecurityNumber)

    assertEquals(2, res.form.otherChildren.length)
    assertEquals('Tupu', res.form.otherChildren[0].firstName)
    assertEquals('Ankka', res.form.otherChildren[0].lastName)
    assertEquals('250718A809H', res.form.otherChildren[0].socialSecurityNumber)
    assertEquals('Tauno', res.form.otherChildren[1].firstName)
    assertEquals('Hanhi', res.form.otherChildren[1].lastName)
    assertEquals('101017A479B', res.form.otherChildren[1].socialSecurityNumber)

    assertEquals(1, res.form.preferences.preferredUnits.length)
    assertEquals(daycareFixture.id, res.form.preferences.preferredUnits[0].id)
    assertEquals(
      '13.08.2021',
      res.form.preferences.preferredStartDate?.format()
    )
    assertEquals('09:00', res.form.preferences.serviceNeed?.startTime)
    assertEquals('17:00', res.form.preferences.serviceNeed?.endTime)
    assertTrue(res.form.preferences.serviceNeed?.shiftCare)
    assertTrue(res.form.preferences.serviceNeed?.partTime)
    assertEquals('Anna Karhula', res.form.preferences.siblingBasis?.siblingName)
    assertEquals('110814A812B', res.form.preferences.siblingBasis?.siblingSsn)
    assertFalse(res.form.preferences.preparatory)
    assertTrue(res.form.preferences.urgent)

    assertTrue(res.form.maxFeeAccepted)

    assertEquals('Olipa hakeminen helppoa!', res.form.otherInfo)

    assertNull(res.form.clubDetails)
  }
}

export const minimalClubForm: {
  form: FormInput
  validateResult: (result: ApplicationDetails) => void
} = {
  form: {
    serviceNeed: {
      preferredStartDate: '13.8.2021'
    },
    unitPreference: {
      preferredUnits: [
        {
          id: clubFixture.id,
          name: clubFixture.name
        }
      ]
    },
    contactInfo: {
      guardianPhone: '(+358) 50-1234567'
    }
  },
  validateResult: (res) => {
    assertEquals('SENT', res.status)

    assertNull(res.form.child.futureAddress)
    assertBlank(res.form.child.allergies)
    assertBlank(res.form.child.diet)
    assertFalse(res.form.child.assistanceNeeded)
    assertBlank(res.form.child.assistanceDescription)

    assertEquals(null, res.form.guardian.futureAddress)
    assertEquals('(+358) 50-1234567', res.form.guardian.phoneNumber)
    assertBlank(res.form.guardian.email)

    assertNull(res.form.secondGuardian)

    assertNull(res.form.otherPartner)

    assertEmpty(res.form.otherChildren)

    assertEquals(1, res.form.preferences.preferredUnits.length)
    assertEquals(clubFixture.id, res.form.preferences.preferredUnits[0].id)
    assertEquals(
      '13.08.2021',
      res.form.preferences.preferredStartDate?.format()
    )
    assertNull(res.form.preferences.serviceNeed)
    assertNull(res.form.preferences.siblingBasis)
    assertFalse(res.form.preferences.preparatory)
    assertFalse(res.form.preferences.urgent)

    assertFalse(res.form.maxFeeAccepted)

    assertBlank(res.form.otherInfo)

    assertFalse(res.form.clubDetails?.wasOnDaycare)
    assertFalse(res.form.clubDetails?.wasOnClubCare)
  }
}

export const fullClubForm: {
  form: FormInput
  validateResult: (result: ApplicationDetails) => void
} = {
  form: {
    serviceNeed: {
      preferredStartDate: '13.8.2021',
      wasOnClubCare: true,
      wasOnDaycare: true,
      assistanceNeeded: true,
      assistanceDescription: 'Keskittymisvaikeus'
    },
    unitPreference: {
      siblingBasis: true,
      siblingName: 'Anna Karhula',
      siblingSsn: '110814A812B',
      preferredUnits: [
        {
          id: clubFixture.id,
          name: clubFixture.name
        }
      ]
    },
    contactInfo: {
      childFutureAddressExists: true,
      childMoveDate: '11.10.2022',
      childFutureStreet: 'Katu 1',
      childFuturePostalCode: '00200',
      childFuturePostOffice: 'Espoo',
      guardianFutureAddressExists: true,
      guardianFutureAddressEqualsChild: true,
      guardianPhone: '(+358) 50-1234567',
      guardianEmail: 'johannes.karhula@example.com'
    },
    additionalDetails: {
      otherInfo: 'Olipa hakeminen helppoa!'
    }
  },
  validateResult: (res) => {
    assertEquals('SENT', res.status)

    assertEquals(
      '11.10.2022',
      res.form.child.futureAddress?.movingDate?.format()
    )
    assertEquals('Katu 1', res.form.child.futureAddress?.street)
    assertEquals('00200', res.form.child.futureAddress?.postalCode)
    assertEquals('Espoo', res.form.child.futureAddress?.postOffice)

    assertBlank(res.form.child.allergies)
    assertBlank(res.form.child.diet)
    assertTrue(res.form.child.assistanceNeeded)
    assertEquals('Keskittymisvaikeus', res.form.child.assistanceDescription)

    assertEquals(
      '11.10.2022',
      res.form.guardian.futureAddress?.movingDate?.format()
    )
    assertEquals('Katu 1', res.form.guardian.futureAddress?.street)
    assertEquals('00200', res.form.guardian.futureAddress?.postalCode)
    assertEquals('Espoo', res.form.guardian.futureAddress?.postOffice)
    assertEquals('(+358) 50-1234567', res.form.guardian.phoneNumber)
    assertEquals('johannes.karhula@example.com', res.form.guardian.email)

    assertNull(res.form.secondGuardian)

    assertNull(res.form.otherPartner)

    assertEmpty(res.form.otherChildren)

    assertEquals(1, res.form.preferences.preferredUnits.length)
    assertEquals(clubFixture.id, res.form.preferences.preferredUnits[0].id)
    assertEquals(
      '13.08.2021',
      res.form.preferences.preferredStartDate?.format()
    )
    assertNull(res.form.preferences.serviceNeed)
    assertEquals('Anna Karhula', res.form.preferences.siblingBasis?.siblingName)
    assertEquals('110814A812B', res.form.preferences.siblingBasis?.siblingSsn)
    assertFalse(res.form.preferences.preparatory)
    assertFalse(res.form.preferences.urgent)

    assertFalse(res.form.maxFeeAccepted)

    assertEquals('Olipa hakeminen helppoa!', res.form.otherInfo)

    assertTrue(res.form.clubDetails?.wasOnClubCare)
    assertTrue(res.form.clubDetails?.wasOnDaycare)
  }
}

export const minimalPreschoolForm: {
  form: FormInput
  validateResult: (result: ApplicationDetails) => void
} = {
  form: {
    serviceNeed: {
      preferredStartDate: '13.8.2021'
    },
    unitPreference: {
      preferredUnits: [
        {
          id: daycareFixture.id,
          name: daycareFixture.name
        }
      ]
    },
    contactInfo: {
      guardianPhone: '(+358) 50-1234567',
      otherGuardianAgreementStatus: 'AGREED'
    }
  },
  validateResult: (res) => {
    assertEquals('SENT', res.status)

    assertNull(res.form.child.futureAddress)
    assertBlank(res.form.child.allergies)
    assertBlank(res.form.child.diet)
    assertFalse(res.form.child.assistanceNeeded)
    assertBlank(res.form.child.assistanceDescription)

    assertEquals(null, res.form.guardian.futureAddress)
    assertEquals('(+358) 50-1234567', res.form.guardian.phoneNumber)
    assertBlank(res.form.guardian.email)

    assertEquals('AGREED', res.form.secondGuardian?.agreementStatus)
    assertBlank(res.form.secondGuardian?.phoneNumber)
    assertBlank(res.form.secondGuardian?.email)

    assertNull(res.form.otherPartner)

    assertEmpty(res.form.otherChildren)

    assertEquals(1, res.form.preferences.preferredUnits.length)
    assertEquals(daycareFixture.id, res.form.preferences.preferredUnits[0].id)
    assertEquals(
      '13.08.2021',
      res.form.preferences.preferredStartDate?.format()
    )
    assertNull(res.form.preferences.serviceNeed)
    assertNull(res.form.preferences.siblingBasis)
    assertFalse(res.form.preferences.preparatory)
    assertFalse(res.form.preferences.urgent)

    assertFalse(res.form.maxFeeAccepted)

    assertBlank(res.form.otherInfo)

    assertNull(res.form.clubDetails)
  }
}

export const fullPreschoolForm: {
  form: FormInput
  validateResult: (result: ApplicationDetails) => void
} = {
  form: {
    serviceNeed: {
      preferredStartDate: '13.8.2021',
      connectedDaycare: true,
      startTime: '09:00',
      endTime: '17:00',
      shiftCare: true,
      preparatory: true,
      assistanceNeeded: true,
      assistanceDescription: 'Keskittymisvaikeus'
    },
    unitPreference: {
      siblingBasis: true,
      siblingName: 'Anna Karhula',
      siblingSsn: '110814A812B',
      preferredUnits: [
        {
          id: daycareFixture.id,
          name: daycareFixture.name
        }
      ]
    },
    contactInfo: {
      childFutureAddressExists: true,
      childMoveDate: '11.10.2022',
      childFutureStreet: 'Katu 1',
      childFuturePostalCode: '00200',
      childFuturePostOffice: 'Espoo',
      guardianFutureAddressExists: true,
      guardianFutureAddressEqualsChild: true,
      guardianPhone: '(+358) 50-1234567',
      guardianEmail: 'johannes.karhula@example.com',
      otherGuardianAgreementStatus: 'NOT_AGREED',
      otherGuardianPhone: '1234567890',
      otherGuardianEmail: 'g2@example.com',
      otherPartnerExists: true,
      otherPartnerFirstName: 'Heikki',
      otherPartnerLastName: 'Heppuli',
      otherPartnerSSN: '210188-389L',
      otherChildrenExists: true,
      otherChildren: [
        {
          firstName: 'Tupu',
          lastName: 'Ankka',
          socialSecurityNumber: '250718A809H'
        }
      ]
    },
    fee: {
      maxFeeAccepted: true
    },
    additionalDetails: {
      otherInfo: 'Olipa hakeminen helppoa!',
      diet: 'Vegaani',
      allergies: 'Pähkinät, chili'
    }
  },
  validateResult: (res) => {
    assertEquals('SENT', res.status)

    assertEquals(
      '11.10.2022',
      res.form.child.futureAddress?.movingDate?.format()
    )
    assertEquals('Katu 1', res.form.child.futureAddress?.street)
    assertEquals('00200', res.form.child.futureAddress?.postalCode)
    assertEquals('Espoo', res.form.child.futureAddress?.postOffice)

    assertEquals('Pähkinät, chili', res.form.child.allergies)
    assertEquals('Vegaani', res.form.child.diet)
    assertTrue(res.form.child.assistanceNeeded)
    assertEquals('Keskittymisvaikeus', res.form.child.assistanceDescription)

    assertEquals(
      '11.10.2022',
      res.form.guardian.futureAddress?.movingDate?.format()
    )
    assertEquals('Katu 1', res.form.guardian.futureAddress?.street)
    assertEquals('00200', res.form.guardian.futureAddress?.postalCode)
    assertEquals('Espoo', res.form.guardian.futureAddress?.postOffice)
    assertEquals('(+358) 50-1234567', res.form.guardian.phoneNumber)
    assertEquals('johannes.karhula@example.com', res.form.guardian.email)

    assertEquals('NOT_AGREED', res.form.secondGuardian?.agreementStatus)
    assertEquals('1234567890', res.form.secondGuardian?.phoneNumber)
    assertEquals('g2@example.com', res.form.secondGuardian?.email)

    assertEquals('Heikki', res.form.otherPartner?.firstName)
    assertEquals('Heppuli', res.form.otherPartner?.lastName)
    assertEquals('210188-389L', res.form.otherPartner?.socialSecurityNumber)

    assertEquals(1, res.form.otherChildren.length)
    assertEquals('Tupu', res.form.otherChildren[0].firstName)
    assertEquals('Ankka', res.form.otherChildren[0].lastName)
    assertEquals('250718A809H', res.form.otherChildren[0].socialSecurityNumber)

    assertEquals(1, res.form.preferences.preferredUnits.length)
    assertEquals(daycareFixture.id, res.form.preferences.preferredUnits[0].id)
    assertEquals(
      '13.08.2021',
      res.form.preferences.preferredStartDate?.format()
    )
    assertEquals('09:00', res.form.preferences.serviceNeed?.startTime)
    assertEquals('17:00', res.form.preferences.serviceNeed?.endTime)
    assertTrue(res.form.preferences.serviceNeed?.shiftCare)
    assertFalse(res.form.preferences.serviceNeed?.partTime)
    assertEquals('Anna Karhula', res.form.preferences.siblingBasis?.siblingName)
    assertEquals('110814A812B', res.form.preferences.siblingBasis?.siblingSsn)
    assertTrue(res.form.preferences.preparatory)
    assertFalse(res.form.preferences.urgent)

    assertTrue(res.form.maxFeeAccepted)

    assertEquals('Olipa hakeminen helppoa!', res.form.otherInfo)

    assertNull(res.form.clubDetails)
  }
}
