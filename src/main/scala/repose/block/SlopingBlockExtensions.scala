package repose.block

import farseek.block._
import farseek.util.ImplicitConversions._
import farseek.world.Direction._
import farseek.world._
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math._
import net.minecraft.world._
import repose.config.ReposeConfig._
import scala.math._

/** @author delvr */
object SlopingBlockExtensions {

    def getCollisionBoundingBox(block: Block, state: IBlockState, w: IBlockAccess, pos: BlockPos): AxisAlignedBB = {
        val box: AxisAlignedBB = block.getCollisionBoundingBox(state, w, pos)
        if(box == null || box.maxY == 0) null // snow_layer with data 0 makes a 0-thickness box that still blocks side movement
        else box
    }

    def addCollisionBoxToList(block: Block, state: IBlockState, w: World, pos: BlockPos, box: AxisAlignedBB,
                              intersectingBoxes: java.util.List[AxisAlignedBB], collidingEntity: Entity, flag: Boolean) {
        if(state.getCollisionBoundingBox(w, pos) != null) { // optimization
            implicit val world = w
            if(collidingEntity.canUseSlope && state.canSlopeAt(pos))
                intersectingBoxes ++= state.slopingCollisionBoxes(pos).filter(box.intersects)
            else
                block.addCollisionBoxToList(state, w, pos, box, intersectingBoxes, collidingEntity, flag)
        }
    }

    def isEntityInsideOpaqueBlock(entity: EntityLivingBase): Boolean = { // doesn't work with top-level Entity
        implicit val world = entity.world
        for(i <- 0 until 8) {
            val dx = (((i >> 0) % 2).toFloat - 0.5F) * entity.width * 0.8F
            val dy = (((i >> 1) % 2).toFloat - 0.5F) * 0.1F
            val dz = (((i >> 2) % 2).toFloat - 0.5F) * entity.width * 0.8F
            val x = floor(entity.posX + dx.toDouble).toInt
            val y = floor(entity.posY + entity.getEyeHeight.toDouble + dy.toDouble).toInt
            val z = floor(entity.posZ + dz.toDouble).toInt
            val pos = new BlockPos(x, y, z)
            val state = world.getBlockState(pos)
            if(state.isNormalCube && !(entity.canUseSlope && state.canSlopeAt(pos)))
              return true
        }
        false
    }

    implicit class SlopingBlockValue(val state: IBlockState) extends AnyVal {

        def canSlope: Boolean = (slopingBlocks.matches(granularBlocksChoice)     &&     reposeGranularBlocks.contains(state)) ||
                                (slopingBlocks.matches(naturalStoneBlocksChoice) && reposeNaturalStoneBlocks.contains(state))

        def canSlopeAt(pos: BlockPos)(implicit w: World): Boolean =
            canSlope && Option(state.getCollisionBoundingBox(w, pos)).forall(
                _.maxY > 0.5 && w.getBlockState(pos.up).getCollisionBoundingBox(w, pos.up) == null)

        def slopingCollisionBoxes(pos: BlockPos)(implicit w: World): Seq[AxisAlignedBB] = {
            val height = blockHeight(pos)
            OrdinalDirections.map(cornerBox(pos, _, height))
        }

        private def cornerBox(pos: BlockPos, d: Direction, blockHeight: Double)(implicit w: World) = {
            val stepHeight = blockHeight - 0.5
            val height = if(stepHigh(pos.add(d.x, 0,  0 ), stepHeight) &&
                            stepHigh(pos.add( 0 , 0, d.z), stepHeight) &&
                            stepHigh(pos.add(d.x, 0, d.z), stepHeight)) blockHeight else stepHeight
            new AxisAlignedBB(pos.getX + max(0.0, d.x/2.0), pos.getY         , pos.getZ + max(0.0, d.z/2.0),
                              pos.getX + max(0.5, d.x    ), pos.getY + height, pos.getZ + max(0.5, d.z    ))
        }

        private def stepHigh(pos: BlockPos, stepHeight: Double)(implicit w: World) = {
            val neighbor = w.getBlockState(pos)
            neighbor.getBlock.isSolid && blockHeight(pos) >= stepHeight
        }
    }

    private def blockHeight(pos: BlockPos)(implicit w: World): Double = {
        val box = w.getBlockState(pos).getCollisionBoundingBox(w, pos)
        if(box == null) 0 else box.maxY
    }

    implicit class EntityValue(val entity: Entity) extends AnyVal {
        def canUseSlope: Boolean = entity.isInstanceOf[EntityPlayer] || entity.isInstanceOf[EntityCreature]
    }
}
