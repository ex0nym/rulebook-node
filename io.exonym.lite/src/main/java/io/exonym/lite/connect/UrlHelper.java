package io.exonym.lite.connect;

import com.google.gson.JsonObject;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;

public class UrlHelper {
	
	private static final Logger logger = LogManager.getLogger(UrlHelper.class);
	public static final URI LAMBDA_LOCATION = URI.create("https://exonym.io/lambda.xml");
	public static final URI LAMBDA_FAILOVER_LOCATION = URI.create("https://spectra.plus/lambda.xml");
	private static byte[] comparison = "<?xml version=".getBytes();

	public static String post(String url, JsonObject o) throws Exception {
		HttpPost post = new HttpPost(url);

		post.setEntity(new StringEntity(o.toString()));

		try(CloseableHttpClient client = HttpClients.createDefault()){
			CloseableHttpResponse response = client.execute(post);
			return EntityUtils.toString(response.getEntity());

		} catch (Exception e){
			throw e;

		}
	}

	public static String post(String url, String json) throws Exception {
		HttpPost post = new HttpPost(url);

		post.setEntity(new StringEntity(json));

		try(CloseableHttpClient client = HttpClients.createDefault()){
			CloseableHttpResponse response = client.execute(post);
			return EntityUtils.toString(response.getEntity());

		} catch (Exception e){
			throw e;

		}
	}

	public static byte[] read(URL url) throws IOException {
		logger.info("Trying to read:" + url);
		URLConnection connection = url.openConnection();
		connection.setConnectTimeout(5000);
		connection.setReadTimeout(500);
		try (BufferedInputStream br = new BufferedInputStream(connection.getInputStream())){
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len;
			while((len = br.read(buffer)) != -1){
				bos.write(buffer, 0, len);

			}
			bos.close();
			return bos.toByteArray();

		} catch (UnknownHostException e) {
			throw e;

		} catch (FileNotFoundException e) {
			throw e; 
			
		} catch (Exception e) {
			throw e; 
			
		}
	}

	public static byte[] read(URL url0, URL url1) throws FileNotFoundException, IOException, UnknownHostException {
		try{
			return read(url0);

		} catch (Exception e){
			try {
				return read(url1);

			} catch (IOException ex) {
				ex.initCause(e);
				throw ex;

			}
		}
	}

	public static byte[] readXml(URL url) throws Exception {
		byte[] read = UrlHelper.read(url);
		if (isXml(read)){
			return read;

		} else {
			throw new SecurityException("Expected XML and XmlHelper.isXml() return false");

		}
	}

	public static boolean isXml(byte[] bytes){

		if (bytes==null || bytes.length < comparison.length){
			return false;

		}
		for (int i = 0; i < comparison.length; i++) {
			if (bytes[i]!=comparison[i]){
				return false;

			}
		}
		return true;

	}
}
