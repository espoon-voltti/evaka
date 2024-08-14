// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { ScopedRole } from 'lib-common/api-types/employee-auth'
import DateRange from 'lib-common/date-range'
import FiniteDateRange from 'lib-common/finite-date-range'
import {
  ApplicationForm,
  ApplicationStatus,
  ApplicationType,
  OtherGuardianAgreementStatus
} from 'lib-common/generated/api-types/application'
import {
  AssistanceFactor,
  DaycareAssistance,
  OtherAssistanceMeasure,
  PreschoolAssistance
} from 'lib-common/generated/api-types/assistance'
import {
  AssistanceNeedPreschoolDecisionForm,
  AssistanceNeedVoucherCoefficient
} from 'lib-common/generated/api-types/assistanceneed'
import { ClubTerm } from 'lib-common/generated/api-types/daycare'
import { DocumentContent } from 'lib-common/generated/api-types/document'
import {
  FixedPeriodQuestionnaire,
  HolidayPeriod
} from 'lib-common/generated/api-types/holidayperiod'
import {
  DecisionIncome,
  FeeDecision,
  FeeDecisionStatus,
  FeeThresholds,
  IncomeNotification,
  Invoice
} from 'lib-common/generated/api-types/invoicing'
import { PlacementType } from 'lib-common/generated/api-types/placement'
import { DailyReservationRequest } from 'lib-common/generated/api-types/reservations'
import { ServiceNeedOption } from 'lib-common/generated/api-types/serviceneed'
import HelsinkiDateTime from 'lib-common/helsinki-date-time'
import LocalDate from 'lib-common/local-date'
import LocalTime from 'lib-common/local-time'
import TimeRange from 'lib-common/time-range'
import { UUID } from 'lib-common/types'

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
  createHoliday,
  createHolidayPeriod,
  createHolidayQuestionnaire,
  createIncome,
  createIncomeNotification,
  createOtherAssistanceMeasures,
  createParentships,
  createPedagogicalDocuments,
  createPerson,
  createPlacementPlan,
  createPreschoolAssistances,
  createPreschoolTerm,
  createServiceNeedOption,
  createServiceNeeds,
  createVasuTemplate,
  insertGuardians,
  postAttendances,
  postReservations,
  postReservationsRaw,
  upsertStaffOccupancyCoefficient,
  upsertVtjDataset
} from '../generated/api-clients'
import {
  Caretaker,
  CreateVasuTemplateBody,
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
  DevHoliday,
  DevIncome,
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
  PlacementPlan,
  ReservationInsert,
  VoucherValueDecision
} from '../generated/api-types'

const uniqueLabel = (l = 7): string =>
  Math.random().toString(36).substring(0, l)

export const uuidv4 = (): string =>
  'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0,
      v = c == 'x' ? r : (r & 0x3) | 0x8
    return v.toString(16)
  })

type SemiPartial<T, K extends keyof T> = Partial<T> & Pick<T, K>

export class Fixture {
  static daycare(initial: SemiPartial<DevDaycare, 'areaId'>): DaycareBuilder {
    const id = uniqueLabel()
    return new DaycareBuilder({
      id: uuidv4(),
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
      ...initial
    })
  }

  static daycareGroup(
    initial: SemiPartial<DevDaycareGroup, 'daycareId'>
  ): DaycareGroupBuilder {
    const id = uniqueLabel()
    return new DaycareGroupBuilder({
      id: uuidv4(),
      name: `daycareGroup_${id}`,
      startDate: LocalDate.of(2020, 1, 1),
      endDate: null,
      jamixCustomerNumber: null,
      ...initial
    })
  }

  static careArea(initial?: Partial<DevCareArea>): CareAreaBuilder {
    const id = uniqueLabel()
    return new CareAreaBuilder({
      id: uuidv4(),
      name: `Care Area ${id}`,
      shortName: `careArea_${id}`,
      areaCode: 2230,
      subCostCenter: `subCostCenter_${id}`,
      ...initial
    })
  }

  static preschoolTerm(
    initial: SemiPartial<
      DevPreschoolTerm,
      | 'applicationPeriod'
      | 'extendedTerm'
      | 'finnishPreschool'
      | 'swedishPreschool'
      | 'termBreaks'
    >
  ): PreschoolTermBuilder {
    return new PreschoolTermBuilder({
      id: uuidv4(),
      ...initial
    })
  }

  static clubTerm(initial: DevClubTerm): ClubTermBuilder {
    return new ClubTermBuilder(initial)
  }

  static person(initial?: Partial<DevPerson>): PersonBuilder {
    const id = uniqueLabel()
    return new PersonBuilder({
      id: uuidv4(),
      dateOfBirth: LocalDate.of(2020, 5, 5),
      dateOfDeath: null,
      ssn: '050520A999M',
      email: `email_${id}@evaka.test`,
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
      restrictedDetailsEnabled: false,
      restrictedDetailsEndDate: null,
      streetAddress: `streetAddress_${id}`,
      duplicateOf: null,
      enabledEmailTypes: null,
      forceManualFeeDecisions: false,
      invoiceRecipientName: '',
      invoicingStreetAddress: '',
      invoicingPostalCode: '',
      invoicingPostOffice: '',
      ssnAddingDisabled: null,
      ophPersonOid: null,
      updatedFromVtj: null,
      ...initial
    })
  }

  static family(initial: Family): FamilyBuilder {
    return new FamilyBuilder(initial)
  }

  static employee(initial?: Partial<DevEmployee>): EmployeeBuilder {
    const id = uniqueLabel()
    return new EmployeeBuilder({
      id: uuidv4(),
      email: `email_${id}@evaka.test`,
      externalId: `espoo-ad:${uuidv4()}`,
      firstName: `first_name_${id}`,
      lastName: `last_name_${id}`,
      roles: [],
      active: true,
      employeeNumber: null,
      lastLogin: HelsinkiDateTime.now(),
      preferredFirstName: null,
      ...initial
    })
  }

  static decision(
    initial: SemiPartial<
      DecisionRequest,
      'employeeId' | 'applicationId' | 'unitId'
    >
  ): DecisionBuilder {
    return new DecisionBuilder({
      id: uuidv4(),
      type: 'DAYCARE',
      startDate: LocalDate.of(2020, 1, 1),
      endDate: LocalDate.of(2021, 1, 1),
      status: 'PENDING',
      ...initial
    })
  }

  static employeePin(initial: Partial<DevEmployeePin>): EmployeePinBuilder {
    return new EmployeePinBuilder({
      id: uuidv4(),
      userId: null,
      pin: uniqueLabel(4),
      employeeExternalId: null,
      locked: false,
      ...initial
    })
  }

