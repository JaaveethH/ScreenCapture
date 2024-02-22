# Screen Capture App
![ScreenRecordHD2024-02-22-12-00-58-ezgif com-video-to-gif-converter](https://github.com/JaaveethH/ScreenCapture/assets/83578641/28b27d6a-fe46-4c10-8469-ebda2016cd29)

## Overview

- **Media Projection Usage:** Utilizes Android's media projection API to capture the device screen.
  
- **Media Recorder:** Employs the media recorder functionality to record both video and audio during screen capture.

- **Foreground Service Type and Usage (Android 12 and Above):** Utilizes foreground services to ensure uninterrupted screen recording, specifically taking advantage of changes and improvements introduced in Android 12.

- **Scoped Storage for Recording:** Adheres to Android's scoped storage model for secure and organized storage of recorded videos.

- **Full Control of Media Recorder and Media Projection Functions:** Offers comprehensive control over media recorder and media projection settings, allowing users to customize recording parameters such as resolution, quality, and audio source.


## Table of Contents

- [Installation](#installation)
- [Usage](#usage)
- [Permissions](#permissions)
- [Contributing](#contributing)
- [License](#license)

## Installation

Provide instructions on how to install your app.

```bash
# Clone the repository
git clone https://github.com/JaaveethH/ScreenCapture.git

# Navigate to the project directory
cd screen-capture-app

# Build and install the app on a device or emulator
./gradlew installDebug
