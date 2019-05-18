package com.anggrayudi.materialpreference.util

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.StatFs
import android.provider.DocumentsContract
import android.system.ErrnoException
import android.system.Os
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.list.listItems
import com.anggrayudi.materialpreference.R
import com.anggrayudi.materialpreference.callback.StoragePermissionDenialException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.*
import java.net.URLDecoder
import java.util.*

/**
 * @author Anggrayudi H
 */
object FileUtils {
    private const val TAG = "FileUtils"

    const val STORAGE_PERMISSION_GRANTED = 120
    const val STORAGE_PERMISSION_NOT_GRANTED = 121
    const val SDCARD_URI_PERMISSION_NOT_GRANTED = 122

    const val REQUEST_CODE_STORAGE_GET_FOLDER = 111
    const val REQUEST_CODE_STORAGE_GET_SINGLE_FILE = 112
    const val REQUEST_CODE_STORAGE_GET_SINGLE_MULTI_FILE = 113
    const val REQUEST_CODE_REQUIRE_SDCARD_ROOT_PATH_PERMISSIONS = 114
    const val REQUEST_CODE_REQUIRE_SDCARD_FILE_ROOT_PATH_PERMISSIONS = 115

    const val UNKNOWN_MIME_TYPE = "application/octet-stream"

    var sMovingFile: Boolean = false

