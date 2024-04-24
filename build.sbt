import sbt.util.Logger
import sbt.util.CacheStoreFactory

scalaVersion := "3.4.1"

// See https://www.scala-sbt.org/1.x/docs/Custom-Settings.html#Defining+a+key
val myConfig = settingKey[String]("A configuration for sample task")
val myTask   = taskKey[Unit]("A sample task.")

val root = (project in file("."))
  .settings(
    myConfig := "{}",
    myTask := {
      val log    = streams.value.log
      val config = myConfig.value
      myCachedTaskLogic(streams.value.cacheStoreFactory)(log, config)
    },
    Compile / compile := {
      myTask.value
      (Compile / compile).value
    }
  )

def myTaskLogic(log: Logger, config: String) = {
  log.info("Executing myTask logic!")
  Thread.sleep(5000)
  log.info("Finished myTask logic!")
  s"Processed config: $config"
}

// See https://www.scala-sbt.org/1.x/docs/Caching.html
def myCachedTaskLogic(cache: CacheStoreFactory)(log: Logger, config: String) = {
  import sbt.util.CacheImplicits._

  val cached =
    Tracked.inputChanged[String, String](
      cache.make("my-input-cache")
    ) {
      Function.untupled {
        Tracked.lastOutput[(Boolean, String), String](
          cache.make("my-output-cache")
        ) { case ((inputChanged, arg), outputs) =>
          if (inputChanged || outputs.isEmpty) {
            log.info(s"Regenerating results for argument: $arg")
            log.info(s"Input changed: $inputChanged")
            log.info(s"Last known output: $outputs")
            myTaskLogic(log, arg)
          } else {
            log.info("Using cached version of outputs")
            outputs.getOrElse("")
          }
        }
      }
    }

  cached(config)

}

/*
Additional reference:
  https://github.com/disneystreaming/smithy4s/blob/bb0b26f44c93ee8a97596a22bb2ba771aa31c52f/modules/codegen-plugin/src/smithy4s/codegen/Smithy4sCodegenPlugin.scala
  https://github.com/disneystreaming/smithy4s/blob/bb0b26f44c93ee8a97596a22bb2ba771aa31c52f/modules/codegen-plugin/src/smithy4s/codegen/JsonConverters.scala
  https://github.com/disneystreaming/smithy4s/pull/1499/
 */
