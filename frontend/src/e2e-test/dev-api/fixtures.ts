// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { ScopedRole } from 'lib-common/api-types/employee-auth'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import type {
  ApplicationForm,
  ApplicationStatus,
  ApplicationType,
  OtherGuardianAgreementStatus
} from 'lib-common/generated/api-types/application'
import type {
  AssistanceFactor,
  DaycareAssistance,
  OtherAssistanceMeasure,
  PreschoolAssistance
} from 'lib-common/generated/api-types/assistance'
import type {
  AssistanceNeedPreschoolDecisionForm,
  AssistanceNeedVoucherCoefficient
} from 'lib-common/generated/api-types/assistanceneed'
import type { DocumentContent } from 'lib-common/generated/api-types/document'
import type { HolidayPeriod } from 'lib-common/generated/api-types/holidayperiod'
import { HolidayQuestionnaire } from 'lib-common/generated/api-types/holidayperiod'
import type {
  DecisionIncome,
  FeeDecision,
  FeeDecisionStatus,
  FeeThresholds,
  IncomeNotification
} from 'lib-common/generated/api-types/invoicing'
import type { PlacementType } from 'lib-common/generated/api-types/placement'
import type { DailyReservationRequest } from 'lib-common/generated/api-types/reservations'
import type { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import type {
  ApplicationId,
  AreaId,
  DaycareId,
  EmployeeId,
  EvakaUserId,
  FeeDecisionId,
  GroupId,
  PersonId,
  PlacementId,
  VoucherValueDecisionId
} from 'lib-common/generated/api-types/shared'
import type { EvakaUser } from 'lib-common/generated/api-types/user'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import { evakaUserId, fromUuid, randomId } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'

import {
  addAbsence,
  addAclRoleForDaycare,
  addCalendarEvent,
  addCalendarEventAttendee,
  addCalendarEventTime,
  addDailyServiceTime,
  addDailyServiceTimeNotification,
  addPayment,
  addStaffAttendance,
  addStaffAttendancePlan,
  createAssistanceAction,
  createAssistanceActionOption,
  createAssistanceFactors,
  createAssistanceNeedDecisions,
  createAssistanceNeedPreschoolDecisions,
  createAssistanceNeedVoucherCoefficients,
  createBackupCares,
  createCareAreas,
  createChildDocument,
  createChildren,
  createClubTerm,
  createDaycareAssistances,
  createDaycareCaretakers,
  createDaycareGroupAclRows,
  createDaycareGroupPlacement,
  createDaycareGroups,
  createDaycarePlacements,
  createDaycares,
  createDecisions,
  createDocumentTemplate,
  createEmployee,
  createEmployeePins,
  createFeeThresholds,
  createFridgeChild,
  createHolidayPeriod,
  createHolidayQuestionnaire,
  createIncome,
  createIncomeNotification,
  createIncomeStatement,
  createInvoices,
  createNekkuCustomer,
  createNekkuSpecialDiets,
  createOtherAssistanceMeasures,
  createParentships,
  createPedagogicalDocuments,
  createPerson,
  createPlacementPlan,
  createPreschoolAssistances,
  createPreschoolTerm,
  createServiceNeedOption,
  createServiceNeeds,
  insertGuardians,
  postAttendances,
  postReservations,
  postReservationsRaw,
  upsertStaffOccupancyCoefficient,
  upsertVtjDataset,
  upsertWeakCredentials
} from '../generated/api-clients'
import type {
  Caretaker,
  DecisionRequest,
  DevAbsence,
  DevApplicationWithForm,
  DevAssistanceAction,
  DevAssistanceActionOption,
  DevAssistanceNeedDecision,
  DevAssistanceNeedPreschoolDecision,
  DevBackupCare,
  DevCalendarEvent,
  DevCalendarEventAttendee,
  DevCalendarEventTime,
  DevCareArea,
  DevChild,
  DevChildAttendance,
  DevChildDocument,
  DevChildDocumentDecision,
  DevClubTerm,
  DevDailyServiceTimeNotification,
  DevDailyServiceTimes,
  DevDaycare,
  DevDaycareGroup,
  DevDaycareGroupPlacement,
  DevDocumentTemplate,
  DevEmployee,
  DevEmployeePin,
  DevFridgeChild,
  DevIncome,
  DevIncomeStatement,
  DevInvoice,
  DevInvoiceRow,
  DevParentship,
  DevPayment,
  DevPedagogicalDocument,
  DevPerson,
  DevPlacement,
  DevPreschoolTerm,
  DevServiceNeed,
  DevStaffAttendance,
  DevStaffAttendancePlan,
  DevUpsertStaffOccupancyCoefficient,
  NekkuCustomer,
  NekkuSpecialDiet,
  PlacementPlan,
  PlacementSource,
  ReservationInsert,
  VoucherValueDecision
} from '../generated/api-types'
import { upsertDummyIdpUser } from '../utils/dummy-idp'

import FixedPeriodQuestionnaire = HolidayQuestionnaire.FixedPeriodQuestionnaire

const uniqueLabel = (l = 7): string =>
  Math.random().toString(36).substring(0, l)

export const uuidv4 = (): string =>
  'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0,
      v = c === 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })

type SemiPartial<T, K extends keyof T> = Partial<T> & Pick<T, K>

