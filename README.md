# MeepMeep

This repository is a fork of [MeepMeep](https://github.com/NoahBres/MeepMeep) that supports [Road Runner 1.0](https://rr.brott.dev/docs/v1-0/new-features/).

<img src="/images/readme/screen-recording.gif" width="500" height="500"/>

# Table of Contents

- [Installing (Android Studio)](#-installing-android-studio)
- [Misc](#misc)
  - [Poor Performance?](#poor-performance)
  - [Adding a second bot](#adding-a-second-bot)
  - [Pulling Specific Jitpack Commits](#pulling-specific-jitpack-commits)
  - [Misc. Notes](#notes)

# Installing (Android Studio)

**Video instructions found here: https://youtu.be/vdn1v404go8. Please note that it was recorded in 2021 and may be outdated at the time of viewing**

1.  In Android Studio, click on the "FtcRobotController" Module, then right click on the FtcRobotController folder and click `New > Module`
    <img src="/images/readme/installationStep1.png" width="751" height="287"/>
2.  On the left part of this window, select "Java or Kotlin Library"
    <img src="/images/readme/installationStep2.png" width="544" height="382"/>

3.  From here, remove the `:ftcrobotcontroller:lib` in the "Library Name" section, and rename it to `MeepMeepTesting`. You may use whatever name you wish but the rest of the instructions will assume you have chosen the name `MeepMeepTesting`. Ensure that you also change the "class name" section to match.

4.  Hit "Finish" at the bottom right of the Module Create window.

5.  Open up the `build.gradle` file for the MeepMeepTesting module (or whatever you chose to name it prior). In this file, change all instances `JavaVersion.VERSION_1_7` to `JavaVersion.VERSION_1_8`
    <img src="/images/readme/installationStep5.png" width="566" height="274"/>

6.  At the bottom of the file add the following gradle snippet:

        
        repositories {
            maven { url = 'https://maven.brott.dev/' }
        }

        dependencies {
            implementation 'com.acmerobotics.roadrunner:MeepMeep:0.1.7'
        }
        

7.  When android studio prompts you to make a gradle sync, click "Sync Now".
    <img src="/images/readme/installationStep7.png" width="644" height="20"/>

8.  Create a class for your MeepMeepTesting java module if it does not yet exist. Paste the following sample in it. Feel free to change this later.

```java
package com.example.meepmeeptesting;

import com.acmerobotics.roadrunner.Pose2d;
import com.noahbres.meepmeep.MeepMeep;
import com.noahbres.meepmeep.roadrunner.DefaultBotBuilder;
import com.noahbres.meepmeep.roadrunner.entity.RoadRunnerBotEntity;

public class MeepMeepTesting {
    public static void main(String[] args) {
        MeepMeep meepMeep = new MeepMeep(800);

        RoadRunnerBotEntity myBot = new DefaultBotBuilder(meepMeep)
                // Set bot constraints: maxVel, maxAccel, maxAngVel, maxAngAccel, track width
                .setConstraints(60, 60, Math.toRadians(180), Math.toRadians(180), 15)
                .build();

        myBot.runAction(myBot.getDrive().actionBuilder(new Pose2d(0, 0, 0))
                .lineToX(30)
                .turn(Math.toRadians(90))
                .lineToY(30)
                .turn(Math.toRadians(90))
                .lineToX(0)
                .turn(Math.toRadians(90))
                .lineToY(0)
                .turn(Math.toRadians(90))
                .build());

        meepMeep.setBackground(MeepMeep.Background.FIELD_POWERPLAY_OFFICIAL)
                .setDarkMode(true)
                .setBackgroundAlpha(0.95f)
                .addEntity(myBot)
                .start();
    }
}
```

9. Create a run configuration for Android Studio.
   1. First, click on the drop down menu on the top bar of Android Studio, where it says "TeamCode" with a little Android logo next to it.
   2. Click `Edit Configurations`
   3. Click on the "+" symbol in the top left of the window, and when it prompts you, select "Application".
   4. Change the name to your liking (ex. meepmeep-run)
   5. Where it says "module not specified", click to open the dropdown, then select your JRE.
   6. Where it says "cp <no module>" click it to open the dropdown, and then select FtcRobotController.MeepMeepTesting.main
   7. Where it says "Main Class", click the little "file" icon to the right of the text and then select the name of the main class for your MeepMeepTesting module.
   8. From here, in the bottom right of the window, press "Apply" then "Ok".
   9. It will now automatically switch to that Run/Debug Configuration profile.
10. If at any point you would like to build code onto your Control Hub or Phone, then click the Run/Debug configuration profile at the top to open the dropdown menu and select TeamCode. Perform the same steps to switch back to MeepMeepRun.

# Misc

### Poor Performance?

On some systems, hardware acceleration may not be enabled by default.
To enable hardware acceleration use the cli flag: `-Dsun.java2d.opengl=true`.

Or, enable it _before_ initializing your `MeepMeep` instance with the following snippet:
`System.setProperty("sun.java2d.opengl", "true");`

### Adding a second bot:

Declare a new `RoadRunnerBotEntity` and add it via `MeepMeep#addEntity(Entity)`.

<img src="/images/readme/two-bot-demo.gif" width="500" height="500"/>

```java
package com.example.meepmeeptesting;

import com.acmerobotics.roadrunner.Pose2d;
import com.noahbres.meepmeep.MeepMeep;
import com.noahbres.meepmeep.core.colorscheme.scheme.ColorSchemeBlueDark;
import com.noahbres.meepmeep.core.colorscheme.scheme.ColorSchemeRedDark;
import com.noahbres.meepmeep.roadrunner.DefaultBotBuilder;
import com.noahbres.meepmeep.roadrunner.entity.RoadRunnerBotEntity;

public class MeepMeepTesting {
    public static void main(String[] args) {
        MeepMeep meepMeep = new MeepMeep(800);

        // Declare our first bot
        RoadRunnerBotEntity myFirstBot = new DefaultBotBuilder(meepMeep)
                // We set this bot to be blue
                .setColorScheme(new ColorSchemeBlueDark())
                .setConstraints(60, 60, Math.toRadians(180), Math.toRadians(180), 15)
                .build();

        myFirstBot.runAction(myFirstBot.getDrive().actionBuilder(new Pose2d(0, 0, 0))
                .lineToX(30)
                .turn(Math.toRadians(90))
                .lineToY(30)
                .turn(Math.toRadians(90))
                .lineToX(0)
                .turn(Math.toRadians(90))
                .lineToY(0)
                .turn(Math.toRadians(90))
                .build());

        // Declare out second bot
        RoadRunnerBotEntity mySecondBot = new DefaultBotBuilder(meepMeep)
                // We set this bot to be red
                .setColorScheme(new ColorSchemeRedDark())
                .setConstraints(60, 60, Math.toRadians(180), Math.toRadians(180), 15)
                .build();

        mySecondBot.runAction(mySecondBot.getDrive().actionBuilder(new Pose2d(30, 30, Math.toRadians(180)))
                .lineToX(0)
                .turn(Math.toRadians(90))
                .lineToY(0)
                .turn(Math.toRadians(90))
                .lineToX(30)
                .turn(Math.toRadians(90))
                .lineToY(30)
                .turn(Math.toRadians(90))
                .build());

        meepMeep.setBackground(MeepMeep.Background.FIELD_FREIGHTFRENZY_ADI_DARK)
                .setDarkMode(true)
                .setBackgroundAlpha(0.95f)
                // Add both of our declared bot entities
                .addEntity(myFirstBot)
                .addEntity(mySecondBot)
                .start();
    }
}
```

### Notes:

Default Bot Settings:

- Constraints
- Max Vel: 30in/s
- Max Accel: 30in/s/s
- Max Ang Vel: 60deg/s
- Max Ang Accel: 60deg/s/s
- Track Width: 15in
- Bot Width: 18in
- Bot Height: 18in
- Start Pose: (x: 0in, y: 0in, heading: 0rad)
- Color Scheme: Inherited from MeepMeep.colorManager unless overriden
- Drive Train Type: Mecanum
