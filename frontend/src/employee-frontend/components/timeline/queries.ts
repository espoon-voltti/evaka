// SPDX-FileCopyrightText: 2017-2023 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Queries } from 'lib-common/query'

import { getTimeline } from '../../generated/api-clients/timeline'

const q = new Queries()

export const timelineQuery = q.query(getTimeline)
