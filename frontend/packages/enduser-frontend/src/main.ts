// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// The Vue build version to load with the `import` command
// (runtime-only or standalone) has been set in webpack.base.conf with an alias.
import Vue from 'vue'
import App from '@/App.vue'
import router from '@/router'
import store from '@/store'
import VCalendar from 'v-calendar'
import * as constants from '@/constants'
import { VCalendarConfig } from '@/constants'
import Buefy from 'buefy'
import * as VueGoogleMaps from 'vue2-google-maps'
import VueMoment from 'vue-moment'
import Vuelidate from 'vuelidate'
import VueTippy from 'vue-tippy'
import moment from 'moment'
import { dom, library } from '@fortawesome/fontawesome-svg-core'
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome'
import iconSet from '@/icon-set'
import { FontAwesomeLayers } from '@fortawesome/vue-fontawesome'
import '@/filters'
import '@/directives'
import '@/utils/transfer-dom'
import '@/components'
import '@/api/error-interceptor'
import { i18n } from '@/localization/lang'
import * as Sentry from '@sentry/browser'
import * as Integrations from '@sentry/integrations'
import { getEnvironment } from '@evaka/lib-common/src/utils/helpers'
import { config } from '@evaka/enduser-frontend/src/config'

// Load Sentry as early as possible to catch all issues
Sentry.init({
  enabled: config.sentry.enabled,
  dsn: config.sentry.dsn,
  environment: getEnvironment(),
  integrations: [
    // @ts-ignore: Broken typings in @sentry/integrations: https://github.com/getsentry/sentry-javascript/issues/2633
    new Integrations.Vue({ Vue, attachProps: true, logErrors: true })
  ]
})

const runApp = (): void => {
  // FIXME localization variable
  moment.locale('fi')

  Vue.use(VCalendar, VCalendarConfig)
  Vue.use(Buefy, {
    defaultIconPack: 'far'
  })
  Vue.use(VueMoment)
  Vue.use(Vuelidate)
  Vue.use(VueGoogleMaps, {
    load: {
      key: config.maps.googleApiKey,
      language: 'fi'
    }
  })
  Vue.use(VueTippy)

  Vue.use((vue) => {
    vue.prototype.$const = constants
  })

  router.onReady(() => store.dispatch('init'))

  // FA icons added individually
  library.add(iconSet)

  dom.watch() // This will kick of the initial replacement of i to svg tags and configure a MutationObserver

  Vue.component('font-awesome-icon', FontAwesomeIcon)
  Vue.component('font-awesome-layers', FontAwesomeLayers)

  Vue.config.productionTip = false

  new Vue({
    i18n,
    router,
    store,
    render: (h) => h(App)
  }).$mount('#app')

}

// Wrap app startup to make sure polyfills are loaded before they are needed (e.g. load Intl before VCalendar is setup)
if (!global.Intl) {
  require.ensure([
      'intl',
      'intl/locale-data/jsonp/fi.js'
  ], function (require) {
      require('intl')
      require('intl/locale-data/jsonp/fi.js')
      runApp()
  });
} else {
  runApp()
}
