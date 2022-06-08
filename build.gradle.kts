plugins {
    id("com.android.application") version "7.1.2" apply false
    id("com.android.library") version "7.1.2" apply false
    id("org.jetbrains.kotlin.android") version "1.7.0" apply false
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}