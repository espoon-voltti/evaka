<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <main>
    <div class="section container">
      <!-- Application -->
      <spinner v-if="isLoading" />
      <div v-else class="main-form">
        <form
          id="club-application-form"
          :data-application-id="id"
          @submit.prevent="nop"
          class="container"
        >
          <!-- Form -->

          <div v-if="!showSummary">
            <club-form
              v-if="hasClubType"
              :isLoading="isLoading"
              :type="type"
            ></club-form>

            <section
              class="columns is-centered has-text-centered club-application-buttons"
            >
              <div class="column is-narrow">
                <c-button
                  size="wide"
                  :primary="true"
                  :outlined="true"
                  @click="onCancel"
                >
                  {{ $t('form.cancel') }}
                </c-button>
              </div>
              <div class="column is-narrow" v-if="hasCreatedStatus">
                <c-button
                  size="wide"
                  :primary="true"
                  :outlined="true"
                  @click="onSave"
                  :disabled="spinner"
                  data-qa="btn-save-application"
                  v-if="hasCreatedStatus"
                >
                  {{ $t('form.save-as-draft') }}
                </c-button>
              </div>
              <div class="column is-narrow">
                <c-button
                  size="wide"
                  :primary="true"
                  @click="onSubmit"
                  data-qa="btn-check-and-send"
                  :disabled="spinner"
                >
                  {{ $t('form.preview-and-send') }}
                </c-button>
              </div>
            </section>
          </div>
          <!-- Summary -->
          <div v-if="showSummary">
            <application-summary
              :summaryChecked="summaryChecked"
              @summaryCheckedChanged="summaryCheckedChanged"
            />
            <section class="columns is-centered has-text-centered">
              <div class="column is-narrow">
                <c-button
                  size="wide"
                  :primary="true"
                  :outlined="true"
                  @click="showSummary = false"
                >
                  {{ $t('form.back-to-form') | uppercase }}
                </c-button>
              </div>
              <div class="column is-narrow">
                <c-button
                  size="wide"
                  data-qa="btn-send"
                  :primary="true"
                  @click="onSend"
                  :disabled="!summaryChecked || spinner"
                >
                  {{ $t('form.submit') | uppercase }}
                </c-button>
              </div>
            </section>
          </div>
        </form>
      </div>
    </div>

    <!-- Confirmation dialogues -->
    <confirm
      :header="$t('form.confirmation.exit-form')"
      :acceptText="$t('form.confirmation.accept-exit-form')"
      :rejectText="$t('form.confirmation.reject-exit-form')"
      ref="confirmCancel"
    >
      <div slot="body">
        <div>
          {{ $t('form.dirty') }}
        </div>
        <div>
          {{ lastSavedText }}
        </div>
      </div>
    </confirm>

    <confirm
      :header="$t('form.confirmation.change-application-type')"
      :acceptText="$t('form.confirmation.accept-change-application-type')"
      :rejectText="$t('form.confirmation.reject-change-application-type')"
      ref="confirmTypeChange"
    >
      <div slot="body">
        <ul class="modal-list">
          <li
            :key="notification.id"
            v-for="notification in typeChangeNotifications"
            v-html="$t('form.confirmation.location-removed', { notification })"
          ></li>
        </ul>
      </div>
    </confirm>
  </main>
</template>

