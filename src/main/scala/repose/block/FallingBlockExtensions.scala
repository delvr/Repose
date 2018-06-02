package repose.block

import farseek.block._
import farseek.util.ImplicitConversions._
import farseek.util._
import farseek.world._
import java.lang.Package._
import java.util.Random
import net.minecraft.block.Block._
import net.minecraft.block._
import net.minecraft.block.state._
import net.minecraft.entity._
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.init.Blocks._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.server.MinecraftServer
import net.minecraft.util.SoundCategory
import net.minecraft.util.math.MathHelper._
import net.minecraft.util.math._
import net.minecraft.world.World
import repose.config.ReposeConfig._
import repose.entity.item.EntityFallingBlockExtensions._
import scala.collection.JavaConversions._

/** @author delvr */
object FallingBlockExtensions {

    val EnviroMineLoaded: Boolean = getPackage("enviromine") != null

    val FallDelay = 2

    def onBlockAdded(block: Block, w: World, pos: BlockPos, state: IBlockState) {
        implicit val world = w
        if(state.canFallFrom(pos))
            w.scheduleUpdate(pos, block, state.fallDelay)
        else
            block.onBlockAdded(w, pos, state)
    }

    def onBlockPlacedBy(block: Block, w: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, item: ItemStack) {
        implicit val world = w
        if(state.canSpreadFrom(pos))
            state.spreadFrom(pos)
        else
            block.onBlockPlacedBy(w, pos, state, placer, item)
    }

    def neighborChanged(state: IBlockState, w: World, pos: BlockPos, formerNeighbor: Block, neighborPos: BlockPos) {  // doesn't work with top-level IBlockBehaviors
        implicit val world = w
        if(state.canFallFrom(pos)) // optimizing with !canDisplace(formerNeighbor) fails with SpongeForge
            w.scheduleUpdate(pos, state.getBlock, state.fallDelay)
        else
            state.neighborChanged(w, pos, formerNeighbor, neighborPos)
    }

    def updateTick(block: Block, w: World, pos: BlockPos, state: IBlockState, random: Random) {
        implicit val world = w
        if(state.canFallFrom(pos))
            state.fallFrom(pos, pos)
        else
            block.updateTick(w, pos, state, random)
    }

    def onBlockDestroyedByPlayer(block: Block, w: World, pos: BlockPos, state: IBlockState) {
        implicit val world = w
        block.onBlockDestroyedByPlayer(w, pos, state)
        triggerNeighborSpread(pos.up)
    }

    def triggerNeighborSpread(pos: BlockPos)(implicit w: World) {
        if(!populating && !w.isRemote && !w.getBlockState(pos).getBlock.isLiquid) { // Prevent beach destruction
            for(nPos <- pos.neighbors) {
                val neighbor = w.getBlockState(nPos)
                if(neighbor.canSpreadInAvalanche && !occupiedByFallingBlock(nPos) && neighbor.canSpreadFrom(nPos))
                    neighbor.spreadFrom(nPos)
            }
        }
    }

    def canFallThrough(state: IBlockState): Boolean = canDisplace(state) || !state.isTopSolid

    def canSpreadThrough(pos: BlockPos)(implicit w: World): Boolean =
        canDisplace(w.getBlockState(pos)) && canFallThrough(w.getBlockState(pos.down)) && !occupiedByFallingBlock(pos)

    def occupiedByFallingBlock(pos: BlockPos)(implicit w: World): Boolean = {
        val chunk = w.getChunkFromBlockCoords(pos)
        val entityLists = chunk.getEntityLists
        val aabb = FULL_BLOCK_AABB.offset(pos)
        for(t <- entityLists(clamped(0, floor((aabb.minY - 1) / 16D), entityLists.length - 1)).getByClass(classOf[EntityFallingBlock]))
            if(t.getEntityBoundingBox.intersects(aabb)) return true
        for(t <- entityLists(clamped(0, floor((aabb.minY + 1) / 16D), entityLists.length - 1)).getByClass(classOf[EntityFallingBlock]))
            if(t.getEntityBoundingBox.intersects(aabb)) return true
        false
    }

