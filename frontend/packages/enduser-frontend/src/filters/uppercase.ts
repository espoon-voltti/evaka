// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Vue from 'vue'

Vue.filter('uppercase', (value) => {
  return value ? value.toString().toUpperCase() : ''
})
