package it.cammino.risuscito.items

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.fastadapter.IItemVHFactory
import com.mikepenz.fastadapter.items.BaseItem
import com.mikepenz.fastadapter.items.BaseItemFactory
import com.mikepenz.fastadapter.ui.utils.FastAdapterUIUtils
import com.mikepenz.fastadapter.ui.utils.StringHolder
import it.cammino.risuscito.R
import it.cammino.risuscito.objects.PosizioneItem
import it.cammino.risuscito.objects.PosizioneTitleItem
import it.cammino.risuscito.utils.themeColor
import kotlinx.android.synthetic.main.generic_card_item.view.*
import kotlinx.android.synthetic.main.generic_list_item.view.*
import kotlinx.android.synthetic.main.simple_row_item.view.*

fun listaPersonalizzataItem(block: ListaPersonalizzataItem.() -> Unit): ListaPersonalizzataItem = ListaPersonalizzataItem().apply(block)

fun ListaPersonalizzataItem.posizioneTitleItem(block: PosizioneTitleItem.() -> Unit) {
    titleItem = PosizioneTitleItem().apply(block)
}

class ListaPersonalizzataItem : BaseItem<ListaPersonalizzataItem.ViewHolder>() {

    override val type: Int
        get() = R.id.fastadapter_listapers_item_id

    override val factory: IItemVHFactory<ViewHolder> = ListaPersonalizzataItemFactory

    var titleItem: PosizioneTitleItem? = null
    var listItem: List<PosizioneItem>? = null
    var id: Int = 0
        set(value) {
            identifier = value.toLong()
            field = value
        }

    var createClickListener: View.OnClickListener? = null
    var createLongClickListener: View.OnLongClickListener? = null

    override fun bindView(holder: ViewHolder, payloads: MutableList<Any>) {
        super.bindView(holder, payloads)

        // get the context
        val context = holder.itemView.context

        holder.list?.removeAllViews()
        val inflater = LayoutInflater.from(context)
        var itemView: View

        listItem?.let { list ->
            if (list.isNotEmpty()) {
                if (titleItem?.isMultiple == true) {
                    holder.addCanto?.isVisible = true
                    createClickListener?.let { holder.addCanto?.setOnClickListener(it) }
                } else
                    holder.addCanto?.isVisible = false
                for (i in list.indices) {
                    val canto = list[i]
                    itemView = inflater.inflate(R.layout.generic_card_item, holder.list, false)

                    val cantoView = itemView.cantoGenericoContainer

                    StringHolder.applyTo(canto.title, itemView.text_title)
                    StringHolder.applyTo(canto.page, itemView.text_page)
                    StringHolder.applyTo(canto.source, itemView.text_source_canto)
                    StringHolder.applyTo(canto.timestamp, itemView.text_timestamp)
                    itemView.text_id_canto_card.text = canto.idCanto.toString()
                    itemView.item_tag.text = i.toString()
                    cantoView.background = FastAdapterUIUtils.getSelectableBackground(
                            context,
                            context.themeColor(R.attr.colorSecondaryLight),
                            true)
                    if (canto.ismSelected()) {
                        val bgShape = itemView.selected_mark.background as? GradientDrawable
                        bgShape?.setColor(context.themeColor(R.attr.colorSecondary))
                        itemView.text_page.isVisible = false
                        itemView.selected_mark.isVisible = true
                        cantoView.isSelected = true
                    } else {
                        val bgShape = itemView.text_page.background as? GradientDrawable
                        bgShape?.setColor(canto.color?.colorInt ?: Color.WHITE)
                        itemView.text_page.isVisible = true
                        itemView.selected_mark.isVisible = false
                        cantoView.isSelected = false
                    }

                    createClickListener?.let { cantoView.setOnClickListener(it) }
                    createLongClickListener?.let { cantoView.setOnLongClickListener(it) }
                    holder.list?.addView(itemView)
                }
            } else {
                holder.addCanto?.isVisible = true
                createClickListener?.let { holder.addCanto?.setOnClickListener(it) }
            }
        }

        holder.idPosizione?.text = titleItem?.idPosizione.toString()
        holder.nomePosizione?.text = titleItem?.titoloPosizione
        holder.tag?.text = titleItem?.tagPosizione.toString()
    }

    override fun unbindView(holder: ViewHolder) {
        super.unbindView(holder)
        holder.idPosizione?.text = null
        holder.nomePosizione?.text = null
        holder.tag?.text = null
    }

    class ViewHolder(var view: View) : RecyclerView.ViewHolder(view) {

        var idPosizione: TextView? = null
        var nomePosizione: TextView? = null
        var addCanto: View? = null
        var tag: TextView? = null
        var list: LinearLayout? = null

        init {
            idPosizione = itemView.text_id_posizione
            nomePosizione = itemView.titoloPosizioneGenerica
            addCanto = itemView.addCantoGenerico
            tag = itemView.generic_tag
            list = itemView.generic_list
        }
    }
}

object ListaPersonalizzataItemFactory : BaseItemFactory<ListaPersonalizzataItem.ViewHolder>() {

    override val type: Int
        get() = R.id.fastadapter_listapers_item_id

    override val layoutRes: Int
        get() = R.layout.generic_list_item

    override fun getViewHolder(v: View): ListaPersonalizzataItem.ViewHolder {
        return ListaPersonalizzataItem.ViewHolder(v)
    }

}
