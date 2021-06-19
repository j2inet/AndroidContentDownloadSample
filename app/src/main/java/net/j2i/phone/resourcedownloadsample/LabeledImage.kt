package net.j2i.phone.resourcedownloadsample

import android.graphics.Bitmap


class LabeledImage {
    public val picture: Bitmap
    public val description: String

    val captionSource:String
    val imageSource:String

    constructor(picture:Bitmap, description:String, imageSource:String, captionSource:String, ) {
        this.picture = picture
        this.description = description
        this.imageSource = imageSource
        this.captionSource = captionSource
    }
}