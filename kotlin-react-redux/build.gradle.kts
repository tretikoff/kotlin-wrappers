plugins {
    kotlin("js")
    `publishing-conventions`
}

dependencies {
    api(project(":kotlin-extensions"))
    api(project(":kotlin-react"))
    api(project(":kotlin-redux"))

    api(npmv("react-redux"))
}

signing {
    setRequired({
        gradle.taskGraph.hasTask("publish")
    })

    sign(publishing.publications)
}