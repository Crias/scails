resolvers ++= Seq (
  "zentrope" at "http://zentrope.com/maven"
)

libraryDependencies ++= Seq (
    "com.zentrope" %% "xsbt-scalate-precompile-plugin" % "1.4"
)

resolvers += {
  val typesafeRepoUrl = new java.net.URL("http://repo.typesafe.com/typesafe/releases")
  val pattern = Patterns(false, "[organisation]/[module]/[sbtversion]/[revision]/[type]s/[module](-[classifier])-[revision].[ext]")
  Resolver.url("Typesafe Repository", typesafeRepoUrl)(pattern)
}

libraryDependencies <<= (libraryDependencies, sbtVersion) { (deps, version) => 
  deps :+ ("com.typesafe.sbteclipse" %% "sbteclipse" % "1.2" extra("sbtversion" -> version))
}
