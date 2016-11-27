package repose

import farseek.FarseekBaseMod
import net.minecraftforge.fml.common.Mod
import repose.config.ReposeConfig

/** @author delvr */
@Mod(modid = "repose", modLanguage = "scala", guiFactory = FarseekBaseMod.GuiFactory)
object ReposeMod extends FarseekBaseMod {

    lazy val configuration = Some(ReposeConfig)
}
