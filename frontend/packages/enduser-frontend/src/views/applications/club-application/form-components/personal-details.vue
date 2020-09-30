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
              :value="child.firstName"
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
              :value="child.lastName"
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
              :value="child.socialSecurityNumber"
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
            @input="childCorrectingAddressSelected"
            name="childHasCorrectingAddress"
            v-model="childHasCorrectingAddress"
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
                :value="model.childMovingDate"
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
              :disabled="!childCorrectingAddress.editable"
              icon="home"
            >
            </address-component>
          </div>
        </div>

        <!--
        <div class="columns">
          <div class="is-one-third column field">
            <label class="label">
              {{$t('form.persons.child.nationality')}}:
            </label>
            <validation
              :name="$t('form.persons.child.nationality')"
              :value="childNationality"
              :validators="getNationalityValidators"
            >
              <select-list
                v-model="childNationality"
                name="childNationality"
                id="childNationality"
                :isRequired="true"
                :options="countries"
                :placeholder="$t('form.persons.child.nationality-placeholder')"
              ></select-list>
            </validation>
          </div>

          <div class="is-one-third column field">
            <validation
              :name="$t('form.persons.child.language')"
              :value="childLanguage"
              :validators="getLanguageValidators"
            >
              <label class="label">
                {{$t('form.persons.child.language')}}:
              </label>
              <select-list
                isDisabled
                v-model="childLanguage"
                name="childLanguage"
                id="list-child-language"
                :isRequired="true"
                :options="languages"
                :placeholder="$t('form.persons.child.language-placeholder')"
              ></select-list>
            </validation>
          </div>
        </div>
        -->
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
              :value="guardian.firstName"
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
              v-model="guardian.lastName"
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
              :value="guardian.socialSecurityNumber"
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
            @input="guardianCorrectingAddressSelected"
            name="guardianHasCorrectingAddress"
            v-model="guardianHasCorrectingAddress"
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
                :value="model.childMovingDate"
                :validators="getValidators('childMovingDate')"
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
              :disabled="!guardianCorrectingAddress.editable"
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
      child: bind('application', 'child'),
      childAddress: bind('application', 'child.address'),
      childHasCorrectingAddress: bind(
        'application',
        'child.hasCorrectingAddress'
      ),
      childCorrectingAddress: bind('application', 'child.correctingAddress'),
      childNationality: bind('application', 'child.nationality'),
      childLanguage: bind('application', 'child.language'),
      guardian: bind('application', 'guardian'),
      guardianAddress: bind('application', 'guardian.address'),
      guardianPhoneNumber: bind('application', 'guardian.phoneNumber'),
      guardianEmail: bind('application', 'guardian.email'),
      guardianHasCorrectingAddress: bind(
        'application',
        'guardian.hasCorrectingAddress'
      ),
      guardianCorrectingAddress: bind(
        'application',
        'guardian.correctingAddress'
      ),
      type: bind('application', 'type'),
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
      childCorrectingAddressSelected(value) {
        if (!value) {
          this.model.childMovingDate = null
          this.updateMovingDate('child', 'childMovingDate', null)
          this.childCorrectingAddress = {
            ...this.childCorrectingAddress,
            street: '',
            city: '',
            postalCode: ''
          }
        }
      },
      guardianCorrectingAddressSelected(value) {
        if (!value) {
          this.model.guardianMovingDate = null
          this.updateMovingDate('guardian', 'guardianMovingDate', null)
          this.guardianCorrectingAddress = {
            ...this.guardianCorrectingAddress,
            street: '',
            city: '',
            postalCode: ''
          }
        }
      },
      setMovingDates(key, value) {
        let movingDate = this.$store.getters.fieldValue(
          'application',
          `${key}.${value}`
        )
        if (movingDate) {
          movingDate = parse(movingDate, 'dd.MM.yyyy', new Date())
          this.model[value] = format(movingDate, 'yyyy-MM-dd')
        }
      },
      onChildMovingDateSelected(val) {
        this.updateMovingDate('child', 'childMovingDate', val)
      },
      onGuardianMovingDateSelected(val) {
        this.updateMovingDate('guardian', 'guardianMovingDate', val)
      },
      updateMovingDate(key, value, inputValue) {
        let formatted = null
        if (inputValue && inputValue[value]) {
          const date = parse(inputValue[value], 'yyyy-MM-dd', new Date())
          formatted = format(date, 'dd.MM.yyyy')
        }
        this.$store.dispatch('updateForm', {
          form: 'application',
          field: `${key}.${value}`,
          value: formatted
        })
      }
    },
    mounted() {
      this.setMovingDates('child', 'childMovingDate')
      this.setMovingDates('guardian', 'guardianMovingDate')
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
