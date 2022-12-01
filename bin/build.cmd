::/*#! 2> /dev/null                                                                                         #
@ 2>/dev/null # 2>nul & echo off & goto BOF                                                                 #
export SIREUM_HOME=$(cd -P $(dirname "$0")/.. && pwd -P)                                                    #
if [ ! -z ${SIREUM_PROVIDED_SCALA++} ]; then                                                                #
  SIREUM_PROVIDED_JAVA=true                                                                                 #
fi                                                                                                          #
"${SIREUM_HOME}/bin/init.sh"                                                                                #
if [ -n "$COMSPEC" -a -x "$COMSPEC" ]; then                                                                 #
  export SIREUM_HOME=$(cygpath -C OEM -w -a ${SIREUM_HOME})                                                 #
  if [ -z ${SIREUM_PROVIDED_JAVA++} ]; then                                                                 #
    export PATH="${SIREUM_HOME}/bin/win/java":"${SIREUM_HOME}/bin/win/z3":$PATH                             #
    export PATH="$(cygpath -C OEM -w -a ${JAVA_HOME}/bin)":"$(cygpath -C OEM -w -a ${Z3_HOME}/bin)":$PATH   #
  fi                                                                                                        #
elif [ "$(uname)" = "Darwin" ]; then                                                                        #
  if [ -z ${SIREUM_PROVIDED_JAVA++} ]; then                                                                 #
    export PATH="${SIREUM_HOME}/bin/mac/java/bin":"${SIREUM_HOME}/bin/mac/z3/bin":$PATH                     #
  fi                                                                                                        #
elif [ "$(expr substr $(uname -s) 1 5)" = "Linux" ]; then                                                   #
  if [ -z ${SIREUM_PROVIDED_JAVA++} ]; then                                                                 #
    export PATH="${SIREUM_HOME}/bin/linux/java/bin":"${SIREUM_HOME}/bin/linux/z3/bin":$PATH                 #
  fi                                                                                                        #
fi                                                                                                          #
if [ -f "$0.com" ] && [ "$0.com" -nt "$0" ]; then                                                           #
  exec "$0.com" "$@"                                                                                        #
else                                                                                                        #
  rm -fR "$0.com"                                                                                           #
  exec "${SIREUM_HOME}/bin/sireum" slang run -n "$0" "$@"                                                   #
fi                                                                                                          #
:BOF
setlocal
call "%~dp0init.bat"
set NEWER=False
if exist %~dpnx0.com for /f %%i in ('powershell -noprofile -executionpolicy bypass -command "(Get-Item %~dpnx0.com).LastWriteTime -gt (Get-Item %~dpnx0).LastWriteTime"') do @set NEWER=%%i
if "%NEWER%" == "True" goto native
del "%~dpnx0.com" > nul 2>&1
if defined SIREUM_PROVIDED_SCALA set SIREUM_PROVIDED_JAVA=true
if not defined SIREUM_PROVIDED_JAVA set PATH=%~dp0win\java\bin;%~dp0win\z3\bin;%PATH%
"%~dp0sireum.bat" slang run -n "%0" %*
exit /B %errorlevel%
:native
%~dpnx0.com %*
exit /B %errorlevel%
::!#*/
// #Sireum
import org.sireum._


def usage(): Unit = {
  println("Sireum HAMR AADL Runtime Services /build")
  println("Usage: ( compile | test | m2 )+")
}


if (Os.cliArgs.isEmpty) {
  usage()
  Os.exit(0)
}


val homeBin = Os.slashDir
val home = homeBin.up
val mill = homeBin / "mill.bat"
val sireum : Os.Path = homeBin / (if (Os.isWin) "sireum.bat" else "sireum")

val proyekName: String = "sireum-proyek"
val project: Os.Path = homeBin / "project4testing.cmd"


def tipe(): Unit = {
  println("Slang type checking ...")
  Os.proc(ISZ(sireum.string, "slang", "tipe", "--verbose", "-r", "-x", "out", "-s", home.string)).
    at(home).console.runCheck()
  println()
}

def compile(): Unit = {
  tipe()

  println("Compiling ...")
  proc"$sireum proyek compile --project ${project} -n $proyekName --par --sha3 .".at(home).console.runCheck()
  println()
}

def test(): Unit = {
  tipe()

  val names: String = "art"

  println("Testing ...")
  proc"$sireum proyek test --project ${project} -n ${proyekName} --par --sha3 . ${names}".at(home).console.runCheck()
  println()
}


def m2(): Os.Path = {
  tipe()

  val repository = Os.home / ".m2" / "repository"
  val artRepo = repository / "org" / "sireum" / "slang-embedded-art"
  artRepo.removeAll()
  proc"$sireum proyek publish --project $project -n $proyekName --target jvm --par --sha3 --ignore-runtime --m2 ${repository.up.canon} . org.sireum".at(home).console.run()

  return artRepo
}

def cloneRuntime(): Unit ={
  (home / "runtime").removeAll()
  Os.proc(ISZ[String]("git", "clone", "--depth=1", "https://github.com/sireum/runtime")).at(home).console.runCheck()
}

def installToolsViaKekinian(): Unit = {
  val builtIn = home / "runtime" / "library" / "shared" / "src" / "main" / "scala" / "org" / "sireum" / "BuiltInTypes.slang"
  if(!builtIn.exists) {
    builtIn.write(".")
  }
  val kbuild = homeBin / "kbuild.cmd"
  kbuild.downloadFrom("https://raw.githubusercontent.com/sireum/kekinian/master/bin/build.cmd")
  proc"$sireum slang run $kbuild --help".at(homeBin).runCheck()
  kbuild.remove()
  if(builtIn.size == 1) {
    (home / "runtime").removeAll()
  }
}

installToolsViaKekinian()

for (i <- 0 until Os.cliArgs.size) {
  Os.cliArgs(i) match {
    case string"compile" =>
      cloneRuntime()
      compile()
    case string"test" =>
      cloneRuntime()
      test()
    case string"m2" =>
      cloneRuntime()
      m2()
    case cmd =>
      usage()
      eprintln(s"Unrecognized command: $cmd")
      Os.exit(-1)
  }
}
