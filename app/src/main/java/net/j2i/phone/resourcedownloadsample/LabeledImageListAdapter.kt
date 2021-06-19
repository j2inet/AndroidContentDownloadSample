package net.j2i.phone.resourcedownloadsample

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import org.w3c.dom.Text

class LabeledImageListAdapter : ArrayAdapter<LabeledImage> {

    constructor(context: Context, source:ArrayList<LabeledImage>) : super(context,0, source) {

    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val li:LabeledImage = getItem(position) as LabeledImage
        var targetView = convertView ?: LayoutInflater.from(context).inflate(R.layout.picture_listview, parent, false)
        val caption:TextView = targetView?.findViewById(R.id.labeledimage_caption) as TextView
        val imageView:ImageView = targetView.findViewById(R.id.labeledimage_image) as ImageView
        caption.text = li.description
        imageView.setImageBitmap(li.picture)
        return targetView
    }
}