// Top-level build file for Wildlife FieldOps native Android app
buildscript {
    // Pin R8 past 8.5.35: the R8 bundled with AGP 8.5.2 crashes with
    // java.util.ConcurrentModificationException during minifyReleaseWithR8
    // (issuetracker.google.com/359385828, fixed in R8 8.5.45+). 8.6.27 is the
    // stable R8 shipped with AGP 8.6; AGP honors the newest R8 found on the
    // root buildscript classpath, so this overrides the bundled one without
    // touching the AGP version.
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools:r8:8.6.27")
    }
}

plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
    id("com.google.devtools.ksp") version "1.9.25-1.0.20" apply false
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.25" apply false
}
