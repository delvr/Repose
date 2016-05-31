package repose.block

import farseek.block._
import farseek.core._
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

    def onBlockAdded(w: World, pos: BlockPos, state: IBlockState,
                     super_onBlockAdded: ReplacedMethod[Block])(implicit block: Block) {
        implicit val world = w
        if(block.canFallFrom(pos))
            w.scheduleUpdate(pos, block, block.fallDelay)
        else if(!block.isInstanceOf[BlockFalling])
            super_onBlockAdded(w, pos, state)
    }

    def onBlockPlacedBy(w: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, item: ItemStack,
                        super_onBlockPlacedBy: ReplacedMethod[Block])(implicit block: Block) {
        implicit val world = w
        if(block.canSpreadFrom(pos))
            block.spreadFrom(pos)
        else
            super_onBlockPlacedBy(w, pos, state, placer, item)
    }

    def neighborChanged(state: IBlockState, w: World, pos: BlockPos, formerNeighbor: Block,
                        super_neighborChanged: ReplacedMethod[Block])(implicit block: Block) {
        implicit val world = w
        if(!canDisplace(formerNeighbor) && block.canFallFrom(pos))
            w.scheduleUpdate(pos, block, block.fallDelay)
        else if(!block.isInstanceOf[BlockFalling])
            super_neighborChanged(state, w, pos, formerNeighbor)
    }

    def updateTick(w: World, pos: BlockPos, state: IBlockState, random: Random,
                   super_updateTick: ReplacedMethod[Block])(implicit block: Block) {
        implicit val world = w
        if(block.canFallFrom(pos))
            block.fallFrom(pos, pos)
        else if(!block.isInstanceOf[BlockFalling])
            super_updateTick(w, pos, state, random)
    }

    def onBlockDestroyedByPlayer(w: World, pos: BlockPos, state: IBlockState,
                                 super_onBlockDestroyedByPlayer: ReplacedMethod[Block])(implicit block: Block) {
        implicit val world = w
        super_onBlockDestroyedByPlayer(w, pos, state)
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

    def canFallThrough(state: IBlockState, super_canFallThrough: ReplacedMethod[BlockFalling]): Boolean =
        canDisplace(state.getBlock)

    def canSpreadThrough(pos: BlockPos)(implicit w: World) =
        canDisplace(blockAt(pos)) && canDisplace(blockAt(pos.down)) && !occupiedByFallingBlock(pos)

    def occupiedByFallingBlock(pos: BlockPos)(implicit w: World): Boolean = {
        val chunk = w.getChunkFromBlockCoords(pos)
        val entityLists = chunk.getEntityLists
        val aabb = FULL_BLOCK_AABB.offset(pos)
        for(t <- entityLists(floor_double((aabb.minY - 1) / 16D)).getByClass(classOf[EntityFallingBlock]))
            if(t.getEntityBoundingBox.intersectsWith(aabb)) return true
        for(t <- entityLists(floor_double((aabb.minY + 1) / 16D)).getByClass(classOf[EntityFallingBlock]))
            if(t.getEntityBoundingBox.intersectsWith(aabb)) return true
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

        def canFall = block.isInstanceOf[BlockFalling] || (granularFall && block.isGranular)

        def canSpread = canFall && blockSpread

        def canSpreadInAvalanche = !EnviroMineLoaded && canSpread && avalanches && !block.isSoil

        def canFallFrom(pos: BlockPos)(implicit w: World) =
            canFall && !w.isRemote && canDisplace(blockAt(pos.down))

        def fallFrom(pos: BlockPos, posOrigin: BlockPos)(implicit w: World) {
            if(!blocksFallInstantlyAt(pos)) {
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
            canSpread && !w.isRemote && !populating && !canDisplace(blockAt(pos.down))

        def spreadFrom(pos: BlockPos)(implicit w: World) {
            val freeNeighbors = pos.neighbors.filter(canSpreadThrough)
            randomElementOption(freeNeighbors.toArray)(w.rand).foreach(fallFrom(_, pos))
        }
    }
}