export class Fixture {
  static daycare(initial: SemiPartial<DevDaycare, 'areaId'>) {
    const id = uniqueLabel()
    const value: DevDaycare = {
      id: randomId(),
      name: `daycare_${id}`,
      type: ['CENTRE'],
      dailyPreschoolTime: new TimeRange(
        LocalTime.of(9, 0),
        LocalTime.of(13, 0)
      ),
      dailyPreparatoryTime: new TimeRange(
        LocalTime.of(9, 0),
        LocalTime.of(14, 0)
      ),
      costCenter: `costCenter_${id}`,
      visitingAddress: {
        streetAddress: `streetAddress_${id}`,
        postalCode: '02230',
        postOffice: 'Espoo'
      },
      decisionCustomization: {
        daycareName: `decisionDaycareName_${id}`,
        preschoolName: `decisionPreschoolName_${id}`,
        handler: `decisionHandler_${id}`,
        handlerAddress: `decisionHandlerAddress_${id}`
      },
      providerType: 'MUNICIPAL',
      operationTimes: [
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        fullDayTimeRange,
        null,
        null
      ],
      shiftCareOperationTimes: null,
      shiftCareOpenOnHolidays: false,
      enabledPilotFeatures: ['MESSAGING', 'MOBILE'],
      businessId: '',
      iban: '',
      providerId: '',
      partnerCode: '',
      capacity: 0,
      openingDate: null,
      closingDate: null,
      ghostUnit: false,
      invoicedByMunicipality: true,
      uploadChildrenToVarda: true,
      uploadToVarda: true,
      uploadToKoski: true,
      language: 'fi',
      location: null,
      mailingAddress: {
        poBox: null,
        postOffice: null,
        postalCode: null,
        streetAddress: null
      },
      unitManager: {
        email: '',
        name: 'Unit Manager',
        phone: ''
      },
      financeDecisionHandler: null,
      clubApplyPeriod: null,
      daycareApplyPeriod: new DateRange(LocalDate.of(2020, 3, 1), null),
      preschoolApplyPeriod: new DateRange(LocalDate.of(2020, 3, 1), null),
      email: null,
      phone: null,
      url: null,
      ophUnitOid: '1.2.3.4.5',
      ophOrganizerOid: '1.2.3.4.5',
      additionalInfo: null,
      dwCostCenter: 'dw-test',
      mealtimeBreakfast: null,
      mealtimeLunch: null,
      mealtimeSnack: null,
      mealtimeSupper: null,
      mealtimeEveningSnack: null,
      withSchool: false,
      nekkuOrderReductionPercentage: 10,
      nekkuNoWeekendMealOrders: false,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createDaycares({ body: [value] })
        return value
      }
    }
  }

  static daycareGroup(initial: SemiPartial<DevDaycareGroup, 'daycareId'>) {
    const id = uniqueLabel()
    const value: DevDaycareGroup = {
      id: randomId(),
      name: `daycareGroup_${id}`,
      startDate: LocalDate.of(2020, 1, 1),
      endDate: null,
      jamixCustomerNumber: null,
      aromiCustomerId: null,
      nekkuCustomerNumber: null,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createDaycareGroups({ body: [value] })
        return value
      }
    }
  }

  static careArea(initial?: Partial<DevCareArea>) {
    const id = uniqueLabel()
    const value: DevCareArea = {
      id: randomId(),
      name: `Care Area ${id}`,
      shortName: `careArea_${id}`,
      areaCode: 2230,
      subCostCenter: `subCostCenter_${id}`,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createCareAreas({ body: [value] })
        return value
      }
    }
  }

  static preschoolTerm(
    initial: SemiPartial<
      DevPreschoolTerm,
      | 'applicationPeriod'
      | 'extendedTerm'
      | 'finnishPreschool'
      | 'swedishPreschool'
    >
  ) {
    const value: DevPreschoolTerm = {
      id: randomId(),
      termBreaks: [],
      ...initial
    }
    return {
      ...value,
      async save() {
        await createPreschoolTerm({ body: value })
        return value
      }
    }
  }

  static clubTerm(
    initial: SemiPartial<DevClubTerm, 'applicationPeriod' | 'term'>
  ) {
    const value: DevClubTerm = {
      id: randomId(),
      termBreaks: [],
      ...initial
    }
    return {
      ...value,
      async save() {
        await createClubTerm({ body: value })
        return initial
      }
    }
  }

  static person(initial?: Partial<DevPerson>) {
    const id = uniqueLabel()
    const value: DevPerson = {
      id: randomId(),
      dateOfBirth: LocalDate.of(2020, 5, 5),
      dateOfDeath: null,
      ssn: '050520A999M',
      email: `email_${id}@evaka.test`,
      verifiedEmail: null,
      firstName: `firstName_${id}`,
      preferredName: '',
      lastName: `lastName_${id}`,
      language: `fi`,
      nationalities: [],
      phone: '123456789',
      backupPhone: '',
      postalCode: '02230',
      postOffice: 'Espoo',
      residenceCode: `residenceCode_${id}`,
      municipalityOfResidence: `municipalityOfResidence_${id}`,
      restrictedDetailsEnabled: false,
      restrictedDetailsEndDate: null,
      streetAddress: `streetAddress_${id}`,
      duplicateOf: null,
      disabledEmailTypes: [],
      forceManualFeeDecisions: false,
      invoiceRecipientName: '',
      invoicingStreetAddress: '',
      invoicingPostalCode: '',
      invoicingPostOffice: '',
      ssnAddingDisabled: null,
      ophPersonOid: null,
      updatedFromVtj: null,
      ...initial
    }

    const updateMockVtj = async (
      dependants: DevPerson[]
    ): Promise<DevPerson> => {
      const person = value
      const dependantSsns = dependants.flatMap((d) => d.ssn ?? [])
      if (dependantSsns.length !== dependants.length) {
        throw new Error('All dependants must have SSNs')
      }
      await upsertVtjDataset({
        body: {
          persons: [
            {
              firstNames: person.firstName,
              lastName: person.lastName,
              socialSecurityNumber: person.ssn || '',
              address: {
                streetAddress: person.streetAddress || '',
                postalCode: person.postalCode || '',
                postOffice: person.postOffice || '',
                streetAddressSe: person.streetAddress || '',
                postOfficeSe: person.postalCode || ''
              },
              dateOfDeath: person.dateOfDeath ?? null,
              nationalities: [],
              nativeLanguage: null,
              residenceCode:
                person.residenceCode ??
                `${person.streetAddress ?? ''}${person.postalCode ?? ''}${
                  person.postOffice ?? ''
                }`.replace(' ', ''),
              municipalityOfResidence: person.municipalityOfResidence,
              restrictedDetails: {
                enabled: person.restrictedDetailsEnabled || false,
                endDate: person.restrictedDetailsEndDate || null
              }
            }
          ],
          guardianDependants:
            dependantSsns.length > 0
              ? {
                  [person.ssn || '']: dependantSsns
                }
              : {}
        }
      })
      return value
    }

    return {
      ...value,
      async saveAdult(
        opts: {
          updateMockVtjWithDependants?: DevPerson[]
          updateWeakCredentials?: { username: string; password: string }
        } = {}
      ) {
        await createPerson({ body: value, type: 'ADULT' })
        if (opts.updateMockVtjWithDependants !== undefined) {
          await updateMockVtj(opts.updateMockVtjWithDependants)
          if (value.ssn) {
            await upsertDummyIdpUser({
              ssn: value.ssn,
              commonName: `${value.firstName} ${value.lastName}`,
              givenName: value.firstName,
              surname: value.lastName,
              comment: `${opts.updateMockVtjWithDependants.length} huollettavaa`
            })
          }
        }
        if (opts.updateWeakCredentials) {
          await upsertWeakCredentials({
            id: value.id,
            body: opts.updateWeakCredentials
          })
        }
        return value
      },

      async saveChild(opts: { updateMockVtj?: boolean } = {}) {
        await createPerson({ body: value, type: 'CHILD' })
        if (opts.updateMockVtj) {
          await updateMockVtj([])
        }
        return value
      }
    }
  }

  static family(value: Family) {
    return {
      ...value,
      async save() {
        for (const child of value.children) {
          await Fixture.person(child).saveChild({ updateMockVtj: true })
        }
        await Fixture.person(value.guardian).saveAdult({
          updateMockVtjWithDependants: value.children
        })
        if (value.otherGuardian) {
          await Fixture.person(value.otherGuardian).saveAdult({
            updateMockVtjWithDependants: value.children
          })
        }
      }
    }
  }

  static employee(
    initial?: Partial<DevEmployee>,
    unitRoles: {
      unitId: DaycareId
      role: ScopedRole
    }[] = [],
    groupAcl: {
      groupId: GroupId
      createdAt?: HelsinkiDateTime
      updatedAt?: HelsinkiDateTime
    }[] = []
  ) {
    const id = uniqueLabel()
    const value: DevEmployee = {
      id: randomId(),
      email: `email_${id}@evaka.test`,
      externalId: `espoo-ad:${randomId()}`,
      firstName: `first_name_${id}`,
      lastName: `last_name_${id}`,
      roles: [],
      active: true,
      employeeNumber: null,
      lastLogin: HelsinkiDateTime.now(),
      preferredFirstName: null,
      ...initial
    }
    const save = async () => {
      await createEmployee({ body: value })

      for (const { unitId, role } of unitRoles) {
        if (!value.externalId)
          throw new Error("Can't add ACL without externalId")
        await addAclRoleForDaycare({
          daycareId: unitId,
          body: { externalId: value.externalId, role }
        })
      }

      if (groupAcl.length > 0) {
        await createDaycareGroupAclRows({
          body: groupAcl.map(({ groupId, createdAt, updatedAt }) => ({
            groupId,
            employeeId: value.id,
            created: createdAt ?? HelsinkiDateTime.now(),
            updated: updatedAt ?? HelsinkiDateTime.now()
          }))
        })
      }
      return value
    }
    return {
      ...value,
      admin() {
        return Fixture.employee(
          { ...value, roles: ['ADMIN'] },
          unitRoles,
          groupAcl
        )
      },
      financeAdmin() {
        return Fixture.employee(
          { ...value, roles: ['FINANCE_ADMIN'] },
          unitRoles,
          groupAcl
        )
      },
      director() {
        return Fixture.employee(
          { ...value, roles: ['DIRECTOR'] },
          unitRoles,
          groupAcl
        )
      },
      reportViewer() {
        return Fixture.employee(
          { ...value, roles: ['REPORT_VIEWER'] },
          unitRoles,
          groupAcl
        )
      },
      serviceWorker() {
        return Fixture.employee(
          { ...value, roles: ['SERVICE_WORKER'] },
          unitRoles,
          groupAcl
        )
      },
      messenger() {
        return Fixture.employee(
          { ...value, roles: ['MESSAGING'] },
          unitRoles,
          groupAcl
        )
      },
      unitSupervisor(unitId: DaycareId) {
        return Fixture.employee(
          value,
          [...unitRoles, { unitId, role: 'UNIT_SUPERVISOR' }],
          groupAcl
        )
      },
      staff(unitId: DaycareId) {
        return Fixture.employee(
          value,
          [...unitRoles, { unitId, role: 'STAFF' }],
          groupAcl
        )
      },
      specialEducationTeacher(unitId: DaycareId) {
        return Fixture.employee(
          value,
          [...unitRoles, { unitId, role: 'SPECIAL_EDUCATION_TEACHER' }],
          groupAcl
        )
      },
      groupAcl(
        groupId: GroupId,
        createdAt?: HelsinkiDateTime,
        updatedAt?: HelsinkiDateTime
      ) {
        return Fixture.employee(value, unitRoles, [
          ...groupAcl,
          { groupId, createdAt, updatedAt }
        ])
      },
      save
    }
  }

  static decision(
    initial: SemiPartial<
      DecisionRequest,
      'employeeId' | 'applicationId' | 'unitId'
    >
  ) {
    const value: DecisionRequest = {
      id: randomId(),
      type: 'DAYCARE',
      startDate: LocalDate.of(2020, 1, 1),
      endDate: LocalDate.of(2021, 1, 1),
      status: 'PENDING',
      ...initial
    }
    return {
      ...value,
      async save() {
        await createDecisions({ body: [value] })
        return value
      }
    }
  }

  static employeePin(initial: Partial<DevEmployeePin>) {
    const value: DevEmployeePin = {
      id: randomId(),
      userId: null,
      pin: uniqueLabel(4),
      employeeExternalId: null,
      locked: false,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createEmployeePins({ body: [value] })
        return value
      }
    }
  }

  static pedagogicalDocument(
    initial: SemiPartial<DevPedagogicalDocument, 'childId'>
  ) {
    const value: DevPedagogicalDocument = {
      id: randomId(),
      description: 'Test description',
      createdAt: HelsinkiDateTime.now(),
      createdBy: systemInternalUser.id,
      modifiedAt: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser.id,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createPedagogicalDocuments({ body: [value] })
        return value
      }
    }
  }

  static placement(initial: SemiPartial<DevPlacement, 'childId' | 'unitId'>) {
    const value: DevPlacement = {
      id: randomId(),
      type: 'DAYCARE',
      startDate: LocalDate.todayInSystemTz(),
      endDate: LocalDate.todayInSystemTz().addYears(1),
      placeGuarantee: false,
      terminatedBy: null,
      terminationRequestedDate: null,
      createdAt: HelsinkiDateTime.now(),
      createdBy: systemInternalUser.id,
      source: 'MANUAL',
      sourceApplicationId: null,
      sourceServiceApplicationId: null,
      modifiedAt: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser.id,
      ...initial
    }
    return {
      ...value,
      dateRange() {
        return new FiniteDateRange(value.startDate, value.endDate)
      },
      async save() {
        await createDaycarePlacements({ body: [value] })
        return value
      }
    }
  }

  static groupPlacement(
    initial: SemiPartial<
      DevDaycareGroupPlacement,
      'daycareGroupId' | 'daycarePlacementId' | 'startDate' | 'endDate'
    >
  ) {
    const value: DevDaycareGroupPlacement = {
      id: randomId(),
      ...initial
    }
    return {
      ...value,
      async save() {
        await createDaycareGroupPlacement({ body: [value] })
        return value
      }
    }
  }

  static backupCare(initial: SemiPartial<DevBackupCare, 'childId' | 'unitId'>) {
    const value: DevBackupCare = {
      id: randomId(),
      createdAt: HelsinkiDateTime.now(),
      createdBy: systemInternalUser.id,
      modifiedAt: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser.id,
      groupId: null,
      period: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      ...initial
    }
    return {
      ...value,
      async save() {
        await createBackupCares({ body: [value] })
        return value
      }
    }
  }

  static serviceNeed(
    initial: SemiPartial<
      DevServiceNeed,
      'placementId' | 'optionId' | 'confirmedBy'
    >
  ) {
    const value: DevServiceNeed = {
      id: randomId(),
      startDate: LocalDate.todayInSystemTz(),
      endDate: LocalDate.todayInSystemTz(),
      shiftCare: 'NONE',
      partWeek: false,
      confirmedAt: HelsinkiDateTime.now(),
      ...initial
    }
    return {
      ...value,
      async save() {
        await createServiceNeeds({ body: [value] })
        return value
      }
    }
  }

  static serviceNeedOption(initial?: Partial<ServiceNeedOption>) {
    const id = uniqueLabel()
    const value: ServiceNeedOption = {
      id: randomId(),
      daycareHoursPerWeek: 0,
      contractDaysPerMonth: null,
      daycareHoursPerMonth: null,
      defaultOption: false,
      feeCoefficient: 0.0,
      feeDescriptionFi: `Test service need option ${id}`,
      feeDescriptionSv: `Test service need option ${id}`,
      nameFi: `test_service_need_option_${id}`,
      nameSv: `test_service_need_option_${id}`,
      nameEn: `test_service_need_option_${id}`,
      occupancyCoefficient: 0,
      occupancyCoefficientUnder3y: 0,
      realizedOccupancyCoefficient: 0,
      realizedOccupancyCoefficientUnder3y: 0,
      partDay: false,
      partWeek: false,
      updated: HelsinkiDateTime.now(),
      validPlacementType: 'DAYCARE',
      voucherValueDescriptionFi: `Test service need option ${id}`,
      voucherValueDescriptionSv: `Test service need option ${id}`,
      validFrom: LocalDate.of(2000, 1, 1),
      validTo: null,
      showForCitizen: true,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createServiceNeedOption({ body: [value] })
        return value
      }
    }
  }

  static childAdditionalInfo(initial: SemiPartial<DevChild, 'id'>) {
    const value: DevChild = {
      additionalInfo: '',
      allergies: '',
      diet: '',
      dietId: null,
      mealTextureId: null,
      languageAtHome: '',
      languageAtHomeDetails: '',
      medication: '',
      nekkuDiet: null,
      participatesInBreakfast: true,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createChildren({ body: [value] })
        return value
      }
    }
  }

  static assistanceFactor(initial: SemiPartial<AssistanceFactor, 'childId'>) {
    const value: AssistanceFactor = {
      id: randomId(),
      capacityFactor: 1.0,
      validDuring: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      modifiedAt: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createAssistanceFactors({ body: [value] })
        return value
      }
    }
  }

  static daycareAssistance(initial: SemiPartial<DaycareAssistance, 'childId'>) {
    const value: DaycareAssistance = {
      id: randomId(),
      level: 'GENERAL_SUPPORT',
      validDuring: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      modified: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createDaycareAssistances({ body: [value] })
        return value
      }
    }
  }

  static preschoolAssistance(
    initial: SemiPartial<PreschoolAssistance, 'childId'>
  ) {
    const value: PreschoolAssistance = {
      id: randomId(),
      level: 'SPECIAL_SUPPORT',
      validDuring: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      modified: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createPreschoolAssistances({ body: [value] })
        return value
      }
    }
  }

  static otherAssistanceMeasure(
    initial: SemiPartial<OtherAssistanceMeasure, 'childId'>
  ) {
    const value: OtherAssistanceMeasure = {
      id: randomId(),
      type: 'TRANSPORT_BENEFIT',
      validDuring: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      modified: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createOtherAssistanceMeasures({ body: [value] })
        return value
      }
    }
  }

  static assistanceNeedDecision(
    initial: SemiPartial<DevAssistanceNeedDecision, 'childId'>
  ) {
    const value: DevAssistanceNeedDecision = {
      id: randomId(),
      assistanceLevels: ['SPECIAL_ASSISTANCE'],
      careMotivation: null,
      decisionMade: null,
      decisionMaker: {
        employeeId: null,
        title: null,
        name: null,
        phoneNumber: null
      },
      decisionNumber: 1001,
      expertResponsibilities: null,
      guardianInfo: [],
      guardiansHeardOn: null,
      language: 'FI',
      motivationForDecision: null,
      otherRepresentativeDetails: null,
      otherRepresentativeHeard: false,
      pedagogicalMotivation: null,
      preparedBy1: {
        employeeId: null,
        title: null,
        phoneNumber: null,
        name: null
      },
      preparedBy2: {
        employeeId: null,
        title: null,
        phoneNumber: null,
        name: null
      },
      selectedUnit: null,
      sentForDecision: null,
      serviceOptions: {
        consultationSpecialEd: false,
        fullTimeSpecialEd: false,
        interpretationAndAssistanceServices: false,
        partTimeSpecialEd: false,
        specialAides: false
      },
      servicesMotivation: null,
      validityPeriod: new DateRange(LocalDate.of(2019, 1, 2), null),
      endDateNotKnown: false,
      status: 'DRAFT',
      structuralMotivationDescription: null,
      structuralMotivationOptions: {
        additionalStaff: false,
        childAssistant: false,
        groupAssistant: false,
        smallGroup: false,
        smallerGroup: false,
        specialGroup: false
      },
      viewOfGuardians: null,
      unreadGuardianIds: null,
      annulmentReason: '',
      ...initial
    }
    return {
      ...value,
      async save() {
        await createAssistanceNeedDecisions({ body: [value] })
        return value
      }
    }
  }

  static preFilledAssistanceNeedDecision(
    initial: SemiPartial<DevAssistanceNeedDecision, 'childId'>
  ) {
    const value: DevAssistanceNeedDecision = {
      id: randomId(),
      assistanceLevels: ['ENHANCED_ASSISTANCE'],
      careMotivation: 'Care motivation text',
      decisionMade: null,
      decisionMaker: {
        employeeId: null,
        title: null,
        name: null,
        phoneNumber: null
      },
      decisionNumber: 1001,
      expertResponsibilities: 'Expert responsibilities text',
      guardianInfo: [],
      guardiansHeardOn: LocalDate.of(2020, 4, 5),
      language: 'FI',
      motivationForDecision: 'Motivation for decision text',
      otherRepresentativeDetails: 'John Doe, 01020304050, via phone',
      otherRepresentativeHeard: true,
      pedagogicalMotivation: 'Pedagogical motivation text',
      preparedBy1: {
        employeeId: null,
        title: null,
        phoneNumber: null,
        name: null
      },
      preparedBy2: {
        employeeId: null,
        title: null,
        phoneNumber: null,
        name: null
      },
      selectedUnit: null,
      sentForDecision: null,
      serviceOptions: {
        consultationSpecialEd: false,
        fullTimeSpecialEd: false,
        interpretationAndAssistanceServices: true,
        partTimeSpecialEd: true,
        specialAides: false
      },
      servicesMotivation: 'Services motivation text',
      validityPeriod: new DateRange(LocalDate.of(2019, 1, 2), null),
      endDateNotKnown: false,
      status: 'DRAFT',
      structuralMotivationDescription: 'Structural motivation description text',
      structuralMotivationOptions: {
        additionalStaff: false,
        childAssistant: false,
        groupAssistant: true,
        smallGroup: false,
        smallerGroup: true,
        specialGroup: false
      },
      viewOfGuardians: 'VOG text',
      unreadGuardianIds: null,
      annulmentReason: '',
      ...initial
    }
    return {
      ...value,
      async save() {
        await createAssistanceNeedDecisions({ body: [value] })
        return value
      }
    }
  }

  static assistanceNeedPreschoolDecision(
    initial: SemiPartial<DevAssistanceNeedPreschoolDecision, 'childId'>
  ) {
    const value: DevAssistanceNeedPreschoolDecision = {
      id: randomId(),
      decisionNumber: 1000,
      status: 'DRAFT',
      annulmentReason: '',
      sentForDecision: null,
      decisionMade: null,
      unreadGuardianIds: null,
      form: {
        language: 'FI',
        type: null,
        validFrom: null,
        validTo: null,
        extendedCompulsoryEducation: false,
        extendedCompulsoryEducationInfo: '',
        grantedAssistanceService: false,
        grantedInterpretationService: false,
        grantedAssistiveDevices: false,
        grantedServicesBasis: '',
        selectedUnit: null,
        primaryGroup: '',
        decisionBasis: '',
        basisDocumentPedagogicalReport: false,
        basisDocumentPsychologistStatement: false,
        basisDocumentSocialReport: false,
        basisDocumentDoctorStatement: false,
        basisDocumentPedagogicalReportDate: null,
        basisDocumentPsychologistStatementDate: null,
        basisDocumentSocialReportDate: null,
        basisDocumentDoctorStatementDate: null,
        basisDocumentOtherOrMissing: false,
        basisDocumentOtherOrMissingInfo: '',
        basisDocumentsInfo: '',
        guardiansHeardOn: null,
        guardianInfo: [],
        otherRepresentativeHeard: false,
        otherRepresentativeDetails: '',
        viewOfGuardians: '',
        preparer1EmployeeId: null,
        preparer1Title: '',
        preparer1PhoneNumber: '',
        preparer2EmployeeId: null,
        preparer2Title: '',
        preparer2PhoneNumber: '',
        decisionMakerEmployeeId: null,
        decisionMakerTitle: ''
      },
      ...initial
    }
    return {
      ...value,
      async save() {
        await createAssistanceNeedPreschoolDecisions({ body: [value] })
        return value
      },
      with(update: Partial<DevAssistanceNeedPreschoolDecision>) {
        return Fixture.assistanceNeedPreschoolDecision({ ...value, ...update })
      },
      withGuardian(guardianId: PersonId) {
        return Fixture.assistanceNeedPreschoolDecision({
          ...value,
          form: {
            ...value.form,
            guardianInfo: [
              ...value.form.guardianInfo,
              {
                id: randomId(),
                personId: guardianId,
                name: '',
                isHeard: false,
                details: ''
              }
            ]
          }
        })
      },
      withForm(form: Partial<AssistanceNeedPreschoolDecisionForm>) {
        return Fixture.assistanceNeedPreschoolDecision({
          ...value,
          form: {
            ...value.form,
            ...form
          }
        })
      },
      withRequiredFieldsFilled(
        unitId: DaycareId,
        preparerId: EmployeeId,
        decisionMakerId: EmployeeId
      ) {
        return Fixture.assistanceNeedPreschoolDecision({
          ...value,
          form: {
            ...value.form,
            type: 'NEW',
            validFrom: LocalDate.of(2022, 8, 1),
            selectedUnit: unitId,
            primaryGroup: 'Perhoset',
            decisionBasis: 'Perustelu',
            basisDocumentPedagogicalReport: true,
            guardiansHeardOn: LocalDate.of(2022, 8, 1),
            guardianInfo: value.form.guardianInfo.map((g) => ({
              ...g,
              isHeard: true,
              details: 'kasvotusten'
            })),
            viewOfGuardians: 'ok',
            preparer1EmployeeId: preparerId,
            preparer1Title: 'Käsittelijä',
            decisionMakerEmployeeId: decisionMakerId,
            decisionMakerTitle: 'Päättäjä'
          }
        })
      }
    }
  }

  static daycareCaretakers(initial: SemiPartial<Caretaker, 'groupId'>) {
    const value: Caretaker = {
      amount: 1,
      startDate: LocalDate.todayInSystemTz(),
      endDate: null,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createDaycareCaretakers({ body: [value] })
        return value
      }
    }
  }

  static childAttendance(
    initial: SemiPartial<DevChildAttendance, 'childId' | 'unitId'>
  ) {
    const value: DevChildAttendance = {
      date: LocalDate.todayInHelsinkiTz(),
      arrived: LocalTime.nowInHelsinkiTz(),
      departed: LocalTime.nowInHelsinkiTz(),
      modifiedAt: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser.id,
      ...initial
    }
    return {
      ...value,
      async save() {
        await postAttendances({ body: [value] })
        return value
      }
    }
  }

  static feeThresholds(initial?: Partial<FeeThresholds>) {
    const value: FeeThresholds = {
      validDuring: new DateRange(LocalDate.of(2020, 1, 1), null),
      maxFee: 10000,
      minFee: 1000,
      minIncomeThreshold2: 100000,
      incomeMultiplier2: 0.1,
      maxIncomeThreshold2: 200000,
      minIncomeThreshold3: 200000,
      incomeMultiplier3: 0.1,
      maxIncomeThreshold3: 300000,
      minIncomeThreshold4: 300000,
      incomeMultiplier4: 0.1,
      maxIncomeThreshold4: 400000,
      minIncomeThreshold5: 400000,
      incomeMultiplier5: 0.1,
      maxIncomeThreshold5: 500000,
      minIncomeThreshold6: 500000,
      incomeMultiplier6: 0.1,
      maxIncomeThreshold6: 600000,
      incomeThresholdIncrease6Plus: 100000,
      siblingDiscount2: 0.5,
      siblingDiscount2Plus: 0.8,
      temporaryFee: 2800,
      temporaryFeePartDay: 1500,
      temporaryFeeSibling: 1500,
      temporaryFeeSiblingPartDay: 800,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createFeeThresholds({ body: value })
        return value
      }
    }
  }

  static income(initial: SemiPartial<DevIncome, 'personId' | 'modifiedBy'>) {
    const value: DevIncome = {
      id: randomId(),
      validFrom: LocalDate.todayInSystemTz(),
      validTo: LocalDate.todayInSystemTz().addYears(1),
      data: {
        MAIN_INCOME: {
          multiplier: 1,
          amount: 100000,
          monthlyAmount: 100000,
          coefficient: 'MONTHLY_NO_HOLIDAY_BONUS'
        }
      },
      effect: 'INCOME',
      modifiedAt: HelsinkiDateTime.now(),
      isEntrepreneur: false,
      worksAtEcha: false,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createIncome({ body: value })
        return value
      }
    }
  }

  static incomeStatement(initial: SemiPartial<DevIncomeStatement, 'personId'>) {
    const value: DevIncomeStatement = {
      id: randomId(),
      createdAt: HelsinkiDateTime.now(),
      modifiedAt: HelsinkiDateTime.now(),
      createdBy: systemInternalUser.id,
      modifiedBy: systemInternalUser.id,
      data: {
        type: 'HIGHEST_FEE',
        startDate: LocalDate.todayInSystemTz(),
        endDate: null
      },
      status: 'SENT',
      sentAt: HelsinkiDateTime.now(),
      handledAt: null,
      handlerId: null,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createIncomeStatement({ body: value })
        return value
      }
    }
  }

  static incomeNotification(
    initial: SemiPartial<IncomeNotification, 'receiverId'>
  ) {
    const value: IncomeNotification = {
      notificationType: 'INITIAL_EMAIL',
      created: HelsinkiDateTime.now(),
      ...initial
    }
    return {
      ...value,
      async save() {
        await createIncomeNotification({ body: value })
        return value
      }
    }
  }

  static placementPlan(
    initial: SemiPartial<PlacementPlan, 'unitId'> & {
      applicationId: ApplicationId
    }
  ) {
    const value: PlacementPlan & { applicationId: ApplicationId } = {
      periodStart: LocalDate.todayInSystemTz(),
      periodEnd: LocalDate.todayInSystemTz(),
      preschoolDaycarePeriodStart: null,
      preschoolDaycarePeriodEnd: null,
      ...initial
    }
    return {
      ...value,
      async save() {
        const { applicationId, ...body } = value
        await createPlacementPlan({ applicationId, body })
        return value
      }
    }
  }

  static holidayPeriod(initial?: Partial<HolidayPeriod>) {
    const value: HolidayPeriod = {
      id: randomId(),
      period: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      reservationsOpenOn: LocalDate.todayInSystemTz(),
      reservationDeadline: LocalDate.todayInSystemTz(),
      ...initial
    }
    return {
      ...value,
      async save() {
        const { id, ...body } = value
        await createHolidayPeriod({ id, body })
        return value
      }
    }
  }

  static holidayQuestionnaire(initial?: Partial<FixedPeriodQuestionnaire>) {
    const value: FixedPeriodQuestionnaire = {
      id: randomId(),
      type: 'FIXED_PERIOD',
      absenceType: 'OTHER_ABSENCE',
      title: { fi: '', sv: '', en: '' },
      description: { fi: '', sv: '', en: '' },
      descriptionLink: { fi: '', sv: '', en: '' },
      active: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      conditions: {
        continuousPlacement: null
      },
      periodOptions: [],
      periodOptionLabel: { fi: '', sv: '', en: '' },
      requiresStrongAuth: false,
      ...initial
    }
    return {
      ...value,
      async save() {
        const { id, ...body } = value
        await createHolidayQuestionnaire({ id, body })
        return value
      }
    }
  }

  static guardian(child: DevPerson, guardian: DevPerson) {
    const value = {
      childId: child.id,
      guardianId: guardian.id
    }
    return {
      ...value,
      async save() {
        await insertGuardians({ body: [value] })
        return value
      }
    }
  }

  static fridgeChild(
    initial: SemiPartial<DevFridgeChild, 'headOfChild' | 'childId'>
  ) {
    const value: DevFridgeChild = {
      id: randomId(),
      startDate: LocalDate.of(2020, 1, 1),
      endDate: LocalDate.of(2020, 12, 31),
      conflict: false,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createFridgeChild({ body: [value] })
        return value
      }
    }
  }

  static staffOccupancyCoefficient(unitId: DaycareId, employeeId: EmployeeId) {
    const value: DevUpsertStaffOccupancyCoefficient = {
      coefficient: 7,
      employeeId,
      unitId
    }
    return {
      ...value,
      async save() {
        await upsertStaffOccupancyCoefficient({ body: value })
        return value
      }
    }
  }

  static dailyServiceTime(
    initial: SemiPartial<DevDailyServiceTimes, 'childId'>
  ) {
    const value: DevDailyServiceTimes = {
      id: randomId(),
      validityPeriod: new DateRange(LocalDate.of(2020, 1, 1), null),
      type: 'REGULAR',
      regularTimes: new TimeRange(LocalTime.of(1, 0), LocalTime.of(15, 0)),
      mondayTimes: null,
      tuesdayTimes: null,
      wednesdayTimes: null,
      thursdayTimes: null,
      fridayTimes: null,
      saturdayTimes: null,
      sundayTimes: null,
      ...initial
    }
    return {
      ...value,
      async save() {
        await addDailyServiceTime({ body: value })
        return value
      }
    }
  }

  static dailyServiceTimeNotification(
    initial: SemiPartial<DevDailyServiceTimeNotification, 'guardianId'>
  ) {
    const value: DevDailyServiceTimeNotification = {
      id: randomId(),
      ...initial
    }
    return {
      ...value,
      async save() {
        await addDailyServiceTimeNotification({ body: value })
        return value
      }
    }
  }

  static payment(initial: SemiPartial<DevPayment, 'unitId' | 'unitName'>) {
    const value: DevPayment = {
      id: randomId(),
      period: new FiniteDateRange(
        LocalDate.todayInHelsinkiTz(),
        LocalDate.todayInHelsinkiTz()
      ),
      status: 'DRAFT',
      number: 1,
      amount: 0,
      unitBusinessId: null,
      unitIban: null,
      unitProviderId: null,
      unitPartnerCode: null,
      paymentDate: null,
      dueDate: null,
      sentAt: null,
      sentBy: null,
      ...initial
    }

    return {
      ...value,
      async save() {
        await addPayment({ body: value })
        return value
      }
    }
  }

  static realtimeStaffAttendance(
    initial: SemiPartial<DevStaffAttendance, 'employeeId' | 'groupId'>
  ) {
    const value: DevStaffAttendance = {
      id: randomId(),
      arrived: HelsinkiDateTime.now(),
      departed: null,
      occupancyCoefficient: 7,
      type: 'PRESENT',
      departedAutomatically: false,
      modifiedAt: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser.id,
      ...initial
    }
    return {
      ...value,
      async save() {
        await addStaffAttendance({ body: value })
        return value
      }
    }
  }

  static staffAttendancePlan(
    initial: SemiPartial<DevStaffAttendancePlan, 'employeeId'>
  ) {
    const value: DevStaffAttendancePlan = {
      id: randomId(),
      type: 'PRESENT',
      startTime: HelsinkiDateTime.now(),
      endTime: HelsinkiDateTime.now(),
      description: null,
      ...initial
    }
    return {
      ...value,
      async save() {
        await addStaffAttendancePlan({ body: value })
        return value
      }
    }
  }

  static calendarEvent(initial?: Partial<DevCalendarEvent>) {
    const value: DevCalendarEvent = {
      id: randomId(),
      title: '',
      description: '',
      period: new FiniteDateRange(
        LocalDate.of(2020, 1, 1),
        LocalDate.of(2020, 1, 1)
      ),
      modifiedAt: LocalDate.of(2020, 1, 1).toHelsinkiDateTime(LocalTime.MIN),
      modifiedBy: systemInternalUser.id,
      eventType: 'DAYCARE_EVENT',
      ...initial
    }
    return {
      ...value,
      async save() {
        await addCalendarEvent({ body: value })
        return value
      }
    }
  }

  static calendarEventAttendee(
    initial: SemiPartial<DevCalendarEventAttendee, 'calendarEventId' | 'unitId'>
  ) {
    const value: DevCalendarEventAttendee = {
      id: randomId(),
      groupId: null,
      childId: null,
      ...initial
    }
    return {
      ...value,
      async save() {
        await addCalendarEventAttendee({ body: value })
        return value
      }
    }
  }

  static calendarEventTime(
    initial: SemiPartial<DevCalendarEventTime, 'calendarEventId'>
  ) {
    const value: DevCalendarEventTime = {
      id: randomId(),
      date: LocalDate.of(2020, 1, 1),
      childId: null,
      modifiedBy: systemInternalUser.id,
      modifiedAt: LocalDate.of(2020, 1, 1).toHelsinkiDateTime(LocalTime.MIN),
      start: LocalTime.of(8, 0),
      end: LocalTime.of(8, 30),
      ...initial
    }
    return {
      ...value,
      async save() {
        await addCalendarEventTime({ body: value })
        return value
      }
    }
  }

  static attendanceReservation(data: DailyReservationRequest) {
    return {
      ...data,
      async save() {
        await postReservations({ body: [data] })
        return data
      }
    }
  }

  static attendanceReservationRaw(data: ReservationInsert) {
    return {
      ...data,
      async save() {
        await postReservationsRaw({ body: [data] })
        return data
      }
    }
  }

  static absence(initial: SemiPartial<DevAbsence, 'childId'>) {
    const value: DevAbsence = {
      id: randomId(),
      date: LocalDate.todayInHelsinkiTz(),
      absenceType: 'OTHER_ABSENCE',
      modifiedAt: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser.id,
      absenceCategory: 'BILLABLE',
      questionnaireId: null,
      ...initial
    }
    return {
      ...value,
      async save() {
        await addAbsence({ body: value })
        return value
      }
    }
  }

  static documentTemplate(initial?: Partial<DevDocumentTemplate>) {
    const value: DevDocumentTemplate = {
      id: randomId(),
      type: 'PEDAGOGICAL_REPORT',
      placementTypes: [
        'DAYCARE',
        'PRESCHOOL',
        'PRESCHOOL_DAYCARE',
        'PRESCHOOL_DAYCARE_ONLY',
        'PRESCHOOL_CLUB',
        'DAYCARE_PART_TIME',
        'PREPARATORY',
        'PREPARATORY_DAYCARE',
        'PREPARATORY_DAYCARE_ONLY',
        'CLUB',
        'TEMPORARY_DAYCARE',
        'TEMPORARY_DAYCARE_PART_DAY',
        'DAYCARE_FIVE_YEAR_OLDS',
        'DAYCARE_PART_TIME_FIVE_YEAR_OLDS',
        'SCHOOL_SHIFT_CARE'
      ],
      language: 'FI',
      name: 'Pedagoginen selvitys',
      confidentiality: null,
      legalBasis: '§1',
      published: false,
      validity: new DateRange(LocalDate.of(2000, 1, 1), null),
      processDefinitionNumber: null,
      archiveDurationMonths: null,
      archiveExternally: false,
      endDecisionWhenUnitChanges: null,
      content: {
        sections: [
          {
            id: 's1',
            label: 'osio 1',
            infoText: '',
            questions: [
              {
                id: 'q1',
                type: 'TEXT',
                label: 'kysymys 1',
                infoText: '',
                multiline: false
              }
            ]
          }
        ]
      },
      ...initial
    }
    return {
      ...value,
      async save() {
        await createDocumentTemplate({ body: value })
        return value
      },
      withPublished(published: boolean) {
        value.published = published
        return this
      }
    }
  }

  static assistanceAction(
    initial: SemiPartial<DevAssistanceAction, 'childId' | 'modifiedBy'>
  ) {
    const value: DevAssistanceAction = {
      id: randomId(),
      actions: ['ASSISTANCE_SERVICE_CHILD'],
      endDate: LocalDate.todayInSystemTz(),
      startDate: LocalDate.todayInSystemTz(),
      createdAt: HelsinkiDateTime.now(),
      modifiedAt: HelsinkiDateTime.now(),
      otherAction: '',
      ...initial
    }
    return {
      ...value,
      async save() {
        await createAssistanceAction({ body: [value] })
        return value
      }
    }
  }

  static assistanceActionOption(initial?: Partial<DevAssistanceActionOption>) {
    const value: DevAssistanceActionOption = {
      id: randomId(),
      descriptionFi: 'a description',
      nameFi: 'a test assistance action option',
      value: 'TEST_ASSISTANCE_ACTION_OPTION',
      category: 'DAYCARE',
      displayOrder: null,
      validFrom: null,
      validTo: null,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createAssistanceActionOption({ body: [value] })
        return value
      }
    }
  }

  static childDocument(
    initial: SemiPartial<DevChildDocument, 'childId' | 'templateId'>
  ) {
    const value: DevChildDocument = {
      id: randomId(),
      created: null,
      createdBy: systemInternalUser.id,
      status: 'DRAFT',
      modifiedAt: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser.id,
      contentLockedAt: HelsinkiDateTime.now(),
      contentLockedBy: null,
      documentKey: null,
      publishedAt: null,
      publishedBy: null,
      content: {
        answers: [
          {
            questionId: 'q1',
            type: 'TEXT',
            answer: 'test'
          }
        ]
      },
      publishedContent: null,
      answeredAt: null,
      answeredBy: null,
      processId: null,
      decisionMaker: null,
      decision: null,
      ...initial
    }

    return {
      ...value,
      async save() {
        await createChildDocument({ body: value })
        return value
      },
      withModifiedAt(modifiedAt: HelsinkiDateTime) {
        return Fixture.childDocument({
          ...value,
          modifiedAt
        })
      },
      withPublishedAt(publishedAt: HelsinkiDateTime | null) {
        return Fixture.childDocument({
          ...value,
          publishedAt
        })
      },
      withPublishedContent(publishedContent: DocumentContent | null) {
        return Fixture.childDocument({
          ...value,
          publishedContent
        })
      },
      withDecision(
        decision: SemiPartial<
          DevChildDocumentDecision,
          'status' | 'validity' | 'createdBy' | 'modifiedBy'
        >
      ) {
        return Fixture.childDocument({
          ...value,
          decision: {
            id: randomId(),
            createdAt: HelsinkiDateTime.now(),
            modifiedAt: HelsinkiDateTime.now(),
            daycareId: null,
            ...decision
          }
        })
      }
    }
  }

  static assistanceNeedVoucherCoefficient(
    initial: SemiPartial<AssistanceNeedVoucherCoefficient, 'childId'>
  ) {
    const value: AssistanceNeedVoucherCoefficient = {
      id: randomId(),
      coefficient: 1.0,
      validityPeriod: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      modifiedAt: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser,
      ...initial
    }
    return {
      ...value,
      async save() {
        await createAssistanceNeedVoucherCoefficients({ body: [value] })
        return value
      },
      with(update: Partial<AssistanceNeedVoucherCoefficient>) {
        return Fixture.assistanceNeedVoucherCoefficient({ ...value, ...update })
      }
    }
  }

  static parentship(
    initial: SemiPartial<DevParentship, 'childId' | 'headOfChildId'>
  ) {
    const value: DevParentship = {
      id: randomId(),
      createdAt: HelsinkiDateTime.now(),
      startDate: LocalDate.todayInSystemTz(),
      endDate: LocalDate.todayInSystemTz(),
      ...initial
    }
    return {
      ...value,
      async save() {
        await createParentships({ body: [value] })
        return value
      }
    }
  }

  static invoice(
    initial: SemiPartial<DevInvoice, 'headOfFamilyId' | 'areaId'>
  ) {
    const value: DevInvoice = {
      id: randomId(),
      status: 'DRAFT',
      number: null,
      invoiceDate: LocalDate.of(2021, 2, 1),
      dueDate: LocalDate.of(2021, 2, 14),
      periodStart: LocalDate.of(2021, 1, 1),
      periodEnd: LocalDate.of(2021, 1, 31),
      createdAt: HelsinkiDateTime.now(),
      sentAt: null,
      sentBy: null,
      codebtor: null,
      revisionNumber: 0,
      replacedInvoiceId: null,
      rows: [],
      ...initial
    }

    return {
      ...value,
      addRow(row: SemiPartial<DevInvoiceRow, 'childId' | 'unitId'>) {
        return Fixture.invoice({
          ...value,
          rows: [
            ...value.rows,
            {
              id: randomId(),
              amount: 1,
              unitPrice: 28900,
              periodStart: value.periodStart,
              periodEnd: value.periodEnd,
              product: 'DAYCARE',
              description: '',
              correctionId: null,
              idx: value.rows.length,
              ...row
            }
          ]
        })
      },
      async save() {
        await createInvoices({ body: [value] })
        return value
      }
    }
  }

  static nekkuSpecialDiets(initial: NekkuSpecialDiet[]) {
    const value: NekkuSpecialDiet[] = initial

    return {
      ...value,
      async save() {
        await createNekkuSpecialDiets({ body: value })
        return value
      }
    }
  }

  static nekkuCustomer(initial: NekkuCustomer) {
    const value: NekkuCustomer = initial

    return {
      ...value,
      async save() {
        await createNekkuCustomer({ body: value })
        return value
      }
    }
  }
}

