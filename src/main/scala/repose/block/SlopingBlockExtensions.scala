package repose.block

import farseek.util.ImplicitConversions._
import farseek.world.Direction._
import farseek.world._
import net.minecraft.block.state.IBlockState
import net.minecraft.entity._
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math._
import net.minecraft.world._
import repose.config.ReposeConfig._
import scala.math._

/** @author delvr */
object SlopingBlockExtensions {

    def getCollisionBoundingBox(state: IBlockState, w: IBlockAccess, pos: BlockPos): AxisAlignedBB = {
        val box: AxisAlignedBB = state.getCollisionBoundingBox(w, pos)
        if(box == null || box.maxY == 0) null // snow_layer with data 0 makes a 0-thickness box that still blocks side movement
        else box
    }

    def addCollisionBoxToList(state: IBlockState, w: World, pos: BlockPos, box: AxisAlignedBB,
                              intersectingBoxes: java.util.List[AxisAlignedBB], collidingEntity: Entity, flag: Boolean): Unit = {
        if(state.getCollisionBoundingBox(w, pos) != null) { // optimization
            implicit val world: World = w
            if(collidingEntity.canUseSlope && state.canSlopeAt(pos))
                intersectingBoxes ++= state.slopingCollisionBoxes(pos).filter(box.intersects)
            else
                state.addCollisionBoxToList(w, pos, box, intersectingBoxes, collidingEntity, flag)
        }
    }

    implicit class SlopingBlockValue(val state: IBlockState) extends AnyVal {

        def canSlope: Boolean = (slopingBlocks.matches(granularBlocksChoice)     &&     reposeGranularBlocks.contains(state)) ||
                                (slopingBlocks.matches(naturalStoneBlocksChoice) && reposeNaturalStoneBlocks.contains(state))

        def canSlopeAt(pos: BlockPos)(implicit w: World): Boolean =
            canSlope && Option(state.getCollisionBoundingBox(w, pos)).forall(
                _.maxY > 0.5 && w.getBlockState(pos.up).getCollisionBoundingBox(w, pos.up) == null)

        def slopingCollisionBoxes(pos: BlockPos)(implicit w: World): Seq[AxisAlignedBB] = {
            val height = blockHeight(pos)
            val stepHeight = height - 0.5
            val slopingShore = slopingShores.value
            val submerged = w.getBlockState(pos.up).getMaterial.isLiquid
            OrdinalDirections.map(cornerBox(pos, _, height, stepHeight, slopingShore, submerged))
        }

        private def cornerBox(pos: BlockPos, d: Direction, blockHeight: Double, stepHeight: Double, slopingShore: Boolean, submerged: Boolean)(implicit w: World) = {
            val height = if(stepHigh(pos.add(d.x, 0,  0 ), stepHeight, slopingShore, submerged) &&
                            stepHigh(pos.add( 0 , 0, d.z), stepHeight, slopingShore, submerged) &&
                            stepHigh(pos.add(d.x, 0, d.z), stepHeight, slopingShore, submerged)) blockHeight else stepHeight
            new AxisAlignedBB(pos.getX + max(0.0, d.x/2.0), pos.getY         , pos.getZ + max(0.0, d.z/2.0),
                              pos.getX + max(0.5, d.x    ), pos.getY + height, pos.getZ + max(0.5, d.z    ))
        }

        private def stepHigh(nPos: BlockPos, stepHeight: Double, slopingShore: Boolean, submerged: Boolean)(implicit w: World): Boolean = {
            val neighbor = w.getBlockState(nPos)
            (!slopingShore && !submerged && neighbor.getMaterial.isLiquid) ||
              (neighbor.getMaterial.blocksMovement && neighbor.blockHeight(nPos) >= stepHeight)
        }

        def blockHeight(pos: BlockPos)(implicit w: World): Double =
            Option(state.getCollisionBoundingBox(w, pos)).fold(0d)(_.maxY)
    }

    implicit class EntityValue(val entity: Entity) extends AnyVal {
        def canUseSlope: Boolean = entity match {
            case player: EntityPlayer => sneakingInSlopes.value || !player.isSneaking
            case creature: EntityCreature => true // excludes water mobs
            case _ => false // excludes falling block entities etc.
        }
    }
}
