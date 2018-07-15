package com.anggrayudi.materialpreference.util;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v4.content.FileProvider;
import android.support.v4.provider.DocumentFile;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStatVfs;
import android.view.View;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.anggrayudi.materialpreference.R;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public final class FileUtils {
    private static final String TAG = "FileUtils";

    public static final int REQUEST_CODE_STORAGE_GET_FOLDER = 11;
    public static final int REQUEST_CODE_STORAGE_GET_SINGLE_FILE = 12;
    public static final int REQUEST_CODE_STORAGE_GET_SINGLE_MULTI_FILE = 13;
    public static final int REQUEST_CODE_REQUIRE_SDCARD_ROOT_PATH_PERMISSIONS = 14;
    public static final int REQUEST_CODE_REQUIRE_SDCARD_FILE_ROOT_PATH_PERMISSIONS = 15;

    private static final String AUTHORITY_OPEN_FILE = "com.anggrayudi.wdm.fileopenprovider";
    public static boolean sMovingFile;

    public static String resolvePathFromUri(Intent data) {
        String decodedPath = data.getData().getPath();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            switch (data.getData().getAuthority()) {
                case "com.android.externalstorage.documents":
                    String withoutUri = TextUtil.getStringBetween(decodedPath, '/', ':')
                            + decodedPath.substring(decodedPath.indexOf(':'), decodedPath.length());
                    String sdcardId = withoutUri.substring(0, withoutUri.indexOf(':') + 1);
                    String subFolder = withoutUri.substring(withoutUri.indexOf(':') + 1, withoutUri.length());
                    if (sdcardId.equals("primary:")) {
                        sdcardId = SaveDir.EXTERNAL + "/";
                    }
                    return sdcardId + subFolder;

                case "com.android.providers.downloads.documents":
                case "com.android.providers.media.documents":
                case "com.google.android.apps.docs.storage":
            }
        } else if ("file".equals(data.getData().getScheme())) {
            return data.getData().getPath();
        }
        return SaveDir.EXTERNAL;
    }

    public static String resolveSdCardPath(Intent data) {
        String decodedPath = decodeUri(data.getData().getPath());
        String withoutUri = TextUtil.getStringBetween(decodedPath, '/', ':')
                + decodedPath.substring(decodedPath.indexOf(':'), decodedPath.length());
        String sdcardId = withoutUri.substring(0, withoutUri.indexOf(':') + 1);
        String subFolder = withoutUri.substring(withoutUri.indexOf(':') + 1, withoutUri.length());
        if (sdcardId.equals("primary:")) {
            sdcardId = SaveDir.EXTERNAL + "/";
        }
        return sdcardId + subFolder;
    }

    public static boolean saveUriPermission(Context context, Intent data) {
        String path = data.getData().getPath();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && path.startsWith("/tree/")
                && !path.startsWith("/tree/primary")) {
            Uri root = Uri.parse("content://com.android.externalstorage.documents/tree/" +
                    TextUtil.getStringBetween(data.getData().getPath(), '/', ':') + "%3A");
            if (data.getData().equals(root)) {
                try {
                    int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                    context.getContentResolver().takePersistableUriPermission(root, takeFlags);
                    return true;
                } catch (SecurityException e) {
                    Toast.makeText(context, R.string.please_grant_storage_permission, Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(context, R.string.not_root_path, Toast.LENGTH_SHORT).show();
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isSdCardUriPermissionsGranted(Context context, Intent data) {
        Uri root = Uri.parse("content://com.android.externalstorage.documents/tree/" +
                TextUtil.getStringBetween(data.getData().getPath(), '/', ':') + "%3A");
        for (UriPermission permission : context.getContentResolver().getPersistedUriPermissions()) {
            if (permission.isReadPermission() && permission.isWritePermission() &&
                    permission.getUri().equals(root))
                return true;
        }
        if (root.equals(data.getData())) {
            try {
                int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                context.getContentResolver().takePersistableUriPermission(root, takeFlags);
                return true;
            } catch (SecurityException ignore) {
            }
        }
        return false;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static boolean isSdCardUriPermissionsGranted(Context context, String sdcardId) {
        Uri root = Uri.parse("content://com.android.externalstorage.documents/tree/" + sdcardId + "%3A");
        for (UriPermission permission : context.getContentResolver().getPersistedUriPermissions()) {
            if (permission.isReadPermission() && permission.isWritePermission() &&
                    permission.getUri().equals(root))
                return true;
        }
        return false;
    }

    /**
     * Get {@link DocumentFile} object from SD card.
     *
     * @param directory SD card ID followed by directory name, for example {@code 6881-2249:Download/Archive},
     *                  where ID for SD card is {@code 6881-2249}
     * @param fileName  for example {@code intel_haxm.zip}
     * @return <code>null</code> if does not exist
     */
    @Nullable
    private static DocumentFile getExternalFile(Context context, String directory, String fileName) {
        return getExternalFile(context, generateFileLocation(directory, fileName));
    }

    @Nullable
    private static DocumentFile getExternalFile(Context context, String file) {
        DocumentFile current = getExternalRoot(context, file);
        String cleanedPath = cleanSdCardPath(file);
        if (!cleanedPath.isEmpty()) {
            String[] dirs = cleanedPath.contains("/") ? cleanedPath.split("/") : new String[]{cleanedPath};
            for (int i = 0; i < dirs.length && current != null; i++) {
                current = current.findFile(dirs[i]);
            }
        }
        return current;
    }

    private static DocumentFile getExternalFolder(Context context, String file) {
        Uri uri = Uri.parse("content://com.android.externalstorage.documents/tree/" + Uri.encode(file));
        return DocumentFile.fromTreeUri(context, uri);
    }

    private static DocumentFile getExternalRoot(Context context, String folder) {
        return getExternalFolder(context, folder.substring(0, folder.indexOf(':') + 1));
    }

    public static OutputStream openOutputStream(Context context, DocumentFile file) throws FileNotFoundException {
        if (file.getUri().getScheme().equals("file")) {
            // handle file from internal storage
            return new FileOutputStream(new File(file.getUri().getPath()), true);
        } else {
            return context.getContentResolver().openOutputStream(file.getUri(), "wa");
        }
    }

    public static InputStream openInputStream(Context context, DocumentFile file) throws FileNotFoundException {
        if (file.getUri().getScheme().equals("file")) {
            // handle file from internal storage
            return new FileInputStream(new File(file.getUri().getPath()));
        } else {
            return context.getContentResolver().openInputStream(file.getUri());
        }
    }

    public static InputStream openInputStream(Context context, String file) throws FileNotFoundException {
        if (file.startsWith("/")) {
            return new FileInputStream(new File(file));
        } else {
            DocumentFile documentFile = getExternalFile(context, file);
            if (documentFile != null && documentFile.isFile())
                return context.getContentResolver().openInputStream(documentFile.getUri());
            else
                throw new FileNotFoundException("File not found: " + file);
        }
    }

    private static String decodeUri(String uri) {
        try {
            return URLDecoder.decode(uri, "ISO-8859-1");
        } catch (UnsupportedEncodingException ignore) {
            return uri;
        }
    }

    private static String cleanSdCardPath(String file) {
        String tree = file.substring(file.indexOf(':') + 1, file.length());
        StringBuilder resolvedPath = new StringBuilder();
        String[] directories = tree.split("/");
        for (String directory : directories) {
            directory = directory.trim();
            if (!directory.isEmpty()) {
                resolvedPath.append(directory).append("/");
            }
        }
        if (resolvedPath.toString().endsWith("/"))
            resolvedPath = new StringBuilder(resolvedPath.substring(0, resolvedPath.length() - 1));

        return resolvedPath.toString();
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static DocumentFile mkdirs(Context context, String folder) {
        if (folder.startsWith("/")) {
            File file = new File(folder);
            file.mkdirs();
            if (file.isDirectory())
                return DocumentFile.fromFile(file);
        } else {
            DocumentFile currentDirectory = getExternalRoot(context, folder);
            String cleanedPath = cleanSdCardPath(folder);
            if (!cleanedPath.isEmpty()) {
                String[] s = cleanedPath.contains("/") ? cleanedPath.split("/") : new String[]{cleanedPath};
                for (String dir : s) {
                    if (currentDirectory != null) {
                        DocumentFile documentFile = currentDirectory.findFile(dir);
                        if (documentFile == null || documentFile.isFile())
                            currentDirectory = currentDirectory.createDirectory(dir);
                        else if (documentFile.isDirectory())
                            currentDirectory = documentFile;
                    }
                }
            }
            if (currentDirectory != null && currentDirectory.getUri().getPath().endsWith(cleanedPath))
                return currentDirectory;
        }
        return null;
    }

    @SuppressWarnings({"ResultOfMethodCallIgnored", "ConstantConditions"})
    public static DocumentFile create(Context context, String folder, String fileName) {
        if (folder.startsWith("/")) {
            File file = new File(folder, fileName);
            file.getParentFile().mkdirs();
            return create(file) ? DocumentFile.fromFile(file) : null;
        } else {
            DocumentFile directory = mkdirs(context, folder);
            return directory != null ? directory.createFile(null, fileName) : null;
        }
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static DocumentFile recreate(Context context, String folder, String fileName) {
        if (folder.startsWith("/")) {
            File file = new File(folder, fileName);
            file.delete();
            file.getParentFile().mkdirs();
            return create(file) ? DocumentFile.fromFile(file) : null;
        } else {
            DocumentFile directory = mkdirs(context, folder);
            if (directory != null) {
                DocumentFile file = directory.findFile(fileName);
                if (file != null && file.isFile())
                    file.delete();

                return directory.createFile(null, fileName);
            }
            return null;
        }
    }

    private static boolean create(File file) {
        try {
            return file.createNewFile();
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean delete(Context context, String folder, String fileName) {
        if (folder.startsWith("/"))
            return new File(folder).delete();
        else {
            DocumentFile documentFile = getExternalFile(context, folder, fileName);
            return documentFile != null && documentFile.delete();
        }
    }

    public static boolean delete(Context context, String folder) {
        if (folder.startsWith("/"))
            return new File(folder).delete();
        else {
            DocumentFile documentFile = getExternalFile(context, folder);
            return documentFile != null && documentFile.delete();
        }
    }

    public static String resolveParentFile(String file) {
        String parent = new File(file).getParent();
        return parent == null ? file.substring(0, file.indexOf(':') + 1) : parent;
    }

    public static boolean fileExists(Context context, String file) {
        File f = new File(file);
        if (file.startsWith("/"))
            return f.isFile();
        else {
            String name = file.substring(file.lastIndexOf(file.contains("/") ? '/' : ':') + 1, file.length());
            DocumentFile documentFile = getExternalFile(context, resolveParentFile(file), name);
            return documentFile != null && documentFile.isFile();
        }
    }

    public static boolean fileExists(Context context, String folder, String file) {
        if (folder.startsWith("/"))
            return new File(folder, file).isFile();
        else {
            DocumentFile documentFile = getExternalFile(context, folder, file);
            return documentFile != null && documentFile.isFile();
        }
    }

    public static long fileSize(Context context, String folder, String fileName) {
        if (folder.startsWith("/"))
            return new File(folder, fileName).length();
        else {
            DocumentFile documentFile = getExternalFile(context, folder, fileName);
            return documentFile != null ? documentFile.length() : 0;
        }
    }

    public static long fileSize(Context context, String file) {
        if (file.startsWith("/")) {
            return new File(file).length();
        } else {
            DocumentFile documentFile = getExternalFile(context, file);
            return documentFile != null ? documentFile.length() : 0;
        }
    }

    public static DocumentFile asDocumentFile(Context context, String folder, String filename) {
        return folder.startsWith("/")
                ? DocumentFile.fromFile(new File(folder, filename))
                : getExternalFile(context, folder, filename);
    }

    public static DocumentFile asDocumentFile(Context context, String file) {
        return file.startsWith("/")
                ? DocumentFile.fromFile(new File(file))
                : getExternalFile(context, file);
    }

    public static DocumentFile asDocumentFolder(Context context, String folder) {
        return folder.startsWith("/")
                ? DocumentFile.fromFile(new File(folder))
                : getExternalFolder(context, folder);
    }

    public static String getBaseName(String filename) {
        return org.apache.commons.io.FilenameUtils.getBaseName(filename.contains("/") ? filename : filename.replaceFirst(":", "/"));
    }

    public static String getExtension(String filename) {
        return org.apache.commons.io.FilenameUtils.getExtension(filename.contains("/") ? filename : filename.replaceFirst(":", "/"));
    }

    public static boolean samePartition(String path1, String path2) {
        if (path1.startsWith("/") && path2.startsWith("/"))
            return true;

        String sdcard1 = path1.substring(0, path1.indexOf(':'));
        String sdcard2 = path2.substring(0, path2.indexOf(':'));
        return sdcard1.equals(sdcard2);
    }

    public static void writeFileFromInputStream(InputStream stream, File file) {
        int read;
        byte[] buffer = new byte[1024];
        OutputStream output = null;
        try {
            if (file.isDirectory()) {
                file.createNewFile();
                file = new File(file.getAbsolutePath());
            }
            output = new FileOutputStream(file);
            while ((read = stream.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeStream(output);
            closeStream(stream);
        }
    }

    public static synchronized void moveFile(Context context, String srcPath, String destPath, FileMoveCallback callback) {
        if (srcPath.equals(destPath))
            return;

        if (callback instanceof RealTimeFileMoveCallback)
            sMovingFile = true;

        // internal
        if (srcPath.startsWith("/") && destPath.startsWith("/")) {
            File src = new File(srcPath);
            File dest = new File(destPath);
            if (src.renameTo(dest)) {
                callback.onMoveCompleted(src.getParent().equals(dest.getParent()) ?
                        "File renamed" : "File moved successfully", true);
                return;
            }
            InputStream input = null;
            OutputStream output = null;
            try {
                if (callback.onMoveStarted())
                    return;

                if (dest.isDirectory()) {
                    create(dest);
                    dest = new File(dest.getAbsolutePath());
                }

                output = new FileOutputStream(dest);
                input = new FileInputStream(src);
                startMoveFile(src.length(), input, output, callback);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                callback.onMoveCompleted("File not found", false);
            } finally {
                closeStream(input);
                closeStream(output);
            }
            return;
        }
        char splitter = destPath.contains("/") ? '/' : ':';
        String targetName = destPath.substring(destPath.lastIndexOf(splitter) + 1, destPath.length());

        // sd card
        if (!srcPath.startsWith("/") && !destPath.startsWith("/")) {
            DocumentFile src = asDocumentFile(context, srcPath);
            if (src == null || !src.isFile()) {
                callback.onMoveCompleted("File not found", false);
                return;
            }
            if (resolveParentFile(srcPath).equals(resolveParentFile(destPath))) {
                boolean success = src.renameTo(targetName);
                callback.onMoveCompleted(success ? "File renamed" : "Cannot rename file", success);
                return;
            }

            if (samePartition(srcPath, destPath)) {
                DocumentFile dest = mkdirs(context, resolveParentFile(destPath));
                if (dest == null) {
                    callback.onMoveCompleted("Cannot move the file to destination", false);
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    try {
                        boolean movable = targetName.equals(src.getName()) || src.renameTo(targetName);
                        boolean success = movable && DocumentsContract.moveDocument(context.getContentResolver(),
                                src.getUri(), src.getParentFile().getUri(), dest.getUri()) != null;
                        callback.onMoveCompleted(success ? "File moved successfully" : "Failed to move the file", success);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        callback.onMoveCompleted("File not found", false);
                    }
                    return;
                }
                // Under Nougat, we move the file by copying it, and delete the previous file.
            }
        }

        // different partition
        DocumentFile src = asDocumentFile(context, srcPath);
        if (src == null || !src.isFile()) {
            callback.onMoveCompleted("File not found", false);
            return;
        }

        OutputStream output = null;
        InputStream input = null;
        try {
            DocumentFile dest = mkdirs(context, resolveParentFile(destPath));
            if (dest == null) {
                callback.onMoveCompleted("Cannot move the file to destination", false);
                return;
            }

            if (callback.onMoveStarted())
                return;

            dest = dest.createFile(null, targetName);
            if (dest == null) {
                callback.onMoveCompleted("Cannot move the file to destination", false);
                return;
            }

            output = context.getContentResolver().openOutputStream(dest.getUri());
            input = context.getContentResolver().openInputStream(src.getUri());
            if (output == null || input == null) {
                callback.onMoveCompleted("Cannot move the file", false);
                return;
            }
            startMoveFile(src.length(), input, output, callback);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            callback.onMoveCompleted("File not found", false);
        } finally {
            closeStream(output);
            closeStream(input);
        }
    }

    private static void startMoveFile(final long srcSize, InputStream input, OutputStream output, final FileMoveCallback callback) {
        Timer timer = new Timer();
        try {
            int read;
            byte[] buffer = new byte[1024];
            final long[] byteMoved = {0};
            if (callback instanceof RealTimeFileMoveCallback && srcSize > 10 * FileSize.MB) {
                final RealTimeFileMoveCallback moveCallback = (RealTimeFileMoveCallback) callback;
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        moveCallback.onMoving((int) (byteMoved[0] * 100 / srcSize), byteMoved[0]);
                    }
                }, 1, 700);
            }
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
                byteMoved[0] += read;
            }
            timer.cancel();
            callback.onMoveCompleted("File moved successfully", true);
        } catch (IOException e) {
            timer.cancel();
            callback.onMoveCompleted("Failed to move the file", false);
        } finally {
            closeStream(input);
            closeStream(output);
        }
    }

    public static String generateFileLocation(String folder, String fileName) {
        if (!folder.startsWith("/")) {
            String sdcardId = folder.substring(0, folder.indexOf(':') + 1);
            if (sdcardId.equals(folder))
                return folder + fileName;
        }
        return folder + "/" + fileName;
    }

    public static void shareFile(Context context, DocumentFile file, String mimeType) {
        if (file.isFile()) {
            Intent intent = new Intent(Intent.ACTION_SEND)
                    .setType(mimeType == null ? "*/*" : mimeType)
                    .putExtra(Intent.EXTRA_SUBJECT, file.getName())
                    .putExtra(Intent.EXTRA_STREAM, file.getUri());
            if (intent.resolveActivity(context.getPackageManager()) != null)
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_file)));
            else
                Toast.makeText(context, R.string.no_app_share_file, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    public static void shareFile(Context context, List<DocumentFile> files) {
        for (int i = files.size() - 1; i >= 0; i--) {
            if (!files.get(i).isFile())
                files.remove(i);
        }
        if (!files.isEmpty()) {
            ArrayList<Uri> uris = new ArrayList<>(files.size());
            for (DocumentFile file : files)
                uris.add(file.getUri());

            Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE)
                    .setType("*/*")
                    .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            if (intent.resolveActivity(context.getPackageManager()) != null)
                context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_file)));
            else
                Toast.makeText(context, R.string.no_app_share_file, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show();
        }
    }

    public static Intent createOpenIntent(Context context, DocumentFile file) {
        return new Intent(Intent.ACTION_VIEW)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setData(file.getUri().getScheme().equals("file")
                        ? FileProvider.getUriForFile(context, AUTHORITY_OPEN_FILE, new File(file.getUri().getPath()))
                        : file.getUri());
    }

    public static void openDocumentFile(final Context context, final String file, final FileDocumentFindCallback callback) {
        final Handler handler = new Handler();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final DocumentFile documentFile = asDocumentFile(context, file);
                if (documentFile != null) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onDocumentFileFound(documentFile);
                        }
                    });
                }
            }
        }).start();
    }

    public static void openFile(final Context context, String file, final String mimeType) {
        openDocumentFile(context, file, new FileDocumentFindCallback() {
            @Override
            public void onDocumentFileFound(DocumentFile file) {
                openFile(context, file, mimeType);
            }
        });
    }

    @UiThread
    public static void openFile(final Context context, DocumentFile file, String mimeType) {
        if (file == null || !file.isFile()) {
            Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        final Intent intent = createOpenIntent(context, file);
        if (intent.resolveActivity(context.getPackageManager()) != null)
            context.startActivity(intent);
        else if (intent.setType(mimeType == null ? file.getType() : mimeType).resolveActivity(context.getPackageManager()) != null)
            context.startActivity(intent);
        else {
            new MaterialDialog.Builder(context)
                    .title(R.string.open_as)
                    .items(R.array.open_file_options)
                    .itemsCallback(new MaterialDialog.ListCallback() {
                        @Override
                        public void onSelection(MaterialDialog dialog, View itemView, int position, CharSequence text) {
                            String[] mimes = {"image/*", "audio/*", "video/*", "application/zip", "text/*"};
                            if (intent.setType(mimes[position]).resolveActivity(context.getPackageManager()) != null)
                                context.startActivity(intent);
                            else
                                Toast.makeText(context, R.string.no_app_to_open_file, Toast.LENGTH_SHORT).show();
                        }
                    }).show();
        }
    }

    /**
     * Memeriksa filesystem suatu storage.
     */
    public static String getFileSystem(File path) {
        try {
            Process mount = Runtime.getRuntime().exec("mount");
            BufferedReader reader = new BufferedReader(new InputStreamReader(mount.getInputStream()));
            mount.waitFor();

            String line;
            while ((line = reader.readLine()) != null) {
                String[] split = line.split("\\s+");
                for (int i = 0; i < split.length - 1; i++) {
                    if (!split[i].equals("/") && path.getAbsolutePath().startsWith(split[i]))
                        return split[i + 1];
                }
            }
            reader.close();
            mount.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Get available space in bytes.
     */
    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    public static long spaceAvailable(Context context, String path) {
        if (path.startsWith("/")) {
            StatFs stat = new StatFs(path);
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2
                    ? stat.getAvailableBytes()
                    : stat.getBlockSize() * stat.getAvailableBlocks();
        } else {
            try {
                ParcelFileDescriptor pfd = context.getContentResolver()
                        .openFileDescriptor(getExternalFolder(context, path).getUri(), "r");
                assert pfd != null;
                StructStatVfs stats = Os.fstatvfs(pfd.getFileDescriptor());
                return stats.f_bavail * stats.f_bsize;
            } catch (FileNotFoundException | ErrnoException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    public static boolean isSpaceAvailable(Context context, String path, long fileSizeBytes) {
        return spaceAvailable(context, path) > fileSizeBytes + 80 * FileSize.MB;
    }

    public static String getFileName(String url) {
        try {
            url = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return url.substring(url.lastIndexOf('/') + 1);
    }

    public static void closeStream(Closeable closeable) {
        if (closeable != null)
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public interface FileMoveCallback {
        /**
         * @return <code>true</code> if you want to move the file from different thread
         */
        boolean onMoveStarted();

        void onMoveCompleted(String message, boolean success);
    }

    public interface RealTimeFileMoveCallback extends FileMoveCallback {
        void onMoving(int progress, long byteMoved);
    }

    public interface FileDocumentFindCallback {
        void onDocumentFileFound(DocumentFile file);
    }
}
