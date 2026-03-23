# screenshotminer
On desktop sentence mining setups, you can use [ShareX](https://getsharex.com/) to quickly add screenshots and audio to your new [Anki](https://github.com/ankitects/anki) cards mined out of [Yomitan](https://github.com/yomidevs/yomitan). ScreenshotMiner aims to emulate that screenshot insertion method for the Android-equivalent mining setups. Works for screenshots as well as pictures taken with the camera.

### Requirements
* [AnkiDroid](https://github.com/ankidroid/Anki-Android) (Be sure to enable AnkiDroid API. Settings > Advanced > Enable AnkiDroid API)
* [KamWithK/AnkiconnectAndroid](https://github.com/KamWithK/AnkiconnectAndroid) (must be active during screenshotting)
* **Yomitan:** (available on Firefox mobile app and Kiwi Browser)

### App Configuration
* You must grant all permissions as prompted. The notification must be active to keep the service alive.
* `Anki Target Field`: The field where ScreenshotMiner will attempt to place images into.
* `Mining Timeout (ms)`: After ScreenshotMiner detects a new card, you have this many milliseconds to take a screenshot for insertion.
* `Redo Timeout (ms)`: You have this many milliseconds to take a new screenshot to override the previous. This is also the window of time you have to crop your screenshot if you choose to.
* `Auto-delete Screenshots`: You must grant permission to delete the new image from your gallery right after it has been added to AnkiDroid.
