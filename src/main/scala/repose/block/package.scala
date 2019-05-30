package repose

import farseek.block._
import farseek.util.Logging
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.block.{Block, BlockContainer, BlockDirectional}
import net.minecraftforge.common.ToolType._
import repose.config.ReposeConfig._

package object block extends Logging {

  private val NaturalStoneTypeBlacklist =
    Set(classOf[BlockContainer], classOf[BlockDirectional])

  private val NaturalStoneNameBlacklist =
    Set("cobble", "brick", "smooth_", "polished_", "cut_", "chiseled_", "carved_", "glazed_", "_block", "_slab", "_pillar", "_frame", "_concrete", "_coral")

  private lazy val naturalStoneBlocksDefaultBlacklist: Set[Block] = allBlocks.filter(block =>
    NaturalStoneTypeBlacklist.exists(_.isAssignableFrom(block.getClass)) || NaturalStoneNameBlacklist.exists(block.name.contains))

  lazy val naturalStoneStates: Set[IBlockState] =
    logged("Natural stone", allStates.filter(state =>
      state.isMadeOf(Material.ROCK) && state.isFullCube && !naturalStoneBlocksDefaultBlacklist.contains(state.block)))

  lazy val granularStates: Set[IBlockState] =
    logged("Granular", allStates.filter(_.getHarvestTool == SHOVEL))

  lazy val soilStates: Set[IBlockState] = granularStates.filter(_.isMadeOf(Material.GROUND, Material.GRASS))

  lazy val granularBlocks = new StateMatchingList(granularStates, granularBlocksWhitelist, granularBlocksBlacklist)
  lazy val naturalStoneBlocks = new StateMatchingList(naturalStoneStates, naturalStoneBlocksWhitelist, naturalStoneBlocksBlacklist)

  private def logged(listName: String, states: Set[IBlockState]): Set[IBlockState] = {
    info(s"$listName block states default list: ${states.map(_.toString.replace("Block{", "").replace("}", "")).toSeq.sorted.mkString(", ")})")
    states
  }
}

class StateMatchingList(defaults: Set[IBlockState], Whitelist: BlockNamesListValue, Blacklist: BlockNamesListValue) {
  def contains(state: IBlockState): Boolean = (defaults.contains(state) || isInList(state, Whitelist)) && !isInList(state, Blacklist)
  private def isInList(state: IBlockState, list: BlockNamesListValue) = list.get.contains(state.block.name)
}