  static pedagogicalDocument(
    initial: SemiPartial<DevPedagogicalDocument, 'childId'>
  ): PedagogicalDocumentBuilder {
    return new PedagogicalDocumentBuilder({
      id: uuidv4(),
      description: 'Test description',
      ...initial
    })
  }

  static placement(
    initial: SemiPartial<DevPlacement, 'childId' | 'unitId'>
  ): PlacementBuilder {
    return new PlacementBuilder({
      id: uuidv4(),
      type: 'DAYCARE',
      startDate: LocalDate.todayInSystemTz(),
      endDate: LocalDate.todayInSystemTz().addYears(1),
      placeGuarantee: false,
      terminatedBy: null,
      terminationRequestedDate: null,
      ...initial
    })
  }

  static groupPlacement(
    initial: SemiPartial<
      DevDaycareGroupPlacement,
      'daycareGroupId' | 'daycarePlacementId' | 'startDate' | 'endDate'
    >
  ): GroupPlacementBuilder {
    return new GroupPlacementBuilder({
      id: uuidv4(),
      ...initial
    })
  }

  static backupCare(
    initial: SemiPartial<DevBackupCare, 'childId' | 'unitId'>
  ): BackupCareBuilder {
    return new BackupCareBuilder({
      id: uuidv4(),
      groupId: null,
      period: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      ...initial
    })
  }

  static serviceNeed(
    initial: SemiPartial<
      DevServiceNeed,
      'placementId' | 'optionId' | 'confirmedBy'
    >
  ): ServiceNeedBuilder {
    return new ServiceNeedBuilder({
      id: uuidv4(),
      startDate: LocalDate.todayInSystemTz(),
      endDate: LocalDate.todayInSystemTz(),
      shiftCare: 'NONE',
      partWeek: false,
      confirmedAt: HelsinkiDateTime.now(),
      ...initial
    })
  }

  static serviceNeedOption(
    initial?: Partial<ServiceNeedOption>
  ): ServiceNeedOptionBuilder {
    const id = uniqueLabel()

    return new ServiceNeedOptionBuilder({
      id: uuidv4(),
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
      ...initial
    })
  }

  static childAdditionalInfo(
    initial: SemiPartial<DevChild, 'id'>
  ): ChildBuilder {
    return new ChildBuilder({
      additionalInfo: '',
      allergies: '',
      diet: '',
      dietId: null,
      mealTextureId: null,
      languageAtHome: '',
      languageAtHomeDetails: '',
      medication: '',
      ...initial
    })
  }

  static assistanceFactor(
    initial: SemiPartial<AssistanceFactor, 'childId'>
  ): AssistanceFactorBuilder {
    return new AssistanceFactorBuilder({
      id: uuidv4(),
      capacityFactor: 1.0,
      validDuring: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      modified: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser,
      ...initial
    })
  }

  static daycareAssistance(
    initial: SemiPartial<DaycareAssistance, 'childId'>
  ): DaycareAssistanceBuilder {
    return new DaycareAssistanceBuilder({
      id: uuidv4(),
      level: 'GENERAL_SUPPORT',
      validDuring: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      modified: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser,
      ...initial
    })
  }

  static preschoolAssistance(
    initial: SemiPartial<PreschoolAssistance, 'childId'>
  ): PreschoolAssistanceBuilder {
    return new PreschoolAssistanceBuilder({
      id: uuidv4(),
      level: 'SPECIAL_SUPPORT',
      validDuring: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      modified: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser,
      ...initial
    })
  }

  static otherAssistanceMeasure(
    initial: SemiPartial<OtherAssistanceMeasure, 'childId'>
  ): OtherAssistanceMeasureBuilder {
    return new OtherAssistanceMeasureBuilder({
      id: uuidv4(),
      type: 'TRANSPORT_BENEFIT',
      validDuring: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      modified: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser,
      ...initial
    })
  }

  static assistanceNeedDecision(
    initial: SemiPartial<DevAssistanceNeedDecision, 'childId'>
  ): AssistanceNeedDecisionBuilder {
    return new AssistanceNeedDecisionBuilder({
      id: uuidv4(),
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
    })
  }

  static preFilledAssistanceNeedDecision(
    initial: SemiPartial<DevAssistanceNeedDecision, 'childId'>
  ): AssistanceNeedDecisionBuilder {
    return new AssistanceNeedDecisionBuilder({
      id: uuidv4(),
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
    })
  }

  static assistanceNeedPreschoolDecision(
    initial: SemiPartial<DevAssistanceNeedPreschoolDecision, 'childId'>
  ): AssistanceNeedPreschoolDecisionBuilder {
    return new AssistanceNeedPreschoolDecisionBuilder({
      id: uuidv4(),
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
        selectedUnit: '',
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
    })
  }

  static daycareCaretakers(
    initial: SemiPartial<Caretaker, 'groupId'>
  ): DaycareCaretakersBuilder {
    return new DaycareCaretakersBuilder({
      amount: 1,
      startDate: LocalDate.todayInSystemTz(),
      endDate: null,
      ...initial
    })
  }

  static childAttendance(
    initial: SemiPartial<DevChildAttendance, 'childId' | 'unitId'>
  ): ChildAttendanceBuilder {
    return new ChildAttendanceBuilder({
      date: LocalDate.todayInHelsinkiTz(),
      arrived: LocalTime.nowInHelsinkiTz(),
      departed: LocalTime.nowInHelsinkiTz(),
      ...initial
    })
  }

  static feeThresholds(initial?: Partial<FeeThresholds>): FeeThresholdBuilder {
    return new FeeThresholdBuilder({
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
    })
  }

  static income(
    initial: SemiPartial<DevIncome, 'personId' | 'updatedBy'>
  ): IncomeBuilder {
    return new IncomeBuilder({
      id: uuidv4(),
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
      updatedAt: HelsinkiDateTime.now(),
      isEntrepreneur: false,
      worksAtEcha: false,
      ...initial
    })
  }

  static incomeNotification(
    initial: SemiPartial<IncomeNotification, 'receiverId'>
  ): IncomeNotificationBuilder {
    return new IncomeNotificationBuilder({
      notificationType: 'INITIAL_EMAIL',
      created: HelsinkiDateTime.now(),
      ...initial
    })
  }

  static placementPlan(
    initial: SemiPartial<PlacementPlan, 'unitId'> & { applicationId: UUID }
  ): PlacementPlanBuilder {
    return new PlacementPlanBuilder({
      periodStart: LocalDate.todayInSystemTz(),
      periodEnd: LocalDate.todayInSystemTz(),
      preschoolDaycarePeriodStart: null,
      preschoolDaycarePeriodEnd: null,
      ...initial
    })
  }

