# World Cup Predictor

A modern Android app for predicting FIFA World Cup matches with live results, leaderboards, and knockout-stage support.

Built with **Kotlin**, **Jetpack Compose**, **Supabase**, and the **Football-Data.org API**.

---

## Features

### Match Predictions

* Predict scores for all World Cup matches
* Edit predictions until kickoff
* Automatic prediction locking when the match starts
* Support for group stage and knockout stage matches

### Knockout Stage Support

* Predict penalty shootout winners
* Separate scoring rules for matches decided on penalties
* Automatic handling of extra time and penalty scenarios

### Leaderboard

* Live leaderboard across all users
* Medal positions for top 3 players
* Highlighted current user
* Perfect prediction tracker

### User Accounts

* Create account with username and PIN
* Secure PIN storage using hashing
* Login and logout functionality
* Cloud-synced predictions using Supabase

### Notifications

* Daily reminder notifications
* Only shown when predictions are missing
* Supports multiple languages

### Localization

* English
* Swedish
* Finnish

Language is automatically selected based on the device settings.

---

## Scoring System

### Group Stage

| Prediction          | Points |
| ------------------- | ------ |
| Correct winner/draw | 1      |
| Exact score         | +3     |
| Maximum             | 4      |

### Knockout Matches

If the match is decided on penalties:

| Prediction                                  | Points |
| ------------------------------------------- | ------ |
| Exact score after full time                 | 3      |
| Correctly predicted draw but wrong score    | 1      |
| Correct penalty winner                      | +1     |
| Correct winner without predicting penalties | +1     |
| Maximum                                     | 4      |

Example:

Actual result:

Mexico 1-1 Iran
Mexico wins on penalties

| Prediction   | Points |
| ------------ | ------ |
| 1-1 + Mexico | 4      |
| 1-1 + Iran   | 3      |
| 2-2 + Mexico | 2      |
| 2-2 + Iran   | 1      |
| 3-1 Mexico   | 1      |

---

## Technology Stack

### Frontend

* Kotlin
* Jetpack Compose
* Material 3

### Backend

* Supabase
* PostgreSQL

### Data

* Football-Data.org API

### Android

* DataStore
* WorkManager
* Coroutines

---

## Screenshots

### Predictions

*Add screenshot here*

### Leaderboard

*Add screenshot here*

### Match Details

*Add screenshot here*

---

## Installation

1. Download the APK
2. Allow installation from unknown sources
3. Install the application
4. Create an account
5. Start predicting

---

## Future Improvements

* Push notifications for leaderboard changes
* Match statistics and standings
* Tournament winner predictions
* Custom private leagues
* Release build signing and Play Store deployment

---

## Author

Developed by Edvin Livak

Built as a family World Cup prediction app and Android portfolio project.
