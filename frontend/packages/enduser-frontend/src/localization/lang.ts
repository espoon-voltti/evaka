// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Vue from 'vue'
import VueI18n from 'vue-i18n'
import translations from '@/localization/app'

Vue.use(VueI18n)
export const i18n = new VueI18n({
  locale: 'fi',
  fallbackLocale: 'fi',
  messages: translations
})