  static holidayPeriod(initial?: Partial<HolidayPeriod>): HolidayPeriodBuilder {
    return new HolidayPeriodBuilder({
      id: uuidv4(),
      period: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      reservationsOpenOn: LocalDate.todayInSystemTz(),
      reservationDeadline: LocalDate.todayInSystemTz(),
      ...initial
    })
  }

  static holidayQuestionnaire(
    initial?: Partial<FixedPeriodQuestionnaire>
  ): HolidayQuestionnaireBuilder {
    return new HolidayQuestionnaireBuilder({
      id: uuidv4(),
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
    })
  }

  static holiday(initial?: Partial<DevHoliday>): HolidayBuilder {
    return new HolidayBuilder({
      date: LocalDate.todayInHelsinkiTz(),
      description: 'Holiday description',
      ...initial
    })
  }

  static guardian(child: DevPerson, guardian: DevPerson) {
    return new GuardianBuilder({
      childId: child.id,
      guardianId: guardian.id
    })
  }

  static fridgeChild(
    initial: SemiPartial<DevFridgeChild, 'headOfChild' | 'childId'>
  ) {
    return new FridgeChildBuilder({
      id: uuidv4(),
      startDate: LocalDate.of(2020, 1, 1),
      endDate: LocalDate.of(2020, 12, 31),
      conflict: false,
      ...initial
    })
  }

  static staffOccupancyCoefficient(unitId: string, employeeId: string) {
    return new StaffOccupancyCoefficientBuilder({
      coefficient: 7,
      employeeId,
      unitId
    })
  }

  static dailyServiceTime(
    initial: SemiPartial<DevDailyServiceTimes, 'childId'>
  ): DailyServiceTimeBuilder {
    return new DailyServiceTimeBuilder({
      id: uuidv4(),
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
    })
  }

  static dailyServiceTimeNotification(
    initial: SemiPartial<DevDailyServiceTimeNotification, 'guardianId'>
  ): DailyServiceTimeNotificationBuilder {
    return new DailyServiceTimeNotificationBuilder({
      id: uuidv4(),
      ...initial
    })
  }

  static payment(
    initial: SemiPartial<DevPayment, 'unitId' | 'unitName'>
  ): PaymentBuilder {
    return new PaymentBuilder({
      id: uuidv4(),
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
      paymentDate: null,
      dueDate: null,
      sentAt: null,
      sentBy: null,
      ...initial
    })
  }

  static realtimeStaffAttendance(
    initial: SemiPartial<DevStaffAttendance, 'employeeId' | 'groupId'>
  ): RealtimeStaffAttendanceBuilder {
    return new RealtimeStaffAttendanceBuilder({
      id: uuidv4(),
      arrived: HelsinkiDateTime.now(),
      departed: null,
      occupancyCoefficient: 7,
      type: 'PRESENT',
      departedAutomatically: false,
      ...initial
    })
  }

  static staffAttendancePlan(
    initial: SemiPartial<DevStaffAttendancePlan, 'employeeId'>
  ): StaffAttendancePlanBuilder {
    return new StaffAttendancePlanBuilder({
      id: uuidv4(),
      type: 'PRESENT',
      startTime: HelsinkiDateTime.now(),
      endTime: HelsinkiDateTime.now(),
      description: null,
      ...initial
    })
  }

  static calendarEvent(
    initial?: Partial<DevCalendarEvent>
  ): CalendarEventBuilder {
    return new CalendarEventBuilder({
      id: uuidv4(),
      title: '',
      description: '',
      period: new FiniteDateRange(
        LocalDate.of(2020, 1, 1),
        LocalDate.of(2020, 1, 1)
      ),
      modifiedAt: LocalDate.of(2020, 1, 1).toHelsinkiDateTime(LocalTime.MIN),
      eventType: 'DAYCARE_EVENT',
      ...initial
    })
  }

  static calendarEventAttendee(
    initial: SemiPartial<DevCalendarEventAttendee, 'calendarEventId' | 'unitId'>
  ): CalendarEventAttendeeBuilder {
    return new CalendarEventAttendeeBuilder({
      id: uuidv4(),
      groupId: null,
      childId: null,
      ...initial
    })
  }

  static calendarEventTime(
    initial: SemiPartial<DevCalendarEventTime, 'calendarEventId'>
  ): CalendarEventTimeBuilder {
    return new CalendarEventTimeBuilder({
      id: uuidv4(),
      date: LocalDate.of(2020, 1, 1),
      childId: null,
      modifiedBy: systemInternalUser,
      modifiedAt: LocalDate.of(2020, 1, 1).toHelsinkiDateTime(LocalTime.MIN),
      start: LocalTime.of(8, 0),
      end: LocalTime.of(8, 30),
      ...initial
    })
  }

  static attendanceReservation(
    data: DailyReservationRequest
  ): AttendanceReservationBuilder {
    return new AttendanceReservationBuilder(data)
  }

  static attendanceReservationRaw(
    data: ReservationInsert
  ): AttendanceReservationRawBuilder {
    return new AttendanceReservationRawBuilder(data)
  }

  static absence(initial: SemiPartial<DevAbsence, 'childId'>): AbsenceBuilder {
    return new AbsenceBuilder({
      id: uuidv4(),
      date: LocalDate.todayInHelsinkiTz(),
      absenceType: 'OTHER_ABSENCE',
      modifiedAt: HelsinkiDateTime.now(),
      modifiedBy: systemInternalUser,
      absenceCategory: 'BILLABLE',
      questionnaireId: null,
      ...initial
    })
  }

  static documentTemplate(
    initial?: Partial<DevDocumentTemplate>
  ): DocumentTemplateBuilder {
    return new DocumentTemplateBuilder({
      id: uuidv4(),
      type: 'PEDAGOGICAL_REPORT',
      language: 'FI',
      name: 'Pedagoginen selvitys',
      confidential: true,
      legalBasis: '§1',
      published: false,
      validity: new DateRange(LocalDate.of(2000, 1, 1), null),
      processDefinitionNumber: null,
      archiveDurationMonths: null,
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
    })
  }

  static assistanceAction(
    initial: SemiPartial<DevAssistanceAction, 'childId' | 'updatedBy'>
  ): AssistanceActionBuilder {
    return new AssistanceActionBuilder({
      id: uuidv4(),
      actions: ['ASSISTANCE_SERVICE_CHILD'],
      endDate: LocalDate.todayInSystemTz(),
      startDate: LocalDate.todayInSystemTz(),
      otherAction: '',
      ...initial
    })
  }

