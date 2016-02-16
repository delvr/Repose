package repose.config

import farseek.block._
import farseek.config._
import farseek.util._
import net.minecraft.block.Block
import repose.ReposeMod

/** Configuration settings for Repose.
  * @author delvr
  */
object ReposeConfig extends ConfigCategory(None, ReposeMod.name) {

    import repose.config.SlopingBlocksValues._

    val slopingBlocks = new MultiChoiceSetting(this, "Sloping Blocks",
        "Makes certain blocks act as walkable slopes.", GranularBlocks, Non, GranularBlocks, NaturalStone, Both)

    val granularFall = new BooleanSetting(this, "Soil Fall",
        s"If true, all blocks made of granular material (not just sand and gravel) can fall down. " +
        s"(These blocks are: ${displayNames(granularBlocks)}.)", true)

    val blockSpread = new BooleanSetting(this, "Block Spread",
        s"If true, blocks that fall will also spread into piles instead of forming columns.", true)

    val avalanches = new BooleanSetting(this, "Avalanches",
        s"If true, non-soil blocks that can spread will also trigger avalanches when disturbed. " +
        s"Note: this option has no effect when using the EnviroMine mod.", true)

    val minSupportBlocks = new NumericSetting(this, "Minimum Support Blocks",
        s"Some blocks can avoid cave-ins by having this many stable blocks beside them. A block is considered stable" +
        s"if it and the block below it cannot fall. Set to 5 to disable this and make all blocks fall regardless.",
        5, 1, 5, 1)
}

/** Allowed values for [[ReposeConfig]] [[MultiChoiceSetting]]s.
  * @author delvr
  */
object SlopingBlocksValues {
    val GranularBlocks = CustomChoice("Granular Materials", displayNames(granularBlocks))
    val NaturalStone   = CustomChoice("Natural Stone"     , displayNames(naturalStoneBlocks))
    
    def displayNames(blocks: Set[Block]) = sortedCSV(blocks.map(displayName).toSeq)
}
