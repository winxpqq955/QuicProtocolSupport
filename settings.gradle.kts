pluginManagement {
	repositories {
		maven("https://maven.fabricmc.net/") {
			name = "Fabric"
		}
		gradlePluginPortal()
	}
}

include("velocity-support")