<script>
  import { config } from '@evaka/enduser-frontend/src/config'
  import router from '@/router'
  import { mapGetters } from 'vuex'
  import form, { bind } from '@/mixins/form'
  import { formatTime } from '@/utils/date-utils'
  import { APPLICATION_STATUS } from '@/constants'
  import ClubForm from '@/views/applications/club-application/club-form.vue'

  import ApplicationSummary from '@/views/applications/club-application/form-components/summary/application-summary.vue'
  import Confirm from '@/components/modal/confirm.vue'
  import { scrollToTop } from '@/utils/scroll-to-element'

  let saveInterval = null

  export default {
    data() {
      return {
        currentPage: false,
        lastSaved: null,
        spinner: false,
        validationErrors: [],
        typeChangeNotifications: [],
        formTypeSelected: null,
        confirmRouteLeave: true,
        showSummary: false,
        summaryChecked: false,
        loading: false
      }
    },
    mixins: [form],
    components: {
      ClubForm,
      ApplicationSummary,
      Confirm
    },
    computed: {
      ...mapGetters([
        'applicationForm',
        'isShiftCare',
        'hasCarePlan',
        'hasClubType',
        'getChildBirthday',
        'hasErrors',
        'activeTerm'
      ]),
      id() {
        return this.$route.params.id
      },
      type: bind('application', 'type'),
      status: bind('application', 'status'),
      lastSavedText() {
        return this.lastSaved
          ? this.$t('form.last-saved', { time: formatTime(this.lastSaved) })
          : ''
      },
      isDirty: bind('modified', 'application'),
      isLoading() {
        return this.loading
      },
      hasCreatedStatus() {
        return this.status.value === APPLICATION_STATUS.CREATED.value
      },
      isNewApplication() {
        return localStorage.getItem('isNew') === 'true'
      }
    },
    methods: {
      nop() {
        // noop
      },
      save() {
        return this.$store
          .dispatch('saveApplication', {
            type: this.type.value,
            applicationId: this.id,
            form: {}
          })
          .then((id) => {
            this.lastSaved = new Date()
            return id
          })
      },
      saveAndExit() {
        this.spinner = true
        this.save()
          .then((id) => {
            this.confirmRouteLeave = false
            router.push('/applications')
          })
          .finally(() => {
            this.spinner = false
          })
      },
      sendAndExit() {
        this.spinner = true
        this.$store
          .dispatch('sendApplication', {
            type: this.type.value,
            applicationId: this.id,
            form: {}
          })
          .then(() => {
            this.confirmRouteLeave = false
            router.push('/applications')
          })
          .finally(() => {
            this.spinner = false
          })
      },
      deleteForm() {
        this.$store.dispatch('removeApplication', {
          type: this.type.value,
          applicationId: this.id
        })
      },
      onSubmit() {
        this.$store.dispatch('validateAll', this.showSummary)
        if (!this.hasErrors && !this.showSummary) {
          this.showSummary = !this.showSummary
          scrollToTop()
        } else {
          this.showErrorMessage()
        }
      },
      showErrorMessage() {
        this.$store.dispatch('modals/message', {
          type: 'warning',
          title: this.$t('form.error.title'),
          text: this.$t('form.error.text')
        })
      },
      onSave() {
        this.saveAndExit()
      },
      onSend() {
        if (this.summaryChecked) {
          this.hasCreatedStatus ? this.sendAndExit() : this.saveAndExit()
        }
      },
      goBack() {
        this.confirmRouteLeave = false
        return history.go(-1)
      },
      onCancel() {
        this.confirmLeave(this.goBack)
      },
      confirmLeave(onConfirm) {
        const action = this.isNewApplication ? this.deleteForm : this.nop
        const confirmCancel = this.isDirty
          ? this.$refs.confirmCancel.open()
          : Promise.resolve()
        confirmCancel.then(() => {
          action()
          return onConfirm()
        }, this.nop)
      },
      summaryCheckedChanged(value) {
        this.summaryChecked = value
      },
      loadUnits() {
        const date = this.activeTerm.start
        const type = 'CLUB'
        this.$store.dispatch('loadApplicationUnits', { type, date })
      }
    },
    beforeRouteLeave(to, from, next) {
      this.confirmRouteLeave ? this.confirmLeave(next) : next()
    },
    async created() {
      this.loading = true

      await this.$store.dispatch('loadApplication', {
        type: this.type.value,
        applicationId: this.id
      })
      if (this.isNewApplication) {
        saveInterval = setInterval(this.save, config.application.saveIntervalMs)
      }
      this.loading = false
      this.loadUnits()
    },
    beforeDestroy() {
      clearInterval(saveInterval)
    },
    watch: {
      activeTerm() {
        this.loadUnits()
      }
    }
  }
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="scss" scoped>
  .club-heading {
    margin-bottom: 60px;

    .form-guidelines {
      margin-bottom: 2rem;
    }
  }

  .form-guidelines {
    @media screen and (min-width: 1000px) {
      max-width: 75%;
    }
  }

  #application {
    padding: 2rem 0;
  }

  .form-collapse {
    .collapse-item {
      margin-bottom: 2rem;
    }
  }

  .club-application-buttons .column {
    min-width: 18%;

    button {
      width: 100%;
    }
  }

  .buttons {
    button:not(:last-child) {
      margin-bottom: 0.75rem;
    }
  }

  .modal-list {
    list-style: disc;
    list-style-position: inside;
  }
</style>

<style lang="scss">
  .overflow-visible {
    .card-content {
      overflow: visible;
    }
  }

  .with-daycare {
    display: inline-block;
    margin: 1rem 0 0.5rem 1rem;

    @media all and (min-width: 768px) {
      margin: 0 0 0 1rem;
    }

    &.is-active {
      .checkbox {
        &:before {
          border-color: rgba(#3273dc, 0.7);
        }
        &:after {
          border-color: #3273dc;
        }
      }
    }
  }

  .fade-enter-active,
  .fade-leave-active {
    transition: opacity 0.3s;
  }

  .fade-enter, .fade-leave-to /* .fade-leave-active below version 2.1.8 */ {
    opacity: 0;
  }
</style>
