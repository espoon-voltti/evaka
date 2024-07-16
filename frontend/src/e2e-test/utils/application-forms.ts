// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ApplicationFormData } from 'lib-common/api-types/application/ApplicationFormData'
import {
  ApplicationDetails,
  OtherGuardianAgreementStatus
} from 'lib-common/generated/api-types/application'
import { JsonOf } from 'lib-common/json'

import { testClub, testDaycare } from '../dev-api/fixtures'
import { DevPerson } from '../generated/api-types'

function assertEquals<T>(expected: T, actual: T) {
  if (actual !== expected)
    throw Error(`expected ${String(expected)}, got ${String(actual)}`)
}

function assertBlank(actual: string | undefined) {
  if (actual !== '') throw Error(`expected empty string, got ${String(actual)}`)
}

function assertEmpty<T>(actual: T[]) {
  if (actual.length !== 0)
    throw Error(`expected empty array, got ${String(actual)}`)
}

function assertTrue(actual: boolean | undefined) {
  if (actual !== true) throw Error(`expected true, got ${String(actual)}`)
}

function assertFalse(actual: boolean | undefined) {
  if (actual !== false) throw Error(`expected false, got ${String(actual)}`)
}

function assertNull<T>(actual: T) {
  if (actual !== null) throw Error(`expected null, got ${String(actual)}`)
}

export const sections = [
  'serviceNeed',
  'unitPreference',
  'contactInfo',
  'additionalDetails'
] as const

export type Section = (typeof sections)[number]

export type FormInput = Partial<{
  [K in Section]: Partial<ApplicationFormData[K]>
}>

export const minimalDaycareForm = ({
  otherGuardianAgreementStatus
}: {
  otherGuardianAgreementStatus?: OtherGuardianAgreementStatus
} = {}): {
  form: JsonOf<FormInput>
  validateResult: (
    result: ApplicationDetails,
    vtjSiblingsLivingInSameAddress: DevPerson[]
  ) => void
} => ({
  form: {
    serviceNeed: {
      preferredStartDate: '16.08.2021',
      startTime: '09:00',
      endTime: '17:00'
    },
    unitPreference: {
      preferredUnits: [
        {
          id: testDaycare.id,
          name: testDaycare.name
        }
      ]
    },
    contactInfo: {
      guardianPhone: '(+358) 50-1234567',
      noGuardianEmail: true,
      otherGuardianAgreementStatus
    }
  },
  validateResult: (res, vtjSiblingsLivingInSameAddress) => {
    assertEquals('SENT', res.status)

    assertNull(res.form.child.futureAddress)
    assertBlank(res.form.child.allergies)
    assertBlank(res.form.child.diet)
    assertFalse(res.form.child.assistanceNeeded)
    assertBlank(res.form.child.assistanceDescription)

    assertEquals(null, res.form.guardian.futureAddress)
    assertEquals('(+358) 50-1234567', res.form.guardian.phoneNumber)
    assertEquals('', res.form.guardian.email)

    if (otherGuardianAgreementStatus) {
      assertEquals(
        otherGuardianAgreementStatus,
        res.form.secondGuardian?.agreementStatus
      )
    } else {
      assertNull(res.form.secondGuardian)
    }

    assertNull(res.form.otherPartner)

    assertEquals(
      vtjSiblingsLivingInSameAddress.length,
      res.form.otherChildren.length
    )
    for (let i = 0; i < vtjSiblingsLivingInSameAddress.length; i++) {
      const { firstName, lastName, ssn } = vtjSiblingsLivingInSameAddress[i]
      assertEquals(firstName, res.form.otherChildren[i].firstName)
      assertEquals(lastName, res.form.otherChildren[i].lastName)
      assertEquals(ssn, res.form.otherChildren[i].socialSecurityNumber)
    }

    assertEquals(1, res.form.preferences.preferredUnits.length)
    assertEquals(testDaycare.id, res.form.preferences.preferredUnits[0].id)
    assertEquals(
      '16.08.2021',
      res.form.preferences.preferredStartDate?.format()
    )
    assertEquals('09:00', res.form.preferences.serviceNeed?.startTime)
    assertEquals('17:00', res.form.preferences.serviceNeed?.endTime)
    assertFalse(res.form.preferences.serviceNeed?.shiftCare)
    assertFalse(res.form.preferences.serviceNeed?.partTime)
    assertNull(res.form.preferences.siblingBasis)
    assertFalse(res.form.preferences.preparatory)
    assertFalse(res.form.preferences.urgent)

    assertBlank(res.form.otherInfo)

    assertNull(res.form.clubDetails)
  }
})

