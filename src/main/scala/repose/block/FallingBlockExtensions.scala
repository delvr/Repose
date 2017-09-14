package repose.block

import farseek.block._
import farseek.util.ImplicitConversions._
import farseek.util._
import farseek.world._
import java.lang.Package._
import java.util.Random
import net.minecraft.block.Block._
import net.minecraft.block._
import net.minecraft.block.state.IBlockState
import net.minecraft.entity._
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.MathHelper._
import net.minecraft.util.math._
import net.minecraft.world.World
import repose.config.ReposeConfig._
import repose.entity.item.EntityFallingBlockExtensions._
import scala.collection.JavaConversions._

/** @author delvr */
object FallingBlockExtensions {

    val EnviroMineLoaded = getPackage("enviromine") != null

    val FallDelay = 2

    def onBlockAdded(block: Block, w: World, pos: BlockPos, state: IBlockState) {
        implicit val world = w
        if(block.canFallFrom(pos))
            w.scheduleUpdate(pos, block, block.fallDelay)
        else if(!block.isInstanceOf[BlockFalling])
            block.onBlockAdded(w, pos, state)
    }

    def onBlockPlacedBy(block: Block, w: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, item: ItemStack) {
        implicit val world = w
        if(block.canSpreadFrom(pos))
            block.spreadFrom(pos)
        else
            block.onBlockPlacedBy(w, pos, state, placer, item)
    }

    def neighborChanged(block: Block, state: IBlockState, w: World, pos: BlockPos, formerNeighbor: Block, neighborPos: BlockPos) {
        implicit val world = w
        if(!canDisplace(formerNeighbor) && block.canFallFrom(pos))
            w.scheduleUpdate(pos, block, block.fallDelay)
        else if(!block.isInstanceOf[BlockFalling])
            block.neighborChanged(state, w, pos, formerNeighbor, neighborPos)
    }

    def updateTick(block: Block, w: World, pos: BlockPos, state: IBlockState, random: Random) {
        implicit val world = w
        if(block.canFallFrom(pos))
            block.fallFrom(pos, pos)
        else if(!block.isInstanceOf[BlockFalling])
            block.updateTick(w, pos, state, random)
    }

    def onBlockDestroyedByPlayer(block: Block, w: World, pos: BlockPos, state: IBlockState) {
        implicit val world = w
        block.onBlockDestroyedByPlayer(w, pos, state)
        triggerNeighborSpread(pos.up)
    }

    def triggerNeighborSpread(pos: BlockPos)(implicit w: World) {
        if(!populating && !w.isRemote && !blockAt(pos).isLiquid) { // Prevent beach destruction
            for(nPos <- pos.neighbors) {
                val neighbor = blockAt(nPos)
                if(neighbor.canSpreadInAvalanche && !occupiedByFallingBlock(nPos) && neighbor.canSpreadFrom(nPos))
                    neighbor.spreadFrom(nPos)
            }
        }
    }

    def canFallThrough(state: IBlockState): Boolean = canDisplace(state.getBlock)

    def canSpreadThrough(pos: BlockPos)(implicit w: World) =
        canDisplace(blockAt(pos)) && canDisplace(blockAt(pos.down)) && !occupiedByFallingBlock(pos)

    def occupiedByFallingBlock(pos: BlockPos)(implicit w: World): Boolean = {
        val chunk = w.getChunkFromBlockCoords(pos)
        val entityLists = chunk.getEntityLists
        val aabb = FULL_BLOCK_AABB.offset(pos)
        for(t <- entityLists(floor((aabb.minY - 1) / 16D)).getByClass(classOf[EntityFallingBlock]))
            if(t.getEntityBoundingBox.intersects(aabb)) return true
        for(t <- entityLists(floor((aabb.minY + 1) / 16D)).getByClass(classOf[EntityFallingBlock]))
            if(t.getEntityBoundingBox.intersects(aabb)) return true
        false
    }

    def canDisplace(block: Block) = !block.isSolid

    def copyTileEntityTags(pos: BlockPos, tags: NBTTagCompound)(implicit w: World) {
        tileEntityOptionAt(pos).foreach { tileEntity =>
            val newTags = new NBTTagCompound
            tileEntity.writeToNBT(newTags)
            for(tag: String <- tags.getKeySet) {
                if(tag != "x" && tag != "y" && tag != "z")
                    newTags.setTag(tag, tags.getTag(tag))
            }
            tileEntity.readFromNBT(newTags)
            tileEntity.markDirty()
        }
    }

    implicit class FallingBlockValue(val block: Block) extends AnyVal {

        def fallDelay = FallDelay

        def canFall(implicit w: World) = !w.isRemote && (block.isInstanceOf[BlockFalling] ||
                (granularFall && reposeGranularBlocks.value.contains(block)))

        def canSpread(implicit w: World) = canFall && blockSpread

        def canSpreadInAvalanche(implicit w: World) = !EnviroMineLoaded && canSpread && avalanches && !block.isSoil

        def canFallFrom(pos: BlockPos)(implicit w: World) =
            canFall && canDisplace(blockAt(pos.down))

        def fallFrom(pos: BlockPos, posOrigin: BlockPos)(implicit w: World) {
            if(!blocksFallInstantlyAt(pos) && MinecraftServer.getCurrentTimeMillis - w.getMinecraftServer.currentTime <= 2000L) {
                spawnFallingBlock(pos, posOrigin)
            } else {
                val data = dataAt(posOrigin)
                val tileEntity = tileEntityOptionAt(posOrigin)
                deleteBlockAt(posOrigin)
                downFrom(pos.down).find(!block.canFallFrom(_)).foreach { posLanded =>
                    setBlockAt(posLanded, block, data)
                    tileEntity.foreach{entity =>
                        val tags = new NBTTagCompound
                        entity.writeToNBT(tags)
                        copyTileEntityTags(posLanded, tags)
                    }
                }
            }
        }

        def canSpreadFrom(pos: BlockPos)(implicit w: World) =
            canSpread && !populating && !canDisplace(blockAt(pos.down))

        def spreadFrom(pos: BlockPos)(implicit w: World) {
            val freeNeighbors = pos.neighbors.filter(canSpreadThrough)
            randomElementOption(freeNeighbors.toArray)(w.rand).foreach(fallFrom(_, pos))
        }
    }
}
