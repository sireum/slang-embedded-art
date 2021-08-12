::#! 2> /dev/null                                   #
@ 2>/dev/null # 2>nul & echo off & goto BOF         #
if [ -z ${SIREUM_HOME} ]; then                      #
  echo "Please set SIREUM_HOME env var"             #
  exit -1                                           #
fi                                                  #
exec ${SIREUM_HOME}/bin/sireum slang run "$0" "$@"  #
:BOF
setlocal
if not defined SIREUM_HOME (
  echo Please set SIREUM_HOME env var
  exit /B -1
)
%SIREUM_HOME%\bin\sireum.bat slang run "%0" %*
exit /B %errorlevel%
::!#
// #Sireum

import org.sireum._
import org.sireum.project.ProjectUtil._
import org.sireum.project.Project

val library = "library"
val test = "test"

val art = "slang-embedded-art"

val homeDir = Os.slashDir.up.canon

val artShared = moduleSharedPub(
  id = art,
  baseDir = homeDir,
  sharedDeps = sharedId(library),
  sharedIvyDeps = ISZ(),
  pubOpt = pub(
    desc = "HAMR AADL Runtime",
    url = "github.com/sireum/art",
    licenses = bsd2,
    devs = ISZ(jasonBelt, johnHatcliff, matthewWeis, robby)
  )
)

val project = Project.empty + artShared

projectCli(Os.cliArgs, project)