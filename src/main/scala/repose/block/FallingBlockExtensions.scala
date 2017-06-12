package repose.block

import com.bioxx.tfc.Blocks.Terrain._
import farseek.block._
import farseek.util.ImplicitConversions._
import farseek.util._
import farseek.world._
import java.lang.Package._
import java.util.Random
import net.minecraft.block.{Block, BlockFalling}
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.init.Blocks._
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import repose.config.ReposeConfig._
import repose.entity.item.EntityFallingBlockExtensions._
import scala.collection.mutable

/** @author delvr */
object FallingBlockExtensions {

    private lazy val tfcOldFallingBlockClasses: Set[Class[_]] = Set(classOf[BlockSand], classOf[BlockGravel], classOf[BlockDirt], classOf[BlockCobble])

    val EnviroMineLoaded = getPackage("enviromine") != null

    val FallDelay = 2

    def onBlockAdded(block: Block, w: World, x: Int, y: Int, z: Int) {
        implicit val world = w
        if(block.canFallFrom(x, y, z))
            w.scheduleBlockUpdate(x, y, z, block, block.fallDelay)
        else if(!isOldFallingBlock(block))
            block.onBlockAdded(w, x, y, z)
    }

    def onPostBlockPlaced(block: Block, w: World, x: Int, y: Int, z: Int, data: Int) {
        implicit val world = w
        if(block.canSpreadFrom(x, y, z))
            block.spreadFrom(x, y, z)
        else
            block.onPostBlockPlaced(w, x, y, z, data)
    }

    def onNeighborBlockChange(block: Block, w: World, x: Int, y: Int, z: Int, formerNeighbor: Block) {
        implicit val world = w
        if(formerNeighbor.isSolid && block.canFallFrom(x, y, z))
            w.scheduleBlockUpdate(x, y, z, block, block.fallDelay)
        else if(!isOldFallingBlock(block))
            block.onNeighborBlockChange(w, x, y, z, formerNeighbor)
    }

    def updateTick(block: Block, w: World, x: Int, y: Int, z: Int, random: Random) {
        implicit val world = w
        val xyz = (x, y, z)
        if(block.canFallFrom(xyz))
            block.fallFrom(xyz, xyz)
        else if(!isOldFallingBlock(block))
            block.updateTick(w, x, y, z, random)
    }

    def onBlockDestroyedByPlayer(block: Block, w: World, x: Int, y: Int, z: Int, data: Int) {
        implicit val world = w
        block.onBlockDestroyedByPlayer(w, x, y, z, data)
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

    def isOldFallingBlock(block: Block) = block.isInstanceOf[BlockFalling] || (tfcLoaded && tfcOldFallingBlockClasses.exists(_.isAssignableFrom(block.getClass)))

    def canFallThrough(xyz: XYZ)(implicit w: World): Boolean = canDisplaceAt(xyz)

    def canSpreadThrough(xyz: XYZ)(implicit w: World) =
        canDisplaceAt(xyz) && canDisplaceAt(xyz.below) && !occupiedByFallingBlock(xyz)

    def occupiedByFallingBlock(xyz: XYZ)(implicit w: World) =
        !w.getEntitiesWithinAABB(classOf[EntityFallingBlock],
            stone.getCollisionBoundingBoxFromPool(w, xyz.x, xyz.y, xyz.z)).isEmpty

    def canDisplaceAt(xyz: XYZ)(implicit w: World) = blockAt(xyz).getBlocksMovement(w, xyz.x, xyz.y, xyz.z)

    def copyTileEntityTags(xyz: XYZ, tags: NBTTagCompound)(implicit w: World) {
        if(!tags.hasNoTags) tileEntityOptionAt(xyz).foreach { tileEntity =>
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

        def canFall =  isOldFallingBlock(block) || (granularFall && reposeGranularBlocks.value.contains(block))

        def canSpread = canFall && blockSpread

        def canSpreadInAvalanche = !EnviroMineLoaded && canSpread && avalanches && !block.isSoil && reposeGranularBlocks.value.contains(block)

        def canFallFrom(xyz: XYZ)(implicit w: World) = canFall && !w.isRemote && canDisplaceAt(xyz.below)

        def fallFrom(xyz: XYZ, xyzOrigin: XYZ)(implicit w: World) {
            if(!blocksFallInstantlyAt(xyz)) {
                spawnFallingBlock(xyz, xyzOrigin)
            } else {
                val data = dataAt(xyzOrigin)
                val tileEntity = tileEntityOptionAt(xyzOrigin)
                deleteBlockAt(xyzOrigin)
                downFrom(xyz - 1).find(!block.canFallFrom(_)).foreach { xyzLanded =>
                    setBlockAt(xyzLanded, block, data, notifyNeighbors = false)
                    tileEntity.foreach{entity =>
                        val tags = new NBTTagCompound
                        entity.writeToNBT(tags)
                        copyTileEntityTags(xyzLanded, tags)
                    }
                }
            }
        }

        def canSpreadFrom(xyz: XYZ)(implicit w: World) =
            canSpread && !w.isRemote && !populating && !canDisplaceAt(xyz.below)

        def spreadFrom(xyz: XYZ)(implicit w: World) {
            val freeNeighbors = xyz.neighbors.filter(canSpreadThrough)
            randomElementOption(freeNeighbors.toArray)(w.rand).foreach(fallFrom(_, xyz))
        }
    }
}
