package farseek

import farseek.fml.assertPassedStage
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.state.{IProperty, IStateHolder}
import net.minecraftforge.fml.ModLoadingStage.LOAD_REGISTRIES
import net.minecraftforge.registries.ForgeRegistries
import scala.collection.JavaConverters._

package object block {

  def currentBlocks: Set[Block] = ForgeRegistries.BLOCKS.iterator.asScala.toSet

  lazy val allBlocks: Set[Block] = {
    assertPassedStage(LOAD_REGISTRIES)
    currentBlocks
  }

  lazy val allStates: Set[IBlockState] = allBlocks.flatMap(_.allStates)

  implicit class BlockOps(val block: Block) extends AnyVal {

    def apply(): IBlockState = block.getDefaultState

    def apply[A <: Comparable[A]](property: IProperty[A]): A = block()(property)

    def apply[A <: Comparable[A], V <: A](propertyValue: (IProperty[A], V)): IBlockState = block()(propertyValue)

    def name: String = block.getRegistryName.toString

    def allStates: Seq[IBlockState] = block.getStateContainer.getValidStates.asScala
  }

  implicit class IStateHolderOps[T](val state: IStateHolder[T]) extends AnyVal {

    def apply[A <: Comparable[A]](property: IProperty[A]): A = state.get(property)

    def apply[A <: Comparable[A], V <: A](propertyValue: (IProperty[A], V)): T = state.`with`(propertyValue._1, propertyValue._2)
  }

  implicit class IBlockStateOps(val state: IBlockState) extends AnyVal {

    def block: Block = state.getBlock

    def material: Material = state.getMaterial

    def isMadeOf(materials: Material*): Boolean = materials.contains(material)

    def blocksMovement: Boolean = material.blocksMovement

    def isLiquid: Boolean = material.isLiquid
  }
}
