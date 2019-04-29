package repose

import farseek.FarseekBaseMod
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLModIdMappingEvent
import repose.block.FallingBlockExtensions
import repose.config.ReposeConfig

/** @author delvr */
@Mod(modid = "repose", modLanguage = "scala", useMetadata = true, guiFactory = FarseekBaseMod.GuiFactory)
object ReposeMod extends FarseekBaseMod {

    lazy val configuration = Some(ReposeConfig)

    @EventHandler def handle(event: FMLModIdMappingEvent): Unit = FallingBlockExtensions.setSpongeNeighborOverrides()
}
