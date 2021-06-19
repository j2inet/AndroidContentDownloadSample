package net.j2i.phone.resourcedownloadsample

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class ViewImageActivity: AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewimage)

        val imageName = intent.getStringExtra("image")
        val captionName = intent.getStringExtra("caption")

        val sourceFilePath = File(filesDir, imageName).absolutePath
        val image = BitmapFactory.decodeFile(sourceFilePath)
        val caption = File(filesDir, captionName).bufferedReader().readText()

        val captionView = findViewById<TextView>(R.id.detailed_caption)
        val imageView = findViewById<ImageView>(R.id.detailed_image)

        captionView.text = caption
        imageView.setImageBitmap(image)
    }
}