// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

declare module '@evaka/customizations/employee' {
  import React from 'react'

  import {
    DaycareAssistanceLevel,
    OtherAssistanceMeasureType,
    PreschoolAssistanceLevel
  } from 'lib-common/generated/api-types/assistance'
  import { AssistanceMeasure } from 'lib-common/generated/api-types/assistanceaction'
  import {
    AbsenceType,
    ProviderType
  } from 'lib-common/generated/api-types/daycare'
  import { VoucherValueDecisionType } from 'lib-common/generated/api-types/invoicing'
  import {
    PlacementType,
    PlacementPlanRejectReason
  } from 'lib-common/generated/api-types/placement'
  import {
    Lang,
    VasuLang,
    Translations,
    VasuTranslations
  } from 'lib-customizations/employee'
  import {
    BaseAppConfig,
    FeatureFlags,
    ImgProps
  } from 'lib-customizations/types'

  export const appConfig: BaseAppConfig
  export const translations: Record<Lang, Translations>
  export const vasuTranslations: Record<VasuLang, VasuTranslations>
  export const featureFlags: FeatureFlags
  export const cityLogo: React.JSX.Element | ImgProps
  export const placementTypes: PlacementType[]
  export const absenceTypes: AbsenceType[]
  export const assistanceMeasures: AssistanceMeasure[]
  export const daycareAssistanceLevels: DaycareAssistanceLevel[]
  export const otherAssistanceMeasureTypes: OtherAssistanceMeasureType[]
  export const placementPlanRejectReasons: PlacementPlanRejectReason[]
  export const preschoolAssistanceLevels: PreschoolAssistanceLevel[]
  export const unitProviderTypes: ProviderType[]
  export const voucherValueDecisionTypes: VoucherValueDecisionType[]
}
