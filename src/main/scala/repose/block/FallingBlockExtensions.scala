package repose.block

import farseek.block._
import farseek.util.ImplicitConversions._
import farseek.util.Reflection._
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
import net.minecraft.util._
import net.minecraft.util.math.MathHelper._
import net.minecraft.util.math._
import net.minecraft.world.World
import repose.config.ReposeConfig._
import repose.entity.item.EntityFallingBlockExtensions._
import scala.collection.JavaConversions._

/** @author delvr */
object FallingBlockExtensions {

    private lazy val spongeMixinBlockNeighborOverrideField = classOf[Block].field("hasNeighborOverride")

    def setSpongeNeighborOverrides(): Unit = spongeMixinBlockNeighborOverrideField.foreach(field =>
        Block.REGISTRY.iterator.foreach(field.setValue(true, _))) // Enable everything because config could change mid-game (todo: implement observer)

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

    def neighborChanged(state: IBlockState, w: World, pos: BlockPos, formerNeighbor: Block, neighborPos: BlockPos) { // doesn't work with top-level IBlockBehaviors
        implicit val world = w
        if(state.canFallFrom(pos))
            w.scheduleUpdate(pos, state.getBlock, state.fallDelay)
        else
            state.neighborChanged(w, pos, formerNeighbor, neighborPos)
    }

    // Legacy Block override compatible with certain versions of SpongeForge
    def neighborChanged(block: Block, state: IBlockState, w: World, pos: BlockPos, formerNeighbor: Block, neighborPos: BlockPos) {
        implicit val world = w
        if(state.canFallFrom(pos))
            w.scheduleUpdate(pos, state.getBlock, state.fallDelay)
        else
            block.neighborChanged(state, w, pos, formerNeighbor, neighborPos)
    }

    def updateTick(block: Block, w: World, pos: BlockPos, state: IBlockState, random: Random) {
        implicit val world = w
        if(state.canFallFrom(pos))
            state.fallFrom(pos, pos)
        else
            block.updateTick(w, pos, state, random)
    }

    def onPlayerDestroy(block: Block, w: World, pos: BlockPos, state: IBlockState) {
        implicit val world = w
        block.onPlayerDestroy(w, pos, state)
        triggerNeighborSpread(pos.up)
    }

    def triggerNeighborSpread(pos: BlockPos)(implicit w: World) {
        if(!populating && !w.isRemote && !w.getBlockState(pos).getMaterial.isLiquid) { // Prevent beach destruction
            for(nPos <- pos.neighbors) {
                val neighbor = w.getBlockState(nPos)
                if(neighbor.canSpreadInAvalanche && !occupiedByFallingBlock(nPos) && neighbor.canSpreadFrom(nPos))
                    neighbor.spreadFrom(nPos)
            }
        }
    }

    def canFallThrough(pos: BlockPos)(implicit w: World): Boolean = {
        val state = w.getBlockState(pos)
        if(breakOnPartialBlocks.value)
          canDisplace(state) || !hasSolidTop(pos, state)
        else
          canDisplace(state) && !hasSolidTop(pos, state)
    }

    def hasSolidTop(pos: BlockPos, state: IBlockState)(implicit w: World): Boolean = {
        val topBox = new AxisAlignedBB(0, 0.99, 0, 1, 1, 1).offset(pos)
        val intersectingBoxes = new java.util.ArrayList[AxisAlignedBB]
        state.addCollisionBoxToList(w, pos, topBox, intersectingBoxes, null, false)
        !intersectingBoxes.isEmpty
    }

    def canSpreadThrough(pos: BlockPos)(implicit w: World): Boolean =
        canDisplace(w.getBlockState(pos)) && canFallThrough(pos.down) && !occupiedByFallingBlock(pos)

    def occupiedByFallingBlock(pos: BlockPos)(implicit w: World): Boolean = {
        val chunk = w.getChunk(pos)
        val entityLists = chunk.getEntityLists
        val fullBlockBox = FULL_BLOCK_AABB.offset(pos)
        for(t <- entityLists(clamped(0, floor((fullBlockBox.minY - 1) / 16D), entityLists.length - 1)).getByClass(classOf[EntityFallingBlock]))
            if(t.getEntityBoundingBox.intersects(fullBlockBox)) return true
        for(t <- entityLists(clamped(0, floor((fullBlockBox.minY + 1) / 16D), entityLists.length - 1)).getByClass(classOf[EntityFallingBlock]))
            if(t.getEntityBoundingBox.intersects(fullBlockBox)) return true
        false
    }

    def canDisplace(state: IBlockState): Boolean = !state.getMaterial.blocksMovement

    def onLanding(collisionPos: BlockPos, state: IBlockState, entityTags: Option[NBTTagCompound])(implicit w: World): Unit = {
        val block = state.getBlock
        val pos = if(!breakOnPartialBlocks.value && !canDisplace(w.getBlockState(collisionPos))) collisionPos.up else collisionPos
        val stateHere = w.getBlockState(pos)
        if(breakOnPartialBlocks.value &&
          (!canDisplace(stateHere) || // ex.: landing on a slab
            canDisplace(w.getBlockState(pos.down)) || // ex.: landing on a ladder
            hasSolidTop(pos, stateHere))) // ex. landing IN a ladder (falling instantly)
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

        def canFall(implicit w: World): Boolean = !populating && !w.isRemote && granularFall.value && reposeGranularBlocks.value.contains(state)

        def canSpread(implicit w: World): Boolean = canFall && blockSpread.value

        def canSpreadInAvalanche(implicit w: World): Boolean = !EnviroMineLoaded && canSpread && avalanches.value && !state.getBlock.isSoil

        def canFallFrom(pos: BlockPos)(implicit w: World): Boolean = canFall && w.isBlockLoaded(pos.down) && canFallThrough(pos.down)

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
            canSpread && w.isBlockLoaded(pos) && !canFallThrough(pos.down)

        def spreadFrom(pos: BlockPos)(implicit w: World) {
            val freeNeighbors = pos.neighbors.filter(canSpreadThrough)
            randomElementOption(freeNeighbors.toArray)(w.rand).foreach(fallFrom(_, pos))
        }
    }
}
