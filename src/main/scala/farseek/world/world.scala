package farseek

import net.minecraft.block.state.IBlockState
import net.minecraft.util.math.BlockPos
import net.minecraft.world.{IBlockReader, IWorldWriter}

package object world {

  implicit class IBlockReaderOps(val reader: IBlockReader) extends AnyVal {
    def apply(pos: BlockPos): IBlockState = reader.getBlockState(pos)
  }

  implicit class IWorldWriterOps(val writer: IWorldWriter) extends AnyVal {
    def update(pos: BlockPos, state: IBlockState): Boolean = writer.setBlockState(pos, state, 3)
  }
}