export const fullDaycareForm = ({
  otherGuardianAgreementStatus
}: {
  otherGuardianAgreementStatus?: OtherGuardianAgreementStatus
} = {}): {
  form: JsonOf<FormInput>
  validateResult: (
    result: ApplicationDetails,
    vtjSiblingsLivingInSameAddress: DevPerson[]
  ) => void
} => ({
  form: {
    serviceNeed: {
      preferredStartDate: '16.08.2021',
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
          id: testDaycare.id,
          name: testDaycare.name
        }
      ]
    },
    contactInfo: {
      childFutureAddressExists: true,
      childMoveDate: '11.10.2021',
      childFutureStreet: 'Katu 1',
      childFuturePostalCode: '00200',
      childFuturePostOffice: 'Espoo',
      guardianFutureAddressExists: true,
      guardianFutureAddressEqualsChild: true,
      guardianPhone: '(+358) 50-1234567',
      guardianEmail: 'johannes.karhula@example.com',
      guardianEmailVerification: 'johannes.karhula@example.com',
      otherGuardianAgreementStatus,
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
    additionalDetails: {
      otherInfo: 'Olipa hakeminen helppoa!',
      diet: 'Vegaani',
      allergies: 'Pähkinät, chili'
    }
  },
  validateResult: (res, vtjSiblingsLivingInSameAddress) => {
    assertEquals('SENT', res.status)

    assertEquals(
      '11.10.2021',
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
      '11.10.2021',
      res.form.guardian.futureAddress?.movingDate?.format()
    )
    assertEquals('Katu 1', res.form.guardian.futureAddress?.street)
    assertEquals('00200', res.form.guardian.futureAddress?.postalCode)
    assertEquals('Espoo', res.form.guardian.futureAddress?.postOffice)
    assertEquals('(+358) 50-1234567', res.form.guardian.phoneNumber)
    assertEquals('johannes.karhula@example.com', res.form.guardian.email)

    if (otherGuardianAgreementStatus) {
      assertEquals(
        otherGuardianAgreementStatus,
        res.form.secondGuardian?.agreementStatus
      )
    } else {
      assertNull(res.form.secondGuardian)
    }

    assertEquals('Heikki', res.form.otherPartner?.firstName)
    assertEquals('Heppuli', res.form.otherPartner?.lastName)
    assertEquals('210188-389L', res.form.otherPartner?.socialSecurityNumber)

    assertEquals(
      vtjSiblingsLivingInSameAddress.length + 2,
      res.form.otherChildren.length
    )
    for (let i = 0; i < vtjSiblingsLivingInSameAddress.length; i++) {
      const { firstName, lastName, ssn } = vtjSiblingsLivingInSameAddress[i]
      assertEquals(firstName, res.form.otherChildren[i].firstName)
      assertEquals(lastName, res.form.otherChildren[i].lastName)
      assertEquals(ssn, res.form.otherChildren[i].socialSecurityNumber)
    }
    const tupuIndex = vtjSiblingsLivingInSameAddress.length + 0
    assertEquals('Tupu', res.form.otherChildren[tupuIndex].firstName)
    assertEquals('Ankka', res.form.otherChildren[tupuIndex].lastName)
    assertEquals(
      '250718A809H',
      res.form.otherChildren[tupuIndex].socialSecurityNumber
    )
    const taunoIndex = vtjSiblingsLivingInSameAddress.length + 1
    assertEquals('Tauno', res.form.otherChildren[taunoIndex].firstName)
    assertEquals('Hanhi', res.form.otherChildren[taunoIndex].lastName)
    assertEquals(
      '101017A479B',
      res.form.otherChildren[taunoIndex].socialSecurityNumber
    )

    assertEquals(1, res.form.preferences.preferredUnits.length)
    assertEquals(testDaycare.id, res.form.preferences.preferredUnits[0].id)
    assertEquals(
      '16.08.2021',
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

    assertEquals('Olipa hakeminen helppoa!', res.form.otherInfo)

    assertNull(res.form.clubDetails)
  }
})

