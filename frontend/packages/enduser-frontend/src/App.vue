<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div id="app">
    <navigation-view></navigation-view>
    <router-view></router-view>

    <MessagesDisplay />

    <div id="modal-root"></div>

    <ViewLoader />
  </div>
</template>

<script>
  import Vue from 'vue'
  import NavigationView from '@/views/navigation-view.vue'
  import MessagesDisplay from '@/components/modal/messages-display.vue'
  import { ViewLoader } from '@/components/common/view-loader'
  import { currentLanguage } from '@/utils/helpers'

  export default Vue.extend({
    components: {
      NavigationView,
      MessagesDisplay,
      ViewLoader
    },

    mounted() {
      const { query } = this.$route

      if (query && query.loginError) {
        this.$store.dispatch('modals/message', {
          type: 'error',
          title: this.$t('general.error.login.title'),
          text: this.$t('general.error.login.text')
        })

        this.$router.replace('/')
      }

      this.$i18n.locale = currentLanguage()
    }
  })
</script>

<style lang="scss">
  @import './assets/styles.scss';
</style>
