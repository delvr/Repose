package repose.config

import farseek.block._
import farseek.config._
import farseek.util._
import net.minecraft.block.Block
import net.minecraft.block.Block._
import net.minecraft.init.Blocks._
import repose.ReposeMod

/** Configuration settings for Repose.
  * @author delvr
  */
object ReposeConfig extends ConfigCategory(None, ReposeMod.name) {

    val reposeGranularBlocks = new BlockSetSetting(this, "Granular Blocks",
        "The blocks considered to be granular.", () => granularBlocks - SNOW)

    val reposeNaturalStoneBlocks = new BlockSetSetting(this, "Natural Stone Blocks",
        "The blocks considered to be natural stone.", () => naturalStoneBlocks)

    val     granularBlocksChoice = CustomChoice("Granular Materials",   () => displayNames(reposeGranularBlocks.value))

    val naturalStoneBlocksChoice = CustomChoice("Natural Stone Blocks", () => displayNames(reposeNaturalStoneBlocks.value))

    val slopingBlocks = new MultiChoiceSetting(this, "Sloping Blocks",
        "Makes certain blocks act as walkable slopes.", () => granularBlocksChoice, Non,
        granularBlocksChoice, naturalStoneBlocksChoice, Both)

    val granularFall = new BooleanSetting(this, "Soil Fall",
        s"If true, all blocks made of granular material (not just sand and gravel) can fall down.", () => true)

    val blockSpread = new BooleanSetting(this, "Block Spread",
        s"If true, blocks that fall will also spread into piles instead of forming columns.", () => true)

    val avalanches = new BooleanSetting(this, "Avalanches",
        s"If true, non-soil blocks that can spread will also trigger avalanches when disturbed. " +
        s"Note: this option has no effect when using the EnviroMine mod.", () => true)

    private def displayNames(blocks: Set[Block]) = sortedCSV(blocks.map(displayName).toSeq)
}

class BlockSetSetting(category: ConfigCategory, name: String, help: String, defaultValue: () => Set[Block])
        extends SetSetting[Block](category, name, help, defaultValue) {

    protected def parseElement(s: String) = Option(getBlockFromName(s))
    protected def writeElement(b: Block) = b.getRegistryName.toString
}
