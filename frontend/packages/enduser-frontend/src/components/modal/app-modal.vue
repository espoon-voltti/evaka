<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <Modal :size="size" @close="onOverlayClick" :visible="isOpen">
    <slot />
  </Modal>
</template>

<script>
  import Modal from './modal'

  export default {
    components: {
      Modal
    },
    name: 'AppModal',

    data() {
      return {
        resolve: null,
        reject: null
      }
    },

    props: {
      size: {
        type: String,
        default: 'small',
        validator: (value) => ['small', 'large'].includes(value)
      },
      closeOnOverlayClick: {
        type: Boolean,
        default: false
      },
      closeOnEsc: {
        type: Boolean,
        default: true
      }
    },

    computed: {
      isOpen: {
        get() {
          return !!this.resolve && !!this.reject
        }
      }
    },

    methods: {
      open() {
        return new Promise((resolve, reject) => {
          this.resolve = resolve
          this.reject = reject
        }).then(
          (payload) => {
            this.resolve = null
            this.reject = null
            return payload
          },
          (e) => {
            this.resolve = null
            this.reject = null
            return Promise.reject(e)
          }
        )
      },
      onKeyUp(key) {
        if (this.closeOnEsc && key.keyCode === 27) {
          this.rejectModal()
        }
      },
      resolveModal(payload) {
        this.resolve(payload)
      },

      rejectModal(payload) {
        this.reject(payload)
      },

      onOverlayClick() {
        if (this.closeOnOverlayClick) {
          this.rejectModal()
        }
      }
    },
    created() {
      window.addEventListener('keyup', this.onKeyUp)
    },
    beforeDestroy() {
      window.removeEventListener('keyup', this.onKeyUp)
    }
  }
</script>

<style lang="scss" scoped></style>
