// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Vue from 'vue'

Vue.directive('focus', {
  inserted: (el) => {
    el.focus()
  }
})