  static assistanceActionOption(
    initial?: Partial<DevAssistanceActionOption>
  ): AssistanceActionOptionBuilder {
    return new AssistanceActionOptionBuilder({
      id: uuidv4(),
      descriptionFi: 'a description',
      nameFi: 'a test assistance action option',
      value: 'TEST_ASSISTANCE_ACTION_OPTION',
      ...initial
    })
  }

  static childDocument(
    initial: SemiPartial<DevChildDocument, 'childId' | 'templateId'>
  ): ChildDocumentBuilder {
    return new ChildDocumentBuilder({
      id: uuidv4(),
      status: 'DRAFT',
      modifiedAt: HelsinkiDateTime.now(),
      contentModifiedAt: HelsinkiDateTime.now(),
      contentModifiedBy: null,
      publishedAt: null,
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
      ...initial
    })
  }

  static assistanceNeedVoucherCoefficient(
    initial: SemiPartial<AssistanceNeedVoucherCoefficient, 'childId'>
  ): AssistanceNeedVoucherCoefficientBuilder {
    return new AssistanceNeedVoucherCoefficientBuilder({
      id: uuidv4(),
      validityPeriod: new FiniteDateRange(
        LocalDate.todayInSystemTz(),
        LocalDate.todayInSystemTz()
      ),
      coefficient: 1.0,
      ...initial
    })
  }

  static parentship(
    initial: SemiPartial<DevParentship, 'childId' | 'headOfChildId'>
  ): ParentshipBuilder {
    return new ParentshipBuilder({
      id: uuidv4(),
      createdAt: HelsinkiDateTime.now(),
      startDate: LocalDate.todayInSystemTz(),
      endDate: LocalDate.todayInSystemTz(),
      ...initial
    })
  }

  static vasuTemplate(
    initial?: Partial<CreateVasuTemplateBody>
  ): VasuTemplateBuilder {
    return new VasuTemplateBuilder({
      name: 'testipohja',
      valid: new FiniteDateRange(
        LocalDate.of(2020, 1, 1),
        LocalDate.of(2200, 1, 1)
      ),
      type: 'DAYCARE',
      language: 'FI',
      ...initial
    })
  }
}

abstract class FixtureBuilder<T> {
  constructor(public data: T) {}
}

export class DaycareBuilder extends FixtureBuilder<DevDaycare> {
  id(id: string): DaycareBuilder {
    this.data.id = id
    return this
  }

  async save() {
    await createDaycares({ body: [this.data] })
    return this.data
  }
}

export class DaycareGroupBuilder extends FixtureBuilder<DevDaycareGroup> {
  async save() {
    await createDaycareGroups({ body: [this.data] })
    return this.data
  }
}

export class CareAreaBuilder extends FixtureBuilder<DevCareArea> {
  async save() {
    await createCareAreas({ body: [this.data] })
    return this.data
  }
}

export class PreschoolTermBuilder extends FixtureBuilder<DevPreschoolTerm> {
  async save() {
    await createPreschoolTerm({ body: this.data })
    return this.data
  }
}

export class ClubTermBuilder extends FixtureBuilder<DevClubTerm> {
  async save() {
    await createClubTerm({ body: this.data })
    return this.data
  }
}

export class PersonBuilder extends FixtureBuilder<DevPerson> {
  async saveAdult(opts: { updateMockVtjWithDependants?: DevPerson[] } = {}) {
    await createPerson({ body: this.data, type: 'ADULT' })
    if (opts.updateMockVtjWithDependants !== undefined) {
      await this.updateMockVtj(opts.updateMockVtjWithDependants)
    }
    return this.data
  }

  async saveChild(opts: { updateMockVtj?: boolean } = {}) {
    await createPerson({ body: this.data, type: 'CHILD' })
    if (opts.updateMockVtj) {
      await this.updateMockVtj([])
    }
    return this.data
  }

  private async updateMockVtj(dependants: DevPerson[]): Promise<DevPerson> {
    const person = this.data
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
    return this.data
  }
}

export interface Family {
  guardian: DevPerson
  otherGuardian?: DevPerson
  children: DevPerson[]
}

export class FamilyBuilder extends FixtureBuilder<Family> {
  async save() {
    for (const child of this.data.children) {
      await Fixture.person(child).saveChild({ updateMockVtj: true })
    }
    await Fixture.person(this.data.guardian).saveAdult({
      updateMockVtjWithDependants: this.data.children
    })
    if (this.data.otherGuardian) {
      await Fixture.person(this.data.otherGuardian).saveAdult({
        updateMockVtjWithDependants: this.data.children
      })
    }
  }
}

export class EmployeeBuilder extends FixtureBuilder<DevEmployee> {
  daycareAcl: { unitId: string; role: ScopedRole }[]
  groupAcl: string[]

  constructor(data: DevEmployee) {
    super(data)
    this.daycareAcl = []
    this.groupAcl = []
  }

  admin(): EmployeeBuilder {
    return new EmployeeBuilder({ ...this.data, roles: ['ADMIN'] })
  }

  financeAdmin(): EmployeeBuilder {
    return new EmployeeBuilder({ ...this.data, roles: ['FINANCE_ADMIN'] })
  }

  director(): EmployeeBuilder {
    return new EmployeeBuilder({ ...this.data, roles: ['DIRECTOR'] })
  }

  reportViewer(): EmployeeBuilder {
    return new EmployeeBuilder({ ...this.data, roles: ['REPORT_VIEWER'] })
  }

  serviceWorker(): EmployeeBuilder {
    return new EmployeeBuilder({ ...this.data, roles: ['SERVICE_WORKER'] })
  }

  messenger(): EmployeeBuilder {
    return new EmployeeBuilder({ ...this.data, roles: ['MESSAGING'] })
  }

  unitSupervisor(unitId: string): EmployeeBuilder {
    return new EmployeeBuilder(this.data).withDaycareAcl(
      unitId,
      'UNIT_SUPERVISOR'
    )
  }

  specialEducationTeacher(unitId: string): EmployeeBuilder {
    return new EmployeeBuilder(this.data).withDaycareAcl(
      unitId,
      'SPECIAL_EDUCATION_TEACHER'
    )
  }

  staff(unitId: string): EmployeeBuilder {
    return new EmployeeBuilder(this.data).withDaycareAcl(unitId, 'STAFF')
  }

  withDaycareAcl(unitId: string, role: ScopedRole): this {
    this.daycareAcl.push({ unitId, role })
    return this
  }

