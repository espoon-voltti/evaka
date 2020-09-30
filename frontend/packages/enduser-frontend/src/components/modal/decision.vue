<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div>
    <modal v-if="showModal" @close="close" modalClassNames="modal-decision">
      <!-- Header -->
      <span slot="header">
        <slot name="header"
          ><span class="modal-header-row" @click="onReject">X</span></slot
        >
      </span>
      <!-- Body -->
      <div slot="body" class="modal-body-content">
        <div class="round-fg" :class="iconBackground">
          <font-awesome-icon :icon="icon"></font-awesome-icon>
        </div>
        <slot name="body">
          <span class="modal-body-title">{{ title }}</span>
          <span class="modal-body-text">{{ message }}</span>
        </slot>
      </div>
      <!-- Footer -->
      <div slot="footer" class="field is-grouped buttons modal-footer-content">
        <!-- Reject button -->
        <button
          class="button is-primary is-borderless"
          taborder="1"
          id="decision-modal-close-button"
          v-if="initialPhase"
          @click="onReject"
          data-qa="btn-reject"
        >
          {{ cancelButtonText }}
        </button>
        <!-- Accept button -->
        <button
          class="button is-primary is-borderless"
          taborder="0"
          id="decision-modal-ok-button"
          v-if="initialPhase"
          @click="onAccept"
          data-qa="btn-accept"
        >
          {{ acceptText }}
        </button>
        <button
          class="button is-primary is-borderless"
          taborder="2"
          @click="onFinish"
          v-if="!initialPhase"
          data-qa="btn-accept"
        >
          {{ closeText }}
        </button>
      </div>
    </modal>
  </div>
</template>

<script>
  import Modal from '@/components/modal/base-modal.vue'

  export default {
    props: {
      initialShow: {
        type: Boolean,
        default: false
      },

      acceptText: {
        type: String,
        default() {
          return this.$t('decision-view.modal.acceptText')
        }
      },
      rejectText: {
        type: String,
        default() {
          return this.$t('decision-view.modal.rejectText')
        }
      },
      closeText: {
        type: String,
        default() {
          return this.$t('decision-view.modal.closeText')
        }
      }
    },
    data() {
      return {
        willAccept: true,
        initialPhase: true,
        successResult: true,
        errorCode: null,
        showModal: this.initialShow,
        resolution: null,
        resolve: null,
        reject: null
      }
    },
    components: {
      Modal
    },
    computed: {
      icon() {
        if (this.initialPhase) {
          return ['fal', 'exclamation']
        } else {
          if (this.successResult) {
            return ['fal', 'check']
          } else {
            return ['fal', 'times']
          }
        }
      },
      iconBackground() {
        if (this.initialPhase) return 'bg-blue'
        if (this.successResult) return 'bg-green'
        return 'bg-red'
      },
      title() {
        if (this.initialPhase && this.willAccept)
          return this.$t('decision-view.modal.acceptTitleText')
        if (this.initialPhase && !this.willAccept)
          return this.$t('decision-view.modal.rejectTitleText')
        if (this.successResult && this.willAccept)
          return this.$t('decision-view.modal.acceptSuccessfulTitleText')
        if (this.successResult && !this.willAccept)
          return this.$t('decision-view.modal.rejectSuccessfulTitleText')
        return this.$t('decision-view.modal.rejectErrorTitleText')
      },
      message() {
        if (this.initialPhase && this.willAccept)
          return this.$t('decision-view.modal.acceptMessageText')
        if (this.initialPhase && !this.willAccept)
          return this.$t('decision-view.modal.rejectMessageText')
        if (this.successResult && this.willAccept)
          // TODO: return this.$t('decision-view.modal.acceptSuccessfulMessageText')
          return ''
        if (this.successResult && !this.willAccept)
          return this.$t('decision-view.modal.rejectSuccessfulMessageText')
        if (this.errorCode) return this.$t('responses.' + this.errorCode)
        if (this.willAccept)
          return this.$t('decision-view.modal.acceptErrorMessageText')
        return this.$t('decision-view.modal.rejectErrorMessageText')
      },
      cancelButtonText() {
        return this.initialPhase ? this.rejectText : this.closeText
      }
    },
    methods: {
      resetResolution() {
        this.resolution = new Promise((resolve, reject) => {
          this.resolve = resolve
          this.reject = reject
        })
      },
      open(willAccept) {
        this.showModal = true
        this.willAccept = willAccept
        this.resetResolution()
        return this.resolution
      },
      close() {
        this.showModal = false
        this.initialPhase = true
        this.willAccept = true
        this.$emit('close')
      },
      onAccept() {
        this.$emit('accept')
        this.resolve()
      },
      onReject() {
        if (!this.initialPhase) {
          this.onFinish()
        }
        this.close()
        this.$emit('reject')
        this.reject()
      },
      onResult(isSuccessful, errorCode) {
        this.errorCode = errorCode
        this.initialPhase = !this.initialPhase
        this.successResult = isSuccessful
        this.resetResolution()
        return this.resolution
      },
      onFinish() {
        this.$emit('finish')
        this.resolve()
        this.close()
      }
    },
    created() {
      // Create promise wheter opened with method call or prop
      this.resetResolution()
    }
  }
</script>

<style lang="scss">
  .modal-decision {
    .modal-wrapper {
      height: 90vh !important;
      max-height: 34rem !important;
      max-width: 31rem !important;
      width: 90vw !important;
    }

    .modal-container {
      display: grid;
      grid-template-columns: 1fr;
      grid-template-rows: 1fr 6fr 1fr;
      height: 100%;
      overflow-y: auto;
    }

    .modal-header {
      text-align: right;
    }
  }
</style>

<style lang="scss" scoped>
  .modal-header-row {
    color: $grey-dark;
    cursor: pointer;
    padding: 0 0.5rem;
    font-size: 1.5rem;
  }

  .modal-body-content {
    align-items: center;
    display: flex;
    flex-direction: column;
    padding: 0 3.4rem;
  }

  .modal-enter {
    opacity: 0;
  }

  .modal-leave-active {
    opacity: 0;
  }

  .modal-enter .modal-container,
  .modal-leave-active .modal-container {
    -webkit-transform: scale(1.1);
    transform: scale(1.1);
  }

  .svg-inline--fa {
    color: $white;
    font-size: 2em;
    margin-top: 1rem;
    margin-left: 1rem;
    width: 1em;
  }

  .round-fg {
    border-radius: 50%;
    height: 4rem;
    margin: 1rem 0;
    width: 4rem;
  }

  .bg-blue {
    background-color: $blue;
  }

  .bg-green {
    background-color: $green;
  }

  .bg-red {
    background-color: $red;
  }

  .modal-body-title {
    font-size: 2rem;
    font-weight: 100;
    margin: 1rem 0;
    text-align: center;
  }

  .modal-body-text {
    font-size: 1rem;
    margin: 0.6rem 0;
    text-align: center;
  }

  .modal-footer-content {
    justify-content: center;
  }
</style>
