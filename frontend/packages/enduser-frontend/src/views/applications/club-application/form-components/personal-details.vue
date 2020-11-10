<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div class="personal-details">
    <div class="persons-notification columns">
      <div class="column is-three-quarters-tablet">
        <p>
          {{ $t('form.persons.info') }}
        </p>
      </div>
    </div>

    <section class="form-fields-wrapper columns has-text-left">
      <div class="child-info column">
        <c-section-title :icon="['fal', 'child']" id="title-child-information">
           {{ $t('form.persons.child.title') }}
        </c-section-title>

        <div class="columns is-multiline">
          <div class="column is-half-tablet is-one-third-desktop">
            <text-field
              :required="true"
              :value="child.person.firstName"
              name="childFirstName"
              :label="$t('form.persons.child.first-name')"
              :placeholder="$t('form.persons.child.first-name-placeholder')"
              icon="id-card"
              disabled
            >
            </text-field>
          </div>
          <div class="column is-half-tablet is-one-third-desktop">
            <text-field
              :required="true"
              :value="child.person.lastName"
              name="childLastName"
              :label="$t('form.persons.child.last-name')"
              :placeholder="$t('form.persons.child.last-name-placeholder')"
              icon="id-card"
              disabled
            >
            </text-field>
          </div>
          <div class="column is-half-tablet is-one-third-desktop">
            <identity-number
              :value="child.person.socialSecurityNumber"
              :required="true"
              disabled
            >
            </identity-number>
          </div>
        </div>

        <address-component
          v-model="childAddress"
          :disabled="true"
          :isRequired="false"
          icon="home"
        >
        </address-component>

        <div class="address-changed">
          <c-form-checkbox
            @input="toggleChildCorrectingAddress"
            name="childHasCorrectingAddress"
            :value="childHasCorrectingAddress"
            :label="$t('form.persons.new-addr')"
            class="tag is-medium incorrect-address"
            :class="{ active: childHasCorrectingAddress }"
           >
          </c-form-checkbox>
          <c-instructions :instruction="$t('form.persons.new-addr-info')">
          </c-instructions>
          <div class="correcting-address" v-if="childHasCorrectingAddress">
            <div class="changed-address-date">
              <validation
                :name="$t('general.input.moving-date')"
                :value="childMovingDate"
                :validators="getValidators('childMovingDate')"
              >
                <c-datepicker
                  :label="$t('general.input.moving-date')"
                  v-model="model"
                  name="childMovingDate"
                  class="moving-date-datepicker"
                  :required="childHasCorrectingAddress"
                  :placeholder="$t('general.input.moving-date')"
                  @input="onChildMovingDateSelected"
                >
                </c-datepicker>
              </validation>
            </div>

            <address-component
              v-model="childCorrectingAddress"
              :isRequired="true"
              icon="home"
            >
            </address-component>
          </div>
        </div>
      </div>
    </section>

    <hr />

    <!--
      FIRST GUARDIAN
    -->

    <section class="form-fields-wrapper columns has-text-left">
      <div class="guardian-info column">
        <c-section-title :icon="['fal', 'user']">
          {{ $t('form.persons.guardian1.title') }}
        </c-section-title>

        <div class="columns is-multiline">
          <div class="column is-half-tablet is-one-third-desktop">
            <text-field
              :required="true"
              :value="guardian.person.firstName"
              name="guardianFirstName"
              :label="$t('form.persons.guardian1.first-name')"
              :placeholder="$t('form.persons.guardian1.first-name-placeholder')"
              icon="id-card"
              disabled
            >
            </text-field>
          </div>
          <div class="column is-half-tablet is-one-third-desktop">
            <text-field
              :required="true"
              v-model="guardian.person.lastName"
              name="guardianLastName"
              :label="$t('form.persons.guardian1.last-name')"
              :placeholder="$t('form.persons.guardian1.last-name-placeholder')"
              icon="id-card"
              disabled
            >
            </text-field>
          </div>
          <div class="column is-half-tablet is-one-third-desktop">
            <identity-number
              :value="guardian.person.socialSecurityNumber"
              :required="true"
              disabled
            >
            </identity-number>
          </div>
        </div>

        <address-component
          v-model="guardianAddress"
          :isRequired="false"
          :disabled="true"
          icon="home"
        >
        </address-component>

        <div class="address-changed">
          <c-form-checkbox
            @input="toggleGuardianCorrectingAddress"
            name="guardianHasCorrectingAddress"
            :value="guardianHasCorrectingAddress"
            :label="$t('form.persons.new-addr')"
            class="tag is-medium incorrect-address"
            :class="{ active: guardianHasCorrectingAddress }"
          >
          </c-form-checkbox>
          <c-instructions :instruction="$t('form.persons.new-addr-info')">
          </c-instructions>
          <div class="correcting-address" v-if="guardianHasCorrectingAddress">
            <div class="changed-address-date">
              <validation
                :name="$t('general.input.moving-date')"
                :value="guardianMovingDate"
                :validators="getValidators('guardianMovingDate')"
              >
                <c-datepicker
                  :label="$t('general.input.moving-date')"
                  v-model="model"
                  class="moving-date-datepicker"
                  name="guardianMovingDate"
                  :required="guardianHasCorrectingAddress"
                  :placeholder="$t('general.input.moving-date')"
                  @input="onGuardianMovingDateSelected"
                >
                </c-datepicker>
              </validation>
            </div>

            <address-component
              v-model="guardianCorrectingAddress"
              :isRequired="true"
              icon="home"
            >
            </address-component>
          </div>
        </div>

        <div class="columns">
          <div class="column is-half-tablet is-one-third-desktop">
            <phone-number
              id="input-guardian1-phone"
              :label="$t('general.input.tel')"
              v-model="guardianPhoneNumber"
              :required="true"
            >
            </phone-number>
          </div>
          <div class="column is-half-tablet is-one-third-desktop">
            <email-component
              id="input-guardian1-email"
              :label="$t('general.input.email')"
              v-model="guardianEmail"
              icon="at"
            >
            </email-component>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script>
  import { mapGetters } from 'vuex'
  import form, { bind } from '@/mixins/form'
  import { parse, format } from 'date-fns'
  import AddressComponent from '@/components/form-controls/address-component.vue'
  import IdentityNumber from '@/components/form-controls/identity-number.vue'
  import PhoneNumber from '@/components/form-controls/phone-number.vue'
  import EmailComponent from '@/components/form-controls/email-component.vue'
  import TextField from '@/components/form-controls/text-field.vue'
  import Validation from '@/components/validation/validation.vue'
  import { required as requiredValidator } from '@/components/validation/validators.js'

  export default {
    data() {
      return {
        validators: [],
        model: {
          childMovingDate: null,
          guardianMovingDate: null
        }
      }
    },
    mixins: [form],
    props: {
      applicationType: Array
    },
    components: {
      TextField,
      AddressComponent,
      IdentityNumber,
      PhoneNumber,
      EmailComponent,
      Validation
    },
    computed: {
      ...mapGetters([
        'countries',
        'languages'
      ]),
      child: bind('application', 'form.child'),
      childAddress: bind('application', 'form.child.address'),
      childCorrectingAddress: bind('application', 'form.child.futureAddress'),
      childMovingDate: bind('application', 'form.child.futureAddress.movingDate'),
      childNationality: bind('application', 'form.child.nationality'),
      childLanguage: bind('application', 'form.child.language'),
      guardian: bind('application', 'form.guardian'),
      guardianAddress: bind('application', 'form.guardian.address'),
      guardianPhoneNumber: bind('application', 'form.guardian.phoneNumber'),
      guardianEmail: bind('application', 'form.guardian.email'),
      guardianCorrectingAddress: bind(
        'application',
        'form.guardian.futureAddress'
      ),
      guardianMovingDate: bind('application', 'form.guardian.futureAddress.movingDate'),
      type: bind('application', 'type'),
      childHasCorrectingAddress(){
        return this.childCorrectingAddress !== null
      },
      guardianHasCorrectingAddress(){
        return this.guardianCorrectingAddress !== null
      },
      getNationalityValidators() {
        return [requiredValidator]
      },
      getLanguageValidators() {
        return [requiredValidator]
      }
    },
    methods: {
      getValidators(value) {
        return value ? [requiredValidator, ...this.validators] : this.validators
      },
      toggleChildCorrectingAddress(selected) {
        if (selected) {
          this.childCorrectingAddress = {
            movingDate: null,
            street: '',
            postalCode: '',
            postOffice: ''
          }
        } else {
          this.childCorrectingAddress = null
        }
      },
      toggleGuardianCorrectingAddress(selected) {
        if (selected) {
          this.guardianCorrectingAddress = {
            movingDate: null,
            street: '',
            postalCode: '',
            postOffice: ''
          }
        } else {
          this.guardianCorrectingAddress = null
        }
      },
      setMovingDates(key) {
        let movingDate = this.$store.getters.fieldValue(
          'application',
          `form.${key}.futureAddress.movingDate`
        )
        if (movingDate) {
          movingDate = parse(movingDate, 'dd.MM.yyyy', new Date())
          this.model[`${key}MovingDate`] = format(movingDate, 'yyyy-MM-dd')
        }
      },
      onChildMovingDateSelected(val) {
        this.updateMovingDate('child', val)
      },
      onGuardianMovingDateSelected(val) {
        this.updateMovingDate('guardian', val)
      },
      updateMovingDate(key, inputValue) {
        let formatted = null
        if (inputValue && inputValue[`${key}MovingDate`]) {
          const date = parse(inputValue[`${key}MovingDate`], 'yyyy-MM-dd', new Date())
          formatted = format(date, 'dd.MM.yyyy')
        }
        this.$store.dispatch('updateForm', {
          form: 'application',
          field: `form.${key}.futureAddress.movingDate`,
          value: formatted
        })
      }
    },
    mounted() {
      this.setMovingDates('child')
      this.setMovingDates('guardian')
    }
  }