export interface Family {
  guardian: DevPerson
  otherGuardian?: DevPerson
  children: DevPerson[]
}

export const fullDayTimeRange: TimeRange = new TimeRange(
  LocalTime.MIN,
  LocalTime.parse('23:59')
)

export const nonFullDayTimeRange: TimeRange = new TimeRange(
  LocalTime.of(1, 0),
  LocalTime.of(23, 0)
)

export const preschoolTerm2020 = Fixture.preschoolTerm({
  finnishPreschool: new FiniteDateRange(
    LocalDate.of(2020, 8, 13),
    LocalDate.of(2021, 6, 4)
  ),
  swedishPreschool: new FiniteDateRange(
    LocalDate.of(2020, 8, 18),
    LocalDate.of(2021, 6, 4)
  ),
  extendedTerm: new FiniteDateRange(
    LocalDate.of(2020, 8, 1),
    LocalDate.of(2021, 6, 4)
  ),
  applicationPeriod: new FiniteDateRange(
    LocalDate.of(2020, 1, 8),
    LocalDate.of(2021, 6, 4)
  ),
  termBreaks: []
})

export const preschoolTerm2021 = Fixture.preschoolTerm({
  finnishPreschool: new FiniteDateRange(
    LocalDate.of(2021, 8, 11),
    LocalDate.of(2022, 6, 3)
  ),
  swedishPreschool: new FiniteDateRange(
    LocalDate.of(2021, 8, 11),
    LocalDate.of(2022, 6, 3)
  ),
  extendedTerm: new FiniteDateRange(
    LocalDate.of(2021, 8, 1),
    LocalDate.of(2022, 6, 3)
  ),
  applicationPeriod: new FiniteDateRange(
    LocalDate.of(2021, 1, 8),
    LocalDate.of(2022, 6, 3)
  ),
  termBreaks: [
    new FiniteDateRange(LocalDate.of(2021, 10, 18), LocalDate.of(2021, 10, 24))
  ]
})

