resolvers ++= Seq (
  "zentrope" at "http://zentrope.com/maven"
)

libraryDependencies ++= Seq (
    "com.zentrope" %% "xsbt-scalate-precompile-plugin" % "1.4"
)