    def canDisplace(state: IBlockState): Boolean = !state.getBlock.isSolid

    def onLanding(pos: BlockPos, state: IBlockState, entityTags: Option[NBTTagCompound])(implicit w: World): Unit = {
        val block = state.getBlock
        val stateHere = w.getBlockState(pos)
        // blockHere: landing on a slab; pos.down: landing on a ladder
        if(!canDisplace(stateHere) || canDisplace(w.getBlockState(pos.down)))
            block.dropBlockAsItem(w, pos, state, 0)
        else {
            if(!w.isAirBlock(pos))
                stateHere.getBlock.dropBlockAsItem(w, pos, w.getBlockState(pos), 0)
            w.setBlockState(pos, state)
            block match {
                case bf: BlockFalling => bf.onEndFalling(w, pos, state, stateHere)
                case _ =>
            }
            entityTags.foreach(copyTileEntityTags(pos, _))
            if(!serverDelayed && state.canSpreadFrom(pos))
                state.spreadFrom(pos)
        }
        val sound = block.getSoundType(state, w, pos, null)
        w.playSound(null, pos, sound.breakSound, SoundCategory.BLOCKS, sound.getVolume, sound.getPitch)
    }

    def copyTileEntityTags(pos: BlockPos, tags: NBTTagCompound)(implicit w: World) {
        Option(w.getTileEntity(pos)).foreach { tileEntity =>
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

    def serverDelayed(implicit w: World): Boolean = MinecraftServer.getCurrentTimeMillis - w.getMinecraftServer.currentTime > 2000L

    implicit class FallingBlockValue(val state: IBlockState) extends AnyVal {

        def fallDelay: Int = FallDelay

        def canFall(implicit w: World): Boolean = !w.isRemote && granularFall.value && reposeGranularBlocks.value.contains(state)

        def canSpread(implicit w: World): Boolean = canFall && blockSpread.value

        def canSpreadInAvalanche(implicit w: World): Boolean = !EnviroMineLoaded && canSpread && avalanches.value && !state.getBlock.isSoil

        def canFallFrom(pos: BlockPos)(implicit w: World): Boolean = canFall && w.isBlockLoaded(pos.down) && canFallThrough(w.getBlockState(pos.down))

        def fallFrom(pos: BlockPos, posOrigin: BlockPos)(implicit w: World) {
            val origState = w.getBlockState(posOrigin)
            val origBlock = origState.getBlock
            val state = if(origBlock == GRASS || origBlock == GRASS_PATH || origBlock == FARMLAND) DIRT.getDefaultState else origState
            val tileEntity = Option(w.getTileEntity(posOrigin))
            if(!blocksFallInstantlyAt(pos) && !serverDelayed) {
                spawnFallingBlock(state, pos, posOrigin, tileEntity)
            } else {
                w.setBlockToAir(posOrigin)
                downFrom(pos.down).find(!state.canFallFrom(_)).foreach { posLanded =>
                    val entityTags = tileEntity.map { entity =>
                        val tags = new NBTTagCompound
                        entity.writeToNBT(tags)
                        tags
                    }
                    onLanding(posLanded, state, entityTags)
                }
            }
        }

        def canSpreadFrom(pos: BlockPos)(implicit w: World): Boolean =
            canSpread && !populating && w.isBlockLoaded(pos) && !canFallThrough(w.getBlockState(pos.down))

        def spreadFrom(pos: BlockPos)(implicit w: World) {
            val freeNeighbors = pos.neighbors.filter(canSpreadThrough)
            randomElementOption(freeNeighbors.toArray)(w.rand).foreach(fallFrom(_, pos))
        }
    }
}
