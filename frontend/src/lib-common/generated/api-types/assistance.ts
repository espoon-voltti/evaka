// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications
/* eslint-disable import/order, prettier/prettier, @typescript-eslint/no-namespace */

import { AssistanceActionResponse } from './assistanceaction'
import { AssistanceNeedResponse } from './assistanceneed'

/**
* Generated from fi.espoo.evaka.assistance.AssistanceController.AssistanceResponse
*/
export interface AssistanceResponse {
  assistanceActions: AssistanceActionResponse[]
  assistanceNeeds: AssistanceNeedResponse[]
}
