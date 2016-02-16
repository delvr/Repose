package repose.block

import farseek.block._
import farseek.core._
import farseek.util.ImplicitConversions._
import farseek.util._
import farseek.world._
import java.lang.Package._
import java.util.Random
import net.minecraft.block._
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.init.Blocks._
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import repose.config.ReposeConfig._
import repose.entity.item.EntityFallingBlockExtensions._
import scala.collection.mutable

/** @author delvr */
object FallingBlockExtensions {

    val EnviroMineLoaded = getPackage("enviromine") != null

    val FallDelay = 2

    def onBlockAdded(w: World, x: Int, y: Int, z: Int,
                     super_onBlockAdded: ReplacedMethod[Block])(implicit block: Block) {
        implicit val world = w
        if(block.canFallFrom(x, y, z))
            w.scheduleBlockUpdate(x, y, z, block, block.fallDelay)
        else if(!block.isInstanceOf[BlockFalling])
            super_onBlockAdded(w, x, y, z)
    }

    def onPostBlockPlaced(w: World, x: Int, y: Int, z: Int, data: Int,
                          super_onPostBlockPlaced: ReplacedMethod[Block])(implicit block: Block) {
        implicit val world = w
        if(block.canSpreadFrom(x, y, z))
            block.spreadFrom(x, y, z)
        else
            super_onPostBlockPlaced(w, x, y, z, data)
    }

    def onNeighborBlockChange(w: World, x: Int, y: Int, z: Int, formerNeighbor: Block,
                              super_onNeighborBlockChange: ReplacedMethod[Block])(implicit block: Block) {
        implicit val world = w
        if(!canDisplace(formerNeighbor) && block.canFallFrom(x, y, z))
            w.scheduleBlockUpdate(x, y, z, block, block.fallDelay)
        else if(!block.isInstanceOf[BlockFalling])
            super_onNeighborBlockChange(w, x, y, z, formerNeighbor)
    }

    def updateTick(w: World, x: Int, y: Int, z: Int, random: Random,
                   super_updateTick: ReplacedMethod[Block])(implicit block: Block) {
        implicit val world = w
        val xyz = (x, y, z)
        if(block.canFallFrom(xyz))
            block.fallFrom(xyz, xyz)
        else if(!block.isInstanceOf[BlockFalling])
            super_updateTick(w, x, y, z, random)
    }

    def onBlockDestroyedByPlayer(w: World, x: Int, y: Int, z: Int, data: Int,
                                 super_onBlockDestroyedByPlayer: ReplacedMethod[Block])(implicit block: Block) {
        implicit val world = w
        super_onBlockDestroyedByPlayer(w, x, y, z, data)
        triggerNeighborSpread(x, y + 1, z)
    }

    def triggerNeighborSpread(xyz: XYZ)(implicit w: World) {
        if(!populating && !w.isRemote && !blockAt(xyz).isLiquid) { // Prevent beach destruction
            for((nx, ny, nz) <- xyz.neighbors) {
                val neighbor = blockAt(nx, ny, nz)
                if(neighbor.canSpreadInAvalanche && !occupiedByFallingBlock(nx, ny, nz) && neighbor.canSpreadFrom(nx, ny, nz))
                    neighbor.spreadFrom(nx, ny, nz)
            }
        }
    }

    def canFallThrough(w: World, x: Int, y: Int, z: Int,
                       super_canFallThrough: ReplacedMethod[BlockFalling]): Boolean = canDisplace(blockAt(x, y, z)(w))

    def canSpreadThrough(xyz: XYZ)(implicit w: World) =
        canDisplace(blockAt(xyz)) && canDisplace(blockBelow(xyz)) && !occupiedByFallingBlock(xyz)

    def occupiedByFallingBlock(xyz: XYZ)(implicit w: World) =
        !w.getEntitiesWithinAABB(classOf[EntityFallingBlock],
            stone.getCollisionBoundingBoxFromPool(w, xyz.x, xyz.y, xyz.z)).isEmpty

    def canDisplace(block: Block) = !block.isSolid

    def isSticky(block: Block) = block.isDirt || block.isSoil || block.isClay

    def copyTileEntityTags(xyz: XYZ, tags: NBTTagCompound)(implicit w: World) {
        tileEntityOptionAt(xyz).foreach { tileEntity =>
            val newTags = new NBTTagCompound
            tileEntity.writeToNBT(newTags)
            for(tag <- tags.func_150296_c: mutable.Set[String]) {
                if(tag != "x" && tag != "y" && tag != "z")
                    newTags.setTag(tag, tags.getTag(tag))
            }
            tileEntity.readFromNBT(newTags)
            tileEntity.markDirty()
        }
    }

    implicit class FallingBlockValue(val block: Block) extends AnyVal {

        def fallDelay = FallDelay

        def canFall = block.isInstanceOf[BlockFalling] || (granularFall && block.isGranular)

        def canSpread = canFall && blockSpread

        def canSpreadInAvalanche = !EnviroMineLoaded && canSpread && avalanches && !block.isSoil

        def hasValidSupportAt(xyz: XYZ)(implicit w: World) =
            isSticky(blockAt(xyz)) &&
              xyz.neighbors.count(
                  neighbour => !canDisplace(blockAt(neighbour)) && !canDisplace(blockBelow(neighbour))
              ) >= minSupportBlocks

        def canFallFrom(xyz: XYZ)(implicit w: World) =
            canFall && !w.isRemote && canDisplace(blockBelow(xyz)) && !hasValidSupportAt(xyz)

        def fallFrom(xyz: XYZ, xyzOrigin: XYZ)(implicit w: World) {
            if(!blocksFallInstantlyAt(xyz)) {
                spawnFallingBlock(xyz, xyzOrigin)
            } else {
                val data = dataAt(xyzOrigin)
                val tileEntity = tileEntityOptionAt(xyzOrigin)
                deleteBlockAt(xyzOrigin)
                downFrom(xyz - 1).find(!block.canFallFrom(_)).foreach { xyzLanded =>
                    setBlockAt(xyzLanded, block, data)
                    tileEntity.foreach{entity =>
                        val tags = new NBTTagCompound
                        entity.writeToNBT(tags)
                        copyTileEntityTags(xyzLanded, tags)
                    }
                }
            }
        }

        def canSpreadFrom(xyz: XYZ)(implicit w: World) =
            canSpread && !w.isRemote && !populating && !canDisplace(blockBelow(xyz)) && !hasValidSupportAt(xyz)

        def spreadFrom(xyz: XYZ)(implicit w: World) {
            val freeNeighbors = xyz.neighbors.filter(canSpreadThrough)
            randomElementOption(freeNeighbors.toArray)(w.rand).foreach(fallFrom(_, xyz))
        }
    }
}
