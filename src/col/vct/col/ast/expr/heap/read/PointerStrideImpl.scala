package vct.col.ast.expr.heap.read

import vct.col.ast.ops.PointerStrideOps
import vct.col.ast.{PointerStride, TInt, Type}
import vct.col.print.{Ctx, Doc, Precedence, Text}

trait PointerStrideImpl[G] extends PointerStrideOps[G] {
  this: PointerStride[G] =>
  override def t: Type[G] = TInt()

  override def precedence: Int = Precedence.ATOMIC
  override def layout(implicit ctx: Ctx): Doc =
    Text("\\pointer_stride(") <> pointer <> ")"
}
