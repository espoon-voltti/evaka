<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div>
    <c-section-title :icon="['fal', 'calendar']">
      {{ $t('form.care.care-season') }}
    </c-section-title>

    <div class="club-care-date columns">
      <div class="is-two-thirds column">
        <div class="apply-term-wrapper">
          <div v-if="hasClubTerms">
            <p class="strong">
              {{ $t('map-filters.apply-term') }}
              <c-instructions
                :instruction="$t('map-filters.apply-term-info')"
              ></c-instructions>
            </p>
            <ClubTermSelect
              :terms="clubTerms"
              :value="termValue"
              @change="selectTerm"
            />
          </div>

          <div v-else>
            <span class="is-italic">{{
              $t('map-filters.no-apply-terms')
            }}</span>
          </div>

          <div
            class="date-picker-wrapper"
            id="club-care-date-picker-wrapper"
            :data-qa-mindate="getMinDate"
          >
            <p class="strong">
              {{ $t('form.care.club-care-date') }}:
              <c-instructions
                :instruction="$t('form.care.club-care-date-info')"
              ></c-instructions>
            </p>
            <validation
              :name="$t('form.care.start-date')"
              :value="model.preferredStartDate"
              :validators="getValidators('preferredStartDate')"
            >
              <c-datepicker
                v-model="model"
                name="preferredStartDate"
                id="club-application-date-picker"
                :minDate="dateConfig.minDate"
                :maxDate="dateConfig.maxDate"
                :disableWeekendDays="true"
                @input="onStartDateSelected"
                :required="true"
              >
              </c-datepicker>
            </validation>
          </div>
        </div>
      </div>
    </div>

    <div class="columns">
      <div class="column">
        <c-form-checkbox
          class="bordered-checkbox"
          v-model="wasOnDaycare"
          :label="$t('form.care.was-on-daycare')"
          :noLabelDots="true"
        >
        </c-form-checkbox>
        <c-instructions
          :instruction="$t('form.care.was-on-daycare-info')"
        ></c-instructions>
      </div>
    </div>
    <div class="columns">
      <div class="column">
        <c-form-checkbox
          class="bordered-checkbox"
          v-model="wasOnClubCare"
          :label="$t('form.care.was-on-club-care')"
          :noLabelDots="true"
        >
        </c-form-checkbox>
        <c-instructions
          :instruction="$t('form.care.was-on-club-care-info')"
        ></c-instructions>
      </div>
    </div>

    <hr class="divider" />

    <div class="club-care-assistance columns">
      <div class="is-two-thirds column">
        <c-section-title :icon="['fal', 'hands']">
          {{ $t('form.care.special-care-title') }}
        </c-section-title>
        <c-form-checkbox
          @input="assistanceNeededSelected"
          class="bordered-checkbox"
          v-model="assistanceNeeded"
          :label="$t('form.care.special-care')"
          :noLabelDots="true"
        >
        </c-form-checkbox>
        <c-instructions
          :instruction="$t('form.care.special-care-info')"
        ></c-instructions>

        <validation
          :name="$t('form.additional.child.assistance-placeholder')"
          :value="assistanceDescription"
          :validators="getValidators(assistanceNeeded)"
        >
          <textarea
            v-if="assistanceNeeded"
            class="assistance-description"
            name="assistanceDescription"
            v-model="assistanceDescription"
            :placeholder="$t('form.additional.child.assistance-placeholder')"
          ></textarea>
        </validation>
      </div>
    </div>
  </div>
</template>

<script>
  import moment from 'moment'
  import { parse, format } from 'date-fns'
  import form, { bind } from '@/mixins/form'
  import { mapGetters } from 'vuex'
  import { parseTzDate } from '@evaka/lib-common/src/utils/date'
  import Validation from '@/components/validation/validation.vue'
  import { required as requiredValidator } from '@/components/validation/validators.js'

  moment.locale('fi')

  export default {
    data() {
      return {
        dateConfig: {
          maxDate: this.datePickerMaxDate,
          minDate: null
        },
        model: {
          preferredStartDate: null
        },
        validators: []
      }
    },
    props: {
      bday: String
    },
    mixins: [form],
    components: {
      Validation
    },
    computed: {
      ...mapGetters(['getChildAge', 'clubTerms', 'selectedTerm', 'activeTerm']),
      wasOnDaycare: bind('application', 'wasOnDaycare'),
      wasOnClubCare: bind('application', 'wasOnClubCare'),
      assistanceNeeded: bind('application', 'careDetails.assistanceNeeded'),
      assistanceDescription: bind(
        'application',
        'careDetails.assistanceDescription'
      ),
      hasClubTerms() {
        return this.clubTerms.length > 0
      },
      termValue() {
        return this.selectedTerm ? this.selectedTerm : this.activeTerm.id
      },
      getMinDate() {
        if (this.dateConfig.minDate) {
          return parse(
            this.dateConfig.minDate,
            this.$const.DATE_FORMAT_DEFAULT,
            new Date()
          )
        }
        return ''
      }
    },
    methods: {
      checkChildAge() {
        this.dateConfig.minDate = moment().format('L')
      },
      selectTerm(term) {
        this.$store.dispatch('updateTerm', term)
      },
      getValidators(value) {
        return value ? [requiredValidator, ...this.validators] : this.validators
      },
      setDates() {
        const minDate = parse(
          this.activeTerm.start,
          this.$const.DATE_FORMAT_DEFAULT,
          new Date()
        )
        const minDateFormatted = parseTzDate(minDate)
        this.dateConfig.minDate = minDate > new Date() ? minDateFormatted : null
        this.dateConfig.maxDate = this.activeTerm.end
        let preferredStartDate = this.$store.getters.fieldValue(
          'application',
          'preferredStartDate'
        )
        if (preferredStartDate) {
          preferredStartDate = parse(
            preferredStartDate,
            this.$const.DATE_FORMAT,
            new Date()
          )
          this.model.preferredStartDate = format(
            preferredStartDate,
            this.$const.DATE_FORMAT_DEFAULT
          )
        }
      },
      assistanceNeededSelected(value) {
        if (!value) {
          this.assistanceDescription = ''
        }
      },
      onStartDateSelected(val) {
        let formatted = null
        if (val.preferredStartDate) {
          const date = parse(
            val.preferredStartDate,
            this.$const.DATE_FORMAT_DEFAULT,
            new Date()
          )
          formatted = format(date, this.$const.DATE_FORMAT)
        }
        this.$store.dispatch('updateForm', {
          form: 'application',
          field: 'preferredStartDate',
          value: formatted
        })
      }
    },
    mounted() {
      this.checkChildAge()
      this.setDates()
    },
    watch: {
      activeTerm: {
        deep: true,
        handler() {
          this.setDates()
        }
      }
    }
  }
</script>

<style lang="scss" scoped>
  #club-application-date-picker {
    width: 9.5rem;
  }

  .date-picker-wrapper {
    margin-top: 1rem;
  }

  .care-tag {
    display: inline-block;
    padding: 0.375rem 0.75rem;
    font-weight: 500;
    color: white;
    background-color: $blue;
    border-radius: 3px;
  }

  .club-care-date {
    margin-bottom: 0;
  }

  .assistance-description {
    min-height: 14rem;
  }

  .has-errors {
    .assistance-description {
      border-color: $color-error-border;
    }
  }
</style>

<style lang="scss">
  .club-care-date {
    // FIX for caret color, needs to be same as header backgroundColor from DATE_PICKER_STYLE
    .flatpickr-calendar.arrowTop:after {
      border-bottom-color: #3273dc !important;
    }
  }
</style>