    fun resolvePathFromUri(data: Intent): String? {
        val decodedPath = data.data!!.path
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            when (data.data!!.authority) {
                "com.android.externalstorage.documents" -> {
                    val withoutUri = getStringBetween(decodedPath!!, '/', ':') + decodedPath.substring(decodedPath.indexOf(':'), decodedPath.length)
                    var sdcardId = withoutUri.substring(0, withoutUri.indexOf(':') + 1)
                    val subFolder = withoutUri.substring(withoutUri.indexOf(':') + 1, withoutUri.length)
                    if (sdcardId == "primary:") {
                        sdcardId = SaveDir.EXTERNAL + "/"
                    }
                    return sdcardId + subFolder
                }
            }
        } else if ("file" == data.data!!.scheme) {
            return data.data!!.path
        }
        return SaveDir.EXTERNAL
    }

    fun resolveSdCardPath(data: Intent): String {
        val decodedPath = decodeUri(data.data!!.path)
        val withoutUri = getStringBetween(decodedPath!!, '/', ':') + decodedPath.substring(decodedPath.indexOf(':'), decodedPath.length)
        var sdcardId = withoutUri.substring(0, withoutUri.indexOf(':') + 1)
        val subFolder = withoutUri.substring(withoutUri.indexOf(':') + 1, withoutUri.length)
        if (sdcardId == "primary:") {
            sdcardId = SaveDir.EXTERNAL + "/"
        }
        return sdcardId + subFolder
    }

    fun saveUriPermission(context: Context, data: Intent): Boolean {
        val path = data.data!!.path
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && path!!.startsWith("/tree/")
                && !path.startsWith("/tree/primary")) {
            val root = Uri.parse("content://com.android.externalstorage.documents/tree/" +
                    getStringBetween(data.data?.path!!, '/', ':') + "%3A")
            if (data.data == root) {
                try {
                    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    context.contentResolver.takePersistableUriPermission(root, takeFlags)
                    return true
                } catch (e: SecurityException) {
                    Toast.makeText(context, R.string.please_grant_storage_permission, Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, R.string.not_root_path, Toast.LENGTH_SHORT).show()
            }
        }
        return false
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun isSdCardUriPermissionsGranted(context: Context, data: Intent): Boolean {
        val root = Uri.parse("content://com.android.externalstorage.documents/tree/" +
                getStringBetween(data.data?.path!!, '/', ':') + "%3A")
        context.contentResolver.persistedUriPermissions.forEach {
            if (it.isReadPermission && it.isWritePermission && it.uri == root)
                return true
        }
        if (root == data.data) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(root, takeFlags)
                return true
            } catch (ignore: SecurityException) {
            }
        }
        return false
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    fun isSdCardUriPermissionsGranted(context: Context, sdcardId: String): Boolean {
        val root = Uri.parse("content://com.android.externalstorage.documents/tree/$sdcardId%3A")
        context.contentResolver.persistedUriPermissions.forEach {
            if (it.isReadPermission && it.isWritePermission && it.uri == root)
                return true
        }
        return false
    }

    /**
     * Get [DocumentFile] object from SD card.
     * @param directory SD card ID followed by directory name, for example `6881-2249:Download/Archive`,
     * where ID for SD card is `6881-2249`
     * @param fileName for example `intel_haxm.zip`
     * @return `null` if does not exist
     */
    private fun getExternalFile(context: Context, directory: String, fileName: String?): DocumentFile? {
        return try {
            getExternalFile(context, generateFileLocation(directory, fileName))
        } catch (e: StoragePermissionDenialException) {
            null
        }
    }

    @Throws(StoragePermissionDenialException::class)
    private fun getExternalFile(context: Context, file: String): DocumentFile? {
        var current = getExternalRoot(context, file)
        val cleanedPath = cleanSdCardPath(file)
        if (!cleanedPath.isEmpty()) {
            val dirs = if (cleanedPath.contains("/")) cleanedPath.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() else arrayOf(cleanedPath)
            var i = 0
            while (i < dirs.size && current != null) {
                current = current.findFile(dirs[i])
                i++
            }
        }
        return current
    }

    @Throws(StoragePermissionDenialException::class)
    private fun getExternalFolder(context: Context, file: String): DocumentFile? {
        try {
            val uri = Uri.parse("content://com.android.externalstorage.documents/tree/" + Uri.encode(file))
            return DocumentFile.fromTreeUri(context, uri)
        } catch (e: SecurityException) {
            throw StoragePermissionDenialException(context)
        }
    }

    @Throws(StoragePermissionDenialException::class)
    private fun getExternalRoot(context: Context, folder: String): DocumentFile? {
        return getExternalFolder(context, folder.substring(0, folder.indexOf(':') + 1))
    }

    @Throws(FileNotFoundException::class)
    fun openOutputStream(context: Context, file: DocumentFile): OutputStream? {
        return if (file.uri.scheme == "file") {
            // handle file from internal storage
            FileOutputStream(File(file.uri.path!!), true)
        } else {
            context.contentResolver.openOutputStream(file.uri, "wa")
        }
    }

    @Throws(FileNotFoundException::class)
    fun openInputStream(context: Context, file: DocumentFile): InputStream? {
        return if (file.uri.scheme == "file") {
            // handle file from internal storage
            FileInputStream(File(file.uri.path!!))
        } else {
            context.contentResolver.openInputStream(file.uri)
        }
    }

    @Throws(FileNotFoundException::class, StoragePermissionDenialException::class)
    fun openInputStream(context: Context, file: String): InputStream? {
        return if (file.startsWith("/")) {
            FileInputStream(File(file))
        } else {
            val documentFile = getExternalFile(context, file)
            if (documentFile != null && documentFile.isFile)
                context.contentResolver.openInputStream(documentFile.uri)
            else
                throw FileNotFoundException("File not found: $file")
        }
    }

    private fun decodeUri(uri: String?): String? {
        return try {
            URLDecoder.decode(uri, "ISO-8859-1")
        } catch (ignore: UnsupportedEncodingException) {
            uri
        }
    }

    private fun cleanSdCardPath(file: String): String {
        val tree = file.substring(file.indexOf(':') + 1, file.length)
        var resolvedPath = StringBuilder()
        val directories = tree.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        directories.forEach { directory ->
            val dir = directory.trim { it <= ' ' }
            if (dir.isNotEmpty()) {
                resolvedPath.append(dir).append("/")
            }
        }
        if (resolvedPath.toString().endsWith("/"))
            resolvedPath = StringBuilder(resolvedPath.substring(0, resolvedPath.length - 1))

        return resolvedPath.toString()
    }

    @Throws(StoragePermissionDenialException::class)
    fun mkdirs(context: Context, folder: String): DocumentFile? {
        if (folder.startsWith("/")) {
            val file = File(folder)
            file.mkdirs()
            if (file.isDirectory)
                return DocumentFile.fromFile(file)
        } else {
            var currentDirectory = getExternalRoot(context, folder)
            val cleanedPath = cleanSdCardPath(folder)
            if (cleanedPath.isNotEmpty()) {
                val s = if (cleanedPath.contains("/")) cleanedPath.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() else arrayOf(cleanedPath)
                s.forEach {
                    if (currentDirectory != null) {
                        val documentFile = currentDirectory?.findFile(it)
                        try {
                            if (documentFile == null || documentFile.isFile)
                                currentDirectory = currentDirectory?.createDirectory(it)
                            else if (documentFile.isDirectory)
                                currentDirectory = documentFile
                        } catch (e: RuntimeException) {
                        }
                    }
                }
            }
            if (currentDirectory != null && currentDirectory!!.uri.path!!.endsWith(cleanedPath))
                return currentDirectory
        }
        return null
    }

    @Throws(StoragePermissionDenialException::class)
    fun create(context: Context, folder: String, fileName: String): DocumentFile? {
        return if (folder.startsWith("/")) {
            val file = File(folder, fileName)
            file.parentFile.mkdirs()
            if (create(file)) DocumentFile.fromFile(file) else null
        } else {
            val directory = mkdirs(context, folder)
            directory?.createFile(UNKNOWN_MIME_TYPE, fileName)
        }
    }

    @Throws(StoragePermissionDenialException::class)
    fun recreate(context: Context, folder: String, fileName: String): DocumentFile? {
        return if (folder.startsWith("/")) {
            val file = File(folder, fileName)
            file.delete()
            file.parentFile.mkdirs()
            if (create(file)) DocumentFile.fromFile(file) else null
        } else {
            val directory = mkdirs(context, folder)
            if (directory != null) {
                val file = directory.findFile(fileName)
                if (file != null && file.isFile)
                    file.delete()
            }
            directory?.createFile(UNKNOWN_MIME_TYPE, fileName)
        }
    }

    private fun create(file: File): Boolean {
        return try {
            file.createNewFile()
        } catch (e: IOException) {
            false
        }
    }

    @Throws(StoragePermissionDenialException::class)
    fun delete(context: Context, folder: String, fileName: String?): Boolean {
        return if (folder.startsWith("/"))
            File(folder, fileName).delete()
        else {
            val documentFile = getExternalFile(context, folder, fileName)
            documentFile != null && documentFile.delete()
        }
    }

    @Throws(StoragePermissionDenialException::class)
    fun delete(context: Context, folder: String): Boolean {
        return if (folder.startsWith("/"))
            File(folder).delete()
        else {
            val documentFile = getExternalFile(context, folder)
            documentFile != null && documentFile.delete()
        }
    }

    fun resolveParentFile(file: String): String {
        return File(file).parent ?: file.substring(0, file.indexOf(':') + 1)
    }

    @Throws(StoragePermissionDenialException::class)
    fun fileExists(context: Context, file: String): Boolean {
        val f = File(file)
        return if (file.startsWith("/"))
            f.isFile
        else {
            val name = file.substring(file.lastIndexOf(if (file.contains("/")) '/' else ':') + 1, file.length)
            val documentFile = getExternalFile(context, resolveParentFile(file), name)
            documentFile != null && documentFile.isFile
        }
    }

    @Throws(StoragePermissionDenialException::class)
    fun fileExists(context: Context, folder: String, file: String): Boolean {
        return if (folder.startsWith("/"))
            File(folder, file).isFile
        else {
            val documentFile = getExternalFile(context, folder, file)
            documentFile != null && documentFile.isFile
        }
    }

    @Throws(StoragePermissionDenialException::class)
    fun fileSize(context: Context, folder: String, fileName: String): Long {
        return if (folder.startsWith("/"))
            File(folder, fileName).length()
        else {
            val documentFile = getExternalFile(context, folder, fileName)
            documentFile?.length() ?: 0
        }
    }

    @Throws(StoragePermissionDenialException::class)
    fun fileSize(context: Context, file: String): Long {
        return if (file.startsWith("/")) {
            File(file).length()
        } else {
            val documentFile = getExternalFile(context, file)
            documentFile?.length() ?: 0
        }
    }

    fun asDocumentFile(context: Context, folder: String, filename: String?): DocumentFile? {
        return if (folder.startsWith("/"))
            DocumentFile.fromFile(File(folder, filename))
        else
            getExternalFile(context, folder, filename)
    }

    @Throws(StoragePermissionDenialException::class)
    fun asDocumentFile(context: Context, file: String): DocumentFile? {
        return if (file.startsWith("/"))
            DocumentFile.fromFile(File(file))
        else
            getExternalFile(context, file)
    }

    @Throws(StoragePermissionDenialException::class)
    fun asDocumentFolder(context: Context, folder: String): DocumentFile? {
        return if (folder.startsWith("/"))
            DocumentFile.fromFile(File(folder))
        else
            getExternalFolder(context, folder)
    }

    fun getBaseName(filename: String): String {
        return org.apache.commons.io.FilenameUtils.getBaseName(if (filename.contains("/")) filename else filename.replaceFirst(":".toRegex(), "/"))
    }

    fun getExtension(filename: String): String {
        return org.apache.commons.io.FilenameUtils.getExtension(if (filename.contains("/")) filename else filename.replaceFirst(":".toRegex(), "/"))
    }

    fun samePartition(path1: String, path2: String): Boolean {
        if (path1.startsWith("/") && path2.startsWith("/"))
            return true
        else if (path1.startsWith("/") || path2.startsWith("/"))
            return false

        val sdcard1 = path1.substring(0, path1.indexOf(':'))
        val sdcard2 = path2.substring(0, path2.indexOf(':'))
        return sdcard1 == sdcard2
    }

    fun closeStream(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun writeFileFromInputStream(stream: InputStream, file: File) {
        var f = file
        var output: OutputStream? = null
        try {
            if (f.isDirectory) {
                f.createNewFile()
                f = File(f.absolutePath)
            }
            output = FileOutputStream(f)
            val buffer = ByteArray(1024)
            var read = stream.read(buffer)
            while (read != -1) {
                output.write(buffer, 0, read)
                read = stream.read(buffer)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            closeStream(output)
            closeStream(stream)
        }
    }

    @Synchronized
    @Throws(StoragePermissionDenialException::class)
    fun moveFile(context: Context, srcPath: String, destPath: String, callback: FileMoveCallback) {
        if (srcPath == destPath)
            return

        if (callback is RealTimeFileMoveCallback)
            sMovingFile = true

        // internal
        if (srcPath.startsWith("/") && destPath.startsWith("/")) {
            val src = File(srcPath)
            var dest = File(destPath)
            if (src.renameTo(dest)) {
                callback.onMoveCompleted(if (src.parent == dest.parent)
                    "File renamed"
                else
                    "File moved successfully", true)
                return
            }
            var input: InputStream? = null
            var output: OutputStream? = null
            try {
                if (callback.onMoveStarted())
                    return

                if (dest.isDirectory) {
                    create(dest)
                    dest = File(dest.absolutePath)
                }

                output = FileOutputStream(dest)
                input = FileInputStream(src)
                startMoveFile(src.length(), input, output, callback)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
                callback.onMoveCompleted("File not found", false)
            } finally {
                closeStream(input)
                closeStream(output)
            }
            return
        }
        val splitter = if (destPath.contains("/")) '/' else ':'
        val targetName = destPath.substring(destPath.lastIndexOf(splitter) + 1, destPath.length)

        // sd card
        if (!srcPath.startsWith("/") && !destPath.startsWith("/")) {
            val src = asDocumentFile(context, srcPath)
            if (src == null || !src.isFile) {
                callback.onMoveCompleted("File not found", false)
                return
            }
            if (resolveParentFile(srcPath) == resolveParentFile(destPath)) {
                val success = src.renameTo(targetName)
                callback.onMoveCompleted(if (success) "File renamed" else "Cannot rename file", success)
                return
            }

            if (samePartition(srcPath, destPath)) {
                val dest = mkdirs(context, resolveParentFile(destPath))
                if (dest == null) {
                    callback.onMoveCompleted("Cannot move the file to destination", false)
                    return
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try {
                        val movable = targetName == src.name || src.renameTo(targetName)
                        val success = movable && DocumentsContract.moveDocument(context.contentResolver,
                                src.uri, src.parentFile!!.uri, dest.uri) != null
                        callback.onMoveCompleted(if (success) "File moved successfully" else "Failed to move the file", success)
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                        callback.onMoveCompleted("File not found", false)
                    }

                    return
                }
                // Under Nougat, we move the file by copying it, and delete the previous file.
            }
        }

        // different partition
        val src = asDocumentFile(context, srcPath)
        if (src == null || !src.isFile) {
            callback.onMoveCompleted("File not found", false)
            return
        }

        var output: OutputStream? = null
        var input: InputStream? = null
        try {
            var dest = mkdirs(context, resolveParentFile(destPath))
            if (dest == null) {
                callback.onMoveCompleted("Cannot move the file to destination", false)
                return
            }

            if (callback.onMoveStarted())
                return

            dest = dest.createFile(src.type ?: UNKNOWN_MIME_TYPE, targetName)
            @Suppress("UnusedEquals")
            if (dest == null) {
                callback.onMoveCompleted("Cannot move the file to destination", false)
                return
            }

            output = context.contentResolver.openOutputStream(dest.uri)
            input = context.contentResolver.openInputStream(src.uri)
            if (output == null || input == null) {
                callback.onMoveCompleted("Cannot move the file", false)
                return
            }
            startMoveFile(src.length(), input, output, callback)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            callback.onMoveCompleted("File not found", false)
        } finally {
            closeStream(output)
            closeStream(input)
        }
    }

    private fun startMoveFile(srcSize: Long, input: InputStream, output: OutputStream, callback: FileMoveCallback) {
        val timer = Timer()
        try {
            val buffer = ByteArray(1024)
            var byteMoved: Long = 0
            if (callback is RealTimeFileMoveCallback && srcSize > 10 * FileSize.MB) {
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        callback.onMoving((byteMoved * 100 / srcSize).toInt(), byteMoved)
                    }
                }, 1, 700)
            }
            var read = input.read(buffer)
            while (read != -1) {
                output.write(buffer, 0, read)
                byteMoved += read
                read = input.read(buffer)
            }
            timer.cancel()
            callback.onMoveCompleted("File moved successfully", true)
        } catch (e: IOException) {
            timer.cancel()
            callback.onMoveCompleted("Failed to move the file", false)
        } finally {
            closeStream(input)
            closeStream(output)
        }
    }

    fun generateFileLocation(folder: String, fileName: String?): String {
        if (!folder.startsWith("/")) {
            val sdcardId = folder.substring(0, folder.indexOf(':') + 1)
            if (sdcardId == folder)
                return folder + fileName!!
        }
        return "$folder/$fileName"
    }

    fun shareFile(context: Context, file: DocumentFile?, mimeType: String?, authority: String) {
        if (file != null && file.isFile) {
            val intent = Intent(Intent.ACTION_SEND)
                    .setType(mimeType ?: "*/*")
                    .putExtra(Intent.EXTRA_SUBJECT, file.name)
                    .putExtra(Intent.EXTRA_STREAM, if (file.uri.scheme == "file")
                        FileProvider.getUriForFile(context, authority, File(file.uri.path!!))
                    else
                        file.uri)
            if (intent.resolveActivity(context.packageManager) != null)
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_file)))
            else
                Toast.makeText(context, R.string.no_app_share_file, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    fun shareFile(context: Context, files: MutableList<DocumentFile>, authority: String) {
        files.indices.reversed().forEach {
            if (!files[it].isFile)
                files.removeAt(it)
        }
        if (files.isNotEmpty()) {
            val uris = ArrayList<Uri>(files.size)
            files.forEach {
                uris.add(if (it.uri.scheme == "file")
                    FileProvider.getUriForFile(context, authority, File(it.uri.path!!))
                else
                    it.uri)
            }

            val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
                    .setType("*/*")
                    .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            if (intent.resolveActivity(context.packageManager) != null)
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_file)))
            else
                Toast.makeText(context, R.string.no_app_share_file, Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show()
        }
    }

    fun createOpenIntent(context: Context, file: DocumentFile, authority: String): Intent {
        return Intent(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(if (file.uri.scheme == "file")
                    FileProvider.getUriForFile(context, authority, File(file.uri.path!!))
                else
                    file.uri)
    }

    private fun openDocumentFile(context: Context, ioScope: CoroutineScope, uiScope: CoroutineScope,
                                 file: String, callback: FileDocumentFindCallback) {
        ioScope.launch {
            try {
                asDocumentFile(context, file)?.let {
                    uiScope.launch { callback.onDocumentFileFound(it) }
                }
            } catch (e: StoragePermissionDenialException) {
                uiScope.launch { Toast.makeText(context, e.message, Toast.LENGTH_SHORT).show() }
            }
        }
    }

    fun openFile(context: Context, ioScope: CoroutineScope, uiScope: CoroutineScope,
                 file: String, mimeType: String, authority: String) {
        openDocumentFile(context, ioScope, uiScope, file, object : FileDocumentFindCallback {
            override fun onDocumentFileFound(file: DocumentFile) {
                openFile(context, file, mimeType, authority)
            }
        })
    }

    @UiThread
    fun openFile(context: Context, file: DocumentFile?, mimeType: String?, authority: String) {
        if (file == null || !file.isFile) {
            Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = createOpenIntent(context, file, authority)
        when {
            intent.resolveActivity(context.packageManager) != null -> context.startActivity(intent)
            intent.setType(mimeType ?: file.type).resolveActivity(context.packageManager) != null -> context.startActivity(intent)
            else -> MaterialDialog(context)
                    .title(R.string.open_as)
                    .listItems(R.array.open_file_options, waitForPositiveButton = false) { dialog, index, _ ->
                        dialog.dismiss()
                        val mimes = arrayOf("image/*", "audio/*", "video/*", "application/zip", "text/*")
                        if (intent.setType(mimes[index]).resolveActivity(context.packageManager) != null)
                            context.startActivity(intent)
                        else
                            Toast.makeText(context, R.string.no_app_to_open_file, Toast.LENGTH_SHORT).show()
                    }.show()
        }
    }

    /** Memeriksa filesystem suatu storage. */
    @WorkerThread
    fun getFileSystem(path: File): String? {
        try {
            val mount = Runtime.getRuntime().exec("mount")
            val reader = BufferedReader(InputStreamReader(mount.inputStream))
            mount.waitFor()

            var line = reader.readLine()
            while (line != null) {
                val split = line.split("\\s+".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (i in 0 until split.size - 1) {
                    if (split[i] != "/" && path.absolutePath.startsWith(split[i]))
                        return split[i + 1]
                }
                line = reader.readLine()
            }
            reader.close()
            mount.destroy()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        return null
    }

    /** Get available space in bytes. */
    @Suppress("DEPRECATION")
    @SuppressLint("NewApi")
    fun availableSpace(context: Context, path: String): Long {
        val createFolderAndGetSpace: () -> Long = {
            try {
                val folder = mkdirs(context, path)
                if (folder != null)
                    availableSpace(context, path)
                else
                    0L
            } catch (e: StoragePermissionDenialException) {
                0L
            }
        }
        try {
            return if (path.startsWith("/")) {
                val stat = StatFs(path)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                    stat.availableBytes
                else
                    (stat.blockSize * stat.availableBlocks).toLong()
            } else {
                val pfd = context.contentResolver
                        .openFileDescriptor(getExternalFolder(context, path)!!.uri, "r")!!
                val stats = Os.fstatvfs(pfd.fileDescriptor)
                stats.f_bavail * stats.f_bsize
            }
        } catch (e: StoragePermissionDenialException) {
        } catch (e: FileNotFoundException) {
            return createFolderAndGetSpace()
        } catch (e: SecurityException) {
        } catch (e: ErrnoException) {
            if (e.message?.contains("No such file or directory") == true)
                return createFolderAndGetSpace()
        }
        return 0
    }

    fun getFileName(url: String): String {
        var u = url
        try {
            u = URLDecoder.decode(url, "UTF-8")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }
        return u.substring(u.lastIndexOf('/') + 1)
    }

    fun getStringBetween(str: String, start: Char, end: Char): String {
        if (start != end) {
            for (i in str.indexOf(end) - 1 downTo 0) {
                if (str[i] == start) {
                    return str.substring(i + 1, str.indexOf(end))
                }
            }
        } else {
            for (i in str.indexOf(end) + 1 until str.length) {
                if (str[i] == start) {
                    return str.substring(str.indexOf(start) + 1, i)
                }
            }
        }
        return ""
    }

    interface FileMoveCallback {
        /**
         * @return `true` if you want to move the file from different thread
         */
        fun onMoveStarted(): Boolean

        fun onMoveCompleted(message: String, success: Boolean)
    }

    interface RealTimeFileMoveCallback : FileMoveCallback {
        fun onMoving(progress: Int, byteMoved: Long)
    }

    interface FileDocumentFindCallback {
        fun onDocumentFileFound(file: DocumentFile)
    }
}
