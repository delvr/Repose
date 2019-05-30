package repose.config

import net.minecraft.util.ResourceLocation
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.common.ForgeConfigSpec._
import net.minecraftforge.registries.ForgeRegistries
import repose.ReposeMod
import scala.collection.JavaConverters._

object ReposeConfig extends ForgeConfigSpec.Builder {

  type BlockNamesListValue = ConfigValue[java.util.List[_ <: String]]

  push(ReposeMod.Id)

  val granularBlocksWhitelist: BlockNamesListValue = comment(
    s"List of names of blocks considered granular in addition to the default list.")
    .translation("repose.configgui.granularBlocksWhitelist").defineBlockNamesList("granularBlocksWhitelist")

  val granularBlocksBlacklist: BlockNamesListValue = comment(
    s"List of names of blocks NOT considered granular even if present in the default list.")
    .translation("repose.configgui.granularBlocksBlacklist").defineBlockNamesList("granularBlocksBlacklist")

  val naturalStoneBlocksWhitelist: BlockNamesListValue = comment(
    s"List of names of blocks considered natural stone in addition to the default list.")
    .translation("repose.configgui.naturalStoneBlocksWhitelist").defineBlockNamesList("naturalStoneBlocksWhitelist")

  val naturalStoneBlocksBlacklist: BlockNamesListValue = comment(
    s"List of names of blocks NOT considered natural stone even if present in the default list.")
    .translation("repose.configgui.naturalStoneBlocksBlacklist").defineBlockNamesList("naturalStoneBlocksBlacklist")

  val slopingBlockTypes: ConfigValue[String] = comment(
    s"The types of blocks that form walkable slopes. Choices are: ${SlopingBlockTypeChoices.values.mkString(", ")}")
    .translation("repose.configgui.slopingBlockTypes").defineInList("slopingBlockTypes",
      SlopingBlockTypeChoices.GranularBlocks, SlopingBlockTypeChoices.values.asJava)

  val granularFall: BooleanValue = comment(
    s"If true, all blocks made of granular material (not just sand and gravel) can fall down.")
    .translation("repose.configgui.granularFall").define("soilFall", true)

  val blockSpread: BooleanValue = comment(
    s"If true, blocks that fall will also spread into piles instead of forming columns.")
    .translation("repose.configgui.blockSpread").define("blockSpread", true)

  val avalanches: BooleanValue = comment(
    s"If true, non-soil blocks that can spread will also trigger avalanches when disturbed.")
    .translation("repose.configgui.avalanches").define("avalanches", true)

  val sneakingDownSlopes: BooleanValue = comment(
    s"If false, sneaking players don't go down slopes.")
    .translation("repose.configgui.sneakingDownSlopes").define("sneakingDownSlopes", true)

  val slopingShores: BooleanValue = comment(
    s"If false, blocks don't slope at the edge of liquids.")
    .translation("repose.configgui.slopingShores").define("slopingShores", true)

  val maxServerLag: IntValue = comment(
    s"Restricts block falling and spreading behavior when server lag reaches this value in milliseconds (-1 for no maximum)")
    .translation("repose.configgui.maxServerLag").defineInRange("maxServerLag", 2000, -1, Int.MaxValue)

  pop()

  implicit class BuilderOps(val builder: Builder) extends AnyVal {
    def defineBlockNamesList(name: String, defaultValue: Seq[String] = Seq()): BlockNamesListValue =
      builder.defineList(name, defaultValue.asJava, t => ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(t.toString)))
  }

}

object SlopingBlockTypeChoices {
  val None = "none"
  val GranularBlocks = "granularBlocks"
  val NaturalStoneBlocks = "naturalStoneBlocks"
  val Both = "both"
  val values: Seq[String] = Seq(None, GranularBlocks, NaturalStoneBlocks, Both)
}