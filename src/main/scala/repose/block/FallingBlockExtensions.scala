package repose.block

import farseek.block._
import farseek.util._
import farseek.world._
import java.util.Random
import net.minecraft.block._
import net.minecraft.block.state._
import net.minecraft.entity.MoverType.SELF
import net.minecraft.entity._
import net.minecraft.entity.item.EntityFallingBlock
import net.minecraft.init.Blocks._
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util._
import net.minecraft.util.math.MathHelper.floor
import net.minecraft.util.math._
import net.minecraft.world.{IWorld, World}
import repose.block.SlopingBlockExtensions._
import repose.config.ReposeConfig._
import scala.collection.JavaConverters._

object FallingBlockExtensions {

  val FallDelay = 2

  private def populating = BlockFalling.fallInstantly

  def onBlockAdded(state: IBlockState, w: World, pos: BlockPos, oldState: IBlockState): Unit = {
    if(state.canFallFrom(pos)(w))
      w.getPendingBlockTicks.scheduleTick(pos, state.block, state.fallDelay)
    else
      state.onBlockAdded(w, pos, oldState)
  }

  def onBlockPlacedBy(block: Block, w: World, pos: BlockPos, state: IBlockState, placer: EntityLivingBase, item: ItemStack): Unit = {
    if(state.canSpreadFrom(pos)(w))
      state.spreadFrom(pos)(w)
    else
      block.onBlockPlacedBy(w, pos, state, placer, item)
  }

  def neighborChanged(state: IBlockState, w: World, pos: BlockPos, formerNeighbor: Block, neighborPos: BlockPos): Unit = {
    if(state.canFallFrom(pos)(w))
      w.getPendingBlockTicks.scheduleTick(pos, state.block, state.fallDelay)
    else
      state.neighborChanged(w, pos, formerNeighbor, neighborPos)
  }

  def tick(state: IBlockState, w: World, pos: BlockPos, random: Random): Unit = {
    if(state.canFallFrom(pos)(w))
      state.fallFrom(pos, pos)(w)
    else
      state.tick(w, pos, random)
  }

  def onPlayerDestroy(block: Block, w: IWorld, pos: BlockPos, state: IBlockState): Unit = {
    block.onPlayerDestroy(w, pos, state)
    triggerNeighborSpread(pos.up)(w.getWorld)
  }

  def triggerNeighborSpread(pos: BlockPos)(implicit w: World): Unit = {
    if(!populating && !w.isRemote && !w(pos).isLiquid) { // Prevent beach destruction
      for(nPos <- pos.neighbors) {
        val neighbor = w(nPos)
        if(neighbor.canSpreadInAvalanche && !occupiedByFallingBlock(nPos) && neighbor.canSpreadFrom(nPos))
          neighbor.spreadFrom(nPos)
      }
    }
  }

  def canFallThrough(pos: BlockPos)(implicit w: World): Boolean = {
    val state = w(pos)
    canDisplace(state) || !state.isTopSolid
  }

  def canSpreadThrough(pos: BlockPos)(implicit w: World): Boolean =
    canDisplace(w(pos)) && canFallThrough(pos.down) && !occupiedByFallingBlock(pos)

  def occupiedByFallingBlock(pos: BlockPos)(implicit w: World): Boolean =
    !w.getEntitiesWithinAABB(classOf[EntityFallingBlock], new AxisAlignedBB(pos)).isEmpty

  def canDisplace(state: IBlockState): Boolean = !state.blocksMovement

  def onLanding(pos: BlockPos, state: IBlockState, entityTags: Option[NBTTagCompound])(implicit w: World): Unit = {
    val stateHere = w(pos)
    if(!canDisplace(stateHere) || // ex.: landed on a slab
        canDisplace(w(pos.down))) // ex.: landed on a ladder
      state.dropBlockAsItem(w, pos, 0)
    else {
      if(!w.isAirBlock(pos))
        stateHere.dropBlockAsItem(w, pos, 0)
      w(pos) = state
      stateHere.block match {
        case bf: BlockFalling => bf.onEndFalling(w, pos, state, stateHere)
        case _ =>
      }
      entityTags.foreach(copyTileEntityTags(pos, _))
      if(!serverDelayed && state.canSpreadFrom(pos))
        state.spreadFrom(pos)
    }
    val sound = state.getSoundType(w, pos, null)
    w.playSound(null, pos, sound.getBreakSound, SoundCategory.BLOCKS, sound.getVolume, sound.getPitch)
  }

  def copyTileEntityTags(pos: BlockPos, tags: NBTTagCompound)(implicit w: World): Unit = {
    Option(w.getTileEntity(pos)).foreach{ tileEntity =>
      val newTags = new NBTTagCompound
      tileEntity.write(newTags)
      for(tag: String <- tags.keySet.asScala) {
        if(tag != "x" && tag != "y" && tag != "z")
          newTags.put(tag, tags.get(tag))
      }
      tileEntity.read(newTags)
      tileEntity.markDirty()
    }
  }

