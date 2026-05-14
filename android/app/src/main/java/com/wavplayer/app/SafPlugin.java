package com.wavplayer.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Base64;

import androidx.activity.result.ActivityResult;
import androidx.documentfile.provider.DocumentFile;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

@CapacitorPlugin(name = "SafPlugin")
public class SafPlugin extends Plugin {

    private static final String PREFS_NAME = "saf_folders";
    private static final String PREFS_KEY = "folders";

    // ── Pick a directory via SAF ──
    @PluginMethod
    public void pickDirectory(PluginCall call) {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION |
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );
        startActivityForResult(call, intent, "onPickDirectoryResult");
    }

    @ActivityCallback
    private void onPickDirectoryResult(PluginCall call, ActivityResult result) {
        if (call == null) return;
        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
            Uri treeUri = result.getData().getData();
            if (treeUri == null) {
                call.reject("No URI returned");
                return;
            }
            // Persist permission across reboots
            getContext().getContentResolver().takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );

            DocumentFile dir = DocumentFile.fromTreeUri(getContext(), treeUri);
            String name = (dir != null && dir.getName() != null) ? dir.getName() : "Unknown";

            saveFolderToPrefs(treeUri.toString(), name);

            JSObject ret = new JSObject();
            ret.put("uri", treeUri.toString());
            ret.put("name", name);
            call.resolve(ret);
        } else {
            call.reject("cancelled");
        }
    }

    // ── Get saved folders from SharedPreferences ──
    @PluginMethod
    public void getSavedFolders(PluginCall call) {
        JSONArray arr = loadFoldersFromPrefs();
        JSObject ret = new JSObject();
        ret.put("folders", arr);
        call.resolve(ret);
    }

    // ── Remove a saved folder ──
    @PluginMethod
    public void removeSavedFolder(PluginCall call) {
        String uri = call.getString("uri");
        if (uri == null) { call.reject("uri required"); return; }
        try {
            getContext().getContentResolver().releasePersistableUriPermission(
                Uri.parse(uri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            );
        } catch (Exception ignored) {}
        removeFolderFromPrefs(uri);
        call.resolve();
    }

    // ── List .wav files in a directory ──
    @PluginMethod
    public void listWavFiles(PluginCall call) {
        String uriStr = call.getString("uri");
        if (uriStr == null) { call.reject("uri required"); return; }
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(getContext(), Uri.parse(uriStr));
            if (dir == null) { call.reject("Cannot open directory"); return; }

            JSONArray files = new JSONArray();
            for (DocumentFile f : dir.listFiles()) {
                if (f.isFile() && f.getName() != null && f.getName().toLowerCase().endsWith(".wav")) {
                    JSONObject obj = new JSONObject();
                    obj.put("name", f.getName());
                    obj.put("uri", f.getUri().toString());
                    files.put(obj);
                }
            }

            JSObject ret = new JSObject();
            ret.put("files", files);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("listWavFiles failed: " + e.getMessage());
        }
    }

    // ── Copy file to cache for playback, return accessible path ──
    @PluginMethod
    public void getFileForPlayback(PluginCall call) {
        String fileUri = call.getString("uri");
        if (fileUri == null) { call.reject("uri required"); return; }
        try {
            Uri uri = Uri.parse(fileUri);
            InputStream is = getContext().getContentResolver().openInputStream(uri);
            if (is == null) { call.reject("Cannot open file"); return; }

            // Use unique temp file to avoid conflicts during track switching
            File cacheFile = new File(getContext().getCacheDir(), "play_" + System.currentTimeMillis() + ".wav");
            FileOutputStream fos = new FileOutputStream(cacheFile);
            byte[] buf = new byte[16384];
            int len;
            while ((len = is.read(buf)) != -1) fos.write(buf, 0, len);
            fos.close();
            is.close();

            // Clean up old cache files (keep only last 3)
            cleanPlaybackCache(cacheFile.getName());

            JSObject ret = new JSObject();
            ret.put("path", cacheFile.getAbsolutePath());
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("getFileForPlayback failed: " + e.getMessage());
        }
    }

    // ── Read file data as Base64 (for WAV processing in JS) ──
    @PluginMethod
    public void readFileData(PluginCall call) {
        String fileUri = call.getString("uri");
        if (fileUri == null) { call.reject("uri required"); return; }
        try {
            Uri uri = Uri.parse(fileUri);
            InputStream is = getContext().getContentResolver().openInputStream(uri);
            if (is == null) { call.reject("Cannot open file"); return; }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int len;
            while ((len = is.read(buf)) != -1) baos.write(buf, 0, len);
            is.close();

            String base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
            JSObject ret = new JSObject();
            ret.put("data", base64);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("readFileData failed: " + e.getMessage());
        }
    }

    // ── Write a file (Base64 data) into a directory ──
    @PluginMethod
    public void writeFile(PluginCall call) {
        String dirUri = call.getString("dirUri");
        String fileName = call.getString("fileName");
        String base64Data = call.getString("data");
        if (dirUri == null || fileName == null || base64Data == null) {
            call.reject("dirUri, fileName, data required");
            return;
        }
        try {
            DocumentFile dir = DocumentFile.fromTreeUri(getContext(), Uri.parse(dirUri));
            if (dir == null) { call.reject("Cannot open directory"); return; }

            // Delete existing file with same name first
            DocumentFile existing = dir.findFile(fileName);
            if (existing != null) existing.delete();

            DocumentFile newFile = dir.createFile("audio/x-wav", fileName);
            if (newFile == null) { call.reject("Cannot create file"); return; }

            OutputStream os = getContext().getContentResolver().openOutputStream(newFile.getUri());
            if (os == null) { call.reject("Cannot write to file"); return; }

            byte[] data = Base64.decode(base64Data, Base64.NO_WRAP);
            os.write(data);
            os.close();

            JSObject ret = new JSObject();
            ret.put("uri", newFile.getUri().toString());
            ret.put("name", newFile.getName());
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("writeFile failed: " + e.getMessage());
        }
    }

    // ── Delete a file ──
    @PluginMethod
    public void deleteFile(PluginCall call) {
        String fileUri = call.getString("uri");
        if (fileUri == null) { call.reject("uri required"); return; }
        try {
            // Use DocumentFile.fromSingleUri for document URIs from SAF tree
            Uri uri = Uri.parse(fileUri);
            DocumentFile file = DocumentFile.fromSingleUri(getContext(), uri);
            if (file != null && file.exists() && file.delete()) {
                call.resolve();
            } else {
                // Try tree-based approach
                // The URI might be a tree document URI
                call.reject("Failed to delete file");
            }
        } catch (Exception e) {
            call.reject("deleteFile failed: " + e.getMessage());
        }
    }

    // ── SharedPreferences helpers ──
    private void saveFolderToPrefs(String uri, String name) {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(PREFS_KEY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            // Check duplicate
            for (int i = 0; i < arr.length(); i++) {
                if (arr.getJSONObject(i).getString("uri").equals(uri)) return;
            }
            JSONObject obj = new JSONObject();
            obj.put("uri", uri);
            obj.put("name", name);
            arr.put(obj);
            prefs.edit().putString(PREFS_KEY, arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void removeFolderFromPrefs(String uri) {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(PREFS_KEY, "[]");
        try {
            JSONArray arr = new JSONArray(json);
            JSONArray filtered = new JSONArray();
            for (int i = 0; i < arr.length(); i++) {
                if (!arr.getJSONObject(i).getString("uri").equals(uri)) {
                    filtered.put(arr.getJSONObject(i));
                }
            }
            prefs.edit().putString(PREFS_KEY, filtered.toString()).apply();
        } catch (Exception ignored) {}
    }

    private JSONArray loadFoldersFromPrefs() {
        SharedPreferences prefs = getContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(PREFS_KEY, "[]");
        try {
            return new JSONArray(json);
        } catch (Exception e) {
            return new JSONArray();
        }
    }

    private void cleanPlaybackCache(String keepName) {
        try {
            File cacheDir = getContext().getCacheDir();
            File[] playFiles = cacheDir.listFiles((d, name) -> name.startsWith("play_") && name.endsWith(".wav"));
            if (playFiles != null && playFiles.length > 3) {
                // Sort by modified time, delete oldest
                java.util.Arrays.sort(playFiles, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
                for (int i = 0; i < playFiles.length - 3; i++) {
                    if (!playFiles[i].getName().equals(keepName)) {
                        playFiles[i].delete();
                    }
                }
            }
        } catch (Exception ignored) {}
    }
}