  withGroupAcl(groupId: string): this {
    this.groupAcl.push(groupId)
    return this
  }

  async save() {
    await createEmployee({ body: this.data })
    for (const { unitId, role } of this.daycareAcl) {
      if (!this.data.externalId)
        throw new Error("Can't add ACL without externalId")
      await addAclRoleForDaycare({
        daycareId: unitId,
        body: { externalId: this.data.externalId, role }
      })
    }
    if (this.groupAcl.length > 0) {
      await createDaycareGroupAclRows({
        body: this.groupAcl.map((groupId) => ({
          groupId,
          employeeId: this.data.id
        }))
      })
    }
    return this.data
  }
}

export class DecisionBuilder extends FixtureBuilder<DecisionRequest> {
  async save() {
    await createDecisions({ body: [this.data] })
    return this.data
  }
}

export class EmployeePinBuilder extends FixtureBuilder<DevEmployeePin> {
  async save() {
    await createEmployeePins({ body: [this.data] })
    return this.data
  }
}

export class PedagogicalDocumentBuilder extends FixtureBuilder<DevPedagogicalDocument> {
  async save() {
    await createPedagogicalDocuments({ body: [this.data] })
    return this.data
  }
}

export class ServiceNeedOptionBuilder extends FixtureBuilder<ServiceNeedOption> {
  async save() {
    await createServiceNeedOption({ body: [this.data] })
    return this.data
  }
}

export class PlacementBuilder extends FixtureBuilder<DevPlacement> {
  dateRange(): FiniteDateRange {
    return new FiniteDateRange(this.data.startDate, this.data.endDate)
  }

  async save() {
    await createDaycarePlacements({ body: [this.data] })
    return this.data
  }
}

export class GroupPlacementBuilder extends FixtureBuilder<DevDaycareGroupPlacement> {
  async save() {
    await createDaycareGroupPlacement({ body: [this.data] })
    return this.data
  }
}

export class BackupCareBuilder extends FixtureBuilder<DevBackupCare> {
  async save() {
    await createBackupCares({ body: [this.data] })
    return this.data
  }
}

export class ServiceNeedBuilder extends FixtureBuilder<DevServiceNeed> {
  async save() {
    await createServiceNeeds({ body: [this.data] })
    return this.data
  }
}

export class ChildBuilder extends FixtureBuilder<DevChild> {
  async save() {
    await createChildren({ body: [this.data] })
    return this.data
  }

  with(data: Partial<DevChild>): this {
    this.data = {
      ...this.data,
      ...data
    }
    return this
  }
}

export class AssistanceFactorBuilder extends FixtureBuilder<AssistanceFactor> {
  async save() {
    await createAssistanceFactors({ body: [this.data] })
    return this.data
  }
}

export class DaycareAssistanceBuilder extends FixtureBuilder<DaycareAssistance> {
  async save() {
    await createDaycareAssistances({ body: [this.data] })
    return this.data
  }
}

export class PreschoolAssistanceBuilder extends FixtureBuilder<PreschoolAssistance> {
  async save() {
    await createPreschoolAssistances({ body: [this.data] })
    return this.data
  }
}

export class OtherAssistanceMeasureBuilder extends FixtureBuilder<OtherAssistanceMeasure> {
  async save() {
    await createOtherAssistanceMeasures({ body: [this.data] })
    return this.data
  }
}

export class AssistanceNeedDecisionBuilder extends FixtureBuilder<DevAssistanceNeedDecision> {
  async save() {
    await createAssistanceNeedDecisions({ body: [this.data] })
    return this.data
  }
}

export class AssistanceNeedVoucherCoefficientBuilder extends FixtureBuilder<AssistanceNeedVoucherCoefficient> {
  async save() {
    await createAssistanceNeedVoucherCoefficients({ body: [this.data] })
    return this.data
  }
}

export class AssistanceNeedPreschoolDecisionBuilder extends FixtureBuilder<DevAssistanceNeedPreschoolDecision> {
  async save() {
    await createAssistanceNeedPreschoolDecisions({ body: [this.data] })
    return this.data
  }

  with(value: Partial<DevAssistanceNeedPreschoolDecision>): this {
    this.data = {
      ...this.data,
      ...value
    }
    return this
  }

  withGuardian(guardianId: UUID) {
    this.data.form.guardianInfo.push({
      id: uuidv4(),
      personId: guardianId,
      name: '',
      isHeard: false,
      details: ''
    })
    return this
  }

  withForm(form: Partial<AssistanceNeedPreschoolDecisionForm>) {
    this.data.form = {
      ...this.data.form,
      ...form
    }
    return this
  }

