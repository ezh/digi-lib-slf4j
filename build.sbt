//
// Copyright (c) 2012-2013 Alexey Aksenov ezh@ezh.msk.ru
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

name := "Digi-Lib-SLF4J"

description := "SLF4J binding for Digi components"

organization := "org.digimead"

version <<= (baseDirectory) { (b) => scala.io.Source.fromFile(b / "version").mkString.trim }

scalaVersion := "2.10.1"

crossScalaVersions := Seq("2.10.1")

scalacOptions ++= Seq("-encoding", "UTF-8", "-deprecation", "-unchecked", "-Xcheckinit", "-feature") ++
  (if (true || (System getProperty "java.runtime.version" startsWith "1.7")) Seq() else Seq("-optimize")) // -optimize fails with jdk7

javacOptions ++= Seq("-Xlint:unchecked", "-Xlint:deprecation")

publishTo  <<= baseDirectory  { (base) => Some(Resolver.file("file",  base / "publish/releases" )) }

resolvers += ("snapshots" at "http://oss.sonatype.org/content/repositories/snapshots")

moduleConfigurations := {
  val digilib = "digi-lib" at "http://ezh.github.com/digi-lib/releases"
  Seq(
    ModuleConfiguration("org.digimead", "digi-lib", digilib)
  )
}

libraryDependencies ++= {
  Seq(
    "org.digimead" %% "digi-lib" % "0.2.2-SNAPSHOT",
    "org.slf4j" % "slf4j-api" % "1.7.1"
  )
}

sourceDirectory in Test  <<= baseDirectory / "Testing Infrastructure Is Absent"
