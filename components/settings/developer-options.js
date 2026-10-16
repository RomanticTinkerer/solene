import React, { Component } from 'react'
import { StyleSheet, View } from 'react-native'
import DateTimePicker from 'react-native-modal-datetime-picker'
import { LocalDate } from 'js-joda'
import moment from 'moment'
import PropTypes from 'prop-types'
import { connect } from 'react-redux'

import AppPage from '../common/app-page'
import AppSwitch from '../common/app-switch'
import AppText from '../common/app-text'
import AppTextInput from '../common/app-text-input'
import Segment from '../common/segment'
import SettingsButton from './common/settings-button'

import {
  pregnancyModeObservable,
  savePregnancyMode,
  cycleOverridesObservable,
  saveCycleOverrides,
  calendricOverrideObservable,
  saveCalendricOverride,
  saveDeveloperOptionsUnlocked,
  mlPredictionObservable,
  saveMlPrediction,
} from '../../local-storage'
import { getTodayDateString } from '../../lib/today'
import { loadModel } from '../../lib/prediction/models'
import { goBack } from '../../slices/navigation'
import { showToast } from '../helpers/general'
import { Spacing } from '../../styles'
import labels from '../../i18n/en/settings'

const { developerOptions: t } = labels

function parsePositiveInt(value, fallback) {
  const parsed = parseInt(value, 10)
  if (Number.isNaN(parsed) || parsed < 1) return fallback
  return parsed
}

function getModelMeta() {
  try {
    const model = loadModel('cycleLength')
    return {
      rmse: model.metrics && model.metrics.rmse,
      split: model.metrics && model.metrics.split,
    }
  } catch (err) {
    return { rmse: null, split: null }
  }
}

class DeveloperOptions extends Component {
  static propTypes = {
    goBack: PropTypes.func.isRequired,
  }

  constructor(props) {
    super(props)

    const cycleOverrides = cycleOverridesObservable.value
    const calendricOverride = calendricOverrideObservable.value

    this.state = {
      pregnancyMode: pregnancyModeObservable.value,
      cycleOverridesEnabled: cycleOverrides.enabled,
      maxCycleLength: String(cycleOverrides.maxCycleLength),
      minCyclesForPrediction: String(cycleOverrides.minCyclesForPrediction),
      maxBreakInBleeding: String(cycleOverrides.maxBreakInBleeding),
      calendricEnabled: calendricOverride.enabled,
      calendricDate: calendricOverride.date || getTodayDateString(),
      isDatePickerVisible: false,
      mlPrediction: mlPredictionObservable.value !== false,
    }
  }

  onMlPredictionToggle = (value) => {
    this.setState({ mlPrediction: value })
    saveMlPrediction(value)
    showToast(
      value ? t.cyclePrediction.enabledToast : t.cyclePrediction.disabledToast
    )
  }

  onPregnancyModeToggle = (value) => {
    this.setState({ pregnancyMode: value })
    savePregnancyMode(value)
    showToast(value ? t.pregnancyMode.enabledToast : t.pregnancyMode.disabledToast)
  }

  onCycleOverridesToggle = (value) => {
    const next = {
      ...cycleOverridesObservable.value,
      enabled: value,
    }
    this.setState({ cycleOverridesEnabled: value })
    saveCycleOverrides(next)
  }

  persistCycleOverrides = (partial) => {
    const next = {
      enabled: this.state.cycleOverridesEnabled,
      maxCycleLength: parsePositiveInt(
        this.state.maxCycleLength,
        cycleOverridesObservable.value.maxCycleLength
      ),
      minCyclesForPrediction: parsePositiveInt(
        this.state.minCyclesForPrediction,
        cycleOverridesObservable.value.minCyclesForPrediction
      ),
      maxBreakInBleeding: parsePositiveInt(
        this.state.maxBreakInBleeding,
        cycleOverridesObservable.value.maxBreakInBleeding
      ),
      ...partial,
    }
    saveCycleOverrides(next)
  }

  onMaxCycleLengthChange = (value) => {
    this.setState({ maxCycleLength: value })
  }

  onMinCyclesChange = (value) => {
    this.setState({ minCyclesForPrediction: value })
  }

  onMaxBreakChange = (value) => {
    this.setState({ maxBreakInBleeding: value })
  }

  saveCycleFields = () => {
    const maxCycleLength = parsePositiveInt(this.state.maxCycleLength, 99)
    const minCyclesForPrediction = parsePositiveInt(
      this.state.minCyclesForPrediction,
      3
    )
    const maxBreakInBleeding = parsePositiveInt(this.state.maxBreakInBleeding, 1)

    this.setState({
      maxCycleLength: String(maxCycleLength),
      minCyclesForPrediction: String(minCyclesForPrediction),
      maxBreakInBleeding: String(maxBreakInBleeding),
    })

    this.persistCycleOverrides({
      maxCycleLength,
      minCyclesForPrediction,
      maxBreakInBleeding,
    })
    showToast(t.cycleModification.savedToast)
  }