  withRequiredFieldsFilled(
    unitId: UUID,
    preparerId: UUID,
    decisionMakerId: UUID
  ) {
    this.data.form = {
      ...this.data.form,
      type: 'NEW',
      validFrom: LocalDate.of(2022, 8, 1),
      selectedUnit: unitId,
      primaryGroup: 'Perhoset',
      decisionBasis: 'Perustelu',
      basisDocumentPedagogicalReport: true,
      guardiansHeardOn: LocalDate.of(2022, 8, 1),
      guardianInfo: this.data.form.guardianInfo.map((g) => ({
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
    return this
  }
}

export class DaycareCaretakersBuilder extends FixtureBuilder<Caretaker> {
  async save() {
    await createDaycareCaretakers({ body: [this.data] })
    return this.data
  }
}

export class ChildAttendanceBuilder extends FixtureBuilder<DevChildAttendance> {
  async save() {
    await postAttendances({ body: [this.data] })
    return this.data
  }
}

export class IncomeBuilder extends FixtureBuilder<DevIncome> {
  async save() {
    await createIncome({ body: this.data })
    return this.data
  }
}

export class IncomeNotificationBuilder extends FixtureBuilder<IncomeNotification> {
  async save() {
    await createIncomeNotification({ body: this.data })
    return this.data
  }
}

export class FeeThresholdBuilder extends FixtureBuilder<FeeThresholds> {
  async save() {
    await createFeeThresholds({ body: this.data })
    return this.data
  }
}

export class PlacementPlanBuilder extends FixtureBuilder<
  PlacementPlan & { applicationId: UUID }
> {
  async save() {
    const { applicationId, ...body } = this.data
    await createPlacementPlan({ applicationId, body })
    return this.data
  }
}

export class HolidayPeriodBuilder extends FixtureBuilder<HolidayPeriod> {
  async save() {
    const { id, ...body } = this.data
    await createHolidayPeriod({ id, body })
    return this.data
  }
}

export class HolidayQuestionnaireBuilder extends FixtureBuilder<FixedPeriodQuestionnaire> {
  async save() {
    const { id, ...body } = this.data
    await createHolidayQuestionnaire({ id, body })
    return this.data
  }
}

export class HolidayBuilder extends FixtureBuilder<DevHoliday> {
  async save() {
    await createHoliday({ body: this.data })
    return this.data
  }
}

export class GuardianBuilder extends FixtureBuilder<{
  guardianId: string
  childId: string
}> {
  async save() {
    await insertGuardians({ body: [this.data] })
    return this.data
  }
}

export class FridgeChildBuilder extends FixtureBuilder<DevFridgeChild> {
  async save() {
    await createFridgeChild({ body: [this.data] })
    return this.data
  }
}

export class StaffOccupancyCoefficientBuilder extends FixtureBuilder<DevUpsertStaffOccupancyCoefficient> {
  async save() {
    await upsertStaffOccupancyCoefficient({ body: this.data })
    return this.data
  }
}

export class DailyServiceTimeBuilder extends FixtureBuilder<DevDailyServiceTimes> {
  async save() {
    await addDailyServiceTime({ body: this.data })
    return this.data
  }
}

export class DailyServiceTimeNotificationBuilder extends FixtureBuilder<DevDailyServiceTimeNotification> {
  async save() {
    await addDailyServiceTimeNotification({ body: this.data })
    return this.data
  }
}

export class PaymentBuilder extends FixtureBuilder<DevPayment> {
  async save() {
    await addPayment({ body: this.data })
    return this.data
  }
}

export class CalendarEventBuilder extends FixtureBuilder<DevCalendarEvent> {
  async save() {
    await addCalendarEvent({ body: this.data })
    return this.data
  }
}

export class CalendarEventAttendeeBuilder extends FixtureBuilder<DevCalendarEventAttendee> {
  async save() {
    await addCalendarEventAttendee({ body: this.data })
    return this.data
  }
}

export class CalendarEventTimeBuilder extends FixtureBuilder<DevCalendarEventTime> {
  async save() {
    await addCalendarEventTime({ body: this.data })
    return this.data
  }
}

export class RealtimeStaffAttendanceBuilder extends FixtureBuilder<DevStaffAttendance> {
  async save() {
    await addStaffAttendance({ body: this.data })
    return this.data
  }
}

export class StaffAttendancePlanBuilder extends FixtureBuilder<DevStaffAttendancePlan> {
  async save() {
    await addStaffAttendancePlan({ body: this.data })
    return this.data
  }
}

export class AttendanceReservationBuilder extends FixtureBuilder<DailyReservationRequest> {
  async save() {
    await postReservations({ body: [this.data] })
    return this.data
  }
}

export class AttendanceReservationRawBuilder extends FixtureBuilder<ReservationInsert> {
  async save() {
    await postReservationsRaw({ body: [this.data] })
    return this
  }
}

export class AbsenceBuilder extends FixtureBuilder<DevAbsence> {
  async save() {
    await addAbsence({ body: this.data })
    return this.data
  }
}

export class DocumentTemplateBuilder extends FixtureBuilder<DevDocumentTemplate> {
  async save() {
    await createDocumentTemplate({ body: this.data })
    return this.data
  }

  withPublished(published: boolean) {
    this.data.published = published
    return this
  }
}

export class ChildDocumentBuilder extends FixtureBuilder<DevChildDocument> {
  async save() {
    await createChildDocument({ body: this.data })
    return this.data
  }

  withModifiedAt(modifiedAt: HelsinkiDateTime) {
    this.data.modifiedAt = modifiedAt
    return this
  }

  withPublishedAt(publishedAt: HelsinkiDateTime | null) {
    this.data.publishedAt = publishedAt
    return this
  }

  withPublishedContent(publishedContent: DocumentContent | null) {
    this.data.publishedContent = publishedContent
    return this
  }
}

export class AssistanceActionBuilder extends FixtureBuilder<DevAssistanceAction> {
  async save() {
    await createAssistanceAction({ body: [this.data] })
    return this.data
  }
}

export class AssistanceActionOptionBuilder extends FixtureBuilder<DevAssistanceActionOption> {
  async save() {
    await createAssistanceActionOption({ body: [this.data] })
    return this.data
  }
}

export class ParentshipBuilder extends FixtureBuilder<DevParentship> {
  async save() {
    await createParentships({ body: [this.data] })
    return this.data
  }
}

export class VasuTemplateBuilder extends FixtureBuilder<CreateVasuTemplateBody> {
  async save() {
    await createVasuTemplate({ body: this.data })
    return this.data
  }

  async saveAndReturnId() {
    return createVasuTemplate({ body: this.data })
  }
}

export const fullDayTimeRange: TimeRange = new TimeRange(
  LocalTime.MIN,
  LocalTime.parse('23:59')
)

export const nonFullDayTimeRange: TimeRange = new TimeRange(
  LocalTime.of(1, 0),
  LocalTime.of(23, 0)
)

export const preschoolTerm2020: DevPreschoolTerm = {
  id: uuidv4(),
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
    LocalDate.of(2020, 1, 20)
  ),
  termBreaks: []
}

export const preschoolTerm2021: DevPreschoolTerm = {
  id: uuidv4(),
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
    LocalDate.of(2021, 1, 20)
  ),
  termBreaks: []
}

export const preschoolTerm2022: DevPreschoolTerm = {
  id: uuidv4(),
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
    LocalDate.of(2022, 1, 21)
  ),
  termBreaks: []
}

export const preschoolTerm2023: DevPreschoolTerm = {
  id: uuidv4(),
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
    LocalDate.of(2023, 1, 20)
  ),
  termBreaks: [
    new FiniteDateRange(LocalDate.of(2023, 10, 16), LocalDate.of(2023, 10, 20)),
    new FiniteDateRange(LocalDate.of(2023, 12, 23), LocalDate.of(2024, 1, 7)),
    new FiniteDateRange(LocalDate.of(2024, 2, 19), LocalDate.of(2024, 2, 23))
  ]
}

export const preschoolTerms = [
  preschoolTerm2020,
  preschoolTerm2021,
  preschoolTerm2022,
  preschoolTerm2023
]

export const clubTerm2020: ClubTerm = {
  id: uuidv4(),
  term: new FiniteDateRange(
    LocalDate.of(2020, 8, 13),
    LocalDate.of(2021, 6, 4)
  ),
  applicationPeriod: new FiniteDateRange(
    LocalDate.of(2020, 1, 8),
    LocalDate.of(2020, 1, 20)
  ),
  termBreaks: []
}

