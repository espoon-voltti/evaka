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
    DeepPartial,
    FeatureFlags,
    ImgProps
  } from 'lib-customizations/types'

  export const appConfig: BaseAppConfig
  export const translations: Record<Lang, DeepPartial<Translations>>
  export const vasuTranslations: Record<VasuLang, DeepPartial<VasuTranslations>>
  export const featureFlags: FeatureFlags
  export const cityLogo: React.JSX.Element | ImgProps
  export const placementTypes: readonly PlacementType[]
  export const absenceTypes: readonly AbsenceType[]
  export const assistanceMeasures: readonly AssistanceMeasure[]
  export const daycareAssistanceLevels: readonly DaycareAssistanceLevel[]
  export const otherAssistanceMeasureTypes: readonly OtherAssistanceMeasureType[]
  export const placementPlanRejectReasons: readonly PlacementPlanRejectReason[]
  export const preschoolAssistanceLevels: readonly PreschoolAssistanceLevel[]
  export const unitProviderTypes: readonly ProviderType[]
  export const voucherValueDecisionTypes: readonly VoucherValueDecisionType[]
}