export const minimalClubForm: {
  form: JsonOf<FormInput>
  validateResult: (result: ApplicationDetails) => void
} = {
  form: {
    serviceNeed: {
      preferredStartDate: '16.08.2021'
    },
    unitPreference: {
      preferredUnits: [
        {
          id: testClub.id,
          name: testClub.name
        }
      ]
    },
    contactInfo: {
      guardianPhone: '(+358) 50-1234567',
      noGuardianEmail: true
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
    assertEquals('', res.form.guardian.email)

    assertNull(res.form.secondGuardian)

    assertNull(res.form.otherPartner)

    assertEmpty(res.form.otherChildren)

    assertEquals(1, res.form.preferences.preferredUnits.length)
    assertEquals(testClub.id, res.form.preferences.preferredUnits[0].id)
    assertEquals(
      '16.08.2021',
      res.form.preferences.preferredStartDate?.format()
    )
    assertNull(res.form.preferences.serviceNeed)
    assertNull(res.form.preferences.siblingBasis)
    assertFalse(res.form.preferences.preparatory)
    assertFalse(res.form.preferences.urgent)

    assertBlank(res.form.otherInfo)

    assertFalse(res.form.clubDetails?.wasOnDaycare)
    assertFalse(res.form.clubDetails?.wasOnClubCare)
  }
}

export const fullClubForm: {
  form: JsonOf<FormInput>
  validateResult: (result: ApplicationDetails) => void
} = {
  form: {
    serviceNeed: {
      preferredStartDate: '16.08.2021',
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
          id: testClub.id,
          name: testClub.name
        }
      ]
    },
    contactInfo: {
      childFutureAddressExists: true,
      childMoveDate: '11.10.2021',
      childFutureStreet: 'Katu 1',
      childFuturePostalCode: '00200',
      childFuturePostOffice: 'Espoo',
      guardianFutureAddressExists: true,
      guardianFutureAddressEqualsChild: true,
      guardianPhone: '(+358) 50-1234567',
      guardianEmail: 'johannes.karhula@example.com',
      guardianEmailVerification: 'johannes.karhula@example.com'
    },
    additionalDetails: {
      otherInfo: 'Olipa hakeminen helppoa!'
    }
  },
  validateResult: (res) => {
    assertEquals('SENT', res.status)

    assertEquals(
      '11.10.2021',
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
      '11.10.2021',
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
    assertEquals(testClub.id, res.form.preferences.preferredUnits[0].id)
    assertEquals(
      '16.08.2021',
      res.form.preferences.preferredStartDate?.format()
    )
    assertNull(res.form.preferences.serviceNeed)
    assertEquals('Anna Karhula', res.form.preferences.siblingBasis?.siblingName)
    assertEquals('110814A812B', res.form.preferences.siblingBasis?.siblingSsn)
    assertFalse(res.form.preferences.preparatory)
    assertFalse(res.form.preferences.urgent)

    assertEquals('Olipa hakeminen helppoa!', res.form.otherInfo)

    assertTrue(res.form.clubDetails?.wasOnClubCare)
    assertTrue(res.form.clubDetails?.wasOnDaycare)
  }
}

