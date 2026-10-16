import React from 'react'
import { ScrollView, StyleSheet, View } from 'react-native'
import PropTypes from 'prop-types'
import moment from 'moment'

import { connect } from 'react-redux'
import { navigate } from '../slices/navigation'
import { setDate } from '../slices/date'

import AppText from './common/app-text'
import Button from './common/button'
import ForecastCard from './predictions/forecast-card'
import illustrations from './predictions/illustrations'

import cycleModule from '../lib/cycle'
import { getTodayDateString } from '../lib/today'
import { getFertilityStatusForDay } from '../lib/sympto-adapter'
import { determinePredictionText, formatWithOrdinalSuffix } from './helpers/home'
import { formatDateForShortText } from './helpers/format-date'

import { Colors, Fonts, Sizes, Spacing } from '../styles'
import { cycleForecast as forecastLabels } from '../i18n/en/labels'
import { useTranslation } from 'react-i18next'
import { LocalDate } from 'js-joda'

const Home = ({ navigate, setDate }) => {
  const { t } = useTranslation()

  const todayDateString = getTodayDateString()

  function navigateToCycleDayView() {
    setDate(todayDateString)
    navigate('CycleDay')
  }

  const { getCycleForecast } = cycleModule()
  const forecast = getCycleForecast()
  const { status, phase, statusText } =
    getFertilityStatusForDay(todayDateString)
  const prediction = determinePredictionText(forecast.predictedMenses, t)
  const cycleDayText = forecast.cycleDayNumber
    ? formatWithOrdinalSuffix(forecast.cycleDayNumber)
    : ''

  const periodMeta =
    forecast.lengthSource === 'model'
      ? forecastLabels.usingModel
      : forecast.lengthSource === 'average'
        ? forecastLabels.usingAverage
        : null

  const ovulationMeta =
    forecast.ovulationSource === 'model'
      ? forecastLabels.usingModel
      : forecast.ovulationSource === 'luteal'
        ? forecastLabels.usingLuteal
        : null

  let fertileBody = null
  if (forecast.fertileStartDate && forecast.fertileEndDate) {
    const today = LocalDate.parse(todayDateString)
    const start = LocalDate.parse(forecast.fertileStartDate)
    const end = LocalDate.parse(forecast.fertileEndDate)
    if (!today.isBefore(start) && !today.isAfter(end)) {
      fertileBody = forecastLabels.fertileNow
    } else {
      fertileBody = forecastLabels.fertileUpcoming(
        formatDateForShortText(start),
        formatDateForShortText(end)
      )
    }
    if (forecast.windowIsNextCycle) {
      fertileBody = `${fertileBody} ${forecastLabels.nextCycle}`
    }
  }

  const progress =
    forecast.cycleDayNumber && forecast.predictedLength
      ? Math.min(1, forecast.cycleDayNumber / forecast.predictedLength)
      : null

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.contentContainer}
    >
      <AppText style={styles.title}>
        {moment(todayDateString).format('MMM Do YYYY')}
      </AppText>

      {forecast.cycleDayNumber && (
        <View style={styles.line}>
          <AppText style={styles.whiteSubtitle}>{cycleDayText}</AppText>
          <AppText style={styles.turquoiseText}>
            {t('labels.home.cycleDay')}
          </AppText>
        </View>
      )}
      {forecast.cycleDayNumber && forecast.predictedLength && (
        <AppText style={styles.progressLabel}>
          {forecastLabels.cycleProgress(
            forecast.cycleDayNumber,
            forecast.predictedLength
          )}
        </AppText>
      )}
      {progress !== null && (
        <View style={styles.progressTrack}>
          <View style={[styles.progressFill, { width: `${progress * 100}%` }]} />
        </View>
      )}
      {phase && (
        <View style={styles.line}>
          <AppText style={styles.whiteSubtitle}>
            {formatWithOrdinalSuffix(phase)}
          </AppText>
          <AppText style={styles.turquoiseText}>
            {t('labels.home.cyclePhase')}
          </AppText>
          <AppText style={styles.turquoiseText}>{status}</AppText>
          <Asterisk />
        </View>
      )}

      {forecast.hasEnoughHistory ? (
        <View>
          <ForecastCard
            illustration={illustrations.period}
            title={forecastLabels.nextPeriod}
            body={prediction}
            meta={periodMeta}
          />
          {forecast.ovulationDate && (
            <ForecastCard
              illustration={illustrations.ovulation}
              title={forecastLabels.ovulation}
              body={forecastLabels.ovulationOn(
                formatDateForShortText(LocalDate.parse(forecast.ovulationDate))
              )}
              meta={ovulationMeta}
            />
          )}
          {fertileBody && (
            <ForecastCard
              illustration={illustrations.fertile}
              title={forecastLabels.fertileWindow}
              body={fertileBody}
            />
          )}
        </View>
      ) : (
        <ForecastCard
          illustration={illustrations.waiting}
          title={forecastLabels.nextPeriod}
          body={forecastLabels.needCycles}
        />
      )}

      {!forecast.profileComplete && (
        <ForecastCard
          illustration={illustrations.profile}
          title={forecastLabels.profileCta}
          body={forecastLabels.addProfile}
        />
      )}

      <Button isCTA isSmall={false} onPress={navigateToCycleDayView}>
        {t('labels.home.addDataForToday')}
      </Button>
      {!forecast.profileComplete && (
        <Button isSmall={false} onPress={() => navigate('CycleProfile')}>
          {forecastLabels.profileCta}
        </Button>
      )}
      <AppText style={styles.disclaimer}>{forecastLabels.disclaimer}</AppText>
      {phase && (
        <View style={styles.asteriskLine}>
          <Asterisk />
          <AppText linkStyle={styles.whiteText} style={styles.greyText}>
            {statusText}
          </AppText>
        </View>
      )}
    </ScrollView>
  )
}

