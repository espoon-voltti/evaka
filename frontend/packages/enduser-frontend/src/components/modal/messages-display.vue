<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="error-messages">
    <message-modal
      v-for="message in messages"
      :key="message.id"
      :type="message.type"
      :text="message.text"
      :title="message.title"
      :index="message.stack"
      @accept="() => closeModal(message.id)"
      @cancel="() => closeModal(message.id)"
    >
    </message-modal>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import { mapGetters, mapActions } from 'vuex'
  import MessageModal from '../modal/message-modal.vue'
  import {
    addErrorListener,
    removeErrorListener
  } from '../../api/error-interceptor'
  import { ApiError } from '../../api/api-error'

  export default Vue.extend({
    name: 'MessageDisplay',
    components: {
      MessageModal
    },
    computed: {
      ...mapGetters({
        messages: 'modals/messages'
      })
    },
    methods: {
      ...mapActions({
        closeModal: 'modals/close',
        displayMessage: 'modals/message'
      }),
      onError(e: ApiError) {
        switch (e.kind) {
          case 'UnknownError':
            this.displayMessage({
              id: 'error',
              type: 'warning',
              title: this.$t('error-modal.unknown-error-title'),
              text: this.$t('error-modal.unknown-error-text')
            })
            break
          case 'UnauthorizedError':
            this.displayMessage({
              id: 'error',
              type: 'warning',
              title: this.$t('error-modal.unauth-error-title'),
              text: this.$t('error-modal.unauth-error-text')
            })
            break
          default:
            const _: never = e
            break
        }
      }
    },
    mounted() {
      addErrorListener(this.onError)
    },
    beforeDestroy() {
      removeErrorListener(this.onError)
    }
  })
</script>
