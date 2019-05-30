package repose

import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.config.ModConfig.Type._
import repose.config.ReposeConfig

object ReposeMod {

  val Id = "repose"

  //TODO: restore event handling, and use default config file name, once https://github.com/MinecraftForge/Scorge/issues/3 is fixed
  //ScorgeModLoadingContext.get.getModEventBus.register(this)

  //@SubscribeEvent def init(event: FMLCommonSetupEvent): Unit =
    ModLoadingContext.get.registerConfig(SERVER, ReposeConfig.build, s"$Id-server.toml")
}
