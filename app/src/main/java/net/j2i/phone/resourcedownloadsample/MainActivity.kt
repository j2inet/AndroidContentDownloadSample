package net.j2i.phone.resourcedownloadsample

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


class MainActivity : AppCompatActivity() {

    val imageList = ArrayList<LabeledImage>()
    lateinit var  imageListAdapter:LabeledImageListAdapter
    lateinit var imageListView:ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        var updater = ContentUpdater(this, 0);
        updater.applyCompleteDownloadSet()
        updater.checkForUpdates()
        loadImageList()
        imageListAdapter = LabeledImageListAdapter(this, imageList)
        imageListView = findViewById(R.id.image_listview) as ListView
        imageListView.adapter = imageListAdapter

        imageListView.setOnItemClickListener(object:AdapterView.OnItemClickListener {
            override fun onItemClick(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val li:LabeledImage = imageListAdapter.getItem(position) as LabeledImage
                val intent = Intent(applicationContext, ViewImageActivity::class.java).apply {
                    putExtra("image", li.imageSource)
                    putExtra("caption", li.captionSource)
                }
                startActivity(intent)
            }

        })
    }


    fun loadImageList() {
        val listSource = File(filesDir, "assetsManifest.json")
        val contents = listSource.bufferedReader().readText()
        val assetsObject = JSONObject(contents)
        val itemList = assetsObject.getJSONArray("items")
        imageList.clear()
        for(i:Int in 0 until itemList.length())
        {
            val current = itemList.get(i) as JSONObject
            val caption = File(this.filesDir, current.getString("caption")).bufferedReader().readText()
            val imageFilePath = File(this.filesDir, current.getString("image")).absolutePath
            val image:Bitmap = BitmapFactory.decodeFile(imageFilePath)
            val li:LabeledImage = LabeledImage(image, caption, current.getString("image"), current.getString("caption"))
            imageList.add(li)
        }
    }
}