// SPDX-FileCopyrightText: 2017-2024 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { getAbsencesOfChild } from '../../generated/api-clients/absence'

const q = new Queries()

export const absencesOfChildQuery = q.query(getAbsencesOfChild)
