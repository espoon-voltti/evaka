// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Vue from 'vue'
import { format } from 'date-fns'
import { DATE_FORMAT_MINUTES } from '@/constants'

Vue.filter('time', (timeStamp) =>
  timeStamp ? format(new Date(timeStamp), DATE_FORMAT_MINUTES) : null
)