  onCalendricToggle = (value) => {
    const date = this.state.calendricDate || getTodayDateString()
    this.setState({ calendricEnabled: value })
    saveCalendricOverride({
      enabled: value,
      date: value ? date : null,
    })
  }

  showDatePicker = () => {
    this.setState({ isDatePickerVisible: true })
  }

  hideDatePicker = () => {
    this.setState({ isDatePickerVisible: false })
  }

  onDatePicked = (date) => {
    const dateString = LocalDate.parse(
      moment(date).format('YYYY-MM-DD')
    ).toString()
    this.setState({
      calendricDate: dateString,
      isDatePickerVisible: false,
      calendricEnabled: true,
    })
    saveCalendricOverride({
      enabled: true,
      date: dateString,
    })
    showToast(t.calendricChange.savedToast)
  }

  lockDeveloperOptions = async () => {
    await saveDeveloperOptionsUnlocked(false)
    showToast(t.lockedToast)
    this.props.goBack()
  }

  render() {
    const {
      pregnancyMode,
      cycleOverridesEnabled,
      maxCycleLength,
      minCyclesForPrediction,
      maxBreakInBleeding,
      calendricEnabled,
      calendricDate,
      isDatePickerVisible,
      mlPrediction,
    } = this.state

    const { rmse, split } = getModelMeta()

    return (
      <AppPage title={t.title}>
        <Segment title={t.pregnancyMode.title}>
          <AppSwitch
            onToggle={this.onPregnancyModeToggle}
            text={t.pregnancyMode.explainer}
            value={pregnancyMode}
          />
        </Segment>

        <Segment title={t.cyclePrediction.title}>
          <AppSwitch
            onToggle={this.onMlPredictionToggle}
            text={t.cyclePrediction.explainer}
            value={mlPrediction}
          />
          {mlPrediction && rmse !== null && (
            <AppText style={styles.dateText}>
              {t.cyclePrediction.accuracy(rmse)}
              {split ? ` Split: ${split}.` : ''}
            </AppText>
          )}
        </Segment>

        <Segment title={t.cycleModification.title}>
          <AppSwitch
            onToggle={this.onCycleOverridesToggle}
            text={t.cycleModification.explainer}
            value={cycleOverridesEnabled}
          />
          {cycleOverridesEnabled && (
            <View style={styles.fields}>
              <AppText style={styles.fieldLabel}>
                {t.cycleModification.maxCycleLength}
              </AppText>
              <AppTextInput
                keyboardType="numeric"
                onBlur={this.saveCycleFields}
                onChangeText={this.onMaxCycleLengthChange}
                value={maxCycleLength}
              />
              <AppText style={styles.fieldLabel}>
                {t.cycleModification.minCyclesForPrediction}
              </AppText>
              <AppTextInput
                keyboardType="numeric"
                onBlur={this.saveCycleFields}
                onChangeText={this.onMinCyclesChange}
                value={minCyclesForPrediction}
              />
              <AppText style={styles.fieldLabel}>
                {t.cycleModification.maxBreakInBleeding}
              </AppText>
              <AppTextInput
                keyboardType="numeric"
                onBlur={this.saveCycleFields}
                onChangeText={this.onMaxBreakChange}
                value={maxBreakInBleeding}
              />
              <SettingsButton onPress={this.saveCycleFields}>
                {t.cycleModification.save}
              </SettingsButton>
            </View>
          )}
        </Segment>

        <Segment title={t.calendricChange.title}>
          <AppSwitch
            onToggle={this.onCalendricToggle}
            text={t.calendricChange.explainer}
            value={calendricEnabled}
          />
          <AppText style={styles.dateText}>
            {calendricEnabled
              ? t.calendricChange.current(calendricDate)
              : t.calendricChange.usingDeviceDate}
          </AppText>
          <SettingsButton onPress={this.showDatePicker}>
            {t.calendricChange.pickDate}
          </SettingsButton>
          <DateTimePicker
            isVisible={isDatePickerVisible}
            mode="date"
            onCancel={this.hideDatePicker}
            onConfirm={this.onDatePicked}
          />
        </Segment>

        <Segment last>
          <AppText>{t.lockExplainer}</AppText>
          <SettingsButton onPress={this.lockDeveloperOptions} secondary>
            {t.lock}
          </SettingsButton>
        </Segment>
      </AppPage>
    )
  }
}

const styles = StyleSheet.create({
  dateText: {
    marginBottom: Spacing.base,
    marginTop: Spacing.base,
  },
  fieldLabel: {
    marginTop: Spacing.base,
  },
  fields: {
    marginTop: Spacing.base,
  },
})

const mapDispatchToProps = (dispatch) => ({
  goBack: () => dispatch(goBack()),
})

export default connect(null, mapDispatchToProps)(DeveloperOptions)
