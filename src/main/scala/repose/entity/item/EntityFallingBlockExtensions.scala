package repose.entity.item

import farseek.entity._
import farseek.util.ImplicitConversions._
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.MoverType._
import net.minecraft.entity._
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper._
import net.minecraft.world.World
import repose.block.FallingBlockExtensions._

/** @author delvr */
object EntityFallingBlockExtensions {

    def spawnFallingBlock(state: IBlockState, pos: BlockPos, posOrigin: BlockPos, tileEntity: Option[TileEntity])(implicit w: World) {
        val e = new EntityFallingBlock(w, pos.getX + 0.5D, pos.getY, pos.getZ + 0.5D, state)
        e.prevPosX = posOrigin.getX + 0.5D
        e.prevPosY = posOrigin.getY
        e.prevPosZ = posOrigin.getZ + 0.5D
        tileEntity.foreach {
            val entityData = e.getEntityData
            entityData.getKeySet.foreach(entityData.removeTag)
            _.writeToNBT(entityData)
        }
        w.spawnEntity(e)
    }

    def onUpdate(entity: Entity) {
        entity match {
            case e: EntityFallingBlock =>
                implicit val w = e.world
                val state = e.getBlock
                val block = state.getBlock
                e.fallTime += 1
                if(e.fallTime < 1000) {
                    val posOrigin = new BlockPos(e.prevPosX, e.prevPosY, e.prevPosZ)
                    e.prevPosX = e.posX
                    e.prevPosY = e.posY
                    e.prevPosZ = e.posZ
                    e.motionY -= 0.04D
                    e.move(SELF, 0D, e.motionY, 0D)
                    if(!w.isRemote) {
                        if(e.fallTime == 1) {
                            w.setBlockToAir(posOrigin)
                            if(state.canSpreadInAvalanche)
                                triggerNeighborSpread(posOrigin.up)
                        }
                        if(state.canSpreadInAvalanche && !serverDelayed) {
                            val box = e.getEntityBoundingBox
                            val yTopCurrent  = floor(box.maxY)
                            val yTopPrevious = floor(box.maxY - e.motionY)
                            if(yTopCurrent < yTopPrevious)
                                triggerNeighborSpread(new BlockPos(e.x, yTopPrevious, e.z))
                        }
                        if(e.onGround) {
                            e.setDead()
                            onLanding(new BlockPos(e), state, if(e.getEntityData.getSize > 0) Some(e.getEntityData) else None)
                        }
                    }
                } else if(!w.isRemote) {
                    e.setDead()
                    block.dropBlockAsItem(w, new BlockPos(e.x, e.y, e.z), state, 0)
                }
            case e: Entity => e.onUpdate()
        }
    }
}
