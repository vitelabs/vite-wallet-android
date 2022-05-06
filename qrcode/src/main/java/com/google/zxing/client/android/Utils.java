package com.google.zxing.client.android;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.*;


public class Utils {


    public static Map<DecodeHintType, Object> GetDefaultHint() {
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        Set<BarcodeFormat> decodeFormats = EnumSet.noneOf(BarcodeFormat.class);
        decodeFormats.addAll(DecodeFormatManager.PRODUCT_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.INDUSTRIAL_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.QR_CODE_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.DATA_MATRIX_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.AZTEC_FORMATS);
        decodeFormats.addAll(DecodeFormatManager.PDF417_FORMATS);

        hints.put(DecodeHintType.POSSIBLE_FORMATS, decodeFormats);
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
        return hints;

    }

    public static Bitmap zoomBitmap(Bitmap bitmap, int width, int height) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        Matrix matrix = new Matrix();
        float scaleWidth = ((float) width / w);
        float scaleHeight = ((float) height / h);
        matrix.postScale(scaleWidth, scaleHeight);
        return Bitmap.createBitmap(bitmap, 0, 0, w, h, matrix, true);
    }

    public static Result decodeFromPhoto(Bitmap photo) {
        Result rawResult = null;
        if (photo != null) {
            Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
            hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
            try {
                Bitmap nphoto = zoomBitmap(photo, photo.getWidth() / 2, photo.getHeight() / 2);
                rawResult = new QRCodeReader()
                        .decode(new BinaryBitmap(new HybridBinarizer(new BitmapLuminanceSource(nphoto))), hints);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                photo.recycle();
            }
        }
        return rawResult;
    }

    public static Bitmap creteQrCodeImage(String content, int width, int height, boolean deleteWhiteOutRect) {
        BitMatrix result;
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        try {
            if (deleteWhiteOutRect) {
                result = new NonPaddingQRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            } else {
                result = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            }
            int[] pixels = new int[result.getWidth() * result.getHeight()];
            for (int y = 0; y < result.getHeight(); y++) {
                int offset = y * result.getWidth();
                for (int x = 0; x < result.getWidth(); x++) {
                    pixels[offset + x] = result.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
                }
            }

            Bitmap bitmap = Bitmap.createBitmap(result.getWidth(), result.getHeight(), Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, result.getWidth(), 0, 0, result.getWidth(), result.getHeight());
            return bitmap;
        } catch (IllegalArgumentException iae) {
            return null;
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return null;

    }

    public static Bitmap creteQrCodeImage(String content, int width, int height) {
        return creteQrCodeImage(content, width, height, false);
    }
}
