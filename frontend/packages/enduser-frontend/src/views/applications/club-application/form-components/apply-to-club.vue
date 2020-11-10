<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div>
    <div class="sibling-basis-wrapper">
      <c-section-title :icon="['fal', 'users']">
        {{ $t('form.apply.sibling-basis-title') }}
      </c-section-title>

      <div class="columns">
        <p
          class="column is-three-quarters-desktop select-unit-info"
          v-html="$t('form.apply.sibling-basis-info')"
        ></p>
      </div>

      <c-tooltip :message="$t('form.apply.sibling-basis-info')"> </c-tooltip>
      <c-form-checkbox
        name="siblingBasis"
        @input="toggleSiblingBasis"
        :value="siblingBasisSelected"
        :noLabelDots="true"
        :label="$t('form.apply.sibling-basis-check')"
        class="sibling-basis"
      >
      </c-form-checkbox>

      <div v-if="siblingBasisSelected" class="sibling-details columns">
        <div class="column is-half">
          <text-field
            v-model="siblingName"
            name="siblingName"
            :leftIcon="['far', 'id-card']"
            :label="$t('form.apply.sibling-name')"
            :placeholder="$t('form.apply.first-surname')"
            :required="siblingBasisSelected"
          >
          </text-field>
        </div>
        <div class="column is-half">
          <identity-number
            :required="siblingBasisSelected"
            :leftIcon="['far', 'id-card']"
            v-model="siblingSsn"
            name="siblingSsn"
            :label="$t('form.apply.sibling-ssn')"
            :placeholder="$t('form.apply.sibling-ssn-label')"
          >
          </identity-number>
        </div>
      </div>
    </div>

    <hr />

    <div class="location-details">
      <c-section-title :icon="['fal', 'map-marker-alt']">
        {{ $t('form.care.title') }}
      </c-section-title>

      <div class="columns">
        <p
          class="column is-three-quarters-desktop select-unit-info"
          v-html="$t('form.apply.general-info')"
        ></p>
      </div>

      <spinner v-if="isLoading"></spinner>
      <div class="columns is-desktop" v-else>
        <section class="column">
          <div class="unit-selector-wrapper">
            <h4 class="title is-4">
              {{ $t('form.apply.location-and-group-title') }}
              <!-- {{unitSelectorLabel}}: -->
            </h4>
            <p>{{ $t('form.apply.location-and-group-description') }}</p>

            <daycare-select-list
              id="list-select-daycare"
              :selected="selectedUnits"
              :options="applicationUnits.data"
              :disabled="applicationUnits.loading || maxUnitsSelected"
              :placeholder="$t('form.apply.select-club')"
              @select="selectUnit"
            />
          </div>
        </section>

        <!-- Selected units -->
        <section class="is-half-desktop column">
          <h4 class="title is-4">
            {{ $t('form.apply.preferred-locations') }}
          </h4>
          <validation
            :name="unitSelectorLabel"
            :value="selectedUnits"
            :validators="getUnitSelectionValidators"
          >
            <sortable-unit-list
              v-model="selectedUnits"
              :value="selectedUnits"
            />

            <div class="has-text-centered select-info-wrapper">
              <p class="select-info min-limit">
                {{
                  $t('form.apply.select-location-info', {
                    minSelections,
                    maxSelections
                  })
                }}
              </p>
              <p class="select-info">
                {{ $t('form.apply.change-order-drag') }}
              </p>
              <p v-if="maxUnitsSelected" class="select-info limit-reached">
                {{ $t('form.apply.select-location-max-selected') }}
              </p>
            </div>
          </validation>
        </section>
      </div>
    </div>
  </div>
</template>

<script>
  import _ from 'lodash'
  import form, { bind } from '@/mixins/form'
  import DaycareSelectList from '@/views/applications/daycare-application/daycare-select-list.vue'
  import TextField from '@/components/form-controls/text-field.vue'
  import { mapGetters } from 'vuex'
  import { applicationTypeToDaycareTypes } from '@/constants'
  import SortableUnitList from '@/components/unit-list/sortable-unit-list.vue'
  import Validation from '@/components/validation/validation.vue'
  import { required } from '@/components/validation/validators.js'
  import IdentityNumber from '@/components/form-controls/identity-number'

  export default {
    data() {
      return {
        ptype: 1,
        sibling: {
          name: ''
        }
      }
    },
    mixins: [form],
    components: {
      IdentityNumber,
      DaycareSelectList,
      TextField,
      SortableUnitList,
      Validation
    },
    props: {
      type: String,
      isLoading: Boolean,
      clubs: Array,
      validator(value) {
        const allowedTypes = _.values(this.$const.APPLICATION_TYPE)
        return _.includes(allowedTypes, value)
      }
    },
    computed: {
      ...mapGetters(['hasServiceNeed', 'activeTerm', 'applicationUnits']),
      siblingBasis: bind('application', 'form.preferences.siblingBasis'),
      siblingName: bind('application', 'form.preferences.siblingBasis.siblingName'),
      siblingSsn: bind('application', 'form.preferences.siblingBasis.siblingSsn'),
      selectedUnits: bind('application', 'form.preferences.preferredUnits'),
      preliminaryCourse: bind('application', 'form.preferences.preparatory'),
      siblingBasisSelected() {
        return this.siblingBasis !== null
      },
      unitSelectorLabel() {
        return this.$t('form.apply.preferred-locations')
      },
      showDragInfo() {
        return this.selectedUnits.length > 1
      },
      maxSelections() {
        return this.$const.MAX_PREFERRED_UNITS
      },
      minSelections() {
        return this.$const.MIN_PREFERRED_UNITS
      },
      maxUnitsSelected() {
        return this.selectedUnits.length >= this.$const.MAX_PREFERRED_UNITS
      },
      applicationType() {
        return applicationTypeToDaycareTypes(this.type) || []
      },
      getUnitSelectionValidators() {
        return [required]
      }
    },
    methods: {
      selectUnit(club) {
        if (this.canSelect(club.id)) {
          this.selectedUnits = _.concat(this.selectedUnits, club.id)
        }
      },
      removeSelection(unitId) {
        this.selectedUnits = _.difference(this.selectedUnits, [unitId])
      },
      canSelect(id) {
        return !(this.maxUnitsSelected || this.isSelected(id))
      },
      isSelected(id) {
        return _.includes(this.selectedUnits, id)
      },
      toggleSiblingBasis(selected) {
        this.siblingBasis = selected ? {
          siblingName: '',
          siblingSsn: ''
        } : null
      }
    },
    watch: {
      activeTerm: {
        handler() {
          this.selectedUnits = []
        }
      }
    }
  }
</script>

<style lang="scss" scoped>
  .sibling-basis-wrapper {
    margin-bottom: 1rem;
  }

  .sibling-basis {
    display: inline-block;
    border: 1px solid #ddd;
    padding: 0.5rem 1rem;
    border-radius: 0.5rem;
    margin-bottom: 1rem;
  }

  .unit-selector-wrapper {
    margin-bottom: 1rem;
  }

  .select-info-wrapper {
    margin: 0 auto;
  }

  .select-info {
    color: #666;
    font-style: italic;
    font-size: 85%;
    display: block;

    &.min-limit {
      margin-top: 1rem;
    }

    &.limit-reached {
      color: $red;
      margin-top: 1rem;
    }
  }

  .care-types {
    margin-top: 1rem;
  }

  .select-from-map {
    margin-bottom: 1rem;
  }

  .open-map-link {
    display: block;
    margin: 1rem 0;
  }
</style>
