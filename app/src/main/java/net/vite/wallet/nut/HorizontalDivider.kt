package net.vite.wallet.nut

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import net.vite.wallet.utils.dp2px

class HorizontalDivider : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        if (parent.getChildAdapterPosition(view) == 0) {
            outRect.left = 0.0F.dp2px().toInt()
            outRect.right = 5.0F.dp2px().toInt()
        } else {
            outRect.left = 5.0F.dp2px().toInt()
            outRect.right = 5.0F.dp2px().toInt()
        }
    }
}