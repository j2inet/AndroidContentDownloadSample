package net.j2i.phone.resourcedownloadsample

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import org.json.JSONArray
import java.nio.file.Path
import java.nio.file.Paths
import org.json.JSONObject
import java.io.*
import java.net.URI
import java.net.URL
import java.net.URLConnection
import java.util.*
import kotlin.concurrent.thread

class ContentUpdater {

    companion object {
        public val TAG = "ContentUpdater"
        val STAGING_FOLDER = "staging"
        val COMPLETE_FOLDER = "completed"
    }
    val context:Context
    val downloadQueue = LinkedList<ResourceDownloadRequest>()



    constructor(context: Context, minVersion:Int) {
        this.context = context

        this.ensureFoldersExists()
        extractAssetsFromApplication(minVersion);
        this.clearPartialDownload()
    }



    fun checkForUpdates() {
        thread {
            val updateFile = File(context.filesDir, "updates.json")
            val sourceUpdateText = updateFile.bufferedReader().readText()
            val updateStructure = JSONObject(sourceUpdateText)
            val currentVersion = updateStructure.getInt("version")
            val updateURL = URL(updateStructure.getString("updateURL"))
            val newUpdateText =
                updateURL.openConnection().getInputStream().bufferedReader().readText()
            val newUpdateStructure = JSONObject(newUpdateText)
            val newVersion = newUpdateStructure.getInt("version")
            if (newVersion > currentVersion) {
                val assetsList = newUpdateStructure.getJSONArray("assets")
                for (i: Int in 0 until assetsList.length()) {
                    val current = assetsList.get(i) as JSONObject
                    val dlRequest = ResourceDownloadRequest(
                        current.getString("name"),
                        URL(current.getString("url"))
                    )
                    downloadQueue.add(dlRequest)
                }
                downloadFiles();
            }
        }
    }

    @WorkerThread
    fun downloadFiles() {
        val MAX_RETRY_COUNT = 3
        val failedQueue = LinkedList<ResourceDownloadRequest>()
        var retryCount = 0;
        while(retryCount<MAX_RETRY_COUNT && downloadQueue.count()>0) {

            while (downloadQueue.count()>0) {
                val current = downloadQueue.pop()
                try {
                    downloadFile(current)
                } catch (exc: IOException) {
                    failedQueue.add(current)
                }
            }
            downloadQueue.clear()
            downloadQueue.addAll(failedQueue)
            ++retryCount;
        }
        if(downloadQueue.count()>0) {
            //we've failed to download a complete set.
        } else {
            //A complete set was downloaded
            //I'll mark a set as complete by creating a file. The presence of this file
            //markets a complete set. An absence would indicate a failure.
            val isCompleteFile = File(context.filesDir, COMPLETE_FOLDER + "/isComplete")
            isCompleteFile.createNewFile()
        }
    }

    fun applyCompleteDownloadSet() {
        val isCompleteFile = File(context.filesDir, COMPLETE_FOLDER + "/isComplete")
        if(!isCompleteFile.exists()) {
            return;
        }
        var downloadFolder = File(context.filesDir, COMPLETE_FOLDER)
        val fileListToMove = downloadFolder.listFiles()
        for(f:File in fileListToMove) {
            val destination = File(context.filesDir, f.name)
            f.copyTo(destination, true)
            f.delete()
        }
    }

