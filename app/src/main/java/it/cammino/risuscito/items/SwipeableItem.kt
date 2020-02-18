package it.cammino.risuscito.items

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.IItemVHFactory
import com.mikepenz.fastadapter.drag.IExtendedDraggable
import com.mikepenz.fastadapter.items.BaseItem
import com.mikepenz.fastadapter.items.BaseItemFactory
import com.mikepenz.fastadapter.swipe.ISwipeable
import com.mikepenz.fastadapter.ui.utils.StringHolder
import com.mikepenz.fastadapter.utils.DragDropUtil
import it.cammino.risuscito.R
import it.cammino.risuscito.Utility.helperSetString
import kotlinx.android.synthetic.main.swipeable_item.view.*

fun swipeableItem(block: SwipeableItem.() -> Unit): SwipeableItem = SwipeableItem().apply(block)

class SwipeableItem : BaseItem<SwipeableItem.ViewHolder>(), ISwipeable, IExtendedDraggable<RecyclerView.ViewHolder> {

    override val type: Int
        get() = R.id.fastadapter_swipable_item_id

    override val factory: IItemVHFactory<ViewHolder> = SwipeableItemFactory

    var name: StringHolder? = null
    var setName: Any? = null
        set(value) {
            name = helperSetString(value)
        }

    var idCanto: String = ""

    var swipedDirection: Int = 0
    var swipedAction: Runnable? = null
    override var touchHelper: ItemTouchHelper? = null

    override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)

        // get the context
        val ctx = holder.itemView.context

        //set the text for the name
        StringHolder.applyTo(name, holder.name)
        //set the text for the description or hide

        holder.swipeResultContent?.isVisible = swipedDirection != 0
        holder.itemContent?.isInvisible = swipedDirection != 0

        var swipedAction: CharSequence? = null
        var swipedText: CharSequence? = null
        if (swipedDirection != 0) {
            swipedAction = ctx.getString(R.string.cancel)
            swipedText = ctx.getString(R.string.generic_removed, name?.getText(ctx))
            holder.swipeResultContent?.setBackgroundColor(ContextCompat.getColor(ctx, if (swipedDirection == ItemTouchHelper.LEFT) R.color.md_red_900 else R.color.md_red_900))
        }
        holder.swipedAction?.text = swipedAction ?: ""
        holder.swipedText?.text = swipedText ?: ""
        holder.swipedActionRunnable = this.swipedAction

        DragDropUtil.bindDragHandle(holder, this)
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        holder.name?.text = null
        holder.swipedAction?.text = null
        holder.swipedText?.text = null
        holder.swipedActionRunnable = null
    }

    override fun getDragView(viewHolder: RecyclerView.ViewHolder): View? {
        return (viewHolder as? ViewHolder)?.mDragHandler
    }

    override var isSwipeable = true
    override var isDraggable = true

    /**
     * our ViewHolder
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        internal var name: TextView? = null
        internal var swipeResultContent: View? = null
        internal var itemContent: View? = null
        internal var swipedText: TextView? = null
        internal var swipedAction: TextView? = null
        internal var mDragHandler: View? = null

        internal var swipedActionRunnable: Runnable? = null

        init {
            name = view.swipeable_text1
            swipeResultContent = view.swipe_result_content
            itemContent = view.container
            swipedText = view.swiped_text
            swipedAction = view.swiped_action
            mDragHandler = view.drag_image
            swipedAction?.setOnClickListener {
                swipedActionRunnable?.run()
            }
        }
    }
}

object SwipeableItemFactory : BaseItemFactory<SwipeableItem.ViewHolder>() {

    override val type: Int
        get() = R.id.fastadapter_swipable_item_id

    override val layoutRes: Int
        get() = R.layout.swipeable_item

    override fun getViewHolder(v: View): SwipeableItem.ViewHolder {
        return SwipeableItem.ViewHolder(v)
    }

}