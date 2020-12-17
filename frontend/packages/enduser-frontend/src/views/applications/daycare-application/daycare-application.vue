<!--
SPDX-FileCopyrightText: 2017-2020 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

<template>
  <div id="daycare-preschool-application">
    <div v-if="!showSummary" id="daycare-preschool-form">
      <c-view
        :title="$t(`form.${type}-application.title`)"
        :text="$t(`form.${type}-application.text`)"
      >
        <spinner v-show="isLoading" />

        <div class="columns" v-if="isChecked && !isValid">
          <div class="column">
            <c-warning-box data-qa="validation-errors">
              <div>
                <p>{{ $t('form.validation-instruction') }}:</p>
                <ul class="validation-error-items">
                  <li
                    v-for="(item, name, index) in errorSections"
                    v-bind:key="index"
                  >
                    {{ $t(`form.${type}-application.${name}.title`) }}
                    <ul>
                      <li
                        v-for="(value, index) in item"
                        v-bind:key="value + index"
                      >
                        {{ value }}
                      </li>
                    </ul>
                  </li>
                </ul>
              </div>
            </c-warning-box>
          </div>
        </div>

        <c-form
          v-show="!isLoading"
          ref="form"
          name="vaka"
          role="tablist"
          :data="model"
          v-model="validations"
          id="daycare-application-form"
          :data-application-id="id"
        >
          <!-- HIDE FOR NOW -->
          <!--<div class="columns">-->
          <!--<div class="column">-->
          <!--<c-checkbox v-model="model" name="moveToAnotherUnit">-->
          <!--{{ $t('form.daycare-application.requestAnotherUnit.label') }}-->
          <!--<c-instructions-->
          <!--:instruction="$t('form.daycare-application.requestAnotherUnit.instructions.text')"-->
          <!--/>-->
          <!--</c-checkbox>-->
          <!--</div>-->
          <!--</div>-->
          <CForm :data="model" v-model="validations" :name="sections.service">
            <c-form-section
              :title="$t(`form.${type}-application.service.title`)"
              data-qa="service-section"
              :isOpen="this.activeSection === sections.service"
              :data-open="this.activeSection === sections.service"
              @toggle="toggleActiveSection(sections.service)"
              :errors="errors.service"
            >
              <c-section-title :icon="['fal', 'calendar']">{{
                $t(`form.${type}-application.service.section-title`)
              }}</c-section-title>
              <div class="columns is-multiline">
                <c-text-content
                  v-if="isPreschool"
                  :text="`
                    ${$t(
                      'form.preschool-application.service.startDate.description'
                    )} ${$t(
                    'form.preschool-application.service.startDate.description2'
                  )}
                  `"
                  :asHtml="true"
                  class="column is-12 startdate-description"
                />
                <div class="text-content column is-12">
                  {{
                    withAsterisk(
                      $t(`form.${type}-application.service.startDate.label`)
                    )
                  }}
                  <c-instructions
                      :instruction="
                        $t(
                          `form.${type}-application.service.startDate.instructions.text`
                        )
                      "
                    />
                </div>
                <div class="column column-startdate" v-if="startDateShouldBeEditable" data-qa="preferred-startdate-datepicker">
                  <c-datepicker
                    id="daycare-preschool-datepicker"
                    v-model="model"
                    :required="true"
                    :minDate="minimumStartdate"
                    :max-date="maximumStartDate"
                    name="preferredStartDate"
                    :iconRight="true"
                    :border="true"
                    :placeholder="
                      $t(
                        `form.${type}-application.service.startDate.placeholder`
                      )
                    "
                    :locale="locale"
                    :validationName="
                      $t(`form.${type}-application.service.startDate.validationText`)
                    "
                  >
                    <c-instructions
                      v-if="!isPreschool"
                      :instruction="
                        $t(
                          `form.${type}-application.service.startDate.instructions.text`
                        )
                      "
                    />
                  </c-datepicker>
                  <c-message-box
                    title
                    :narrow="true"
                    v-if="startdateIsUnderFourMonths && !isPreschool"
                  >
                    <div>
                      {{
                        $t(
                          `form.${type}-application.service.startDate.noteOnDelay`
                        )
                      }}
                    </div>
                  </c-message-box>
                </div>
                <div v-else class="column is-4">
                  <c-input :value="formattedStartDate" :disabled="true" />
                </div>
              </div>
              <div class="columns" v-if="!isPreschool">
                <div class="column">
                  <c-checkbox
                    v-model="model"
                    name="urgent"
                    :label="
                      $t('form.daycare-application.service.expedited.label')
                    "
                  ></c-checkbox>
                  <div v-if="model.urgent">
                    <div v-if="attachmentsEnabled" class="attachments-message">
                      <p>
                        {{
                          $t(
                            `form.${type}-application.service.expedited.attachments-message.text`
                          )
                        }}
                      </p>
                      <p class="bold">
                        {{
                          $t(
                            `form.${type}-application.service.expedited.attachments-message.subtitle`
                          )
                        }}
                      </p>
                      <file-upload
                        :files="urgentFiles"
                        data-qa="file-upload-urgent-attachments"
                        :onUpload="uploadUrgencyAttachment"
                        :onDelete="deleteUrgencyAttachment"
                      />
                    </div>
                    <c-message-box
                      v-else
                      :title="
                        $t(
                          'form.daycare-application.service.expedited.message.title'
                        )
                      "
                    >
                      <div
                        v-html="
                          $t(
                            `form.${type}-application.service.expedited.message.text`
                          )
                        "
                      ></div>
                    </c-message-box>
                  </div>
                </div>
              </div>

              <hr />

              <c-section-title :icon="['fal', 'clock']">{{
                $t(`form.${type}-application.service.dailyTitle`)
              }}</c-section-title>
              <c-text-content
                :asHtml="true"
                :text="$t(`form.${type}-application.service.dailyText`)"
              />

              <div class="columns" v-if="isPreschool">
                <div class="column">
                  <c-checkbox
                    v-model="model"
                    name="connectedDaycare"
                    :label="
                      $t(
                        'form.preschool-application.service.connectedDaycare.label'
                      )
                    "
                  >
                    <c-instructions
                      :instruction="
                        $t(
                          'form.preschool-application.service.connectedDaycare.instructions.text'
                        )
                      "
                    />
                  </c-checkbox>
                </div>
              </div>

              <div v-if="!isPreschool">
                <c-radio
                  v-model="model"
                  name="partTime"
                  :inputValue="true"
                  :label="
                    $t(`form.daycare-application.service.dailyTime.partTime`)
                  "
                ></c-radio>

                <c-radio
                  v-model="model"
                  name="partTime"
                  :inputValue="false"
                  :label="
                    $t(`form.daycare-application.service.dailyTime.fullTime`)
                  "
                ></c-radio>
              </div>

              <div v-if="showDailyTime">
                <div class="columns">
                  <div class="column">
                    {{ $t(`form.${type}-application.service.dailyTime.label`) }}
                    <c-instructions
                      :instruction="
                        $t(
                          `form.${type}-application.service.dailyTime.instructions.text`
                        )
                      "
                    />
                    <div class="columns columns-timepicker">
                      <div class="column is-narrow">
                        <c-time-input
                          id="daycare-start"
                          v-model="model"
                          name="serviceStart"
                          :required="true"
                          :leftIcon="['fal', 'clock']"
                          :validationName="
                            $t(
                              `form.daycare-application.service.dailyTime.starts.label`
                            )
                          "
                          :placeholder="
                            $t(
                              'form.daycare-application.service.dailyTime.starts.instruction'
                            )
                          "
                        ></c-time-input>
                      </div>
                      <div class="column is-narrow">
                        <c-time-input
                          id="daycare-end"
                          v-model="model"
                          name="serviceEnd"
                          :leftIcon="['fal', 'clock']"
                          :required="true"
                          :validationName="
                            $t(
                              `form.daycare-application.service.dailyTime.ends.label`
                            )
                          "
                          :placeholder="
                            $t(
                              'form.daycare-application.service.dailyTime.ends.instruction'
                            )
                          "
                        ></c-time-input>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="columns">
                  <div class="column">
                    <c-checkbox
                      v-model="model"
                      name="extendedCare"
                      :label="
                        $t(`form.${type}-application.service.extended.label`)
                      "
                    >
                      <c-instructions
                        :instruction="
                          $t(
                            `form.${type}-application.service.extended.instructions.text`
                          )
                        "
                      />
                    </c-checkbox>
                    <div v-if="model.extendedCare">
                      <div v-if="attachmentsEnabled" class="attachments-message">
                      <p>
                        {{
                          $t(
                            `form.${type}-application.service.extended.attachments-message.text`
                          )
                        }}
                      </p>
                      <p class="bold">
                        {{
                          $t(
                            `form.${type}-application.service.extended.attachments-message.subtitle`
                          )
                        }}
                      </p>
                      <file-upload
                        :files="extendedCareFiles"
                        data-qa="file-upload-extended-care-attachments"
                        :onUpload="uploadExtendedCareAttachment"
                        :onDelete="deleteExtendedCareAttachment"
                      />
                    </div>
                      <c-message-box
                        v-else
                        :title="
                          $t(
                            'form.daycare-application.service.extended.message.title'
                          )
                        "
                      >
                        <div
                          v-html="
                            $t(
                              `form.${type}-application.service.extended.message.text`
                            )
                          "
                        ></div>
                      </c-message-box>
                    </div>
                  </div>
                </div>
              </div>

              <hr />

              <c-section-title :icon="['fal', 'hands']">
                {{
                  $t(`form.${type}-application.service.clubCare.section-title`)
                }}
              </c-section-title>

              <div
                class="columns"
                v-if="isPreschool && !currentLocaleIsSwedish"
              >
                <div class="column">
                  <c-checkbox
                    v-model="model"
                    :name="['careDetails', 'preparatory']"
                    @input="onPreparatoryChanged"
                    :label="
                      $t(
                        'form.preschool-application.service.clubCare.preparatory.label'
                      )
                    "
                  >
                    <c-instructions
                      :instruction="
                        $t(
                          'form.preschool-application.service.clubCare.preparatory.instructions.text'
                        )
                      "
                    />
                  </c-checkbox>
                </div>
              </div>

              <div class="columns">
                <div class="column">
                  <c-checkbox
                    v-model="model"
                    :name="['careDetails', 'assistanceNeeded']"
                    @input="onAssistanceNeededChanged"
                    :label="
                      $t(`form.${type}-application.service.clubCare.label`)
                    "
                  >
                    <c-instructions
                      :instruction="
                        $t(
                          `form.${type}-application.service.clubCare.instructions.text`
                        )
                      "
                    />
                  </c-checkbox>
                  <div v-if="model.careDetails.assistanceNeeded">
                    <c-text-area
                      v-model="model"
                      :name="['careDetails', 'assistanceDescription']"
                      :placeholder="
                        $t(
                          `form.${type}-application.service.clubCare.placeholder`
                        )
                      "
                    ></c-text-area>
                  </div>
                </div>
              </div>
            </c-form-section>
          </CForm>
          <CForm
            :data="model"
            v-model="validations"
            :name="sections.preferredUnits"
          >
            <c-form-section
              :title="$t('form.daycare-application.preferredUnits.title')"
              data-qa="preferred-units-section"
              :isOpen="this.activeSection === sections.preferredUnits"
              :data-open="this.activeSection === sections.preferredUnits"
              @toggle="toggleActiveSection(sections.preferredUnits)"
              :errors="errors.preferredUnits"
            >
              <c-section-title :icon="['fal', 'users']">{{
                $t(`form.${type}-application.siblingBasis.title`)
              }}</c-section-title>

              <c-text-content
                v-if="!isPreschool"
                :text="
                  $t('form.daycare-application.preferredUnits.siblings.text')
                "
              />

              <div v-if="isPreschool">
                <div
                  v-for="(item,
                  index) in $t(
                    'form.preschool-application.preferredUnits.siblings.text',
                    { returnObjects: true }
                  )"
                  :key="index"
                >
                  <p v-html="item" class="preschool-sibling-basis"></p>
                </div>
              </div>

              <div class="columns">
                <div class="column">
                  <c-checkbox
                    v-model="model"
                    :name="['apply', 'siblingBasis']"
                    @input="onSiblingBasisClicked()"
                    :label="`${$t(
                      `form.${type}-application.preferredUnits.siblings.select.label`
                    )}`"
                  ></c-checkbox>
                </div>
              </div>
              <div class="columns" v-if="model.apply.siblingBasis">
                <div class="column is-6">
                  <c-input
                    :required="true"
                    value
                    :leftIcon="['far', 'id-card']"
                    v-model="model"
                    :name="['apply', 'siblingName']"
                    :label="
                      $t(
                        'form.daycare-application.preferredUnits.siblings.input.label'
                      )
                    "
                    :placeholder="
                      $t(
                        'form.daycare-application.preferredUnits.siblings.input.placeholder'
                      )
                    "
                  />
                </div>
                <div class="column is-6">
                  <c-ssn-input
                    :required="true"
                    value
                    :leftIcon="['far', 'id-card']"
                    v-model="model"
                    :name="['apply', 'siblingSsn']"
                    :label="
                      $t(
                        'form.daycare-application.preferredUnits.siblings.input.ssn-label'
                      )
                    "
                    :placeholder="
                      $t(
                        'form.daycare-application.preferredUnits.siblings.input.ssn-placeholder'
                      )
                    "
                  />
                </div>
              </div>

              <hr />

              <div class="form-input-section">
                <c-section-title :icon="['fal', 'map-marker-alt']">
                  {{
                    withAsterisk(
                      $t(
                        'form.daycare-application.preferredUnits.units.section-title'
                      )
                    )
                  }}
                </c-section-title>
                <c-text-content
                  :text="
                    $t(`form.${type}-application.preferredUnits.units.text`)
                  "
                  class="preferredunits-text-content"
                  :asHtml="true"
                />

                <div class="columns is-multiline">
                  <div
                    class="column is-half-desktop is-full form-preferred-units-section"
                  >
                    <c-title :size="3">
                      {{
                        $t(
                          'form.daycare-application.preferredUnits.select.title'
                        )
                      }}
                    </c-title>

                    <div class="columns is-multiline">
                      <div class="column">
                        <p>
                          {{
                            $t(
                              'form.daycare-application.preferredUnits.select.language'
                            )
                          }}
                        </p>
                        <div class="chipses">
                          <c-chip
                            v-for="(lang, index) in languages"
                            :key="index"
                            v-model="language"
                            :text="lang.label"
                            :inputValue="lang.value"
                          />
                        </div>
                      </div>
                    </div>

                    <c-text-content
                      :text="
                        $t(
                          'form.daycare-application.preferredUnits.select.text'
                        )
                      "
                    />

                    <daycare-select-list
                      id="list-select-daycare"
                      :selected="preferredUnits"
                      :options="
                        applicationUnits.data | filterByLanguage(language)
                      "
                      :disabled="applicationUnits.loading || maxUnitsSelected"
                      :placeholder="
                        withAsterisk($t('form.apply.select-daycare'))
                      "
                      @select="addUnit"
                    />
                  </div>
                  <div class="column is-half-desktop is-full selected-units">
                    <c-title :size="3">
                      {{
                        $t(
                          'form.daycare-application.preferredUnits.select.selectedTitle'
                        )
                      }}
                    </c-title>

                    <sortable-unit-list
                      v-model="model.apply.preferredUnits"
                      @remove="removeUnit"
                    />

                    <div class="has-text-centered select-info-wrapper">
                      <p class="select-info min-limit">
                        {{
                          $t(
                            'form.daycare-application.preferredUnits.units.selected-info'
                          )
                        }}
                      </p>
                      <p class="select-info">
                        {{ $t('form.apply.change-order-drag') }}
                      </p>
                      <p
                        v-if="maxUnitsSelected"
                        class="select-info limit-reached"
                      >
                        {{ $t('form.apply.select-location-max-selected') }}
                      </p>
                    </div>

                    <span
                      class="help is-danger"
                      v-if="!isValid && preferredUnits.length === 0"
                      >{{ preferredUnitsValidationText() }}</span
                    >

                    <c-info-box v-if="isPreschool" id="outsourced-service-info">
                      {{
                        $t(
                          'form.preschool-application.preferredUnits.outsourced-service-info'
                        )
                      }}
                    </c-info-box>
                  </div>
                </div>
              </div>
            </c-form-section>
          </CForm>
          <CForm
            :data="model"
            v-model="validations"
            :name="sections.personalInfo"
          >
            <c-form-section
              :title="$t('form.daycare-application.personalInfo.title')"
              data-qa="personal-info-section"
              :isOpen="this.activeSection === sections.personalInfo"
              :data-open="this.activeSection === sections.personalInfo"
              @toggle="toggleActiveSection(sections.personalInfo)"
              :errors="errors.personalInfo"
            >
              <c-text-content
                :text="$t('form.daycare-application.personalInfo.text')"
                :asHtml="true"
              />

              <c-section-title :icon="['fal', 'child']">{{
                $t('form.persons.child.title')
              }}</c-section-title>

              <child-info
                :required="false"
                :disablePersonFields="true"
                v-model="model"
                name="child"
              />

              <hr />

              <c-section-title :icon="['fal', 'user']">{{
                $t('form.persons.guardian1.title')
              }}</c-section-title>

              <guardian-info
                :required="false"
                :disablePersonFields="true"
                v-model="model"
                name="guardian"
              />

              <c-info-box bold>
                {{
                  $t(
                    'form.daycare-application.personalInfo.guardian-required-details'
                  )
                }}
              </c-info-box>

              <hr />

              <div class="form-input-section">
                <c-section-title :icon="['fal', 'user']">{{
                  $t('form.persons.guardian2.title')
                }}</c-section-title>

                <div class="columns">
                  <div v-if="hasOtherVtjGuardianInSameAddress" class="column">
                    <c-content class="is-italic">
                      {{
                        $t(
                          'form.persons.guardian2.other-vtj-guardian-exists-info'
                        )
                      }}
                    </c-content>
                  </div>

                  <div v-else>
                    <div v-if="model.hasOtherVtjGuardian" class="column">
                      <c-content
                        class="is-italic"
                        data-qa="other-guardian-different-address-label"
                      >
                        {{
                          $t(
                            'form.persons.guardian2.other-vtj-guardian-exists-different-address-info'
                          )
                        }}
                      </c-content>

                      <c-radio
                        v-model="model"
                        name="otherGuardianAgreementStatus"
                        inputValue="AGREED"
                        :label="
                          $t(`form.persons.guardian2.agreement-status.AGREED`)
                        "
                        data-qa="agreement-status-agreed"
                      ></c-radio>

                      <c-radio
                        v-model="model"
                        name="otherGuardianAgreementStatus"
                        inputValue="NOT_AGREED"
                        :label="
                          $t(
                            `form.persons.guardian2.agreement-status.NOT_AGREED`
                          )
                        "
                        data-qa="agreement-status-not-agreed"
                      ></c-radio>

                      <c-radio
                        v-model="model"
                        name="otherGuardianAgreementStatus"
                        inputValue="RIGHT_TO_GET_NOTIFIED"
                        :label="
                          $t(
                            `form.persons.guardian2.agreement-status.RIGHT_TO_GET_NOTIFIED`
                          )
                        "
                        data-qa="agreement-status-right-to-get-notified"
                      ></c-radio>
                      <span
                        class="help is-danger"
                        v-if="
                          !isValid &&
                          model.otherGuardianAgreementStatus === null
                        "
                      >
                        {{
                          $t(`form.persons.guardian2.agreement-status.NOT_SET`)
                        }}
                      </span>

                      <div
                        class="columns other-guardian-info"
                        v-if="isOtherGuardianInfoNeeded"
                      >
                        <div class="column is-4">
                          <c-tel-input
                            :required="false"
                            v-model="model"
                            :name="['guardian2', 'phoneNumber']"
                            :label="
                              $t('form.persons.guardian2.other-guardian-phone')
                            "
                            :leftIcon="['far', 'mobile-alt']"
                            data-qa="other-guardian-tel"
                          />
                        </div>

                        <div class="column is-6">
                          <c-email-input
                            v-model="model"
                            :name="['guardian2', 'email']"
                            :label="
                              $t('form.persons.guardian2.other-guardian-email')
                            "
                            :leftIcon="['far', 'at']"
                            :required="false"
                            data-qa="other-guardian-email"
                          />
                        </div>
                      </div>
                    </div>
                    <div v-else>
                      <c-content class="is-italic">
                        {{
                          $t(
                            'form.persons.guardian2.other-vtj-guardian-does-not-exists-info'
                          )
                        }}
                      </c-content>
                    </div>
                  </div>
                </div>
              </div>

              <div class="form-input-section" v-if="showDailyTime">
                <c-section-title :icon="['fal', 'user']">{{
                  $t('form.persons.other-adults.title')
                }}</c-section-title>

                <c-checkbox
                  v-model="model"
                  name="hasOtherAdults"
                  :label="
                    $t('form.persons.other-adults.add-other-adult-daycare')
                  "
                  @input="onOtherAdultsChanged"
                >
                  <c-instructions
                    :instruction="
                      $t(
                        'form.daycare-application.personalInfo.otherPersonsInstructions'
                      )
                    "
                  />
                </c-checkbox>

                <c-content v-if="model.hasOtherAdults">
                  <div class="columns">
                    <div class="column">
                      <Person
                        :required="true"
                        v-model="model"
                        :name="`otherAdults[0]`"
                        :isChild="false"
                        :isOther="true"
                      />
                    </div>
                  </div>
                </c-content>
              </div>

              <div class="form-input-section" v-if="showDailyTime">
                <c-section-title :icon="['fal', 'child']">{{
                  $t('form.persons.other-children-section.title')
                }}</c-section-title>

                <c-checkbox
                  v-model="model"
                  name="hasOtherChildren"
                  :label="
                    $t('form.persons.other-children-section.label-daycare')
                  "
                >
                  <c-instructions
                    :instruction="
                      $t(
                        'form.daycare-application.personalInfo.otherChildrenInstructions'
                      )
                    "
                  />
                </c-checkbox>

                <c-content v-if="model.hasOtherChildren">
                  <div
                    v-for="(child, index) in model.otherChildren"
                    :key="index"
                    class="columns"
                  >
                    <div class="column">
                      <Person
                        :required="true"
                        v-model="model"
                        :name="`otherChildren[${index}]`"
                      />
                    </div>
                    <div class="column is-narrow">
                      <c-button
                        :primary="true"
                        :borderless="true"
                        @click="removeOtherChildren(index)"
                        >{{ $t('general.remove') }}</c-button
                      >
                    </div>
                  </div>

                  <c-button
                    :primary="true"
                    :borderless="true"
                    @click="addOtherChildren"
                    >{{ $t('general.add-new') }}</c-button
                  >
                </c-content>
              </div>
            </c-form-section>
          </CForm>

          <!-- Suostumus korkeimpaan maksuun -->
          <CForm
            :data="model"
            v-model="validations"
            :name="sections.additional"
            v-if="showMaxFeeAccepted"
          >
            <c-form-section
              :title="$t('form.daycare-application.payment.title')"
              data-qa="payment-section"
              :isOpen="this.activeSection === sections.payment"
              :data-open="this.activeSection === sections.payment"
              @toggle="toggleActiveSection(sections.payment)"
              :errors="errors.payment"
            >
              <c-text-content
                :text="$t('form.daycare-application.payment.text')"
                :asHtml="true"
              />

              <c-checkbox
                class="paymentCheckbox"
                v-model="model"
                name="maxFeeAccepted"
                :label="`${$t(`form.daycare-application.payment.checkbox`)}`"
              />

              <c-text-content
                :text="$t('form.daycare-application.payment.text2')"
                :asHtml="true"
              />
            </c-form-section>
          </CForm>

          <CForm :data="model" v-model="validations" :name="sections.additional">
            <c-form-section
              :title="$t('form.daycare-application.additional.title')"
              data-qa="additional-section"
              :isOpen="this.activeSection === sections.additional"
              :data-open="this.activeSection === sections.additional"
              @toggle="toggleActiveSection(sections.additional)"
              :errors="errors.additional"
            >
              <c-section-title :icon="['fal', 'info']">{{
                $t('form.daycare-application.additional.section-title')
              }}</c-section-title>
              <c-text-area
                :name="['additionalDetails', 'otherInfo']"
                :rows="10"
                v-model="model"
                :placeholder="
                  $t(`form.${type}-application.additional.text-area`)
                "
              />

              <c-text-area
                :name="['additionalDetails', 'dietType']"
                v-model="model"
                :label="$t('form.daycare-application.additional.special-diet')"
                :placeholder="
                  $t(
                    'form.daycare-application.additional.special-diet-placeholder'
                  )
                "
              >
                <c-instructions
                  :instruction="
                    $t(`form.${type}-application.additional.special-diet-info`)
                  "
                />
              </c-text-area>

              <c-text-area
                :name="['additionalDetails', 'allergyType']"
                v-model="model"
                :label="$t('form.daycare-application.additional.allergies')"
                :placeholder="
                  $t(
                    'form.daycare-application.additional.allergies-placeholder'
                  )
                "
              >
                <c-instructions
                  :instruction="
                    $t('form.daycare-application.additional.allergies-info')
                  "
                />
              </c-text-area>
            </c-form-section>
          </CForm>
        </c-form>

        <c-button
          slot="toolbar"
          @click="onCancel"
          :primary="true"
          :outlined="true"
          >{{ $t('form.cancel') }}</c-button
        >
        <c-button
          slot="toolbar"
          @click="saveAndExit"
          :primary="true"
          :outlined="true"
          v-if="hasCreatedStatus"
          id="save-as-draft"
          :disabled="loading"
          >{{ $t('form.save-as-draft') }}</c-button
        >
        <c-button
          id="preview-and-send"
          slot="toolbar"
          @click="toSummary"
          :class="{ 'is-danger': !isValid }"
          :primary="true"
          data-qa="btn-check-and-send"
          :disabled="loading"
          >{{ $t('form.preview-and-send') }}</c-button
        >
      </c-view>
    </div>

    <!-- Summary -->
    <div v-else>
      <c-view>
        <application-summary
          :summaryChecked="summaryChecked"
          :form="model"
          @summaryCheckedChanged="summaryCheckedChanged"
          @closeSummary="closeSummary"
        />
        <section class="columns is-centered has-text-centered">
          <div class="column is-narrow">
            <c-button
              size="wide"
              :primary="true"
              :outlined="true"
              @click="closeSummary"
              >{{ $t('form.back-to-form') | uppercase }}</c-button
            >
          </div>
          <div class="column is-narrow">
            <c-button
              size="wide"
              id="daycare-application-btn"
              :primary="true"
              @click="onSend"
              :disabled="!summaryChecked || loading"
              >{{ $t('form.submit') | uppercase }}</c-button
            >
          </div>
        </section>
      </c-view>
    </div>
    <confirm
      :header="$t('form.confirmation.exit-form')"
      :acceptText="$t('form.confirmation.accept-exit-form')"
      :rejectText="$t('form.confirmation.reject-exit-form')"
      ref="confirmCancel"
    >
      <div slot="body">
        <div>{{ $t('form.dirty') }}</div>
      </div>
    </confirm>
  </div>
</template>

<script lang="ts">
  import _ from 'lodash'
  import Vue from 'vue'
  import router from '@/router'
  import { mapGetters } from 'vuex'
  import { config } from '@evaka/enduser-frontend/src/config'
  import DaycareSelectList from './daycare-select-list.vue'
  import SortableUnitList from '@/components/unit-list/sortable-unit-list.vue'
  import { Unit } from '@/types'
  import {
    APPLICATION_STATUS,
    LANGUAGES,
    PRESCHOOL_START_DATE_FI,
    PRESCHOOL_START_DATE_SV
  } from '@/constants'
  import ChildInfo from '../shared/child-info.vue'
  import GuardianInfo from '../shared/guardian-info.vue'
  import Person from '../shared/person.vue'
  import {
    createPerson,
    createDaycareForm,
    createPreschoolForm,
    DAYCARE_SECTION,
    createDaycareSectionError,
    PreschoolFormModel
  } from '../shared/types'
  import Confirm from '@/components/modal/confirm.vue'
  import ApplicationSummary from '@/views/applications/daycare-application/form-components/summary/application-summary.vue'
  import CForm from '@/components/styleguide/c-form'
  import CTimeInput, {
    removeSeconds
  } from '@/components/styleguide/c-time-input'
  import { scrollToTop } from '@/utils/scroll-to-element'
  import {
    minimumDaycareStartdate,
    isUnderFourMonths,
    datepickerTodayFormat
  } from '@/utils/date-utils'
  import { withAsterisk } from '@/utils/helpers'
  import { resetGuardian2Info } from '@evaka/lib-common/src/utils/form'
  import { isValidTimeString } from '@/components/validation/validators'
  import { formatDate } from '@/utils/date-utils'
  import { DATE_FORMAT } from '@/constants'
  import FileUpload from '@/components/common/file-upload.vue'

  export default Vue.extend({
    props: {
      isPreschool: {
        type: Boolean,
        default: false
      }
    },
    components: {
      DaycareSelectList,
      SortableUnitList,
      // MapModal,
      ChildInfo,
      GuardianInfo,
      Person,
      Confirm,
      ApplicationSummary,
      CForm,
      CTimeInput
    },
    data() {
      return {
        loading: false,
        isDirty: false,
        isChecked: false,
        applicationLoaded: false,
        activeSection: DAYCARE_SECTION.service,
        model: this.isPreschool ? createPreschoolForm() : createDaycareForm(),
        confirmRouteLeave: true,
        showSummary: false,
        validations: {},
        sections: DAYCARE_SECTION,
        errors: createDaycareSectionError(),
        isValid: true,
        summaryChecked: false,
        guardianRequired: true,
        language: [this.$t('constants.language.FI.value')],
        debouncedValidate: _.debounce(() => false)
      }
    },
    computed: {
      ...mapGetters(['daycareForm', 'applicationUnits', 'urgentFiles', 'extendedCareFiles', 'originalPreferredStartDate']),
      id(): string {
        return this.$route.params.id
      },
      locale(): string {
        return this.$i18n.locale
      },
      currentLocaleIsSwedish(): boolean {
        return this.locale.toLowerCase() === LANGUAGES.SV
      },
      languages(): any {
        return Object.values(
          this.$t('constants.language', { returnObjects: true })
        )
      },
      showDailyTime(): boolean {
        return (
          (this.isPreschool &&
            (this.model as PreschoolFormModel).connectedDaycare) ||
          !this.isPreschool
        )
      },
      formattedStartDate(): string | null {
        return this.model.preferredStartDate
          ? formatDate(this.model.preferredStartDate, DATE_FORMAT)
          : ''
      },
      minimumStartdate(): null | string {
        // If application is already sent, the preferred start date can only be moved forward
        return (this.model.status.value !== 'CREATED' && this.originalPreferredStartDate) ?
          this.originalPreferredStartDate
        : this.isPreschool
          ? datepickerTodayFormat()
          : minimumDaycareStartdate()
      },
      maximumStartDate(): null | string {
        if(this.isPreschool && new Date() < new Date('2020-01-08')){
          return '2021-06-04'
        } else {
          return null
        }
      },
      startdateIsUnderFourMonths(): boolean {
        return this.model.preferredStartDate
          ? isUnderFourMonths(this.model.preferredStartDate)
          : false
      },
      isLoading(): boolean {
        return this.loading
      },
      type(): string {
        return this.model.type.value
      },
      maxUnitsSelected(): boolean {
        return this.preferredUnits.length >= 3
      },
      preferredUnits(): string[] {
        return this.model.apply.preferredUnits
      },
      formIsValid(): boolean {
        const form = this.$refs.form as any
        return !form.validationErrors.length
      },
      isNewApplication(): boolean {
        return localStorage.getItem('isNew') === 'true'
      },
      hasCreatedStatus(): boolean {
        return (
          (this as any).model.status.value === APPLICATION_STATUS.CREATED.value
        )
      },
      startDateShouldBeEditable(): boolean {
        return (
          [APPLICATION_STATUS.SENT.value, APPLICATION_STATUS.CREATED.value].includes((this as any).model.status.value)
        )
      },
      errorSections() {
        return Object.keys(this.errors)
          .filter((key) => this.errors[key].length > 0)
          .reduce((obj, key) => {
            obj[key] = this.errors[key]
            return obj
          }, {})
      },
      isOtherGuardianInfoNeeded(): boolean {
        return this.model.otherGuardianAgreementStatus === 'NOT_AGREED'
      },
      hasOtherVtjGuardianInSameAddress(): boolean {
        return (
          this.model.hasOtherVtjGuardian &&
          this.model.otherVtjGuardianHasSameAddress
        )
      },
      showMaxFeeAccepted(): boolean {
        return (
          !this.isPreschool ||
          (this.model as PreschoolFormModel).connectedDaycare
        )
      },
      attachmentsEnabled() {
        return config.feature.attachments
      }
    },
    filters: {
      filterByLanguage(units: any, language: string): any[] {
        if (!units) {
          return []
        }
        return units.filter((unit) =>
          language.includes(unit.language.toUpperCase())
        )
      }
    },
    methods: {
      uploadUrgencyAttachment(file) {
        this.$store.dispatch('addUrgencyAttachment', {
          file,
          applicationId: this.id
        })
      },
      uploadExtendedCareAttachment(file) {
        this.$store.dispatch('addExtendedCareAttachment', {
          file,
          applicationId: this.id
        })
      },
      deleteUrgencyAttachment(file) {
        this.$store.dispatch('deleteUrgencyAttachment', file)
      },
      deleteExtendedCareAttachment(file) {
        this.$store.dispatch('deleteExtendedCareAttachment', file)
      },
      onAssistanceNeededChanged(): void {
        if (!this.model.careDetails.assistanceNeeded) {
          this.model.careDetails.assistanceDescription = ''
        }
      },
      emptyGuardian2Info(resetnameInfo: boolean): void {
        this.model = resetGuardian2Info(resetnameInfo, this.model)
      },
      onOtherAdultsChanged(form: PreschoolFormModel): void {
        if (form.hasOtherAdults) {
          this.addOtherAdult()
        } else {
          this.model.otherAdults = []
        }
      },
      onPreparatoryChanged() {
        this.reloadUnits()
      },
      preferredUnitsValidationText() {
        return this.$t(
          'form.daycare-application.preferredUnits.unit-required-validation'
        ).toString()
      },
      serviceTimeValidationText() {
        return (
          this.$t(
            'form.daycare-application.service.dailyTime.label'
          ).toString() +
          this.$t('validation.errors.is-required-field').toString()
        )
      },
      onSiblingBasisClicked() {
        if (!this.model.apply.siblingBasis) {
          this.model.apply.siblingName = ''
          this.model.apply.siblingSsn = ''
        }
      },
      validateForm() {
        this.debouncedValidate.cancel()
        this.errors = createDaycareSectionError()
        const form = this.$refs.form as any
        const validate = form.validate()
        const errors = _.filter(
          validate,
          (validation) => validation.errors.length > 0
        )

        let formIsValid = this.formIsValid

        _.forEach(errors, (error) => {
          const key = error.errors[0].key
          const simpleMessage = error.errors[0].simpleMessage
          const validationName = error.errors[0].validationName
          const findKey = _.findKey(this.validations, key) as string
          if (findKey) {
            this.errors[findKey].push(
              validationName + this.$t(simpleMessage).toString()
            )
            formIsValid = false
          }
        })
        if (this.preferredUnits.length === 0) {
          this.errors.preferredUnits.push(this.preferredUnitsValidationText())
          formIsValid = false
        }

        if (
          this.showDailyTime &&
          (!this.model.serviceStart || !this.model.serviceEnd)
        ) {
          this.errors.service.push(this.serviceTimeValidationText())
          formIsValid = false
        }

        if (
          this.model.hasOtherVtjGuardian &&
          !this.model.otherVtjGuardianHasSameAddress &&
          this.model.otherGuardianAgreementStatus === null
        ) {
          if (
            !this.errors.personalInfo.includes(
              this.$t(
                'form.persons.guardian2.agreement-status.NOT_SET'
              ).toString()
            )
          ) {
            this.errors.personalInfo.push(
              this.$t(
                'form.persons.guardian2.agreement-status.NOT_SET'
              ).toString()
            )
          }
          formIsValid = false
        }

        return formIsValid
      },
      toSummary() {
        this.isChecked = true
        this.isValid = this.validateForm()
        this.showSummary = this.isValid
        scrollToTop()
        if (!this.isValid) {
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
      addUnit(unit: Unit) {
        this.model.apply.preferredUnits.push(unit.id)
      },
      removeUnit(id: string) {
        this.model.apply.preferredUnits = [
          ...this.model.apply.preferredUnits.filter((u) => u !== id)
        ]
      },
      addOtherAdult() {
        this.model.otherAdults.push(createPerson())
      },
      addOtherChildren() {
        this.model.otherChildren.push(createPerson())
      },
      removeOtherChildren(index) {
        this.model.otherChildren.splice(index, 1)
      },
      toggleActiveSection(name: string): void {
        const alreadyOpen = this.activeSection === name
        this.activeSection = alreadyOpen ? '' : name
      },
      confirmLeave(onConfirm) {
        const action = this.isNewApplication ? this.deleteForm : this.nop
        const confirmCancel = this.isDirty
          ? (this.$refs.confirmCancel as any).open()
          : Promise.resolve()
        confirmCancel.then(() => {
          action()
          return onConfirm()
        }, this.nop)
      },
      reloadUnits() {
        const date = this.model.preferredStartDate || this.minimumStartdate
        const type = this.isPreschool
          ? this.model.careDetails.preparatory
            ? 'PREPARATORY'
            : 'PRESCHOOL'
          : 'DAYCARE'
        this.$store.dispatch('loadApplicationUnits', { type, date })
      },
      nop() {
        // no op
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
      async saveAndExit() {
        this.loading = true

        // ensure invalid time value does not cause serialization issues in the backend
        if (!isValidTimeString(this.model.serviceStart)) {
          this.model.serviceStart = ''
        }
        if (!isValidTimeString(this.model.serviceEnd)) {
          this.model.serviceEnd = ''
        }

        const id = await this.$store
          .dispatch('saveApplication', {
            type: this.type,
            applicationId: this.id,
            form: this.model
          })
          .finally(() => {
            this.loading = false
          })

        if (id !== null) {
          this.confirmRouteLeave = false
          router.push('/applications')
        }
      },
      sendAndExit() {
        this.loading = true
        this.$store
          .dispatch('sendApplication', {
            type: this.type,
            applicationId: this.id,
            form: this.model
          })
          .then(() => {
            this.confirmRouteLeave = false
            router.push('/applications')
          })
          .finally(() => {
            this.loading = false
          })
      },
      deleteForm() {
        this.$store.dispatch('removeApplication', {
          type: this.type,
          applicationId: this.id
        })
      },
      summaryCheckedChanged(value) {
        this.summaryChecked = value
      },
      closeSummary() {
        this.showSummary = false
      },
      withAsterisk
    },
    beforeRouteLeave(to, from, next) {
      this.confirmRouteLeave ? this.confirmLeave(next) : next()
    },
    async created() {
      this.loading = true
      this.debouncedValidate = _.debounce(this.validateForm, 500)
      await this.$store.dispatch('loadApplication', {
        type: this.type,
        applicationId: this.id
      })
      this.model = _.cloneDeep((this as any).daycareForm)
      if (
        !this.model.preferredStartDate ||
        this.model.preferredStartDate === ''
      ) {
        if (this.isPreschool) {
          this.model.preferredStartDate = this.currentLocaleIsSwedish
            ? PRESCHOOL_START_DATE_SV
            : PRESCHOOL_START_DATE_FI
        } else {
          this.model.preferredStartDate = ''
        }
      }
      if (this.isPreschool) {
        this.model.serviceStart = removeSeconds(this.model.serviceStart)
        this.model.serviceEnd = removeSeconds(this.model.serviceEnd)
      }
      this.reloadUnits()
      this.loading = false
      // When this page/component is loaded, Vue modifies "this.model" twice.
      // This causes a small problem, since "this.model" is also being watched for any changes (to allow isDirty = true);
      // The timeout waits for initial changes to take place with "this.model" before allowing "this.isDirty" to be set to "true"
      setTimeout(() => (this.applicationLoaded = true), 1000)
    },
    beforeDestroy() {
      this.debouncedValidate.cancel()
    },
    watch: {
      model: {
        handler(val, oldVal) {
          if (this.isChecked) {
            this.isValid = this.debouncedValidate()
          }
          if (this.applicationLoaded) {
            this.isDirty = true
          }
          if (oldVal.preferredStartDate !== val.preferredStartDate) {
            this.reloadUnits()
          }
        },
        deep: true
      },
      applicationUnits: {
        handler() {
          if (!this.applicationUnits.loading) {
            this.model.apply.preferredUnits = this.model.apply.preferredUnits.filter(
              (id) => this.applicationUnits.data.some((unit) => unit.id === id)
            )
          }
        }
      }
    }
  })
</script>

<style lang="scss" scoped>
  hr {
    border: 1px dashed $grey-lighter;
  }

  #daycare-preschool-application,
  #daycare-preschool-form {
    scroll-behavior: smooth;
  }

  .chipses {
    display: flex;

    div:not(:first-child) {
      margin-left: 0.6rem;
    }
  }

  .form-input-section {
    margin-top: 3em;
  }

  .form-preferred-units-section {
    border-right: 1px solid #eee;
  }

  @include onMobile() {
    .form-preferred-units-section {
      border-right: none;
    }
  }

  #daycare-preschool-datepicker {
    width: 12rem;
  }

  .startdate-description {
    margin-bottom: 0 !important;
  }

  #outsourced-service-info {
    margin-top: 2rem;
  }

  .other-guardian-info {
    padding-top: 50px;
  }

  #second-guardian-has-agreed {
    padding: 0.5rem 0 0 3rem;
    word-break: break-all;

    .error-validation-message {
      color: $grey-dark;
      font-size: 0.75rem;
      margin-top: 0.8rem;

      svg {
        color: $orange;
      }
    }
  }

  .remove-person-btn {
    align-self: center;
  }

  .preferredunits-text-content {
    margin-bottom: 4rem !important;
  }

  .daycare-select-list {
    max-width: 30rem;
  }

  .preschool-sibling-basis {
    margin-bottom: 1rem;
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

  .field-datepicker {
    padding-bottom: 0;
  }

  .selected-units {
    padding-left: 1.5rem;
  }

  #daycare-start {
    padding-bottom: 0;
  }

  .base-tooltip {
    position: relative;
    padding: 5px;
  }

  ul.validation-error-items {
    li {
      ul {
        li {
          list-style: circle;
          margin-left: 2rem;
        }
      }
    }
  }

  .paymentCheckbox {
    margin-bottom: 2rem;
  }

  .attachments-message {
    margin: 24px 0;

    p {
      margin: 16px 0;
    }

    .bold {
      font-weight: 600;
    }
  }
</style>
<style lang="scss">
  .space {
    &-right {
      margin-right: 1.5rem;
    }

    &-left {
      margin-left: 4rem;
    }
  }
</style>