export const preschoolTerm2022 = Fixture.preschoolTerm({
  finnishPreschool: new FiniteDateRange(
    LocalDate.of(2022, 8, 11),
    LocalDate.of(2023, 6, 2)
  ),
  swedishPreschool: new FiniteDateRange(
    LocalDate.of(2022, 8, 11),
    LocalDate.of(2023, 6, 2)
  ),
  extendedTerm: new FiniteDateRange(
    LocalDate.of(2022, 8, 1),
    LocalDate.of(2023, 6, 2)
  ),
  applicationPeriod: new FiniteDateRange(
    LocalDate.of(2022, 1, 10),
    LocalDate.of(2023, 6, 2)
  ),
  termBreaks: []
})

export const preschoolTerm2023 = Fixture.preschoolTerm({
  finnishPreschool: new FiniteDateRange(
    LocalDate.of(2023, 8, 11),
    LocalDate.of(2024, 6, 3)
  ),
  swedishPreschool: new FiniteDateRange(
    LocalDate.of(2023, 8, 13),
    LocalDate.of(2024, 6, 6)
  ),
  extendedTerm: new FiniteDateRange(
    LocalDate.of(2023, 8, 1),
    LocalDate.of(2024, 6, 6)
  ),
  applicationPeriod: new FiniteDateRange(
    LocalDate.of(2023, 1, 8),
    LocalDate.of(2024, 6, 6)
  ),
  termBreaks: [
    new FiniteDateRange(LocalDate.of(2023, 10, 16), LocalDate.of(2023, 10, 20)),
    new FiniteDateRange(LocalDate.of(2023, 12, 23), LocalDate.of(2024, 1, 7)),
    new FiniteDateRange(LocalDate.of(2024, 2, 19), LocalDate.of(2024, 2, 23))
  ]
})

