import links from './links'

export default {
  title: 'Settings',
  menuItems: {
    reminders: {
      name: 'Reminders',
      text: 'turn on/off reminders',
    },
    nfpSettings: {
      name: 'NFP settings',
      text: 'define how you want to use NFP',
    },
    cycleProfile: {
      name: 'Cycle profile',
      text: 'age, height and weight for personal predictions',
    },
    dataManagement: {
      name: 'Data',
      text: 'import, export or delete your data',
    },
    password: {
      name: 'Password',
      text: '',
    },
    about: 'About',
    license: 'License',
    settings: 'Settings',
    privacyPolicy: 'Privacy Policy',
    developerOptions: {
      name: 'Developer Options',
      text: 'pregnancy mode, cycle overrides, calendric change',
    },
  },
  developerOptions: {
    title: 'Developer Options',
    unlockedToast: 'Developer Options unlocked',
    lockedToast: 'Developer Options locked',
    tapsRemaining: (n) =>
      n === 1
        ? '1 more tap to unlock Developer Options'
        : `${n} more taps to unlock Developer Options`,
    lock: 'Lock Developer Options',
    lockExplainer:
      'Hide Developer Options from the settings menu. Tap the Settings title 7 times to unlock again.',
    pregnancyMode: {
      title: 'Pregnancy Mode',
      explainer:
        'Enable pregnancy mode for upcoming pregnancy tracking features. This flag is stored locally and does not change medical advice.',
      enabledToast: 'Pregnancy Mode enabled',
      disabledToast: 'Pregnancy Mode disabled',
    },
    cycleModification: {
      title: 'Complete Cycle Modification',
      explainer:
        'Override cycle prediction limits that are normally fixed in the app. Use only for development and testing.',
      maxCycleLength: 'Max cycle length (days)',
      minCyclesForPrediction: 'Min completed cycles for prediction',
      maxBreakInBleeding: 'Max break in bleeding (days)',
      save: 'Save cycle overrides',
      savedToast: 'Cycle overrides saved',
    },
    cyclePrediction: {
      title: 'Cycle Prediction Model',
      explainer:
        'Let an on-device model trained on the NFP cycle dataset predict your next period when your cycles are irregular. If your cycles are steady, or there is too little history, the app keeps using the average of your past cycle lengths. Nothing is sent off your phone either way.',
      enabledToast: 'Model predictions enabled',
      disabledToast: 'Using average cycle length',
      accuracy: (rmse) =>
        `Model error on held-out people: ${rmse.toFixed(2)} days. Your average is used instead whenever your own cycles vary less than that.`,
    },
    calendricChange: {
      title: 'Calendric Change',
      explainer:
        'Override the device date used for Home, Calendar, Chart, and cycle calculations. Useful for testing predictions without waiting.',
      pickDate: 'Pick override date',
      usingDeviceDate: 'Currently using the device date.',
      current: (date) => `App date override: ${date}`,
      savedToast: 'Calendric override updated',
    },
  },
  export: {
    errors: {
      noData: 'There is no data to export',
      couldNotConvert: 'Could not convert data to CSV',
      problemSharing: 'There was a problem sharing the data export file',
    },
    title: 'My drip. data export',
    subject: 'My drip. data export',
    button: 'Export data',
    segmentExplainer:
      'Export data in CSV format for backup or so you can use it elsewhere',
  },
  import: {
    button: 'Import data',
    title: 'Keep existing data?',
    message: `There are two options for the import:
1. Keep existing cycle days and replace only the ones in the import file.
2. Delete all existing cycle days and import cycle days from file.`,
    replaceOption: 'Import and replace',
    deleteOption: 'Import and delete existing',
    errors: {
      couldNotOpenFile: 'Could not open file',
      postFix: 'No data was imported or changed',
      futureEdit: 'Future dates may only contain a note, no other symptoms',
    },
    success: {
      message: 'Data successfully imported',
    },
    segmentExplainer: 'Import data in CSV format',
  },
  deleteSegment: {
    title: 'Delete app data',
    explainer: 'Delete app data from this phone',
    question: 'Do you want to delete app data from this phone?',
    message:
      'Please note that deletion of the app data is permanent and irreversible. We recommend exporting existing data before deletion.',
    confirmation: 'Delete app data permanently',
    errors: {
      couldNotDeleteFile: 'Could not delete data',
      postFix: 'No data was deleted or changed',
      noData: 'There is no data to delete',
    },
    success: {
      message: 'App data successfully deleted',
    },
  },
  tempScale: {
    segmentTitle: 'Temperature scale',
    segmentExplainer:
      'Change the minimum and maximum value for the temperature chart',
    min: 'Min',
    max: 'Max',
    loadError: 'Could not load saved temperature scale settings',
    saveError: 'Could not save temperature scale settings',
  },
  tempReminder: {
    title: 'Temperature reminder',
    noTimeSet: 'Set a time for a daily reminder to take your temperature',
    timeSet: (time) => `Daily reminder set for ${time}`,
    notification: 'Record your morning temperature',
  },
  periodReminder: {
    title: 'Next period reminder',
    reminderText:
      'Get a notification 3 days before your next period is likely to start.',
    notification: (daysToEndOfPrediction) =>
      `Your next period is likely to start in 3 to ${daysToEndOfPrediction} days.`,
  },
  useCervix: {
    title: 'Secondary symptom',
    cervixModeOn:
      'Cervix values are being used for symptothermal fertility detection. You can switch here to use cervical mucus values for symptothermal fertility detection',
    cervixModeOff:
      'By default, cervical mucus values are being used for symptothermal fertility detection. You can switch here to use cervix values for symptothermal fertility detection',
  },
  passwordSettings: {
    title: 'App password',
    explainerDisabled:
      "Encrypt the app's database with a password. You need to enter the password every time the app is started.",
    explainerEnabled:
      'Password protection and database encryption is currently enabled',
    setPassword: 'Set password',
    savePassword: 'Save password',
    changePassword: 'Change password',
    deletePassword: 'Delete password',
    enterCurrent: 'Please enter your current password',
    enterNew: 'Please enter a new password',
    confirmPassword: 'Please confirm your password',
    passwordsDontMatch: "Password and confirmation don't match",
    backupReminder: {
      title: 'Read this before making changes to your password',
      message: `
Just to be safe, please backup your data using the export function before making any changes to your password.\n
Longer passwords are better! Consider using a passphrase.\n
Please also make sure you do not lose your password. There is no way to recover your data if you do.\n
Making any changes to your password setting will keep your data as it was before.\n`,
    },
    deleteBackupReminder: {
      title: 'Read this before deleting your password',
      message: `
Deleting your password means your data will no longer be encrypted.\n
Just to be safe, please backup your data using the export function before deleting your password.\n
Making any changes to your password setting will keep your data as it was before and restart the app.\n    
    `,
    },
    backupReminderAppendix: {
      android:
        'After the password is updated the app will automatically restart.',
      ios: 'After the password is updated the app will automatically close. Please reopen it manually.',
    },
  },
  aboutSection: {
    title: 'About',
    text: `Please note that your data is stored locally on your phone and not on a server. This means your data cannot be read by anyone else unless they have access to your phone. We want to ensure that you stay in control of your own data. If you are planning to switch or reset your phone, please remember to export your data before doing so. You can reinstall the app afterwards and import your data.\n\nIf you encounter any technical issues, don't hesitate to contact us via email. You can also contribute to the code base on Gitlab and visit our website.`,
  },
  philosophy: {
    title: 'Remember to think for yourself',
    text: `drip. makes period predictions for you and helps you apply NFP fertility awareness rules. But please remember that this app is made by humans, and humans make mistakes. Always think for yourself: "Does this make sense?" Remember, you don't need an app to understand your cycle! However, drip. wants to support you and make period tracking easier, more transparent and secure.`,
  },
  version: {
    title: 'Version',
  },
  cycleProfile: {
    title: 'Cycle profile',
    explainer:
      'Optional. These stay on your phone and help the on-device model personalize period and ovulation estimates. Leave a field blank if you would rather not say.',
    age: 'Age (years)',
    height: 'Height (cm)',
    weight: 'Weight (kg)',
    bmi: (value) => `BMI from these values: ${value}`,
    save: 'Save profile',
    savedToast: 'Cycle profile saved',
    clearedToast: 'Cycle profile cleared',
    invalid:
      'Check that age, height and weight are realistic numbers, or leave them blank.',
    clear: 'Clear profile',
  },
  website: {
    title: 'Website',
  },
  preOvu: {
    title: 'Infertile days at cycle start',
    note: `drip. applies NFP's rules for calculating infertile days at the start of the cycle (see ${links.wiki.url} for more info). However, drip. does not currently apply the so called 20-day-rule, which determines infertile days at the cycle start from past cycle lengths in case no past symptothermal info is available.`,
  },
  credits: {
    title: 'Credits',
    note: `We love the drip. team. Thanks and lots of <3 to all of our condriputors. Thanks to Paula Härtel for the symptom tracking icons. All the other icons are made by ${links.smashicons.url}, ${links.pause08.url}, ${links.kazachek.url} & ${links.freepik.url} from ${links.flaticon.url}.\n\nPrediction illustrations (period drop, ovulation blossom, fertile window, profile, and waiting seed) are original ${links.cc0.url} graphics drawn for Solene.`,
  },
  donate: {
    title: 'Support us',
    note: `The drips are developing this app on a volunteer basis. We are always grateful for support. This could mean condriputing to the code, giving feedback, suggesting improvements or features, testing or donating. It helps and motivates us maintaining this app and developing new features. Thank you for your support!`,
  },
}