export const minimalPreschoolForm: {
  form: JsonOf<FormInput>
  validateResult: (result: ApplicationDetails) => void
} = {
  form: {
    serviceNeed: {
      preferredStartDate: '16.08.2021'
    },
    unitPreference: {
      preferredUnits: [
        {
          id: testDaycare.id,
          name: testDaycare.name
        }
      ]
    },
    contactInfo: {
      guardianPhone: '(+358) 50-1234567',
      noGuardianEmail: true,
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
    assertEquals('', res.form.guardian.email)

    assertEquals('AGREED', res.form.secondGuardian?.agreementStatus)
    assertBlank(res.form.secondGuardian?.phoneNumber)
    assertBlank(res.form.secondGuardian?.email)

    assertNull(res.form.otherPartner)

    assertEmpty(res.form.otherChildren)

    assertEquals(1, res.form.preferences.preferredUnits.length)
    assertEquals(testDaycare.id, res.form.preferences.preferredUnits[0].id)
    assertEquals(
      '16.08.2021',
      res.form.preferences.preferredStartDate?.format()
    )
    assertNull(res.form.preferences.serviceNeed)
    assertNull(res.form.preferences.siblingBasis)
    assertFalse(res.form.preferences.preparatory)
    assertFalse(res.form.preferences.urgent)

    assertBlank(res.form.otherInfo)

    assertNull(res.form.clubDetails)
  }
}

export const fullPreschoolForm: {
  form: JsonOf<FormInput>
  validateResult: (
    result: ApplicationDetails,
    vtjSiblingsLivingInSameAddress: DevPerson[]
  ) => void
} = {
  form: {
    serviceNeed: {
      preferredStartDate: '16.08.2021',
      connectedDaycare: true,
      connectedDaycarePreferredStartDate: '16.08.2021',
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
          id: testDaycare.id,
          name: testDaycare.name
        }
      ]
    },
    contactInfo: {
      childFutureAddressExists: true,
      childMoveDate: '11.10.2021',
      childFutureStreet: 'Katu 1',
      childFuturePostalCode: '00200',
      childFuturePostOffice: 'Espoo',
      guardianFutureAddressExists: true,
      guardianFutureAddressEqualsChild: true,
      guardianPhone: '(+358) 50-1234567',
      guardianEmail: 'johannes.karhula@example.com',
      guardianEmailVerification: 'johannes.karhula@example.com',
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
    additionalDetails: {
      otherInfo: 'Olipa hakeminen helppoa!',
      diet: 'Vegaani',
      allergies: 'Pähkinät, chili'
    }
  },
  validateResult: (res, vtjSiblingsLivingInSameAddress) => {
    assertEquals('SENT', res.status)

    assertEquals(
      '11.10.2021',
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
      '11.10.2021',
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

    assertEquals(
      vtjSiblingsLivingInSameAddress.length + 1,
      res.form.otherChildren.length
    )
    for (let i = 0; i < vtjSiblingsLivingInSameAddress.length; i++) {
      const { firstName, lastName, ssn } = vtjSiblingsLivingInSameAddress[i]
      assertEquals(firstName, res.form.otherChildren[i].firstName)
      assertEquals(lastName, res.form.otherChildren[i].lastName)
      assertEquals(ssn, res.form.otherChildren[i].socialSecurityNumber)
    }
    const tupuIndex = vtjSiblingsLivingInSameAddress.length + 0
    assertEquals('Tupu', res.form.otherChildren[tupuIndex].firstName)
    assertEquals('Ankka', res.form.otherChildren[tupuIndex].lastName)
    assertEquals(
      '250718A809H',
      res.form.otherChildren[tupuIndex].socialSecurityNumber
    )

    assertEquals(1, res.form.preferences.preferredUnits.length)
    assertEquals(testDaycare.id, res.form.preferences.preferredUnits[0].id)
    assertEquals(
      '16.08.2021',
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

    assertEquals('Olipa hakeminen helppoa!', res.form.otherInfo)

    assertNull(res.form.clubDetails)
  }
}
