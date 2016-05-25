package repose.block

import farseek.block._
import farseek.core.ReplacedMethod
import farseek.util.ImplicitConversions._
import farseek.world.Direction._
import farseek.world._
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MathHelper._
import net.minecraft.util._
import net.minecraft.world._
import repose.config.ReposeConfig._
import repose.config.SlopingBlocksValues._
import scala.math._

/** @author delvr */
object SlopingBlockExtensions {

    def getCollisionBoundingBox(w: World, pos: BlockPos, state: IBlockState,
                                super_getCollisionBoundingBox: ReplacedMethod[Block])
                                       (implicit block: Block): AxisAlignedBB = {
        val box: AxisAlignedBB = super_getCollisionBoundingBox(w, pos, state)
        if(box == null || box.maxY == pos.getY) null // snow_layer with data 0 makes a 0-thickness box that still blocks side movement
        else box
    }

    def addCollisionBoxesToList(w: World, pos: BlockPos, state: IBlockState, box: AxisAlignedBB,
                                intersectingBoxes: java.util.List[_], collidingEntity: Entity,
                                super_addCollisionBoxesToList: ReplacedMethod[Block])(implicit block: Block) {
        implicit val world = w
        if(collidingEntity.canUseSlope && block.canSlopeAt(pos))
            intersectingBoxes ++= block.slopingCollisionBoxes(pos).filter(box.intersectsWith)
        else
            super_addCollisionBoxesToList(w, pos, state, box, intersectingBoxes, collidingEntity)
    }

    def isEntityInsideOpaqueBlock(super_isEntityInsideOpaqueBlock: ReplacedMethod[Entity])
                                 (implicit entity: Entity): Boolean = {
        implicit val world = entity.worldObj
        for(i <- 0 until 8) {
            val dx = (((i >> 0) % 2).toFloat - 0.5F) * entity.width * 0.8F
            val dy = (((i >> 1) % 2).toFloat - 0.5F) * 0.1F
            val dz = (((i >> 2) % 2).toFloat - 0.5F) * entity.width * 0.8F
            val x = floor_double(entity.posX + dx.toDouble)
            val y = floor_double(entity.posY + entity.getEyeHeight.toDouble + dy.toDouble)
            val z = floor_double(entity.posZ + dz.toDouble)
            val block = world.getBlock(x, y, z)
            if(block.isNormalCube && !(entity.canUseSlope && block.canSlopeAt(x, y, z)))
              return true
        }
        false
    }

    implicit class SlopingBlockValue(val block: Block) extends AnyVal {

        def canSlope = (slopingBlocks.matches(GranularBlocks) && block.isGranular) ||
                       (slopingBlocks.matches(NaturalStone) && block.isNaturalStone)

        def canSlopeAt(pos: BlockPos)(implicit w: World) =
            canSlope && Option(block.getCollisionBoundingBox(w, pos, blockStateAt(pos))).forall(
                _.maxY - pos.getY > 0.5 && blockAt(pos.up).getCollisionBoundingBox(w, pos.up, blockStateAt(pos.up)) == null)

        def slopingCollisionBoxes(pos: BlockPos)(implicit w: IBlockAccess): Seq[AxisAlignedBB] =
            OrdinalDirections.map(cornerBox(pos, _, block.getBlockBoundsMaxY))

        private def cornerBox(pos: BlockPos, d: Direction, blockHeight: Double)(implicit w: IBlockAccess) = {
            val stepHeight = blockHeight - 0.5
            val (x, y, z) = blockPosXyz(pos)
            val height = if(stepHigh(x + d.x, y, z      , stepHeight) &&
                            stepHigh(x      , y, z + d.z, stepHeight) &&
                            stepHigh(x + d.x, y, z + d.z, stepHeight)) blockHeight else stepHeight
            new AxisAlignedBB(x + max(0.0, d.x/2.0), y, z + max(0.0, d.z/2.0), x + max(0.5, d.x), y + height, z + max(0.5, d.z))
        }

        private def stepHigh(x: Int, y: Int, z: Int, stepHeight: Double)(implicit w: IBlockAccess) = {
            val neighbor = blockAt(x, y, z)
            neighbor.isSolid && neighbor.getBlockBoundsMaxY >= stepHeight
        }
    }

    implicit class EntityValue(val entity: Entity) extends AnyVal {
        def canUseSlope = entity.isInstanceOf[EntityPlayer] || entity.isInstanceOf[EntityCreature]
    }
}
