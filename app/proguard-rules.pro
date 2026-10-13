# A Compose + R8 teljes módban stabil, de a Telecom szolgáltatást mindenképp megtartjuk.
-keep class com.bbetterdialer.telecom.** { *; }
-keep class android.telecom.** { *; }
-dontwarn android.telecom.**

# A saját modelljeinket nem kell védeni, de a nevek megőrzése segít a hibakeresésben.
-keepnames class com.bbetterdialer.data.** { *; }