  def blocksFallInstantlyAt(pos: BlockPos)(implicit w: World): Boolean = {
    val (x, y, z) = (pos.x, pos.y, pos.z)
    BlockFalling.fallInstantly || !w.isAreaLoaded(x - 32, y - 32, z - 32, x + 32, y + 32, z + 32, false)
  }

  implicit class FallingBlockValue(val state: IBlockState) extends AnyVal {

    def fallDelay: Int = FallDelay

    def canFall(implicit w: World): Boolean = !populating && !w.isRemote && granularFall.get && granularBlocks.contains(state)

    def canSpread(implicit w: World): Boolean = canFall && blockSpread.get

    def canSpreadInAvalanche(implicit w: World): Boolean = canSpread && avalanches.get && !soilStates.contains(state)

    def canFallFrom(pos: BlockPos)(implicit w: World): Boolean = canFall && w.isBlockLoaded(pos.down) && canFallThrough(pos.down)

    def fallFrom(pos: BlockPos, posOrigin: BlockPos)(implicit w: World): Unit = {
      val origState = w(posOrigin)
      val origBlock = origState.block
      val state = if(origBlock == GRASS_BLOCK || origBlock == GRASS_PATH || origBlock == FARMLAND) DIRT() else origState
      val tileEntity = Option(w.getTileEntity(posOrigin))
      if(!serverDelayed && !blocksFallInstantlyAt(pos)) {
        spawnFallingBlock(state, pos, posOrigin, tileEntity)
      } else {
        w.removeBlock(posOrigin)
        ((pos.y - 1) to 0 by -1).map(new BlockPos(pos.x, _, pos.z)).find(!state.canFallFrom(_)).foreach { posLanded =>
          val entityTags = tileEntity.map{ entity =>
            val tags = new NBTTagCompound
            entity.write(tags)
            tags
          }
          onLanding(posLanded, state, entityTags)
        }
      }
    }

    def canSpreadFrom(pos: BlockPos)(implicit w: World): Boolean =
      canSpread && w(pos).blockHeight(pos) > 0.5 && // exclude things like thin snow layers
        w.isBlockLoaded(pos) && !canFallThrough(pos.down)

    def spreadFrom(pos: BlockPos)(implicit w: World): Unit = {
      val freeNeighbors = pos.neighbors.filter(canSpreadThrough)
      freeNeighbors.randomElementOption(w.rand).foreach(fallFrom(_, pos))
    }
  }

  private def serverDelayed(implicit w: World): Boolean = {
    val server = w.getServer
    val maxLag = maxServerLag.get
    server != null && maxLag >= 0 && Util.milliTime - server.getServerTime > maxLag
  }

  private def spawnFallingBlock(state: IBlockState, pos: BlockPos, posOrigin: BlockPos, tileEntity: Option[TileEntity])(implicit w: World): Boolean = {
    val e = new EntityFallingBlock(w, pos.x + 0.5D, pos.y, pos.z + 0.5D, state)
    e.prevPosX = posOrigin.x + 0.5D
    e.prevPosY = posOrigin.y
    e.prevPosZ = posOrigin.z + 0.5D
    tileEntity.foreach {
      val entityData = e.getEntityData
      entityData.keySet.asScala.foreach(entityData.remove)
      _.write(entityData)
    }
    w.spawnEntity(e)
  }

  def tick(entity: Entity): Unit = {
    entity match {
      case fallingBlock: EntityFallingBlock =>
        import fallingBlock._
        implicit val w: World = world
        val state = getBlockState
        fallTime += 1
        if(fallTime < 1000) {
          val posOrigin = new BlockPos(prevPosX, prevPosY, prevPosZ)
          prevPosX = posX
          prevPosY = posY
          prevPosZ = posZ
          motionY -= 0.04D
          move(SELF, 0D, motionY, 0D)
          if(!w.isRemote) {
            if(fallTime == 1) {
              w.removeBlock(posOrigin)
              if(state.canSpreadInAvalanche)
                triggerNeighborSpread(posOrigin.up)
            }
            if(state.canSpreadInAvalanche && !serverDelayed) {
              val box = getBoundingBox
              val yTopCurrent = floor(box.yMax)
              val yTopPrevious = floor(box.yMax - motionY)
              if(yTopCurrent < yTopPrevious)
                triggerNeighborSpread(new BlockPos(posX, yTopPrevious, posZ))
            }
            if(onGround) {
              remove()
              onLanding(new BlockPos(fallingBlock), state, if(getEntityData.size > 0) Option(getEntityData) else None)
            }
          }
        } else if(!w.isRemote) {
          remove()
          state.dropBlockAsItem(w, new BlockPos(posX, posY, posZ), 0)
        }
      case e: Entity => e.tick()
    }
  }

}
