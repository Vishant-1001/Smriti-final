# Privacy and data

Status: describes the code as of milestone M1. Not reviewed by a privacy professional, and no compliance claim is made.

## Permissions

The merged manifest requests **no** dangerous permissions and **no `INTERNET` permission**. The only `<queries>` entry lets the app find the device's text-to-speech engine. Photos are chosen with the system Photo Picker and songs with the system document picker, and neither needs a storage permission. There is no camera or microphone use.

## What is stored, where

| Data | Where | Why |
|---|---|---|
| Current scene stack, settings, Memory Book pages (captions, photo file names), playlist, selected track | `files/datastore/smriti_save.json` (app-private) | To resume where the person left off |
| Caregiver-chosen photos | `files/memory_photos/<random>.img` | Copied so they keep working offline |
| Caregiver-chosen songs | `files/radio_tracks/<random>.audio` | Same |

Nothing else is stored: no names, ages, health information, timings, error counts, scores or activity log. Deleting a page or song in Caregiver tools deletes its private copy. Uninstalling the app or using "Clear storage" removes everything.

## What leaves the device

Nothing. There is no network code, analytics, advertising, crash reporting, account or cloud sync. Speech uses only on-device voices: voices that need a network connection are treated as unavailable.

## Backup and device transfer

`allowBackup="false"`. `data_extraction_rules.xml` (Android 12+) and `backup_rules.xml` (Android 11 and lower) exclude all app data from cloud backup and device-to-device transfer. The downside: personal Memory Book pages are **not** carried to a new phone. A deliberate, consented export could be designed later.

## Not implemented / not claimed

There is no encryption at rest beyond Android's own file-based encryption, no data-protection compliance claim, and no export feature.
