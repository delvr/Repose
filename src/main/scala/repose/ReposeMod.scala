package repose

import cpw.mods.fml.common.Mod
import farseek.FarseekBaseMod
import repose.config.ReposeConfig

/** Non-core mod class for Repose. `@Mod` annotation parameters for mod and dependencies versions should be replaced by
  * the build process.
  * @see [[repose.core.ReposeCoreMod]] for core mod class.
  * @author delvr
  */
@Mod(modLanguage = "scala", modid = "repose", version = "SNAPSHOT",
    dependencies = "required-after:farseek", guiFactory = "farseek.client.ConfigGui")
object ReposeMod extends FarseekBaseMod {

    val name = "Repose"
    val description = "Implements walkable soil slopes and soil falling/spreading behaviors."
    val authors = Seq("delvr")

    lazy val configuration = Some(ReposeConfig)
}
