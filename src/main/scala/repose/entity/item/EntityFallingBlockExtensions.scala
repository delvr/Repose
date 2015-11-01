package repose.entity.item

import com.bioxx.tfc.Core.TFC_Core._
import farseek.block._
import farseek.core._
import farseek.entity._
import farseek.util._
import farseek.world._
import net.minecraft.block.Block.SoundType
import net.minecraft.block._
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.init.Blocks._
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.util.MathHelper._
import net.minecraft.world.World
import repose.block.FallingBlockExtensions._

/** @author delvr */
object EntityFallingBlockExtensions {

    def onUpdate(super_onUpdate: ReplacedMethod[EntityFallingBlock], entity: EntityFallingBlock) {
        entity.update()
    }

    def spawnFallingBlock(xyz: XYZ, xyzOrigin: XYZ)(implicit w: World) {
        val block = blockAt(xyzOrigin)
        val data   = dataAt(xyzOrigin)
        val (fallingBlock, fallingData) =
            if(block.isInstanceOf[BlockGrass]) (dirt, 0)
            else if(tfcLoaded && isGrass(block)) (getTypeForDirtFromGrass(block), data)
            else (block, data)
        val (x, y, z) = xyz
        val (xOrigin, yOrigin, zOrigin) = xyzOrigin
        val e = new EntityFallingBlock(w, x + 0.5D, y + 0.5D, z + 0.5D, fallingBlock, fallingData)
        e.prevPosX = xOrigin + 0.5D
        e.prevPosY = yOrigin + 0.5D
        e.prevPosZ = zOrigin + 0.5D
        tileEntityOptionAt(xyzOrigin).foreach {
            e.field_145810_d = new NBTTagCompound
            _.writeToNBT(e.field_145810_d)
        }
        w.spawnEntityInWorld(e)
    }

    implicit class EntityFallingBlockValue(val e: EntityFallingBlock) extends AnyVal {

        def block = e.func_145805_f
        def data  = e.field_145814_a

        def update() {
            implicit val w = e.worldObj
            e.field_145812_b += 1 // Age
            if(e.field_145812_b < 1000) {
                val xOrigin = floor_double(e.prevPosX)
                val yOrigin = floor_double(e.prevPosY)
                val zOrigin = floor_double(e.prevPosZ)
                e.prevPosX = e.posX
                e.prevPosY = e.posY
                e.prevPosZ = e.posZ
                e.motionY -= 0.04D
                e.moveEntity(0D, e.motionY, 0D)
                if(!w.isRemote) {
                    if(e.field_145812_b == 1) {
                        if(!block.isDiscreteObject)
                            playBlockSound(_.getBreakSound)
                        deleteBlockAt((xOrigin, yOrigin, zOrigin))
                        if(block.canSpreadInAvalanche)
                            triggerNeighborSpread(xOrigin, yOrigin + 1, zOrigin)
                    }
                    if(block.canSpreadInAvalanche) {
                        val yTopCurrent  = floor_double(e.boundingBox.maxY)
                        val yTopPrevious = floor_double(e.boundingBox.maxY - e.motionY)
                        if(yTopCurrent < yTopPrevious)
                            triggerNeighborSpread(e.x, yTopPrevious, e.z)
                    }
                    if(e.onGround) {
                        e.setDead()
                        val blockHere = blockAt(e.x, e.y, e.z)
                        // blockHere: landing on a slab; blockBelow: landing on a ladder
                        if(!canDisplace(blockHere) || canDisplace(blockBelow(e.x, e.y, e.z))) {
                            playBlockSound(_.getBreakSound, block)
                            block.dropBlockAsItem(w, e.x, e.y, e.z, data, 0)
                        } else {
                            if(!w.isAirBlock(e.x, e.y, e.z)) {
                                playBlockSound(_.getBreakSound, blockHere)
                                blockHere.dropBlockAsItem(w, e.x, e.y, e.z, dataAt(e.x, e.y, e.z), 0)
                            }
                            setBlockAt(e.xyz, block, data)
                            if(e.field_145810_d != null)
                                copyTileEntityTags(e.xyz, e.field_145810_d)
                            playBlockSound(_.getStepResourcePath)
                            if(block.canSpreadFrom(e.x, e.y, e.z))
                                block.spreadFrom(e.x, e.y, e.z)
                        }
                    }
                }
            } else if(!w.isRemote) {
                e.setDead()
                block.dropBlockAsItem(w, e.x, e.y, e.z, data, 0)
            }
        }

        private def playBlockSound(sound: SoundType => String, b: Block = block) {
            e.worldObj.playSoundAtEntity(e, sound(b.stepSound), b.stepSound.getVolume / 4F, b.stepSound.getPitch)
        }
    }
}
