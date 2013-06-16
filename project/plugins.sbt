resolvers ++= Seq(
  Classpaths.typesafeResolver,
  "scct-github-repository" at "http://mtkopone.github.com/scct/maven-repo",
  "oss sonatype" at "https://oss.sonatype.org/content/groups/public/",
  "digimead-maven" at "http://storage.googleapis.com/maven.repository.digimead.org/"
)

//addSbtPlugin("org.digimead" % "sbt-aspectj-nested" % "0.0.1.1-SNAPSHOT")

addSbtPlugin("org.digimead" % "sbt-osgi-manager" % "0.0.1.4")

addSbtPlugin("org.digimead" % "sbt-dependency-manager" % "0.6.4.5")
