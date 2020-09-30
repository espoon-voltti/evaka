<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <Modal
    class="c-modal-wrapper"
    size="small"
    @close="close"
    :visible="showModal"
  >
    <div class="c-modal-content">
      <div class="icon-outer-wrapper">
        <div class="icon-container bg-orange">
          <font-awesome-icon :icon="['fal', 'exclamation']"></font-awesome-icon>
        </div>
      </div>
      <c-title :size="2" class="c-modal-title">
        {{ header }}
      </c-title>
      <p>
        {{ message }}
      </p>
    </div>
    <div class="c-modal-controlWrapper">
      <c-button
        :borderless="true"
        :primary="true"
        @click="onReject"
        tabindex="1"
      >
        {{ rejectText }}
      </c-button>
      <c-button
        :borderless="true"
        :primary="true"
        @click="onAccept"
        tabindex="1"
      >
        {{ acceptText }}
      </c-button>
    </div>
  </Modal>
</template>

<script>
  import Modal from '@/components/modal/modal.vue'

  export default {
    props: {
      initialShow: {
        type: Boolean,
        default: false
      },
      header: {
        type: String,
        default: ''
      },
      message: String,
      acceptText: {
        type: String,
        default: ''
      },
      rejectText: {
        type: String,
        default: ''
      }
    },
    data() {
      return {
        showModal: this.initialShow,
        resolution: null,
        resolve: null,
        reject: null
      }
    },
    components: {
      Modal
    },
    methods: {
      resetResolution() {
        this.resolution = new Promise((resolve, reject) => {
          this.resolve = resolve
          this.reject = reject
        })
      },
      open() {
        this.showModal = true
        this.resetResolution()
        return this.resolution
      },
      close() {
        this.showModal = false
        this.$emit('close')
      },
      onAccept() {
        this.close()
        this.$emit('accept')
        this.resolve()
      },
      onReject() {
        this.close()
        this.$emit('reject')
        this.reject()
      }
    },
    created() {
      // Create promise wheter opened with method call or prop
      this.resetResolution()
    }
  }
</script>
