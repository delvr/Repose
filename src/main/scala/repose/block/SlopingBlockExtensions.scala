package repose.block

import farseek.block._
import farseek.util._
import farseek.world._
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.{Entity, EntityLivingBase}
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos._
import net.minecraft.util.math._
import net.minecraft.util.math.shapes.{VoxelShape, VoxelShapes}
import net.minecraft.world._
import repose.config.ReposeConfig._
import repose.config.SlopingBlockTypeChoices
import scala.collection.JavaConverters._
import scala.math._

object SlopingBlockExtensions {

  def getCollisionShape(state: IBlockState, w: IBlockReader, pos: BlockPos): VoxelShape = {
    implicit val world: IBlockReader = w
    if(state.canSlopeAt(pos))
      state.slopingCollisionShapes(pos).fold(VoxelShapes.empty)(VoxelShapes.or)
    else
      state.getCollisionShape(w, pos)
  }

  implicit class SlopingBlockValue(val state: IBlockState) extends AnyVal {

    private def canSlope(blockType: String): Boolean =
      slopingBlockTypes.get == blockType || slopingBlockTypes.get == SlopingBlockTypeChoices.Both

    def canSlope: Boolean =
      (canSlope(SlopingBlockTypeChoices.GranularBlocks)     &&     granularBlocks.contains(state)) ||
      (canSlope(SlopingBlockTypeChoices.NaturalStoneBlocks) && naturalStoneBlocks.contains(state))

    def canSlopeAt(pos: BlockPos)(implicit w: IBlockReader): Boolean = {
      canSlope && blockHeight(pos) > 0.5 && {
        val shapeAbove = w(pos.up).getCollisionShape(w, pos.up)
        shapeAbove.xSize < 1.0 && shapeAbove.zSize < 1.0
      } && (w match {
        case world: World => !world.getEntitiesWithinAABB(classOf[Entity], new AxisAlignedBB(pos.up)).asScala.exists {
          case p: EntityPlayer => !sneakingDownSlopes.get && p.isSneaking && world.getEntitiesWithinAABB(
                                  classOf[EntityPlayer], new AxisAlignedBB(pos)).isEmpty
          case e => e.isInstanceOf[EntityFallingBlock]
        }
        case _ => true
      })
    }

    def slopingCollisionShapes(pos: BlockPos)(implicit w: IBlockReader): Seq[VoxelShape] = {
      val height = blockHeight(pos)
      val stepHeight = height - 0.5
      val slopingShore = slopingShores.get
      val submerged = w(pos.up).isLiquid
      OrdinalDirections.map(d => cornerShape(pos, d._1, d._2, height, stepHeight, slopingShore, submerged))
    }

    private def cornerShape(pos: BlockPos, ns: EnumFacing, ew: EnumFacing, blockHeight: Double, stepHeight: Double,
                            slopingShore: Boolean, submerged: Boolean)(implicit w: IBlockReader) = {
      val height = if(stepHigh(pos + ew,      stepHeight, slopingShore, submerged) &&
                      stepHigh(pos + ns,      stepHeight, slopingShore, submerged) &&
                      stepHigh(pos + ew + ns, stepHeight, slopingShore, submerged)) blockHeight else stepHeight
      VoxelShapes.create(new AxisAlignedBB(max(0.0, ew.x / 2.0), 0,       max(0.0, ns.z / 2.0),
                                           max(0.5, ew.x),       height,  max(0.5, ns.z)))
    }

    private def stepHigh(nPos: BlockPos, stepHeight: Double, slopingShore: Boolean, submerged: Boolean)
                        (implicit w: IBlockReader): Boolean = {
      w match {
        case wr: IWorldReaderBase if !wr.isBlockLoaded(nPos) => true
        case _ =>
          val neighbor = w(nPos)
          (!slopingShore && !submerged && neighbor.isLiquid) ||
            (neighbor.blocksMovement && neighbor.blockHeight(nPos) >= stepHeight)
      }
    }

    def blockHeight(pos: BlockPos)(implicit w: IBlockReader): Double = state.getCollisionShape(w, pos).yMax
  }

  def isEntityInsideOpaqueBlock(entity: EntityLivingBase): Boolean = {
    import entity._
    !noClip && (entity match {
      case player: EntityPlayer => !player.isPlayerSleeping
      case _ => true
    }) && {
      val pos = PooledMutableBlockPos.retain
      try {
        for(i <- 0 until 8) {
          val y = MathHelper.floor(posY + ((((i >> 0) % 2) - 0.5) * 0.1) + getEyeHeight)
          val x = MathHelper.floor(posX + ((((i >> 1) % 2) - 0.5) * width * 0.8))
          val z = MathHelper.floor(posZ + ((((i >> 2) % 2) - 0.5) * width * 0.8))
          if(pos.x != x || pos.y != y || pos.z != z) {
            pos.setPos(x, y, z): PooledMutableBlockPos
            val state = world(pos)
            if(state.causesSuffocation && !state.canSlopeAt(pos)(world))
              return true
          }
        }
        false
      } finally pos.close()
    }
  }
}
