<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <main>
    <spinner v-if="!decisionsLoaded || !this.decision" />
    <div class="section container" v-else>
      <div class="columns is-centered">
        <c-title :size="2" class="decision-title" textAlign="center">
          {{ texts.label }}
        </c-title>
      </div>
      <div class="columns is-centered">
        <div class="column is-three-quarters">
          <div class="decision-heading">
            <p>{{ texts.text }}</p>
          </div>
          <div class="main-form">
            <div class="columns is-centered">
              <div class="column">
                <div class="download-link">
                  <div>
                    <font-awesome-icon
                      :icon="['far', 'file-pdf']"
                      size="lg"
                    ></font-awesome-icon>
                    <a
                      :href="downloadLink"
                      target="_blank"
                      data-qa="link-open-decision-pdf"
                      >{{ downloadLabel }}</a
                    >
                  </div>
                </div>
              </div>
            </div>
            <div class="columns is-centered">
              <div
                class="is-one-third column form-info-label has-text-right-tablet has-text-left-mobile"
              >
                {{ $t('decision-view.form.childName') }}
              </div>
              <div class="is-two-third column form-info-text" data-qa="decision-child-name-text">
                {{ this.decision.childName }}
              </div>
            </div>
            <div class="columns is-centered">
              <div
                class="is-one-third column form-info-label has-text-right-tablet has-text-left-mobile"
              >
                {{ $t('decision-view.form.clubInfo') }}
              </div>
              <div class="is-two-third column form-info-text">
                {{ this.decision.unit }}
              </div>
            </div>
            <div class="columns is-centered">
              <div
                class="is-one-third column form-info-label has-text-right-tablet has-text-left-mobile"
              >
                {{ $t('decision-view.form.forTime') }}
              </div>
              <div class="is-two-third column form-info-text">
                {{ this.decision.startDate | date }} -
                {{ this.decision.endDate | date }}
              </div>
            </div>
            <div
              class="columns is-centered acceptance-row"
              v-if="canBeAccepted"
            >
              <div
                class="is-one-third column form-info-label has-text-right-tablet has-text-left-mobile acceptance-column"
              >
                {{ $t('decision-view.form.acceptance') }}
              </div>
              <div class="is-two-third column form-info-text decision-radios">
                <div class="radio radio-accept">
                  <input
                    class="input"
                    type="radio"
                    id="enduser_decision_accept"
                    name="enduser_decision"
                    :value="true"
                    v-model="modifiedDecision.accepted"
                    checked
                  />
                  <label
                    class="label label-decision"
                    for="enduser_decision_accept"
                  >
                    <div class="tick">
                      <font-awesome-icon
                        :icon="['far', 'check']"
                      ></font-awesome-icon>
                    </div>
                    {{ $t('decision-view.form.willAccept') }}
                    <span
                      v-if="!canChoosePreferredStartDate"
                      id="preferred-startdate-text"
                    >
                      {{ decision.startDate | date }}
                      {{ $t('decision-view.form.willAcceptStarting') }}
                      <c-instructions
                        :instruction="
                          $t('decision-view.form.startDate-instructions')
                        "
                      />
                    </span>
                  </label>
                </div>
                <div
                  class="date-picker-wrapper"
                  v-if="canChoosePreferredStartDate"
                >
                  <c-datepicker
                    id="decision-application-date-picker"
                    v-model="modifiedDecision"
                    :name="['', 'startDate']"
                    :placeholder="
                      $t('decision-view.form.startDate-placeholder')
                    "
                    :minDate="dateConfig.minDate"
                    :maxDate="dateConfig.maxDate"
                    :iconRight="true"
                    :border="true"
                    :locale="locale"
                  >
                  </c-datepicker>
                  <div class="acceptance-row-info">
                    <span>{{
                      $t('decision-view.form.willAcceptStarting')
                    }}</span>
                    <c-instructions
                      :instruction="$t('decision-view.form.startDateMove')"
                    ></c-instructions>
                  </div>
                </div>

                <div></div>
              </div>
            </div>
            <div class="columns is-centered" v-if="canBeAccepted">
              <div class="is-one-third column form-info-label"></div>
              <div
                class="is-two-third column form-info-text column-radio-reject"
              >
                <div class="radio">
                  <input
                    class="input"
                    type="radio"
                    id="enduser_decision_reject"
                    name="enduser_decision"
                    :value="false"
                    v-model="modifiedDecision.accepted"
                  />
                  <label class="label" for="enduser_decision_reject">
                    <div class="tick">
                      <font-awesome-icon
                        :icon="['far', 'check']"
                      ></font-awesome-icon>
                    </div>
                    {{ $t('decision-view.form.willReject') }}
                  </label>
                </div>
              </div>
            </div>

            <div
              class="columns is-centered acceptance-row"
              v-if="!this.isPending"
            >
              <div
                class="is-one-third column form-info-label has-text-right-tablet has-text-left-mobile"
              >
                {{ $t('decision-view.form.acceptance') }}
              </div>
              <div class="is-two-third column form-info-text">
                <span v-if="isAccepted"
                  >{{ $t('decision-view.decision-accepted') }} ({{
                    this.decision.requestedStartDate | date
                  }}) {{ $t('decision-view.starting') }}
                </span>
                <span v-if="isRejected">{{
                  $t('decision-view.decision-rejected')
                }}</span>
              </div>
            </div>
            <div class="columns is-multiline is-centered buttons">
              <div class="column is-12" v-if="hasBlockingDecision">
                <div class="form-warning-text">
                  <p>{{ $t('decision-view.decision-blocked') }}</p>
                  <c-button
                    :outlined="true"
                    :primary="true"
                    @click="openDecision(blockingDecision)"
                  >
                    {{ $t('decision-view.form.show-blocking-decision') }}
                  </c-button>
                </div>
              </div>
            </div>
            <div class="columns is-mobile is-multiline is-centered buttons">
              <div class="column decision-buttons">
                <c-button
                  :outlined="true"
                  :primary="true"
                  class="is-pulled-right"
                  @click="returnToDecisions"
                  data-qa="btn-return-to-decisions"
                >
                  {{ $t('decision-view.form.return-to-decision') }}
                </c-button>
              </div>
              <div class="column decision-buttons">
                <c-button
                  v-if="canBeAccepted"
                  :primary="true"
                  @click="onDecisionMade"
                  data-qa="btn-send-decision"
                >
                  {{ $t('decision-view.form.btnOk') }}
                </c-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <modal ref="AcceptRejectModal" v-if="this.isPending"></modal>
  </main>
