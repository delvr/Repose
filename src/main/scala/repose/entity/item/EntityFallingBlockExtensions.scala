package repose.entity.item

import com.bioxx.tfc.Core.TFC_Sounds._
import com.bioxx.tfc.api.TFCBlocks
import farseek.block._
import farseek.entity._
import farseek.util.ImplicitConversions._
import farseek.util._
import farseek.world._
import net.minecraft.block.Block.SoundType
import net.minecraft.block.material.Material
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.init.Blocks._
import net.minecraft.util.MathHelper._
import net.minecraft.world.World
import repose.block.FallingBlockExtensions._

/** @author delvr */
object EntityFallingBlockExtensions {

    def spawnFallingBlock(xyz: XYZ, xyzOrigin: XYZ)(implicit w: World) {
        val falling = removeGrass(blockAndDataAt(xyzOrigin))
        val e = new EntityFallingBlock(w, xyz.x + 0.5D, xyz.y.toDouble, xyz.z + 0.5D, falling.block, falling.data)
        e.prevPosX = xyzOrigin.x + 0.5D
        e.prevPosY = xyzOrigin.y
        e.prevPosZ = xyzOrigin.z + 0.5D
        tileEntityOptionAt(xyzOrigin).foreach(_.writeToNBT(e.getEntityData))
        val material = falling.block.getMaterial
        e.field_145809_g = material == Material.rock || material == Material.iron || material == Material.anvil
        w.spawnEntityInWorld(e)
    }

    def onUpdate(entity: Entity) {
        entity match {
            case e: EntityFallingBlock =>
                implicit val w = e.worldObj
                val block = e.field_145811_e
                val data = e.field_145814_a
                e.field_145812_b += 1
                val age = e.field_145812_b
                if(age < 1000) {
                    if(age == 1 && !w.isRemote) {
                        val xyzOrigin = (floor_double(e.prevPosX), floor_double(e.prevPosY), floor_double(e.prevPosZ))
                        if(!block.isDiscreteObject)
                            if(tfcLoaded) w.playSoundAtEntity(e, if(e.field_145809_g) FALLININGROCKSHORT else FALLININGDIRTSHORT, 1F, 0.8F + (w.rand.nextFloat / 2))
                            else playBlockSound(e, _.getBreakSound)
                        deleteBlockAt(xyzOrigin)
                        if(block.canSpreadInAvalanche)
                            triggerNeighborSpread(xyzOrigin.above)
                    }
                    e.prevPosX = e.posX
                    e.prevPosY = e.posY
                    e.prevPosZ = e.posZ
                    e.motionY -= 0.04D
                    e.moveEntity(0D, e.motionY, 0D)
                    if(!w.isRemote) {
                        val (x, y, z) = (e.x, e.y, e.z)
                        if(block.canSpreadInAvalanche) {
                            val box = e.boundingBox
                            val yTopCurrent  = floor_double(box.maxY)
                            val yTopPrevious = floor_double(box.maxY - e.motionY)
                            if(yTopCurrent < yTopPrevious)
                                triggerNeighborSpread(x, yTopPrevious, z)
                        }
                        if(e.onGround) {
                            e.setDead()
                            val xyz = (x, y, z)
                            // can't displace here: landed on a slab; can displace below: landed on a ladder/fence etc
                            if(!canDisplaceAt(xyz) || canDisplaceAt(xyz.below))
                                block.dropBlockAsItem(w, x, y, z, data, 0)
                            else {
                                if(!w.isAirBlock(x, y, z))
                                    blockAt(xyz).dropBlockAsItem(w, x, y, z, dataAt(xyz), 0)
                                setBlockAt(xyz, block, data)
                                copyTileEntityTags(xyz, e.getEntityData)
                                if(block.canSpreadFrom(xyz))
                                    block.spreadFrom(xyz)
                            }
                            playBlockSound(e, _.getStepResourcePath)
                        }
                    }
                } else if(!w.isRemote) {
                    e.setDead()
                    block.dropBlockAsItem(w, e.x, e.y, e.z, data, 0)
                }
            case e: Entity => e.onUpdate()
        }
    }

    private def playBlockSound(e: EntityFallingBlock, sound: SoundType => String) {
        val b = e.field_145811_e
        e.worldObj.playSoundAtEntity(e, sound(b.stepSound), b.stepSound.getVolume / 4F, b.stepSound.getPitch)
    }

    private def removeGrass(bd: BlockAndData): BlockAndData = {
        val b = bd.block
        if(b == grass) dirt
        else if(!tfcLoaded) bd
        else if(b == TFCBlocks.grass  || b == TFCBlocks.dryGrass )  TFCBlocks.dirt  -> bd.data
        else if(b == TFCBlocks.grass2 || b == TFCBlocks.dryGrass2) TFCBlocks.dirt2 -> bd.data
        else if(b == TFCBlocks.clayGrass ) TFCBlocks.clay  -> bd.data
        else if(b == TFCBlocks.clayGrass2) TFCBlocks.clay2 -> bd.data
        else if(b == TFCBlocks.peatGrass ) TFCBlocks.peat  -> bd.data
        else bd
    }
}
