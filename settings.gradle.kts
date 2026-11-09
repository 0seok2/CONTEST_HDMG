pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositories { google(); mavenCentral() }
    // TODO: Pleos Vehicle SDK 저장소 주소를 확인하면 여기에 maven { url = uri("...") } 추가
}
rootProject.name = "PedalGuardian"
include(":app")
