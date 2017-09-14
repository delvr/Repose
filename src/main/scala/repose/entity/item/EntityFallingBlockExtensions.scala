package repose.entity.item

import farseek.entity._
import farseek.util.ImplicitConversions._
import farseek.world._
import net.minecraft.block._
import net.minecraft.entity.MoverType._
import net.minecraft.entity._
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.init.Blocks._
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper._
import net.minecraft.world.World
import repose.block.FallingBlockExtensions._

/** @author delvr */
object EntityFallingBlockExtensions {

    def spawnFallingBlock(pos: BlockPos, posOrigin: BlockPos)(implicit w: World) {
        val block = blockAt(posOrigin)
        val data   = dataAt(posOrigin)
        val fallingBlock =
            if(block.isInstanceOf[BlockGrass]) (DIRT, 0)
            else (block, data)
        val e = new EntityFallingBlock(w, pos.getX + 0.5D, pos.getY, pos.getZ + 0.5D, fallingBlock)
        e.prevPosX = posOrigin.getX + 0.5D
        e.prevPosY = posOrigin.getY
        e.prevPosZ = posOrigin.getZ + 0.5D
        tileEntityOptionAt(posOrigin).foreach {
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
                val blockState = e.getBlock
                val block = blockState.getBlock
                val sound = block.getSoundType
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
                            deleteBlockAt(posOrigin)
                            if(block.canSpreadInAvalanche)
                                triggerNeighborSpread(posOrigin.up)
                        }
                        if(block.canSpreadInAvalanche) {
                            val box = e.getEntityBoundingBox
                            val yTopCurrent  = floor(box.maxY)
                            val yTopPrevious = floor(box.maxY - e.motionY)
                            if(yTopCurrent < yTopPrevious)
                                triggerNeighborSpread(e.x, yTopPrevious, e.z)
                        }
                        if(e.onGround) {
                            e.setDead()
                            val pos = new BlockPos(e)
                            val blockHere = blockAt(pos)
                            // blockHere: landing on a slab; pos.down: landing on a ladder
                            if(!canDisplace(blockHere) || canDisplace(blockAt(pos.down)))
                                block.dropBlockAsItem(w, pos, blockState, 0)
                            else {
                                if(!w.isAirBlock(pos))
                                    blockHere.dropBlockAsItem(w, pos, blockStateAt(pos), 0)
                                setBlockAt(pos, block, blockState)
                                if(e.getEntityData.getSize > 0) // not null!
                                    copyTileEntityTags(pos, e.getEntityData)
                                if(block.canSpreadFrom(pos))
                                    block.spreadFrom(pos)
                            }
                            e.world.playSound(null, e.posX, e.posY, e.posZ, sound.breakSound,
                                SoundCategory.BLOCKS, sound.getVolume, sound.getPitch)
                        }
                    }
                } else if(!w.isRemote) {
                    e.setDead()
                    block.dropBlockAsItem(w, (e.x, e.y, e.z), blockState, 0)
                }
            case e: Entity => e.onUpdate()
        }
    }
}
