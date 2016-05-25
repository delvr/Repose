package repose.entity.item

import farseek.block._
import farseek.core._
import farseek.entity._
import farseek.util.ImplicitConversions._
import farseek.world._
import net.minecraft.block.Block.SoundType
import net.minecraft.block._
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.init.Blocks._
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper._
import net.minecraft.world.World
import repose.block.FallingBlockExtensions._

/** @author delvr */
object EntityFallingBlockExtensions {

    def onUpdate(super_onUpdate: ReplacedMethod[EntityFallingBlock], entity: EntityFallingBlock) {
        entity.update()
    }

    def spawnFallingBlock(pos: BlockPos, posOrigin: BlockPos)(implicit w: World) {
        val block = blockAt(posOrigin)
        val data   = dataAt(posOrigin)
        val fallingBlock =
            if(block.isInstanceOf[BlockGrass]) (dirt, 0)
            else (block, data)
        val e = new EntityFallingBlock(w, pos.getX + 0.5D, pos.getY, pos.getZ + 0.5D, fallingBlock)
        e.prevPosX = posOrigin.getX + 0.5D
        e.prevPosY = posOrigin.getY
        e.prevPosZ = posOrigin.getZ + 0.5D
        tileEntityOptionAt(posOrigin).foreach {
            e.getEntityData.getKeySet.foreach(e.getEntityData.removeTag)
            _.writeToNBT(e.getEntityData)
        }
        w.spawnEntityInWorld(e)
    }

    implicit class EntityFallingBlockValue(val e: EntityFallingBlock) extends AnyVal {

        def blockState = e.getBlock
        def block = blockState.getBlock

        def update() {
            implicit val w = e.worldObj
            e.fallTime += 1 // Age
            if(e.fallTime < 1000) {
                val xOrigin = floor_double(e.prevPosX)
                val yOrigin = floor_double(e.prevPosY)
                val zOrigin = floor_double(e.prevPosZ)
                e.prevPosX = e.posX
                e.prevPosY = e.posY
                e.prevPosZ = e.posZ
                e.motionY -= 0.04D
                e.moveEntity(0D, e.motionY, 0D)
                if(!w.isRemote) {
                    if(e.fallTime == 1) {
                        if(!block.isDiscreteObject)
                            playBlockSound(_.getBreakSound)
                        deleteBlockAt((xOrigin, yOrigin, zOrigin))
                        if(block.canSpreadInAvalanche)
                            triggerNeighborSpread(xOrigin, yOrigin + 1, zOrigin)
                    }
                    if(block.canSpreadInAvalanche) {
                        val yTopCurrent  = floor_double(e.getEntityBoundingBox.maxY)
                        val yTopPrevious = floor_double(e.getEntityBoundingBox.maxY - e.motionY)
                        if(yTopCurrent < yTopPrevious)
                            triggerNeighborSpread(e.x, yTopPrevious, e.z)
                    }
                    if(e.onGround) {
                        e.setDead()
                        val pos = new BlockPos(e.x, e.y, e.z)
                        val blockHere = blockAt(pos)
                        // blockHere: landing on a slab; blockBelow: landing on a ladder
                        if(!canDisplace(blockAt(pos)) || canDisplace(blockAt(pos.down))) {
                            playBlockSound(_.getBreakSound, block)
                            block.dropBlockAsItem(w, pos, blockState, 0)
                        } else {
                            if(!w.isAirBlock(pos)) {
                                playBlockSound(_.getBreakSound, blockHere)
                                blockHere.dropBlockAsItem(w, pos, blockStateAt(pos), 0)
                            }
                            setBlockAt(e.xyz, block, blockState)
                            if(e.getEntityData != null)
                                copyTileEntityTags(e.xyz, e.getEntityData)
                            playBlockSound(_.getStepSound)
                            if(block.canSpreadFrom(pos))
                                block.spreadFrom(pos)
                        }
                    }
                }
            } else if(!w.isRemote) {
                e.setDead()
                block.dropBlockAsItem(w, (e.x, e.y, e.z), blockState, 0)
            }
        }

        private def playBlockSound(sound: SoundType => String, b: Block = block) {
            e.worldObj.playSoundAtEntity(e, sound(b.stepSound), b.stepSound.getVolume / 4F, b.stepSound.getFrequency)
        }
    }
}
