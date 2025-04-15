package net.kdt.pojavlaunch.customcontrols;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class LayoutBitmaps {
    private static final Random mKeyPicker = new Random(System.nanoTime());
    private final Map<String, Bitmap> mBitmaps;

    private LayoutBitmaps() {
        mBitmaps = new HashMap<>();
    }

    private LayoutBitmaps(ZipInputStream zipIn) throws IOException {
        this();
        for(ZipEntry entry = zipIn.getNextEntry(); entry != null; entry = zipIn.getNextEntry()) {
            if(entry.isDirectory()) continue;
            mBitmaps.put(entry.getName(), BitmapFactory.decodeStream(zipIn));
            zipIn.closeEntry();
        }
    }

    private String pickKey() {
        String key;
        do {
            key = Integer.toString(mKeyPicker.nextInt());
        } while (mBitmaps.containsKey(key));
        return key;
    }

    public Bitmap getBitmap(String key) {
        return mBitmaps.get(key);
    }

    public String putBitmap(Bitmap bitmap, String oldKey) {
        String newKey = pickKey();
        mBitmaps.remove(oldKey);
        if(bitmap != null) mBitmaps.put(newKey, bitmap);
        return newKey;
    }

    public void store(OutputStream outputStream) throws IOException {
        if(mBitmaps.isEmpty()) return;
        try(ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
            for(Map.Entry<String, Bitmap> bitmapEntry : mBitmaps.entrySet()) {
                Bitmap outBitmap = bitmapEntry.getValue();
                if(outBitmap == null) continue;
                zipOutputStream.putNextEntry(new ZipEntry(bitmapEntry.getKey()));
                outBitmap.compress(Bitmap.CompressFormat.WEBP, 100, zipOutputStream);
                zipOutputStream.closeEntry();
            }
        }
    }

    private static ControlsContainer createEmpty(String controlsJson) {
        return new ControlsContainer(controlsJson, new LayoutBitmaps());
    }

    private static boolean copyJsonOnly(InputStream input, OutputStream output) throws IOException {
        int bracketCounter = 0;
        boolean trigger = false;
        for(int chr = input.read(); chr != -1; chr = input.read()) {
            if(chr == '{') {
                trigger = true;
                bracketCounter++;
            }
            if(chr == '}') bracketCounter--;
            output.write(chr);
            if(bracketCounter == 0 && trigger) return true;
        }
        return false;
    }

    private static ControlsContainer load(FileInputStream fileInputStream) throws IOException{
        String controlsString;
        try(ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            boolean isValid = copyJsonOnly(fileInputStream, byteArrayOutputStream);
            if(!isValid) return null;
            controlsString = byteArrayOutputStream.toString("UTF-8");
        }
        if(fileInputStream.available() == 0) return createEmpty(controlsString);
        try(ZipInputStream zipInputStream = new ZipInputStream(fileInputStream)) {
            return new ControlsContainer(controlsString, new LayoutBitmaps(zipInputStream));
        }catch (ZipException e) {
            return createEmpty(controlsString);
        }
    }

    public static ControlsContainer load(File jsonLocation) throws IOException {
        try (FileInputStream fileInputStream = new FileInputStream(jsonLocation);) {
            return load(fileInputStream);
        }
    }

    public static final class ControlsContainer {
        public final String mControlsJson;
        public final LayoutBitmaps mLayoutZip;

        public ControlsContainer(String mControlsJson, LayoutBitmaps mLayoutZip) {
            this.mControlsJson = mControlsJson;
            this.mLayoutZip = mLayoutZip;
        }
    }
}
