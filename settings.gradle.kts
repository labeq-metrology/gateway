import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.mavenCentral

rootProject.name = "gateway"
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}