    private fun assetFilePath(context: Context, assetName: String, overwrite:Boolean = false): String? {
        val file = File(context.filesDir, assetName)
        if (!overwrite && file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        try {
            context.assets.open(assetName).use { inputStream ->
                FileOutputStream(file).use { os ->
                    val buffer = ByteArray(4 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        os.write(buffer, 0, read)
                    }
                    os.flush()
                }
                return file.absolutePath
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error process asset $assetName to file path")
        }
        return null
    }

    fun downloadFile(d:ResourceDownloadRequest) {
        downloadFile(d.name, d.source)
    }

    fun downloadFile(name:String, source: URL) {
        val DOWNLOAD_BUFFER_SIZE = 8192
        val urlConnection:URLConnection = source.openConnection()
        urlConnection.connect();
        val length:Int = urlConnection.contentLength

        val inputStream:InputStream = BufferedInputStream(source.openStream(), DOWNLOAD_BUFFER_SIZE)
        val targetFile = File(context.filesDir, STAGING_FOLDER + "/"+ name)
        targetFile.createNewFile();
        val outputStream = targetFile.outputStream()
        val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
        var bytesRead = 0
        var totalBytesRead = 0;
        var percentageComplete = 0.0f
        do {
            bytesRead = inputStream.read(buffer,0,DOWNLOAD_BUFFER_SIZE)
            if(bytesRead>-1) {
                totalBytesRead += bytesRead
                percentageComplete = 100F * totalBytesRead.toFloat() / length.toFloat()
                outputStream.write(buffer, 0, bytesRead)
            }
        } while(bytesRead > -1)
        outputStream.close()
        inputStream.close()
        val destinationFile = File(context.filesDir, COMPLETE_FOLDER + "/"+ name)
        targetFile.copyTo(destinationFile, true, DEFAULT_BUFFER_SIZE)
        targetFile.delete()
    }


    fun extractAssetsFromApplication(minVersion:Int, overwrite:Boolean = false) {
        //ensure that updates.json exists in the file system
        val updateFileName = "updates.json"
        val file = File(context.filesDir, updateFileName)
        val updatesFilePath = assetFilePath(this.context,updateFileName, overwrite);
        //Load the contents of updates.json
        val updateFile = File(updatesFilePath).inputStream();
        val contents = updateFile.bufferedReader().readText()
        //Use a JSONObject to parse out the file's data
        val updateObject = JSONObject(contents)
        //IF the version in the file is below some version, assume that it is
        //an old version left over from a previous version of the application.
        //restart the extraction process with the overwrite flag set
        val assetVersion = updateObject.getInt("version")
        if(assetVersion < minVersion) {
            extractAssetsFromApplication(minVersion,true)
            return
        }
        //Let's start processing the individual asset items.
        val assetList = updateObject.get("assets") as JSONArray
        for(i in 0 until assetList.length()) {
            val currentObject = assetList.get(i) as JSONObject
            val currentFileName = currentObject.getString("name")
            val uri:String? =  currentObject.getString("url")

            if(uri.isNullOrEmpty() || uri == "null") {
                //There is no URL associated with the file. It must be within
                // the application package. Copy it from the application package
                //and write it to the file system
                assetFilePath(this.context, currentFileName, overwrite)
            } else {
                //If there is a URL associated with the asset, then add it to the download
                //queue. It will be downloaded later.
                val downloadRequest = ResourceDownloadRequest(currentFileName, URL(uri))
                downloadQueue.add(downloadRequest)
            }
        }
    }

    fun ensureFoldersExists() {
       val applicationFilesFolder = context.filesDir.absoluteFile;
        val stagingFolderPath = Paths.get(applicationFilesFolder.absolutePath, STAGING_FOLDER)
        val stagingFolder:File =  stagingFolderPath.toFile()
        if(!stagingFolder.exists()) {
            stagingFolder.mkdir()
        }
        val downloadSetPath = Paths.get(applicationFilesFolder.absolutePath, COMPLETE_FOLDER)
        val completedFolder:File = downloadSetPath.toFile()
        if(!completedFolder.exists()) {
            completedFolder.mkdir()
        }
    }

    fun clearPartialDownload() {
        val stagingFolder = File(context.filesDir, STAGING_FOLDER)
        //If we have a staging folder, we need to check it's contents and delete them
        if(stagingFolder.exists())
        {
            val fileList = stagingFolder.listFiles()
            for(f in fileList) {
                f.delete()
            }
        }
    }
}