export const clubTerm2021: ClubTerm = {
  id: uuidv4(),
  term: new FiniteDateRange(
    LocalDate.of(2021, 8, 11),
    LocalDate.of(2022, 6, 3)
  ),
  applicationPeriod: new FiniteDateRange(
    LocalDate.of(2021, 1, 8),
    LocalDate.of(2021, 1, 20)
  ),
  termBreaks: []
}

export const clubTerm2022: ClubTerm = {
  id: uuidv4(),
  term: new FiniteDateRange(
    LocalDate.of(2022, 8, 10),
    LocalDate.of(2023, 6, 3)
  ),
  applicationPeriod: new FiniteDateRange(
    LocalDate.of(2022, 1, 8),
    LocalDate.of(2022, 1, 20)
  ),
  termBreaks: []
}

export const clubTerm2023: ClubTerm = {
  id: uuidv4(),
  term: new FiniteDateRange(
    LocalDate.of(2023, 8, 10),
    LocalDate.of(2024, 6, 3)
  ),
  applicationPeriod: new FiniteDateRange(
    LocalDate.of(2023, 1, 8),
    LocalDate.of(2023, 1, 20)
  ),
  termBreaks: [
    new FiniteDateRange(LocalDate.of(2023, 10, 16), LocalDate.of(2023, 10, 20)),
    new FiniteDateRange(LocalDate.of(2023, 12, 23), LocalDate.of(2024, 1, 7)),
    new FiniteDateRange(LocalDate.of(2024, 2, 19), LocalDate.of(2024, 2, 23))
  ]
}

export const clubTerms = [
  clubTerm2020,
  clubTerm2021,
  clubTerm2022,
  clubTerm2023
]

export const testCareArea: DevCareArea = {
  id: '674dfb66-8849-489e-b094-e6a0ebfb3c71',
  name: 'Superkeskus',
  shortName: 'super-keskus',
  areaCode: 299,
  subCostCenter: '99'
}

export const testCareArea2: DevCareArea = {
  id: '7a5b42db-451b-4394-b6a6-86993ea0ed45',
  name: 'Hyperkeskus',
  shortName: 'hyper-keskus',
  areaCode: 298,
  subCostCenter: '98'
}

export const testClub: DevDaycare = {
  id: '0b5ffd40-2f1a-476a-ad06-2861f433b0d1',
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
  mealtimeEveningSnack: null
}

export const testDaycare: DevDaycare = {
  id: '4f3a32f5-d1bd-4b8b-aa4e-4fd78b18354b',
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
    'MOBILE_MESSAGING',
    'PLACEMENT_TERMINATION'
  ],
  businessId: '',
  iban: '',
  providerId: '',
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
  mealtimeEveningSnack: null
}

export const testDaycare2: DevDaycare = {
  id: '6f540c39-e7f6-4222-a004-c527403378ec',
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
  mealtimeEveningSnack: null
}

export const testDaycarePrivateVoucher: DevDaycare = {
  id: '572adb7e-9b3d-11ea-bb37-0242ac130002',
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
    'MOBILE_MESSAGING',
    'PLACEMENT_TERMINATION'
  ],
  invoicedByMunicipality: false,
  businessId: '',
  iban: '',
  providerId: '',
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
  mealtimeEveningSnack: null
}

export const testPreschool: DevDaycare = {
  id: 'b53d80e0-319b-4d2b-950c-f5c3c9f834bc',
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
    'PLACEMENT_TERMINATION'
  ],
  businessId: '',
  iban: '',
  providerId: '',
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
  mealtimeEveningSnack: null
}

export const testAdult = Fixture.person({
  id: '87a5c962-9b3d-11ea-bb37-0242ac130002',
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
}).data

export const testChild = Fixture.person({
  id: '572adb7e-9b3d-11ea-bb37-0242ac130002',
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
}).data

export const testChild2 = Fixture.person({
  id: '5a4f3ccc-5270-4d28-bd93-d355182b6768',
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
}).data

export const testChildRestricted = Fixture.person({
  id: '28e189d7-abbe-4be9-9074-6e4c881f18de',
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
}).data

export const testAdult2 = Fixture.person({
  id: 'fb915d31-738f-453f-a2ca-2e7f61db641d',
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
}).data

export const testChildDeceased = Fixture.person({
  id: 'b8711722-0c1b-4044-a794-5b308207d78b',
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
}).data

export const testChildNoSsn = Fixture.person({
  id: 'a5e87ec8-6221-46f8-8b2b-9ab124d51c22',
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
}).data

const twoGuardiansGuardian1 = {
  id: '9d6289ba-9ffd-11ea-bb37-0242ac130002',
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
}
const twoGuardiansGuardian2 = {
  id: 'd1c30734-c02f-4546-8123-856f8101565e',
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
}
const twoGuardiansChildren = [
  Fixture.person({
    id: '6ec99620-9ffd-11ea-bb37-0242ac130002',
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
  }).data
]
export const familyWithTwoGuardians = {
  guardian: Fixture.person(twoGuardiansGuardian1).data,
  otherGuardian: Fixture.person(twoGuardiansGuardian2).data,
  children: twoGuardiansChildren
}

const separatedGuardiansGuardian1 = {
  id: '1c1b2946-fdf3-4e02-a3e4-2c2a797bafc3',
  firstName: 'John',
  lastName: 'Doe',
  dateOfBirth: LocalDate.of(1980, 1, 1),
  ssn: '010180-1232',
  streetAddress: 'Kamreerintie 2',
  postalCode: '02770',
  postOffice: 'Espoo'
}
const separatedGuardiansGuardian2 = Fixture.person({
  id: '56064714-649f-457e-893a-44832936166c',
  firstName: 'Joan',
  lastName: 'Doe',
  dateOfBirth: LocalDate.of(1979, 2, 1),
  ssn: '010279-123L',
  streetAddress: 'Testikatu 1',
  postalCode: '02770',
  postOffice: 'Espoo'
}).data
const separatedGuardiansChildren = [
  Fixture.person({
    id: '5474ee62-16cf-4cfe-a297-40559e165a32',
    firstName: 'Ricky',
    lastName: 'Doe',
    dateOfBirth: LocalDate.of(2017, 6, 1),
    ssn: '010617A123U',
    streetAddress: 'Kamreerintie 2',
    postalCode: '02770',
    postOffice: 'Espoo'
  }).data
]
export const familyWithSeparatedGuardians = {
  guardian: Fixture.person(separatedGuardiansGuardian1).data,
  otherGuardian: Fixture.person(separatedGuardiansGuardian2).data,
  children: separatedGuardiansChildren
}

