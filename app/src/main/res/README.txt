O-Beat Android icon pack generated from ic_launcher-playstore.png

Copy the 'res' folder contents into: app/src/main/res/

This pack includes:
- Adaptive icons (ic_launcher.xml, ic_launcher_round.xml)
- Foreground from bitmap with 12dp safe inset
- Background color: #16181B
- Monochrome variant from the same bitmap
- Legacy PNGs for mdpi..xxxhdpi
- Optional values/strings_appname_obeat.xml setting app_name=O-Beat

Make sure your AndroidManifest.xml has:
    android:icon="@mipmap/ic_launcher"
    android:roundIcon="@mipmap/ic_launcher"

Then clean & rebuild:
    gradlew :app:clean :app:assembleFullDebug