</template>

<script lang="ts">
import Vue from 'vue'
import { mapGetters } from 'vuex'
import Modal from '@/components/modal/decision.vue'
import { addDays } from 'date-fns'
import { parseTzDate } from '@evaka/lib-common/src/utils/date'
import { DecisionDetails } from '@/store/modules/decisions'
import {
  acceptDecision,
  rejectDecision
} from '@/api/decisions'
import { AxiosError } from 'axios'

export default Vue.extend({
  components: {
    Modal
  },
  data() {
    return {
      modifiedDecision: { startDate: null, accepted: true },
      dateConfig: {
        minDate: null,
        maxDate: null
      }
    }
  },
  computed: {
    ...mapGetters(['decisionsLoaded']),
    locale() {
      return this.$i18n.locale
    },
    downloadLabel() {
      return this.$t('decision-view.download', {
        childName: this.decision.childName
      }).toUpperCase()
    },
    decision(): DecisionDetails | undefined {
      return this.$store.getters.decisionDetailsById(this.$route.params.id)
    },
    isDependantDecision() {
      const type: DecisionDetails['type'] = this.decision.type
      return type === 'PRESCHOOL_DAYCARE'
    },
    blockingDecision() {
      if (this.isDependantDecision) {
        return this.$store.getters.decisions.find(
          (d) =>
            d.applicationId === this.decision.applicationId &&
            (d.type === 'PRESCHOOL' || d.type === 'PREPARATORY_EDUCATION') &&
            d.status !== 'ACCEPTED'
        )
      } else {
        return undefined
      }
    },
    hasBlockingDecision() {
      return !!this.blockingDecision
    },
    canBeAccepted() {
      return this.isPending && !this.hasBlockingDecision
    },
    canChoosePreferredStartDate() {
      const type: DecisionDetails['type'] = this.decision.type
      return type !== 'PREPARATORY_EDUCATION' && type !== 'PRESCHOOL'
    },
    isPending() {
      return this.decision && this.decision.status === 'PENDING'
    },
    isAccepted() {
      return this.decision && this.decision.status === 'ACCEPTED'
    },
    isRejected() {
      return this.decision && this.decision.status === 'REJECTED'
    },
    downloadLink() {
      return `/api/application/citizen/decisions/${this.decision.id}/download`
    },
    texts() {
      const type: DecisionDetails['type'] = this.decision.type
      switch (type) {
        case 'CLUB_DECISION':
          return {
            label: this.$t('decision-view.title_club'),
            text: this.$t('decision-view.text_club')
          }
        case 'DAYCARE':
        case 'DAYCARE_PART_TIME':
          return {
            label: this.$t('decision-view.title_daycare'),
            text: this.$t('decision-view.text_daycare')
          }
        case 'PRESCHOOL':
          return {
            label: this.$t('decision-view.title_preschool'),
            text: this.$t('decision-view.text_preschool')
          }
        case 'PRESCHOOL_DAYCARE':
          return {
            label: this.$t('decision-view.title_connecting_daycare'),
            text: this.$t('decision-view.text_preschool')
          }
        case 'PREPARATORY_EDUCATION':
          return {
            label: this.$t('decision-view.title_preparatory'),
            text: this.$t('decision-view.text_preschool')
          }
        default:
          return {
            label: '',
            text: ''
          }
      }
    }
  },
  created() {
    // This only matters when user navigates directly to a specific decision's
    // view instead of going through the list
    if (!this.decisionsLoaded) {
      this.$store.dispatch('loadDecisions')
    }
  },
  methods: {
    onDecisionMade() {
      if (
        this.modifiedDecision.accepted &&
        !this.modifiedDecision.startDate
      ) {
        this.$store.dispatch('modals/message', {
          type: 'error',
          title: this.$t('decision-view.modal.acceptErrorTitleText'),
          text: this.$t('decision-view.modal.no-date-selected')
        })
        return
      }

      this.$refs.AcceptRejectModal.open(this.modifiedDecision.accepted).then(
        this.onModalOkClicked,
        () => {
          // noop
        }
      )
    },
    returnToDecisions() {
      this.$router.push({ name: 'decision-list' })
    },
    onModalOkClicked() {
      if (this.modifiedDecision.accepted) {
        this.doAccept(
          this.decision.applicationId,
          this.decision.id,
          this.decision.type,
          this.modifiedDecision.startDate
        )
          .then(() => {
            this.onResult(true)
          })
          .catch((e: AxiosError) => {
            this.onResult(false, e.response?.data?.errorCode)
          })
      } else {
        this.doReject(this.decision.applicationId, this.decision.id, this.decision.type)
          .then(() => this.onResult(true))
          .catch((e: AxiosError) => this.onResult(false, e.response?.data?.errorCode))
      }
    },
    doAccept(applicationId: string, id: string, type: string, startDate: any) {
      return acceptDecision(applicationId, id, startDate)
    },
    doReject(applicationId: string, id: string, type: string) {
      return rejectDecision(applicationId, id)
    },
    onResult(isSuccessful: boolean, errorCode?: string) {
      this.$refs.AcceptRejectModal.onResult(isSuccessful, errorCode).then(
        this.onModalFinishedClicked
      )
    },
    onModalFinishedClicked() {
      this.$store.dispatch('viewDecisions')
      this.$store.dispatch('loadDecisions')
    },
    setupDates() {
      if (this.decision) {
        const from = this.decision.startDate
        this.dateConfig.minDate = parseTzDate(from)
        this.dateConfig.maxDate = parseTzDate(addDays(new Date(from), 14))
        this.modifiedDecision.startDate = this.decision.startDate
      }
    },
    openDecision(decision) {
      this.$store.dispatch('viewDecision', decision.id)
    }
  },
  mounted() {
    this.setupDates()
  },
  watch: {
    decision() {
      this.setupDates()
    }
  }
})
</script>

