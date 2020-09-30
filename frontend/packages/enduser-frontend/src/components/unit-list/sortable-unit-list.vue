<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div>
    <div class="draggable-units" v-if="hasUnits">
      <draggable
        v-model="listModel"
        :options="dragOpts"
        @start="startDrag"
        @end="endDrag"
        class="drag-area"
        :class="{ 'counter-disabled': moving }"
        filter=".remove"
      >
        <div
          class="selected-unit unit-container active"
          v-for="(item, index) in listModel"
          :key="index"
        >
          <c-unit-list-item
            :unitId="item"
            :order="index + 1"
            @remove="removeItem"
          >
          </c-unit-list-item>
        </div>
      </draggable>
    </div>

    <div class="no-selected-units" v-else>
      <div>
        <p class="no-unit-title">
          {{
            $t(
              'form.daycare-application.preferredUnits.units.none-selected.title'
            )
          }}
        </p>
        <p class="no-unit-text">
          {{
            $t(
              'form.daycare-application.preferredUnits.units.none-selected.text'
            )
          }}
        </p>
      </div>
    </div>
  </div>
</template>

<script lang="ts">
  import Vue from 'vue'
  import { mapGetters } from 'vuex'
  import _ from 'lodash'
  import Draggable from 'vuedraggable'

  export default Vue.extend({
    data() {
      return {
        moving: false as boolean
      }
    },
    components: {
      Draggable
    },
    props: {
      value: Array as () => string[]
    },
    computed: {
      ...mapGetters(['applicationUnits']),
      dragOpts(): object {
        return {
          group: 'unitPrefs',
          handle: '.unit',
          forceFallback: true,
          fallbackClass: 'dragged-item'
        }
      },
      hasUnits(): boolean {
        return !this.applicationUnits.loading && !_.isEmpty(this.value)
      },
      listModel: {
        get(): string[] {
          return this.value
        },
        set(value: string[]): void {
          this.$emit('input', value)
        }
      }
    },
    methods: {
      removeItem(id: string): void {
        this.listModel = [...this.listModel.filter((itemId) => itemId !== id)]
        this.$emit('remove', this.listModel)
      },
      startDrag(): void {
        this.moving = true
      },
      endDrag(): void {
        this.moving = false
      }
    }
  })
</script>

<style lang="scss" scoped>
  .sibling-basis {
    margin-bottom: 1rem;
    display: inline-block;
    border: 1px solid $grey-light;
    padding: 0.325rem 1rem;
    border-radius: 1rem;
  }

  .draggable-units {
    padding: 1.25rem;
    background-color: #efefef;
    border-radius: 2px;
    margin-bottom: 0.5rem;

    @include onMobile() {
      padding: 1rem;
    }
  }

  .no-selected-units {
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    padding: 2rem 1rem;
    min-height: 200px;
    background-color: $white-bis;
    text-align: center;

    div {
      max-width: 88%;
      padding: 2rem;
      background-color: white;
      border: 1px dashed $grey-light;
    }
  }

  .draggable-units {
    counter-reset: unit;
    counter: unit;
  }

  .unit-container {
    &.selected,
    &:not(.active) {
      opacity: 0.4;

      .unit {
        border-color: $teal;
      }
    }

    &:not(:last-child) {
      margin-bottom: 0.5rem;
    }
  }

  .counter-disabled .unit:before {
    color: $teal;
  }

  .no-unit {
    &-title,
    &-text {
      color: $grey-dark;
      font-style: italic;
    }
    &-title {
      font-weight: 700;
      margin-bottom: 0;
    }
  }
</style>
