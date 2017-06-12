package repose.block

import farseek.block._
import farseek.util.ImplicitConversions._
import farseek.world.Direction._
import farseek.world._
import net.minecraft.block.Block
import net.minecraft.entity._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.AxisAlignedBB._
import net.minecraft.util.MathHelper._
import net.minecraft.world._
import repose.config.ReposeConfig._
import scala.math._

/** @author delvr */
object SlopingBlockExtensions {

    def getCollisionBoundingBoxFromPool(block: Block, w: World, x: Int, y: Int, z: Int): AxisAlignedBB = {
        val box: AxisAlignedBB = block.getCollisionBoundingBoxFromPool(w, x, y, z)
        if(box == null || box.maxY == y) null // snow_layer with data 0 makes a 0-thickness box that still blocks side movement
        else box
    }

    def addCollisionBoxesToList(block: Block, w: World, x: Int, y: Int, z: Int, box: AxisAlignedBB,
                                intersectingBoxes: java.util.List[_], collidingEntity: Entity) {
        if(block.getCollisionBoundingBoxFromPool(w, x, y, z) != null) { // optimization
            implicit val world = w
            if(collidingEntity.canUseSlope && block.canSlopeAt(x, y, z))
                intersectingBoxes ++= block.slopingCollisionBoxes(x, y, z).filter(box.intersectsWith)
            else
                block.addCollisionBoxesToList(w, x, y, z, box, intersectingBoxes, collidingEntity)
        }
    }

    def isEntityInsideOpaqueBlock(entity: EntityLivingBase): Boolean = { // doesn't work with top-level Entity
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

        def canSlope = (slopingBlocks.matches(granularBlocksChoice) && reposeGranularBlocks.contains(block)) ||
                       (slopingBlocks.matches(naturalStoneBlocksChoice) && reposeNaturalStoneBlocks.contains(block))

        def canSlopeAt(x: Int, y: Int, z: Int)(implicit w: World) =
            canSlope && Option(block.getCollisionBoundingBoxFromPool(w, x, y, z)).forall(
                _.maxY - y > 0.5 && blockAt(x, y + 1, z).getCollisionBoundingBoxFromPool(w, x, y + 1, z) == null)

        def slopingCollisionBoxes(x: Int, y: Int, z: Int)(implicit w: IBlockAccess): Seq[AxisAlignedBB] =
            OrdinalDirections.map(cornerBox(x, y, z, _, block.getBlockBoundsMaxY))

        private def cornerBox(x: Int, y: Int, z: Int, d: Direction, blockHeight: Double)(implicit w: IBlockAccess) = {
            val stepHeight = blockHeight - 0.5
            val height = if(stepHigh(x + d.x, y, z      , stepHeight) &&
            stepHigh(x      , y, z + d.z, stepHeight) &&
            stepHigh(x + d.x, y, z + d.z, stepHeight)) blockHeight else stepHeight
            getBoundingBox(x + max(0.0, d.x/2.0), y, z + max(0.0, d.z/2.0), x + max(0.5, d.x), y + height, z + max(0.5, d.z))
        }

        private def stepHigh(nx: Int, y: Int, nz: Int, stepHeight: Double)(implicit w: IBlockAccess) = {
            val neighbor = blockAt(nx, y, nz)
            neighbor.isSolid && neighbor.getBlockBoundsMaxY >= stepHeight
        }
    }

    implicit class EntityValue(val entity: Entity) extends AnyVal {
        def canUseSlope = entity.isInstanceOf[EntityPlayer] || entity.isInstanceOf[EntityCreature]
    }
}