<!-- Add "scoped" attribute to limit CSS to this component only -->
<style lang="scss" scoped>
  .decision-title {
    color: $grey-dark;
    margin-bottom: 1rem;
  }

  .decision-heading {
    margin-bottom: 3rem;
    text-align: center;
  }

  .acceptance-row {
    margin-top: 3rem;

    &-info {
      margin-top: 8px;
    }
  }

  .decision-radios {
    display: flex;
    flex-direction: row;

    #preferred-startdate-text {
      margin-left: 0.4rem;
    }

    @include onMobile() {
      flex-direction: column;
    }
    align-items: flex-start;
  }

  .decision-radios .radio {
    margin-bottom: 0;
  }

  .radio-accept {
    margin-top: 2px;
  }

  .form-info-label {
    color: $black;
    font-weight: 600;
    margin: auto 0;
  }

  .date-picker-wrapper {
    display: flex;
  }

  #decision-application-date-picker {
    margin: 0;
    padding: 0;
    width: 9.5rem;

    .clear-dates {
      padding-right: 0;
    }
  }

  .column-radio-reject {
    padding: 0 12px;
  }

  .form-info-text {
    text-align: left;
  }

  .form-warning-text {
    text-align: left;
    border: 2px solid #ffa500;
    border-radius: 3px;
    background: #fedea4;
    max-width: 27rem;
    margin: 0 auto;
    padding: 1rem;
  }

  .decision-btn {
    margin-top: 2.5rem;
    width: 12rem;
  }

  .download-link {
    display: flex;
    justify-content: space-around;
    margin-bottom: 3rem;

    svg {
      margin-right: 10px;
    }

    a {
      font-size: 0.9rem;
      font-weight: 600;
    }
  }

  .decision-buttons {
    .c-button {
      min-width: 14.5rem;
    }
  }

  .label-decision {
    padding-right: 0;
  }

  .buttons {
    margin-top: 2rem;
  }
</style>
<style lang="scss">
  #decision-application-date-picker {
    input {
      padding-left: 0;
      width: 6rem !important;
    }
  }
</style>
