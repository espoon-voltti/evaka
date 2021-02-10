// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import Vue from 'vue'
import Router from 'vue-router'
import store from '@/store'
import VersionContainer from '@/components/common/version-container.vue'
import { config } from '@/config'

import MapView from '@/views/map/map-view.vue'
import Applications from '@/views/applications/index.vue'
import ApplicationList from '@/views/applications/application-list.vue'
import ClubApplication from '@/views/applications/club-application/club-application.vue'
import ClubApplicationPreview from '@/views/applications/club-application/club-application-preview.vue'
import DaycareApplicationPreview from '@/views/applications/daycare-application/daycare-application-preview.vue'
import DaycareApplication from '@/views/applications/daycare-application/daycare-application.vue'
import Decisions from '@/views/decisions/decisions.vue'
import DecisionList from '@/views/decisions/decision-list.vue'
import DecisionView from '@/views/decisions/decision-view.vue'

const router = new Router({
  mode: 'history',
  scrollBehavior: () => {
    return { x: 0, y: 0 }
  },
  routes: [
    {
      path: '/',
      name: 'map',
      component: MapView
    },
    {
      path: '/version-info',
      name: 'version-info',
      component: VersionContainer,
      meta: {
        requiresAuth: true
      }
    },
    {
      path: '/applications',
      component: Applications,
      children: [
        {
          path: '',
          name: 'application-list',
          component: ApplicationList,
          meta: {
            requiresAuth: true
          }
        },
        {
          path: 'club/:id',
          name: 'club-application',
          component: ClubApplication,
          meta: {
            requiresAuth: true
          }
        },
        ...(config.feature.daycareApplication === true
          ? [
              {
                path: 'daycare/:id',
                name: 'daycare-application',
                component: DaycareApplication,
                meta: {
                  requiresAuth: true
                },
                props: { isPreschool: false }
              }
            ]
          : []),
        ...(config.feature.preschoolApplication === true
          ? [
              {
                path: 'preschool/:id',
                name: 'preschool-application',
                component: DaycareApplication,
                meta: {
                  requiresAuth: true
                },
                props: { isPreschool: true }
              }
            ]
          : []),
        {
          path: 'preview/club/:id',
          name: 'club-application-preview',
          component: ClubApplicationPreview,
          meta: {
            requiresAuth: true
          }
        },
        {
          path: 'preview/daycare/:id',
          name: 'daycare-application-preview',
          component: DaycareApplicationPreview,
          meta: {
            requiresAuth: true
          }
        },
        {
          path: 'preview/preschool/:id',
          name: 'preschool-application-preview',
          component: DaycareApplicationPreview,
          meta: {
            requiresAuth: true
          }
        }
      ],
      meta: {
        requiresAuth: true
      }
    },
    {
      path: '/decisions',
      component: Decisions,
      children: [
        {
          path: '',
          name: 'decision-list',
          component: DecisionList,
          meta: {
            requiresAuth: true
          }
        },
        {
          path: ':id',
          name: 'decision-view',
          component: DecisionView,
          meta: {
            requiresAuth: true
          }
        }
      ],
      meta: {
        requiresAuth: true
      }
    },
    {
      path: '*',
      redirect: '/'
    }
  ]
})

router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)

  if (!store.getters.isLoggedIn) {
    store.dispatch('loadAuth').then((isLoggedIn) => {
      if (requiresAuth && !isLoggedIn) {
        const redirectUri = `${window.location.pathname}${window.location.search}${window.location.hash}`
        window.location.href = `/api/application/auth/saml/login?RelayState=${encodeURIComponent(
          redirectUri
        )}`
        return
      } else {
        next()
        store.dispatch('loader/clear')
      }
    })
  } else {
    next()
    store.dispatch('loader/clear')
  }
})

Vue.use(Router)

export default router