export const clubTerm2020 = Fixture.clubTerm({
  term: new FiniteDateRange(
    LocalDate.of(2020, 8, 13),
    LocalDate.of(2021, 6, 4)
  ),
  applicationPeriod: new FiniteDateRange(
    LocalDate.of(2020, 1, 8),
    LocalDate.of(2021, 6, 4)
  ),
  termBreaks: []
})

export const clubTerm2021 = Fixture.clubTerm({
  term: new FiniteDateRange(
    LocalDate.of(2021, 8, 11),
    LocalDate.of(2022, 6, 3)
  ),
  applicationPeriod: new FiniteDateRange(
    LocalDate.of(2021, 1, 8),
    LocalDate.of(2022, 6, 3)
  ),
  termBreaks: [
    new FiniteDateRange(LocalDate.of(2021, 10, 18), LocalDate.of(2021, 10, 24))
  ]
})

export const clubTerm2022 = Fixture.clubTerm({
  term: new FiniteDateRange(
    LocalDate.of(2022, 8, 10),
    LocalDate.of(2023, 6, 3)
  ),
  applicationPeriod: new FiniteDateRange(
    LocalDate.of(2022, 1, 8),
    LocalDate.of(2023, 6, 3)
  ),
  termBreaks: []
})

export const clubTerm2023 = Fixture.clubTerm({
  term: new FiniteDateRange(
    LocalDate.of(2023, 8, 10),
    LocalDate.of(2024, 6, 3)
  ),
  applicationPeriod: new FiniteDateRange(
    LocalDate.of(2023, 1, 8),
    LocalDate.of(2024, 6, 3)
  ),
  termBreaks: [
    new FiniteDateRange(LocalDate.of(2023, 10, 16), LocalDate.of(2023, 10, 20)),
    new FiniteDateRange(LocalDate.of(2023, 12, 23), LocalDate.of(2024, 1, 7)),
    new FiniteDateRange(LocalDate.of(2024, 2, 19), LocalDate.of(2024, 2, 23))
  ]
})

export const clubTerms = [
  clubTerm2020,
  clubTerm2021,
  clubTerm2022,
  clubTerm2023
]

export const testCareArea = Fixture.careArea({
  id: fromUuid<AreaId>('674dfb66-8849-489e-b094-e6a0ebfb3c71'),
  name: 'Superkeskus',
  shortName: 'super-keskus',
  areaCode: 299,
  subCostCenter: '99'
})

export const testCareArea2 = Fixture.careArea({
  id: fromUuid<AreaId>('7a5b42db-451b-4394-b6a6-86993ea0ed45'),
  name: 'Hyperkeskus',
  shortName: 'hyper-keskus',
  areaCode: 298,
  subCostCenter: '98'
})

