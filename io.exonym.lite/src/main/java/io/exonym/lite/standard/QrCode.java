package io.exonym.lite.standard;

import java.io.ByteArrayOutputStream;

import com.google.zxing.BarcodeFormat;
import org.apache.commons.codec.binary.Base64;

import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class QrCode {
	

	private static final Logger logger = LogManager.getLogger(QrCode.class);
	public QrCode() {
		
	}
	
	public static byte[] computeQrCodeAsPng(String stringToEncode, int pixels) throws Exception{
		QRCodeWriter qrCodeWriter = new QRCodeWriter();
		BitMatrix bitMatrix = qrCodeWriter.encode(stringToEncode, BarcodeFormat.QR_CODE, pixels, pixels);
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		MatrixToImageWriter.writeToStream(bitMatrix, "PNG", os);
		return os.toByteArray();
		
	}

	public static String computeQrCodeAsPngB64(String stringToEncode, int pixels) throws Exception {
		return Base64.encodeBase64String(computeQrCodeAsPng(stringToEncode, pixels));

	}

}
