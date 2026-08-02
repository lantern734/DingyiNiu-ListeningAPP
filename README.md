# DingyiNiu Listening App

A WAV audio player for Android. You can use it to split, merge, delete, and undo operations on WAV audio.

It's suitable for content that is primarily human speech with natural pauses. It's not suitable for pure music or content with background music — such audio has no identifiable silence points, and splitting will fail.

If you're learning a foreign language, you can use it with a listening training method.

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
![Platform: Android](https://img.shields.io/badge/Platform-Android-green.svg)
[![Release](https://img.shields.io/github/v/release/lantern734/DingyiNiu-ListeningAPP)](https://github.com/lantern734/DingyiNiu-ListeningAPP/releases/latest)

## Download

The APK and the silence file are available in the [latest release](https://github.com/lantern734/DingyiNiu-ListeningAPP/releases/latest).

## Preparation

Before using, add five folders.

The first folder should contain one file: `silence_1min.wav`, downloaded from Releases. This is a one-minute silent audio file. Putting it in the first folder prevents the App from being unexpectedly interrupted during playback. Click "Add Folder" in the top-left corner to add this folder.

The second through fifth folders are favorites folders. You can put noteworthy content into the favorites, and a single WAV audio file can be placed in different favorites folders. It's recommended to put content of different themes into different favorites folders, which helps categorize WAV audio. Click "Favorites Directory" in the bottom-left corner to set up these 4 favorites folders. After setting them up, close and restart the App, and you'll see these 4 favorites folders in the folder list on the left.

Folders added afterward are used to store the WAV audio you'll actually be working with. Click "Add Folder" in the top-left corner to add such folders. Folders are displayed in the order in which you add them.

Within each folder, WAV files are listed in natural sort order by filename — numbers are compared as numbers, so `name_2.wav` comes before `name_10.wav` rather than after it. This order determines which file plays next, and which file Merge combines with.

## WAV Editing Operations

### 1. Split

Select an audio file and press Split. The App will split the audio into short segments at silence points. The split files will be renamed using the rule: original_name_1, original_name_2, and so on.

The App splits at the exact middle of any silence longer than the threshold. The default threshold is 0.2 seconds. You should choose an appropriate splitting threshold based on the characteristics of the audio. Click the gear icon next to the Split button to modify the threshold.

Splitting is lossless.

### 2. Merge

Select an audio file and press Merge. The App will merge it with the file immediately below it in the same folder's track list. The merged file keeps the name of the upper file.

If the two files share the same sample rate, bit depth, and channel count, merging is lossless. If any of these differ, the App will automatically convert both files to a unified format — taking the maximum of each — and then concatenate them; in this case a notification will tell you the format that was used, and the merge is no longer strictly lossless.

Files produced by splitting always share these three properties, so merging files that came from the same split is always lossless.

### 3. Delete

Select an audio file and press Delete. The App will remove the audio file from both local storage and the playlist.

### 4. Undo

After clicking Split, Merge, or Delete, you can click Undo to cancel the last operation.

## Shortcuts

When headphones are connected to the device, you can use the headphone shortcuts to pause, resume, go to the previous track, and go to the next track.

When a keyboard is connected to the device, you can perform the four operations above via keyboard shortcuts.

When a keyboard is connected to the device, you can use the following keyboard shortcuts:

| Action                           | Shortcut | Action                           | Shortcut |
| -------------------------------- | -------: | -------------------------------- | -------: |
| Split                            |        9 | Merge                            |        0 |
| Delete                           |      Del | Undo                             | Ctrl + Z |
| Add to / remove from Favorites 1 |        1 | Add to / remove from Favorites 2 |        2 |
| Add to / remove from Favorites 3 |        3 | Add to / remove from Favorites 4 |        4 |
| Previous track                   |        ← | Next track                       |        → |
| Play / Pause                     |    Space |                                  |          |


To avoid key conflicts, keyboard shortcuts are available only when the app is in the foreground. Headphones controls can be used whether the app is in the foreground or running in the background.

## For Language Learning

This App was originally built to support a listening training method.

The process is as follows. Find an audio with a transcript, and split it into many short audio segments, each a few seconds long (a duration of 2 to 7 seconds is recommended) and each a complete unit of meaning. Play them in random order. For each audio segment, locate its position in the transcript using only what you hear.

For materials, dictionary audio with example sentences read aloud is the most suitable. It's recommended to pair each meaning of a word with 2 to 3 example sentences. Speeches, interviews, dialogues, and listening exam audio in the target language can also be used. Audio difficulty of B1 or above is recommended. If B1-level audio feels too difficult, it's recommended to first study an introductory textbook for the relevant language. After reaching A2 level, studying B1-difficulty material generally won't pose major obstacles.

If there's no transcript, you can use [Buzz](https://github.com/chidiwilliams/buzz) to generate one — it's a free, open-source local tool.

Turn on shuffle play during training. Locate each audio segment in the transcript by hearing alone. The goal is to recognize and successfully locate each segment within two plays. A single training session is not recommended to exceed 150 segments. If it feels too difficult, you can first listen to the audio sequentially once or twice while reading the transcript, then do the random training.

It's recommended to use headphones during training, because you need to flip through the transcript frequently, and the headphone shortcuts save the time of switching back and forth between apps. If you don't have headphones, it's recommended to use a large-screen Android tablet in split-screen mode, with the transcript on one side and the App on the other.

The author's experience: select a total of 12 hours of audio, then train using this method, ensuring that every segment can be recognized and located within 2 plays — this can raise listening and reading ability in the corresponding language to C1 or even higher. Actual practice time is typically 30 to 40 times the audio duration. Although the time looks long, this method is very efficient. Spending 360 to 480 hours to raise a language's listening and reading ability from A2 to C1 is very fast.

## 🎯 Challenge Mode

Introduced in version 0.2.0, a "Challenge Mode" button has been added to the top-left corner of the interface. This mode is specifically designed for reviewing and reinforcing WAV files you have already mastered.

In Challenge Mode, users can randomly listen to WAV files selected from *all* folders. You can choose the quantity of random tracks for your session: 50, 100, 200, or All. 

The selection mechanism follows a dynamic algorithm:
* 📈 WAV files that you perform well on during the challenge will have a lower probability of being selected in future challenges.
* 📉 WAV files that you struggle with (perform poorly on) will have a higher probability of appearing in future challenges.
* ⭐ WAV files that have been added to your favorites folders have a slightly higher baseline probability of being selected.

**Best Practices for Challenge Mode:**
1. **Prerequisites:** Before starting a challenge, ensure that you have completed the preliminary listening practice for the WAV files across all your folders. 
2. **Focus:** During the challenge, you should concentrate as much as possible and try to fully comprehend the meaning of each WAV segment you hear.

## Notes

1. During shuffle play, if you suddenly want to hear a specific audio within the same folder, you can click directly on the one you want — this won't disrupt the subsequent order. When you click Next, whatever was originally going to play randomly will play. Do not open other folders, or the shuffled order will be disrupted.

2. After the App closes unexpectedly, reopen it and click the Restore button in the bottom-left corner; the current track and the random order will both be restored. However,this feature cannot be used in Challenge Mode.

3. In Challenge Mode, headset shortcuts may not work well. Moving the app to the background can effectively prevent this issue. If you need to keep the app in the foreground, connecting a keyboard is recommended. Keyboard shortcuts work normally in the foreground.

4. After you finish using the App, a notification may remain in the notification bar. If you can't swipe it away, long-press the App icon, select App Info, and tap Force Stop to remove the notification.

5. To use the app on Windows or Apple devices, you can try downloading the `index.html` file from the `www` folder and opening it in a browser. Note that the program can access only folders for which the user has granted permission. On non-Android systems, you may need to grant access to the folders repeatedly.

## For Developers

The App is built with [Capacitor](https://capacitorjs.com/). The core logic is in `www/index.html`, a single-file web app. Wrapped around it are two Android native plugins: `SafPlugin.java` handles file system access, and `MediaButtonPlugin.java` handles media buttons.

Build from source:

```bash
git clone https://github.com/lantern734/DingyiNiu-ListeningAPP.git
cd DingyiNiu-ListeningAPP
npm install
npx cap sync android
npx cap open android
```

Then build the APK in Android Studio.

Project structure:

```
DingyiNiu-ListeningAPP/
├── www/
│   └── index.html
├── android/
│   └── app/src/main/java/com/wavplayer/app/
│       ├── MainActivity.java
│       ├── SafPlugin.java
│       ├── MediaButtonPlugin.java
│       └── MediaPlaybackService.java
├── capacitor.config.json
└── package.json
```

## License

[MIT License](LICENSE).

## Feedback

For bugs or suggestions, please open an [issue](https://github.com/lantern734/DingyiNiu-ListeningAPP/issues).