export const testClub = Fixture.daycare({
  id: fromUuid<DaycareId>('0b5ffd40-2f1a-476a-ad06-2861f433b0d1'),
  areaId: testCareArea.id,
  name: 'Alkuräjähdyksen kerho',
  type: ['CLUB'],
  dailyPreschoolTime: null,
  dailyPreparatoryTime: null,
  openingDate: LocalDate.of(2020, 1, 1),
  costCenter: '31500',
  visitingAddress: {
    streetAddress: 'Kamreerintie 1',
    postalCode: '02210',
    postOffice: 'Espoo'
  },
  decisionCustomization: {
    daycareName: 'Päiväkoti päätöksellä',
    preschoolName: '-',
    handler: 'Käsittelijä',
    handlerAddress: 'Käsittelijän osoite'
  },
  daycareApplyPeriod: null,
  preschoolApplyPeriod: null,
  clubApplyPeriod: new DateRange(LocalDate.of(2020, 3, 1), null),
  providerType: 'MUNICIPAL',
  operationTimes: [
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    null,
    null
  ],
  shiftCareOperationTimes: null,
  shiftCareOpenOnHolidays: false,
  enabledPilotFeatures: ['MESSAGING', 'MOBILE'],
  businessId: '',
  iban: '',
  providerId: '',
  partnerCode: '',
  capacity: 0,
  closingDate: null,
  ghostUnit: false,
  invoicedByMunicipality: true,
  uploadChildrenToVarda: true,
  uploadToVarda: true,
  uploadToKoski: true,
  language: 'fi',
  location: null,
  mailingAddress: {
    poBox: null,
    postOffice: null,
    postalCode: null,
    streetAddress: null
  },
  unitManager: {
    email: '',
    name: 'Unit Manager',
    phone: ''
  },
  financeDecisionHandler: null,
  email: null,
  phone: null,
  url: null,
  ophUnitOid: '1.2.3.4.5',
  ophOrganizerOid: '1.2.3.4.5',
  additionalInfo: null,
  dwCostCenter: 'dw-test',
  mealtimeBreakfast: null,
  mealtimeLunch: null,
  mealtimeSnack: null,
  mealtimeSupper: null,
  mealtimeEveningSnack: null,
  withSchool: false
})

export const testDaycare = Fixture.daycare({
  id: fromUuid<DaycareId>('4f3a32f5-d1bd-4b8b-aa4e-4fd78b18354b'),
  areaId: testCareArea.id,
  name: 'Alkuräjähdyksen päiväkoti',
  type: ['CENTRE', 'PRESCHOOL', 'PREPARATORY_EDUCATION'],
  dailyPreschoolTime: new TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 0)),
  dailyPreparatoryTime: new TimeRange(LocalTime.of(9, 0), LocalTime.of(14, 0)),
  costCenter: '31500',
  visitingAddress: {
    streetAddress: 'Kamreerintie 1',
    postalCode: '02210',
    postOffice: 'Espoo'
  },
  decisionCustomization: {
    daycareName: 'Päiväkoti päätöksellä',
    preschoolName: 'Päiväkoti päätöksellä',
    handler: 'Käsittelijä',
    handlerAddress: 'Käsittelijän osoite'
  },
  providerType: 'MUNICIPAL',
  operationTimes: [
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    nonFullDayTimeRange,
    null,
    null
  ],
  shiftCareOperationTimes: [
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange
  ],
  shiftCareOpenOnHolidays: true,
  location: {
    lat: 60.20377343765089,
    lon: 24.655715743526994
  },
  enabledPilotFeatures: [
    'MESSAGING',
    'MOBILE',
    'RESERVATIONS',
    'VASU_AND_PEDADOC',
    'OTHER_DECISION',
    'CITIZEN_BASIC_DOCUMENT',
    'MOBILE_MESSAGING',
    'PLACEMENT_TERMINATION',
    'SERVICE_APPLICATIONS'
  ],
  businessId: '',
  iban: '',
  providerId: '',
  partnerCode: '',
  capacity: 0,
  openingDate: null,
  closingDate: null,
  ghostUnit: false,
  invoicedByMunicipality: true,
  uploadChildrenToVarda: true,
  uploadToVarda: true,
  uploadToKoski: true,
  language: 'fi',
  mailingAddress: {
    poBox: null,
    postOffice: null,
    postalCode: null,
    streetAddress: null
  },
  unitManager: {
    email: '',
    name: 'Unit Manager',
    phone: ''
  },
  financeDecisionHandler: null,
  clubApplyPeriod: null,
  daycareApplyPeriod: new DateRange(LocalDate.of(2020, 3, 1), null),
  preschoolApplyPeriod: new DateRange(LocalDate.of(2020, 3, 1), null),
  email: null,
  phone: null,
  url: null,
  ophUnitOid: '1.2.3.4.5',
  ophOrganizerOid: '1.2.3.4.5',
  additionalInfo: null,
  dwCostCenter: 'dw-test',
  mealtimeBreakfast: null,
  mealtimeLunch: null,
  mealtimeSnack: null,
  mealtimeSupper: null,
  mealtimeEveningSnack: null,
  withSchool: false
})

export const testDaycare2 = Fixture.daycare({
  id: fromUuid<DaycareId>('6f540c39-e7f6-4222-a004-c527403378ec'),
  areaId: testCareArea2.id,
  name: 'Mustan aukon päiväkoti',
  type: ['CENTRE'],
  dailyPreschoolTime: null,
  dailyPreparatoryTime: null,
  costCenter: '31501',
  visitingAddress: {
    streetAddress: 'Kamreerintie 2',
    postalCode: '02210',
    postOffice: 'Espoo'
  },
  decisionCustomization: {
    daycareName: 'Päiväkoti 2 päätöksellä',
    preschoolName: 'Päiväkoti 2 päätöksellä',
    handler: 'Käsittelijä 2',
    handlerAddress: 'Käsittelijän 2 osoite'
  },
  providerType: 'MUNICIPAL',
  operationTimes: [
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    null,
    null
  ],
  shiftCareOperationTimes: [
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange
  ],
  shiftCareOpenOnHolidays: true,
  location: {
    lat: 60.20350901607783,
    lon: 24.669
  },
  enabledPilotFeatures: ['MESSAGING', 'MOBILE', 'RESERVATIONS'],
  businessId: '',
  iban: '',
  providerId: '',
  partnerCode: '',
  capacity: 0,
  openingDate: null,
  closingDate: null,
  ghostUnit: false,
  invoicedByMunicipality: true,
  uploadChildrenToVarda: true,
  uploadToVarda: true,
  uploadToKoski: true,
  language: 'fi',
  mailingAddress: {
    poBox: null,
    postOffice: null,
    postalCode: null,
    streetAddress: null
  },
  unitManager: {
    email: '',
    name: 'Unit Manager',
    phone: ''
  },
  financeDecisionHandler: null,
  clubApplyPeriod: null,
  daycareApplyPeriod: new DateRange(LocalDate.of(2020, 3, 1), null),
  preschoolApplyPeriod: new DateRange(LocalDate.of(2020, 3, 1), null),
  email: null,
  phone: null,
  url: null,
  ophUnitOid: '1.2.3.4.5',
  ophOrganizerOid: '1.2.3.4.5',
  additionalInfo: null,
  dwCostCenter: 'dw-test',
  mealtimeBreakfast: null,
  mealtimeLunch: null,
  mealtimeSnack: null,
  mealtimeSupper: null,
  mealtimeEveningSnack: null,
  withSchool: false
})

export const testDaycarePrivateVoucher = Fixture.daycare({
  id: fromUuid<DaycareId>('572adb7e-9b3d-11ea-bb37-0242ac130002'),
  areaId: testCareArea.id,
  name: 'PS-yksikkö',
  type: ['CENTRE'],
  dailyPreschoolTime: null,
  dailyPreparatoryTime: null,
  costCenter: '31500',
  visitingAddress: {
    streetAddress: 'Kamreerintie 1',
    postalCode: '02210',
    postOffice: 'Espoo'
  },
  decisionCustomization: {
    daycareName: 'Päiväkoti päätöksellä',
    preschoolName: 'Päiväkoti päätöksellä',
    handler: 'Käsittelijä',
    handlerAddress: 'Käsittelijän osoite'
  },
  providerType: 'PRIVATE_SERVICE_VOUCHER',
  operationTimes: [
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    null,
    null
  ],
  shiftCareOperationTimes: null,
  shiftCareOpenOnHolidays: false,
  location: {
    lat: 60.20377343765089,
    lon: 24.655715743526994
  },
  enabledPilotFeatures: [
    'MESSAGING',
    'MOBILE',
    'RESERVATIONS',
    'VASU_AND_PEDADOC',
    'OTHER_DECISION',
    'MOBILE_MESSAGING',
    'PLACEMENT_TERMINATION'
  ],
  invoicedByMunicipality: false,
  businessId: '',
  iban: '',
  providerId: '',
  partnerCode: '',
  capacity: 0,
  openingDate: null,
  closingDate: null,
  ghostUnit: false,
  uploadChildrenToVarda: true,
  uploadToVarda: true,
  uploadToKoski: true,
  language: 'fi',
  mailingAddress: {
    poBox: null,
    postOffice: null,
    postalCode: null,
    streetAddress: null
  },
  unitManager: {
    email: '',
    name: 'Unit Manager',
    phone: ''
  },
  financeDecisionHandler: null,
  clubApplyPeriod: null,
  daycareApplyPeriod: new DateRange(LocalDate.of(2020, 3, 1), null),
  preschoolApplyPeriod: new DateRange(LocalDate.of(2020, 3, 1), null),
  email: null,
  phone: null,
  url: null,
  ophUnitOid: '1.2.3.4.5',
  ophOrganizerOid: '1.2.3.4.5',
  additionalInfo: null,
  dwCostCenter: 'dw-test',
  mealtimeBreakfast: null,
  mealtimeLunch: null,
  mealtimeSnack: null,
  mealtimeSupper: null,
  mealtimeEveningSnack: null,
  withSchool: false
})

