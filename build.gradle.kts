import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "2.1.21"
	id("fabric-loom") version "1.10-SNAPSHOT"
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
	archivesName.set(project.property("archives_base_name") as String)
}

val targetJavaVersion = 17
java {
	toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
}

loom {
	splitEnvironmentSourceSets()

	mods {
		register("quic_protocol_support") {
			sourceSet("main")
			sourceSet("client")
		}
	}
}

repositories {
	mavenCentral()
	flatDir {
		dirs("libs")
	}
}

dependencies {
	// Minecraft
	minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
	mappings("net.fabricmc:yarn:${project.property("yarn_mappings")}:v2")
	modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
	modImplementation("net.fabricmc:fabric-language-kotlin:${project.property("kotlin_loader_version")}")
	// QUIC
	include("io.netty.incubator:netty-incubator-codec-classes-quic:${project.property("netty_quic_version")}")
	implementation("io.netty.incubator:netty-incubator-codec-classes-quic:${project.property("netty_quic_version")}")
	for (classifier in arrayOf("linux-aarch_64", "linux-x86_64", "windows-x86_64")) {
		include("io.netty.incubator:netty-incubator-codec-native-quic:${project.property("netty_quic_version")}:${classifier}")
	}

	runtimeOnly("io.netty.incubator:netty-incubator-codec-native-quic:${project.property("netty_quic_version")}:windows-x86_64")
	// hashing
	include("commons-codec:commons-codec:${project.property("commons_codec_version")}")
	implementation("commons-codec:commons-codec:${project.property("commons_codec_version")}")
}

tasks.processResources {
	inputs.property("version", project.version)
	inputs.property("minecraft_version", project.property("minecraft_version"))
	inputs.property("loader_version", project.property("loader_version"))
	filteringCharset = "UTF-8"

	filesMatching("fabric.mod.json") {
		expand(
			"version" to project.version,
			"minecraft_version" to project.property("minecraft_version"),
			"loader_version" to project.property("loader_version"),
			"kotlin_loader_version" to project.property("kotlin_loader_version")
		)
	}
}

tasks.withType<JavaCompile>().configureEach {
	options.encoding = "UTF-8"
	options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
	compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

tasks.jar {
	from("LICENSE") {
		rename { "${it}_${project.base.archivesName}" }
	}
}