const restrictedDetailsGuardian = {
  id: '7699f488-3fdc-11eb-b378-0242ac130002',
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
}

const guardian2WithNoRestrictions = {
  id: '1fd05a42-3fdd-11eb-b378-0242ac130002',
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
}

const restrictedDetailsGuardiansChildren = [
  Fixture.person({
    id: '82a2586e-3fdd-11eb-b378-0242ac130002',
    firstName: 'Vadelma',
    lastName: 'Pelimerkki',
    dateOfBirth: LocalDate.of(2017, 5, 15),
    ssn: '150517A9989',
    streetAddress: 'Kamreerintie 4',
    postalCode: '02100',
    postOffice: 'Espoo'
  }).data
]

export const familyWithRestrictedDetailsGuardian = {
  guardian: Fixture.person(restrictedDetailsGuardian).data,
  otherGuardian: Fixture.person(guardian2WithNoRestrictions).data,
  children: restrictedDetailsGuardiansChildren
}

const deadGuardian = Fixture.person({
  id: 'faacfd43-878f-4a70-9e74-2051a18480e6',
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
}).data

const deadGuardianChild = Fixture.person({
  id: '1ad3469b-593d-45e4-a68b-a09f759bd029',
  firstName: 'Kuopus',
  lastName: 'Kuollut',
  dateOfBirth: LocalDate.of(2019, 9, 9),
  ssn: '090917A998M',
  streetAddress: 'Kamreerintie 4',
  postalCode: '02100',
  postOffice: 'Espoo'
}).data

export const familyWithDeadGuardian: Family = {
  guardian: deadGuardian,
  children: [deadGuardianChild]
}

export const testChildZeroYearOld = Fixture.person({
  id: '0909e93d-3aa8-44f8-ac30-ecd77339d849',
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
}).data

export const testAdultRestricted = Fixture.person({
  id: '92d707e9-6cbc-487b-8bde-0097d90044cd',
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
}).data

const applicationForm = (
  type: ApplicationType,
  child: DevPerson,
  guardian: DevPerson,
  guardian2Phone: string,
  guardian2Email: string,
  otherGuardianAgreementStatus: OtherGuardianAgreementStatus | null,
  preferredStartDate: LocalDate,
  preferredUnits: string[],
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

export const applicationFixtureId = '9dd0e1ba-9b3b-11ea-bb37-0242ac130002'
export const applicationFixture = (
  child: DevPerson,
  guardian: DevPerson,
  otherGuardian: DevPerson | undefined = undefined,
  type: 'DAYCARE' | 'PRESCHOOL' | 'CLUB' = 'DAYCARE',
  otherGuardianAgreementStatus: OtherGuardianAgreementStatus | null = null,
  preferredUnits: string[] = [testDaycare.id],
  connectedDaycare = false,
  status: ApplicationStatus = 'SENT',
  preferredStartDate: LocalDate = LocalDate.of(2021, 8, 16),
  transferApplication = false,
  assistanceNeeded = false
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
  checkedByAdmin: false,
  hideFromGuardian: false,
  origin: 'ELECTRONIC',
  status,
  transferApplication,
  allowOtherGuardianAccess: true,
  createdDate: null,
  dueDate: null,
  modifiedDate: null,
  sentDate: null,
  formModified: HelsinkiDateTime.now()
})

const feeThresholds = {
  minIncomeThreshold: 210200,
  maxIncomeThreshold: 479900,
  incomeMultiplier: 0.107,
  minFee: 2700,
  maxFee: 28900
}

export const decisionFixture = (
  applicationId: string,
  startDate: LocalDate,
  endDate: LocalDate
): DecisionRequest => ({
  id: '9dd0e1ba-9b3b-11ea-bb37-0242ac130987',
  employeeId: 'SET_THIS',
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
  daycareId: UUID,
  partner: DevPerson | null,
  validDuring: DateRange = new DateRange(
    LocalDate.todayInSystemTz().subYears(1),
    LocalDate.todayInSystemTz().addYears(1)
  ),
  sentAt: HelsinkiDateTime | null = null,
  id = 'bcc42d48-765d-4fe1-bc90-7a7b4c8205fe',
  documentKey?: string
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
  decisionNumber: null,
  documentKey: documentKey || null,
  created: HelsinkiDateTime.now()
})

export const voucherValueDecisionsFixture = (
  id: UUID,
  adultId: UUID,
  childId: UUID,
  daycareId: UUID,
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

export const invoiceFixture = (
  adultId: UUID,
  childId: UUID,
  areaId: UUID,
  unitId: UUID,
  status: Invoice['status'],
  periodStart = LocalDate.of(2019, 1, 1),
  periodEnd = LocalDate.of(2019, 1, 1)
): Invoice => ({
  id: uuidv4(),
  status,
  headOfFamily: adultId,
  codebtor: null,
  areaId,
  periodStart,
  periodEnd,
  invoiceDate: periodStart,
  dueDate: periodEnd,
  number: null,
  sentAt: null,
  sentBy: null,
  rows: [
    {
      id: uuidv4(),
      child: childId,
      amount: 1,
      unitPrice: 10000,
      periodStart: periodStart,
      periodEnd: periodEnd,
      product: 'DAYCARE',
      unitId,
      description: '',
      price: 10000,
      correctionId: null
    }
  ],
  totalPrice: 10000
})

export const testDaycareGroup: DevDaycareGroup = {
  id: '2f998c23-0f90-4afd-829b-d09ecf2f6188',
  daycareId: testDaycare.id,
  name: 'Kosmiset vakiot',
  startDate: LocalDate.of(2000, 1, 1),
  endDate: null,
  jamixCustomerNumber: null
}

export function createDaycarePlacementFixture(
  id: string,
  childId: string,
  unitId: string,
  startDate = LocalDate.of(2022, 5, 1),
  endDate = LocalDate.of(2023, 8, 31),
  type: PlacementType = 'DAYCARE',
  placeGuarantee = false
): DevPlacement {
  return {
    id,
    type,
    childId,
    unitId,
    startDate,
    endDate,
    placeGuarantee,
    terminationRequestedDate: null,
    terminatedBy: null
  }
}

export const DecisionIncomeFixture = (total: number): DecisionIncome => ({
  data: { MAIN_INCOME: total },
  effect: 'INCOME',
  total: total,
  totalExpenses: 0,
  totalIncome: total,
  worksAtECHA: false
})

export const nullUUID = '00000000-0000-0000-0000-000000000000'

export const systemInternalUser = nullUUID
