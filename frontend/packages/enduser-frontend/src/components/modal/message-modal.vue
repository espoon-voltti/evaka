<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <Modal
    class="c-modal-wrapper"
    size="small"
    @close="$emit('cancel')"
    :style="{ zIndex: index }"
    :visible="true"
  >
    <div class="c-modal-content">
      <div class="icon-outer-wrapper">
        <div class="icon-container" :class="iconBg">
          <font-awesome-icon :icon="['fal', icon]"></font-awesome-icon>
        </div>
      </div>
      <c-title :size="2" class="c-modal-title">
        {{ title }}
      </c-title>
      <p data-qa="message-modal-text-content">
        {{ text }}
      </p>
    </div>
    <div class="c-modal-controlWrapper">
      <c-button :borderless="true" v-if="cancel" @click="$emit('cancel')">
        {{ $t('general.cancel') }}
      </c-button>
      <c-button
        :borderless="true"
        :primary="true"
        v-if="accept"
        @click="$emit('accept')"
        data-qa="btn-modal-ok"
      >
        {{ $t('general.ok') }}
      </c-button>
    </div>
  </Modal>
</template>

<script>
  import Modal from './modal'

  const MODAL_TYPES = {
    success: {
      icon: 'check',
      bgClass: 'bg-green'
    },
    warning: {
      icon: 'exclamation',
      bgClass: 'bg-orange'
    },
    error: {
      icon: 'times',
      bgClass: 'bg-red'
    }
  }

  export default {
    components: {
      Modal
    },

    props: {
      type: {
        type: String,
        default: 'warning',
        validator: (value) => !!MODAL_TYPES[value]
      },

      title: {
        type: String,
        required: true
      },

      text: {
        type: String,
        default: ''
      },

      cancel: {
        type: Boolean,
        default: false
      },

      accept: {
        type: Boolean,
        default: true
      },

      index: {
        type: Number,
        default: 1
      }
    },

    computed: {
      config: {
        get() {
          return MODAL_TYPES[this.type]
        }
      },

      icon: {
        get() {
          return this.config.icon
        }
      },

      iconBg: {
        get() {
          return this.config.bgClass
        }
      }
    }
  }
</script>
