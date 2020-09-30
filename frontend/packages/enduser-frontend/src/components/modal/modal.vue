<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="modal-wrapper" v-transfer-dom="'modal-root'" v-if="visible">
    <Trap>
      <div class="modal-backdrop"></div>
      <div
        v-focus
        tabindex="-1"
        :class="[`modal-container`, `modal-container-${size}`]"
      >
        <div class="modal-innerContent">
          <slot />
        </div>
      </div>
    </Trap>
  </div>
</template>

<script>
  import Trap from 'vue-focus-lock'

  export default {
    name: 'Modal',
    props: {
      size: {
        type: String,
        default: 'small',
        validator: (value) => ['small', 'large'].includes(value)
      },
      visible: {
        type: Boolean,
        default: false,
        required: true
      }
    },
    components: {
      Trap
    }
  }
</script>

<style lang="scss" scoped>
  .modal-wrapper {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 999;

    .modal-container {
      box-shadow: 0 0 70px -20px rgba(0, 0, 0, 0.7);
      position: relative;
      flex-direction: column;
      background-color: white;
      display: flex;

      max-width: 95vw;
      max-height: 98vh;

      overflow-y: auto;

      &-small {
        width: 31rem;
        height: 34rem;
        overflow-x: hidden;

        .modal-innerContent {
          display: flex;
          flex-direction: column;
          flex: 1 1 auto;
          padding: 1.75rem;
        }
      }

      &-large {
        width: 1280px;

        .modal-innerContent {
          padding: 8rem 6rem 4rem 6rem;
        }
      }

      @include onMobile() {
        &-small {
          .modal-innerContent {
            word-wrap: break-word;
            padding: 0.75rem;
          }
        }

        &-large {
          .modal-innerContent {
            word-wrap: break-word;
            padding: 3rem 2rem;
          }
        }
      }
    }
  }

  .modal-backdrop {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
  }
</style>