</script>

<style lang="scss" scoped>
  .moving-date-datepicker {
    width: 9.5rem;
  }

  .persons-notification {
    display: inline-block;
    margin: 0.5rem 0 2rem;

    p {
      margin: 0;
    }
  }

  input[type='checkbox'] {
    margin-right: 0.325rem;
    vertical-align: middle;
  }
</style>

<style lang="scss">
  .form-fields-wrapper {
    .columns {
      margin-top: 0;
      .column {
        padding-top: 0;
        padding-bottom: 0;
      }
    }
    .columns:not(:last-child) {
      margin-bottom: 0;
    }

    .control {
      margin-bottom: 0;
    }
  }

  .address-changed {
    margin-top: 0.5rem;

    @media all and (min-width: 1000px) {
      margin-top: 0.325rem;
    }

    .incorrect-address {
      height: auto;
      padding: 0.75rem;
      margin-bottom: 0.75rem;
      border-radius: 4px;
      white-space: normal;

      &.active {
        margin-bottom: 0;
        border-bottom: 1px dashed #bbb;
        border-bottom-left-radius: 0;
        border-bottom-right-radius: 0;
      }

      .checkbox {
        margin-bottom: 0;
      }
    }

    .correcting-address {
      background-color: whitesmoke;
      border-radius: 3px;
      border-top-left-radius: 0;

      .changed-address-date {
        padding: 0.75rem 0.75rem 0;
        font-weight: 500;
      }

      .columns:last-child {
        margin: 0 0 0.75rem;
        padding: 0 0 1rem;
      }
    }
  }
</style>