const Asterisk = () => {
  return <AppText style={styles.asterisk}>*</AppText>
}

const styles = StyleSheet.create({
  asterisk: {
    color: Colors.orange,
  },
  asteriskLine: {
    flexDirection: 'row',
    alignContent: 'flex-start',
    marginBottom: Spacing.tiny,
    marginTop: Spacing.small,
  },
  container: {
    backgroundColor: Colors.purple,
    flex: 1,
  },
  contentContainer: {
    padding: Spacing.base,
    paddingTop: 0,
  },
  disclaimer: {
    color: Colors.purpleLight,
    fontSize: Sizes.footnote,
    marginTop: Spacing.base,
  },
  greyText: {
    color: Colors.greyLight,
    paddingLeft: Spacing.base,
  },
  line: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    alignContent: 'flex-start',
    marginBottom: Spacing.tiny,
    marginTop: Spacing.small,
  },
  progressFill: {
    backgroundColor: Colors.orange,
    borderRadius: 4,
    height: 8,
  },
  progressLabel: {
    color: Colors.turquoise,
    fontSize: Sizes.small,
    marginTop: Spacing.tiny,
  },
  progressTrack: {
    backgroundColor: Colors.purpleLight,
    borderRadius: 4,
    height: 8,
    marginTop: Spacing.tiny,
    overflow: 'hidden',
  },
  title: {
    color: Colors.purpleLight,
    fontFamily: Fonts.bold,
    fontSize: Sizes.huge,
    marginVertical: Spacing.small,
  },
  turquoiseText: {
    color: Colors.turquoise,
    fontSize: Sizes.subtitle,
  },
  whiteSubtitle: {
    color: 'white',
    fontSize: Sizes.subtitle,
  },
  whiteText: {
    color: 'white',
  },
})

const mapDispatchToProps = (dispatch) => {
  return {
    navigate: (page) => dispatch(navigate(page)),
    setDate: (date) => dispatch(setDate(date)),
  }
}

Home.propTypes = {
  navigate: PropTypes.func,
  setDate: PropTypes.func,
}

export default connect(null, mapDispatchToProps)(Home)
