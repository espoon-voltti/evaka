<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <nav>
    <desktop-menu
      :model="menu"
      :loginAddress="loginAddress"
      :logoutAddress="logoutAddress"
      :citizenFrontendUrl="citizenFrontendUrl"
    />

    <mobile-menu
      :model="menu"
      :open="menuOpen"
      :loginAddress="loginAddress"
      :logoutAddress="logoutAddress"
      :citizenFrontendUrl="citizenFrontendUrl"
    />
  </nav>
</template>

<script>
  import { mapGetters } from 'vuex'
  import DesktopMenu from './desktop'
  import MobileMenu from './mobile'

  export default {
    components: {
      DesktopMenu,
      MobileMenu
    },
    data() {
      return {
        menuOpen: false,
        loginOpen: false,
        username: '',
        password: ''
      }
    },
    computed: {
      ...mapGetters(['isLoggedIn', 'userName']),
      loginAddress() {
        const redirectUri = `${window.location.pathname}${window.location.search}${window.location.hash}`
        return `/api/application/auth/saml/login?RelayState=${encodeURIComponent(
          redirectUri
        )}&locale=${encodeURIComponent(this.$i18n.locale)}`
      },
      logoutAddress() {
        return `/api/application/auth/saml/logout`
      },
      menuItems() {
        return [
          {
            path: '/',
            label: this.$t('menu.routes.map'),
            icon: ['far', 'map'],
            requiresAuth: false
          },
          {
            path: '/applications',
            label: this.$t('menu.routes.application'),
            icon: ['far', 'file-alt'],
            requiresAuth: true
          },
          {
            path: '/decisions',
            label: this.$t('menu.routes.decisions'),
            icon: ['far', 'gavel'],
            requiresAuth: true
          }
        ]
      },
      menu() {
        return this.menuItems.filter((item) =>
          item.requiresAuth ? this.isLoggedIn : true
        )
      },
      citizenFrontendUrl() {
        return window.location.host === 'localhost:9091'
          ? 'http://localhost:9094/citizen'
          : '/citizen'
      },
    }
  }
</script>

<style lang="scss">
  @include onMobile() {
    #app {
      padding-top: 52px;
    }
  }
</style>

<style lang="scss" scoped>
  nav {
    position: relative;
    z-index: 600;
  }
</style>