export const testPreschool = Fixture.daycare({
  id: fromUuid<DaycareId>('b53d80e0-319b-4d2b-950c-f5c3c9f834bc'),
  areaId: testCareArea.id,
  name: 'Alkuräjähdyksen eskari',
  type: ['CENTRE', 'PRESCHOOL', 'PREPARATORY_EDUCATION'],
  dailyPreschoolTime: new TimeRange(LocalTime.of(9, 0), LocalTime.of(13, 0)),
  dailyPreparatoryTime: new TimeRange(LocalTime.of(9, 0), LocalTime.of(14, 0)),
  costCenter: '31501',
  visitingAddress: {
    streetAddress: 'Kamreerintie 1',
    postalCode: '02210',
    postOffice: 'Espoo'
  },
  decisionCustomization: {
    daycareName: 'Eskari päätöksellä',
    preschoolName: 'Eskari päätöksellä',
    handler: 'Käsittelijä',
    handlerAddress: 'Käsittelijän osoite'
  },
  providerType: 'MUNICIPAL',
  operationTimes: [
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    fullDayTimeRange,
    null,
    null
  ],
  shiftCareOperationTimes: null,
  shiftCareOpenOnHolidays: false,
  location: {
    lat: 60.2040261560435,
    lon: 24.65517745652623
  },
  enabledPilotFeatures: [
    'MESSAGING',
    'MOBILE',
    'VASU_AND_PEDADOC',
    'OTHER_DECISION',
    'PLACEMENT_TERMINATION'
  ],
  businessId: '',
  iban: '',
  providerId: '',
  partnerCode: '',
  capacity: 0,
  openingDate: null,
  closingDate: null,
  ghostUnit: false,
  invoicedByMunicipality: true,
  uploadChildrenToVarda: true,
  uploadToVarda: true,
  uploadToKoski: true,
  language: 'fi',
  mailingAddress: {
    poBox: null,
    postOffice: null,
    postalCode: null,
    streetAddress: null
  },
  unitManager: {
    email: '',
    name: 'Unit Manager',
    phone: ''
  },
  financeDecisionHandler: null,
  clubApplyPeriod: null,
  daycareApplyPeriod: new DateRange(LocalDate.of(2020, 3, 1), null),
  preschoolApplyPeriod: new DateRange(LocalDate.of(2020, 3, 1), null),
  email: null,
  phone: null,
  url: null,
  ophUnitOid: '1.2.3.4.5',
  ophOrganizerOid: '1.2.3.4.5',
  additionalInfo: null,
  dwCostCenter: 'dw-test',
  mealtimeBreakfast: null,
  mealtimeLunch: null,
  mealtimeSnack: null,
  mealtimeSupper: null,
  mealtimeEveningSnack: null,
  withSchool: false
})

export const testAdult = Fixture.person({
  id: fromUuid<PersonId>('87a5c962-9b3d-11ea-bb37-0242ac130002'),
  ssn: '070644-937X',
  firstName: 'Johannes Olavi Antero Tapio',
  lastName: 'Karhula',
  email: 'johannes.karhula@evaka.test',
  phone: '123456789',
  language: 'fi',
  dateOfBirth: LocalDate.of(1944, 7, 7),
  streetAddress: 'Kamreerintie 1',
  postalCode: '00340',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
})

export const testChild = Fixture.person({
  id: fromUuid<PersonId>('572adb7e-9b3d-11ea-bb37-0242ac130002'),
  ssn: '070714A9126',
  firstName: 'Jari-Petteri Mukkelis-Makkelis Vetelä-Viljami Eelis-Juhani',
  lastName: 'Karhula',
  preferredName: 'Jari',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: LocalDate.of(2014, 7, 7),
  streetAddress: testAdult.streetAddress,
  postalCode: testAdult.postalCode,
  postOffice: testAdult.postOffice,
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
})

export const testChild2 = Fixture.person({
  id: fromUuid<PersonId>('5a4f3ccc-5270-4d28-bd93-d355182b6768'),
  ssn: '160616A978U',
  firstName: 'Kaarina Veera Nelli',
  lastName: 'Karhula',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: LocalDate.of(2016, 6, 6),
  streetAddress: testAdult.streetAddress,
  postalCode: testAdult.postalCode,
  postOffice: testAdult.postOffice,
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
})

export const testChildRestricted = Fixture.person({
  id: fromUuid<PersonId>('28e189d7-abbe-4be9-9074-6e4c881f18de'),
  ssn: '160620A999J',
  firstName: 'Porri Hatter',
  lastName: 'Karhula',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: LocalDate.of(2014, 7, 7),
  streetAddress: '',
  postalCode: '',
  postOffice: '',
  nationalities: ['FI'],
  restrictedDetailsEnabled: true,
  restrictedDetailsEndDate: null
})

export const testAdult2 = Fixture.person({
  id: fromUuid<PersonId>('fb915d31-738f-453f-a2ca-2e7f61db641d'),
  ssn: '311299-999E',
  firstName: 'Ville',
  lastName: 'Vilkas',
  email: '',
  phone: '555-2580',
  language: 'fi',
  dateOfBirth: LocalDate.of(1999, 2, 1),
  streetAddress: 'Toistie 33',
  postalCode: '02230',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
})

export const testChildDeceased = Fixture.person({
  id: fromUuid<PersonId>('b8711722-0c1b-4044-a794-5b308207d78b'),
  ssn: '150515-999T',
  firstName: 'Unelma',
  lastName: 'Aapinen',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: LocalDate.of(2015, 5, 15),
  dateOfDeath: LocalDate.of(2020, 6, 1),
  streetAddress: 'Aapiskatu 1',
  postalCode: '00340',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
})

export const testChildNoSsn = Fixture.person({
  id: fromUuid<PersonId>('a5e87ec8-6221-46f8-8b2b-9ab124d51c22'),
  firstName: 'Heluna',
  lastName: 'Hetuton',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: LocalDate.of(2018, 8, 15),
  ssn: null,
  streetAddress: 'Suosiellä 1',
  postalCode: '00340',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
})

const twoGuardiansGuardian1 = Fixture.person({
  id: fromUuid<PersonId>('9d6289ba-9ffd-11ea-bb37-0242ac130002'),
  ssn: '220281-9456',
  firstName: 'Mikael Ilmari Juhani Johannes',
  lastName: 'Högfors',
  email: 'mikael.hogfors@evaka.test',
  phone: '123456789',
  language: 'fi',
  dateOfBirth: LocalDate.of(1981, 2, 22),
  streetAddress: 'Kamreerintie 4',
  postalCode: '02100',
  postOffice: 'Espoo',
  residenceCode: 'twoGuardiansSameAddressResidenceCode',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
})
const twoGuardiansGuardian2 = Fixture.person({
  id: fromUuid<PersonId>('d1c30734-c02f-4546-8123-856f8101565e'),
  ssn: '170590-9540',
  firstName: 'Kaarina Marjatta Anna Liisa',
  lastName: 'Högfors',
  email: 'kaarina.hogfors@evaka.test',
  phone: '123456789',
  language: 'fi',
  dateOfBirth: LocalDate.of(1990, 5, 17),
  streetAddress: 'Kamreerintie 4',
  postalCode: '02100',
  postOffice: 'Espoo',
  residenceCode: 'twoGuardiansSameAddressResidenceCode',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
})
const twoGuardiansChildren = [
  Fixture.person({
    id: fromUuid<PersonId>('6ec99620-9ffd-11ea-bb37-0242ac130002'),
    ssn: '071013A960W',
    firstName: 'Antero Onni Leevi Aatu',
    lastName: 'Högfors',
    email: '',
    phone: '',
    language: 'fi',
    dateOfBirth: LocalDate.of(2013, 10, 7),
    streetAddress: 'Kamreerintie 4',
    postalCode: '02100',
    postOffice: 'Espoo',
    nationalities: ['FI'],
    restrictedDetailsEnabled: false,
    restrictedDetailsEndDate: null
  })
]
export const familyWithTwoGuardians = Fixture.family({
  guardian: twoGuardiansGuardian1,
  otherGuardian: twoGuardiansGuardian2,
  children: twoGuardiansChildren
})

const separatedGuardiansGuardian1 = Fixture.person({
  id: fromUuid<PersonId>('1c1b2946-fdf3-4e02-a3e4-2c2a797bafc3'),
  firstName: 'John',
  lastName: 'Doe',
  dateOfBirth: LocalDate.of(1980, 1, 1),
  ssn: '010180-1232',
  streetAddress: 'Kamreerintie 2',
  postalCode: '02770',
  postOffice: 'Espoo'
})
const separatedGuardiansGuardian2 = Fixture.person({
  id: fromUuid<PersonId>('56064714-649f-457e-893a-44832936166c'),
  firstName: 'Joan',
  lastName: 'Doe',
  dateOfBirth: LocalDate.of(1979, 2, 1),
  ssn: '010279-123L',
  streetAddress: 'Testikatu 1',
  postalCode: '02770',
  postOffice: 'Espoo'
})
const separatedGuardiansChildren = [
  Fixture.person({
    id: fromUuid<PersonId>('5474ee62-16cf-4cfe-a297-40559e165a32'),
    firstName: 'Ricky',
    lastName: 'Doe',
    dateOfBirth: LocalDate.of(2017, 6, 1),
    ssn: '010617A123U',
    streetAddress: 'Kamreerintie 2',
    postalCode: '02770',
    postOffice: 'Espoo'
  })
]
export const familyWithSeparatedGuardians = Fixture.family({
  guardian: separatedGuardiansGuardian1,
  otherGuardian: separatedGuardiansGuardian2,
  children: separatedGuardiansChildren
})

const restrictedDetailsGuardian = Fixture.person({
  id: fromUuid<PersonId>('7699f488-3fdc-11eb-b378-0242ac130002'),
  ssn: '080884-999H',
  firstName: 'Kaj Erik',
  lastName: 'Pelimerkki',
  email: 'kaj@example.com',
  phone: '123456789',
  language: 'fi',
  dateOfBirth: LocalDate.of(1984, 8, 8),
  streetAddress: '',
  postalCode: '',
  postOffice: '',
  nationalities: ['FI'],
  restrictedDetailsEnabled: true,
  restrictedDetailsEndDate: null
})

const guardian2WithNoRestrictions = Fixture.person({
  id: fromUuid<PersonId>('1fd05a42-3fdd-11eb-b378-0242ac130002'),
  ssn: '130486-9980',
  firstName: 'Helga Helen',
  lastName: 'Lehtokurppa',
  email: 'helga@example.com',
  phone: '123456789',
  language: 'fi',
  dateOfBirth: LocalDate.of(1986, 4, 13),
  streetAddress: 'Westendinkatu 3',
  postalCode: '02100',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
})

const restrictedDetailsGuardiansChildren = [
  Fixture.person({
    id: fromUuid<PersonId>('82a2586e-3fdd-11eb-b378-0242ac130002'),
    firstName: 'Vadelma',
    lastName: 'Pelimerkki',
    dateOfBirth: LocalDate.of(2017, 5, 15),
    ssn: '150517A9989',
    streetAddress: 'Kamreerintie 4',
    postalCode: '02100',
    postOffice: 'Espoo'
  })
]

export const familyWithRestrictedDetailsGuardian = Fixture.family({
  guardian: restrictedDetailsGuardian,
  otherGuardian: guardian2WithNoRestrictions,
  children: restrictedDetailsGuardiansChildren
})

const deadGuardian = Fixture.person({
  id: fromUuid<PersonId>('faacfd43-878f-4a70-9e74-2051a18480e6'),
  ssn: '080581-999A',
  firstName: 'Kuisma',
  lastName: 'Kuollut',
  email: 'kuisma@example.com',
  phone: '123456789',
  language: 'fi',
  dateOfBirth: LocalDate.of(1981, 5, 8),
  dateOfDeath: LocalDate.of(2021, 5, 1),
  streetAddress: 'Kamreerintie 4',
  postalCode: '02100',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
})

const deadGuardianChild = Fixture.person({
  id: fromUuid<PersonId>('1ad3469b-593d-45e4-a68b-a09f759bd029'),
  firstName: 'Kuopus',
  lastName: 'Kuollut',
  dateOfBirth: LocalDate.of(2019, 9, 9),
  ssn: '090917A998M',
  streetAddress: 'Kamreerintie 4',
  postalCode: '02100',
  postOffice: 'Espoo'
})

export const familyWithDeadGuardian = Fixture.family({
  guardian: deadGuardian,
  children: [deadGuardianChild]
})

