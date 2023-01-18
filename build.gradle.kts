buildscript {
    repositories {
        mavenCentral()
        google()
    }
    dependencies {
        classpath("com.android.tools.build", "gradle", "7.4.0")
        classpath("org.jetbrains.kotlin", "kotlin-gradle-plugin", "1.8.0")
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}
