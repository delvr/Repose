package repose

import cpw.mods.fml.common.Mod
import farseek.FarseekBaseMod
import repose.config.ReposeConfig

/** @author delvr */
@Mod(modid = "repose", modLanguage = "scala", useMetadata = true, dependencies = "after:terrafirmacraft", guiFactory = "farseek.client.ConfigGui")
object ReposeMod extends FarseekBaseMod {

    lazy val configuration = Some(ReposeConfig)
}