export const testChildZeroYearOld = Fixture.person({
  id: fromUuid<PersonId>('0909e93d-3aa8-44f8-ac30-ecd77339d849'),
  ssn: null,
  firstName: 'Vasta Syntynyt',
  lastName: 'Korhonen-Hämäläinen',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: LocalDate.todayInSystemTz(), // Always a zero-year-old
  streetAddress: 'Kamreerintie 2',
  postalCode: '00370',
  postOffice: 'Espoo',
  nationalities: ['FI'],
  restrictedDetailsEnabled: false,
  restrictedDetailsEndDate: null
})

export const testAdultRestricted = Fixture.person({
  id: fromUuid<PersonId>('92d707e9-6cbc-487b-8bde-0097d90044cd'),
  ssn: '031083-910S',
  firstName: 'Seija Anna Kaarina',
  lastName: 'Sotka',
  email: '',
  phone: '',
  language: 'fi',
  dateOfBirth: LocalDate.of(1983, 10, 3),
  streetAddress: '',
  postalCode: '',
  postOffice: '',
  nationalities: ['FI'],
  restrictedDetailsEnabled: true,
  restrictedDetailsEndDate: null
})

const applicationForm = (
  type: ApplicationType,
  child: DevPerson,
  guardian: DevPerson,
  guardian2Phone: string,
  guardian2Email: string,
  otherGuardianAgreementStatus: OtherGuardianAgreementStatus | null,
  preferredStartDate: LocalDate,
  preferredUnits: DaycareId[],
  connectedDaycare = false,
  assistanceNeeded = false
): ApplicationForm => {
  const secondGuardian =
    guardian2Phone || guardian2Email || otherGuardianAgreementStatus
      ? {
          phoneNumber: guardian2Phone,
          email: guardian2Email,
          agreementStatus: otherGuardianAgreementStatus
        }
      : null

  const serviceNeed =
    type === 'PRESCHOOL' && !connectedDaycare
      ? null
      : {
          startTime: '08:00',
          endTime: '16:00',
          partTime: false,
          shiftCare: false,
          serviceNeedOption: null
        }

  return {
    child: {
      dateOfBirth: child.dateOfBirth,
      person: {
        ...child,
        socialSecurityNumber: child.ssn ?? null
      },
      address: {
        street: child.streetAddress ?? '',
        postalCode: child.postalCode ?? '',
        postOffice: child.postOffice ?? ''
      },
      futureAddress: null,
      nationality: 'FI',
      language: 'fi',
      allergies: '',
      diet: '',
      assistanceNeeded,
      assistanceDescription: ''
    },
    guardian: {
      person: {
        ...guardian,
        socialSecurityNumber: guardian.ssn ?? null
      },
      phoneNumber: guardian.phone ?? '',
      email: guardian.email ?? '',
      address: {
        street: guardian.streetAddress ?? '',
        postalCode: guardian.postalCode ?? '',
        postOffice: guardian.postOffice ?? ''
      },
      futureAddress: null
    },
    secondGuardian,
    otherPartner: null,
    maxFeeAccepted: false,
    otherChildren: [],
    preferences: {
      preferredStartDate,
      connectedDaycarePreferredStartDate: null,
      preferredUnits: preferredUnits.map((id) => ({ id, name: id })),
      preparatory: false,
      serviceNeed,
      siblingBasis: null,
      urgent: false
    },
    clubDetails: {
      wasOnClubCare: false,
      wasOnDaycare: false
    },
    otherInfo: ''
  }
}

export const applicationFixtureId = fromUuid<ApplicationId>(
  '9dd0e1ba-9b3b-11ea-bb37-0242ac130002'
)
export const applicationFixture = (
  child: DevPerson,
  guardian: DevPerson,
  otherGuardian: DevPerson | undefined = undefined,
  type: 'DAYCARE' | 'PRESCHOOL' | 'CLUB' = 'DAYCARE',
  otherGuardianAgreementStatus: OtherGuardianAgreementStatus | null = null,
  preferredUnits: DaycareId[] = [testDaycare.id],
  connectedDaycare = false,
  status: ApplicationStatus = 'SENT',
  preferredStartDate: LocalDate = LocalDate.of(2021, 8, 16),
  transferApplication = false,
  assistanceNeeded = false,
  checkedByAdmin = false,
  confidential: boolean | null = null
): DevApplicationWithForm => ({
  id: applicationFixtureId,
  type: type,
  childId: child.id,
  guardianId: guardian.id,
  otherGuardians: otherGuardian ? [otherGuardian.id] : [],
  form: applicationForm(
    type,
    child,
    guardian,
    otherGuardian?.phone ?? '',
    otherGuardian?.email ?? '',
    otherGuardianAgreementStatus,
    preferredStartDate,
    preferredUnits,
    connectedDaycare,
    assistanceNeeded
  ),
  checkedByAdmin,
  confidential,
  hideFromGuardian: false,
  origin: 'ELECTRONIC',
  status,
  transferApplication,
  allowOtherGuardianAccess: true,
  createdAt: HelsinkiDateTime.now(),
  createdBy: systemInternalUser.id,
  modifiedAt: HelsinkiDateTime.now(),
  modifiedBy: systemInternalUser.id,
  dueDate: null,
  sentDate: null
})

const feeThresholds = {
  minIncomeThreshold: 210200,
  maxIncomeThreshold: 479900,
  incomeMultiplier: 0.107,
  minFee: 2700,
  maxFee: 28900
}

export const decisionFixture = (
  employeeId: EmployeeId,
  applicationId: ApplicationId,
  startDate: LocalDate,
  endDate: LocalDate
): DecisionRequest => ({
  id: fromUuid('9dd0e1ba-9b3b-11ea-bb37-0242ac130987'),
  employeeId,
  applicationId: applicationId,
  unitId: testDaycare.id,
  type: 'DAYCARE',
  startDate: startDate,
  endDate: endDate,
  status: 'PENDING'
})

export const feeDecisionsFixture = (
  status: FeeDecisionStatus,
  adult: DevPerson,
  child: DevPerson,
  daycareId: DaycareId,
  partner: DevPerson | null,
  validDuring: FiniteDateRange = new FiniteDateRange(
    LocalDate.todayInSystemTz().subYears(1),
    LocalDate.todayInSystemTz().addYears(1)
  ),
  sentAt: HelsinkiDateTime | null = null,
  id = fromUuid<FeeDecisionId>('bcc42d48-765d-4fe1-bc90-7a7b4c8205fe'),
  documentKey: string | null = null,
  decisionNumber: number | null = null
): FeeDecision => ({
  id,
  status,
  decisionType: 'NORMAL',
  validDuring,
  validFrom: validDuring.start,
  validTo: validDuring.end,
  headOfFamilyId: adult.id,
  partnerId: partner?.id ?? null,
  headOfFamilyIncome: null,
  partnerIncome: null,
  familySize: 2,
  feeThresholds: feeThresholds,
  difference: [],
  children: [
    {
      child: {
        id: child.id,
        dateOfBirth: child.dateOfBirth
      },
      placement: {
        unitId: daycareId,
        type: 'DAYCARE'
      },
      serviceNeed: {
        optionId: null,
        feeCoefficient: 1.0,
        contractDaysPerMonth: null,
        descriptionFi: 'palveluntarve',
        descriptionSv: 'vårdbehövet',
        missing: false
      },
      baseFee: 28900,
      fee: 28900,
      siblingDiscount: 0.0,
      feeAlterations: [],
      finalFee: 28900,
      childIncome: null
    }
  ],
  totalFee: 28900,
  sentAt,
  approvedAt: null,
  approvedById: null,
  decisionHandlerId: null,
  decisionNumber,
  documentKey,
  created: HelsinkiDateTime.now()
})

export const voucherValueDecisionsFixture = (
  id: VoucherValueDecisionId,
  adultId: PersonId,
  childId: PersonId,
  daycareId: DaycareId,
  partner: DevPerson | null = null,
  status: 'DRAFT' | 'SENT' = 'DRAFT',
  validFrom = LocalDate.todayInSystemTz().subYears(1),
  validTo = LocalDate.todayInSystemTz().addYears(1),
  sentAt: HelsinkiDateTime | null = null,
  documentKey?: string
): VoucherValueDecision => ({
  id,
  status,
  validFrom,
  validTo,
  headOfFamilyId: adultId,
  partnerId: partner?.id ?? null,
  headOfFamilyIncome: null,
  partnerIncome: null,
  childIncome: null,
  decisionType: 'NORMAL',
  familySize: 2,
  feeThresholds: feeThresholds,
  child: { id: childId, dateOfBirth: LocalDate.of(2017, 6, 30) },
  placement: {
    unitId: daycareId,
    type: 'DAYCARE'
  },
  serviceNeed: {
    feeCoefficient: 1.0,
    voucherValueCoefficient: 1.0,
    feeDescriptionFi: '',
    feeDescriptionSv: '',
    voucherValueDescriptionFi: '',
    voucherValueDescriptionSv: '',
    missing: false
  },
  baseCoPayment: 28900,
  coPayment: 28900,
  siblingDiscount: 0.0,
  feeAlterations: [],
  finalCoPayment: 28900,
  baseValue: 87000,
  assistanceNeedCoefficient: 1.0,
  voucherValue: 87000,
  difference: [],
  sentAt,
  approvedAt: null,
  approvedById: null,
  decisionNumber: null,
  documentKey: documentKey ?? null,
  created: HelsinkiDateTime.now(),
  decisionHandler: null
})

export const testDaycareGroup = Fixture.daycareGroup({
  id: fromUuid<GroupId>('2f998c23-0f90-4afd-829b-d09ecf2f6188'),
  daycareId: testDaycare.id,
  name: 'Kosmiset vakiot',
  startDate: LocalDate.of(2000, 1, 1),
  endDate: null,
  jamixCustomerNumber: null,
  aromiCustomerId: null,
  nekkuCustomerNumber: null
})

/**
 *  @deprecated Use `Fixture.placement()` instead
 **/
export function createDaycarePlacementFixture(
  id: PlacementId,
  childId: PersonId,
  unitId: DaycareId,
  startDate = LocalDate.of(2022, 5, 1),
  endDate = LocalDate.of(2023, 8, 31),
  type: PlacementType = 'DAYCARE',
  placeGuarantee = false,
  createdAt = HelsinkiDateTime.now(),
  createdBy = systemInternalUser.id,
  source: PlacementSource = 'MANUAL',
  modifiedAt: HelsinkiDateTime | null = HelsinkiDateTime.now(),
  modifiedBy: EvakaUserId | null = systemInternalUser.id
): DevPlacement {
  return Fixture.placement({
    id,
    type,
    childId,
    unitId,
    startDate,
    endDate,
    placeGuarantee,
    createdAt,
    createdBy,
    source,
    modifiedAt,
    modifiedBy,
    terminationRequestedDate: null,
    terminatedBy: null
  })
}

export const DecisionIncomeFixture = (total: number): DecisionIncome => ({
  id: null,
  data: { MAIN_INCOME: total },
  effect: 'INCOME',
  total: total,
  totalExpenses: 0,
  totalIncome: total,
  worksAtECHA: false
})

const nullUUID = evakaUserId(fromUuid('00000000-0000-0000-0000-000000000000'))

export const systemInternalUser: EvakaUser = {
  id: nullUUID,
  name: 'eVaka',
  type: 'SYSTEM'
}
export const employeeToEvakaUser = (employee: DevEmployee): EvakaUser => ({
  id: evakaUserId(employee.id),
  name: employee.firstName + ' ' + employee.lastName,
  type: 'EMPLOYEE'
})
