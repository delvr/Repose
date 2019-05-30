package farseek

import java.util.Random
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumFacing._
import net.minecraft.util.math.shapes.VoxelShape
import net.minecraft.util.math.{AxisAlignedBB, BlockPos}

package object util {

  val CardinalDirections: IndexedSeq[EnumFacing] = IndexedSeq(NORTH, EAST, SOUTH, WEST)
  val  OrdinalDirections: IndexedSeq[(EnumFacing, EnumFacing)] = IndexedSeq((NORTH, EAST), (SOUTH, EAST), (SOUTH, WEST), (NORTH, WEST))

  implicit class BlockPosOps(val pos: BlockPos) extends AnyVal {
    def x: Int = pos.getX
    def y: Int = pos.getY
    def z: Int = pos.getZ
    def +(facing: EnumFacing): BlockPos = pos.offset( facing)
    def -(facing: EnumFacing): BlockPos = pos.offset(-facing)
    def neighbors: IndexedSeq[BlockPos] = CardinalDirections.map(pos.offset)
  }

  implicit class EnumFacingOps(val facing: EnumFacing) extends AnyVal {
    def x: Int = facing.getXOffset
    def y: Int = facing.getYOffset
    def z: Int = facing.getZOffset
    def unary_- : EnumFacing = facing.getOpposite
  }

  implicit class VoxelShapeOps(val shape: VoxelShape) extends AnyVal {
    def xMin: Double = shape.getStart(Axis.X)
    def yMin: Double = shape.getStart(Axis.Y)
    def zMin: Double = shape.getStart(Axis.Z)
    def xMax: Double = shape.getEnd(Axis.X)
    def yMax: Double = shape.getEnd(Axis.Y)
    def zMax: Double = shape.getEnd(Axis.Z)
    def xSize: Double = xMax - xMin
    def ySize: Double = yMax - yMin
    def zSize: Double = zMax - zMin
  }

  implicit class AxisAlignedBBOps(val box: AxisAlignedBB) extends AnyVal {
    def xMin: Double = box.minX
    def yMin: Double = box.minY
    def zMin: Double = box.minZ
    def xMax: Double = box.maxX
    def yMax: Double = box.maxY
    def zMax: Double = box.maxZ
    def xSize: Double = xMax - xMin
    def ySize: Double = yMax - yMin
    def zSize: Double = zMax - zMin
  }

  implicit class SeqOps[A](val seq: Seq[A]) extends AnyVal {
    def randomElement(implicit random: Random): A = seq(random.nextInt(seq.size))
    def randomElementOption(implicit random: Random): Option[A] = if(seq.nonEmpty) Some(randomElement) else None
  }
}
