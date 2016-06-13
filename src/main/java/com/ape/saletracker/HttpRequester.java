package com.ape.saletracker;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Vector;
	 
	/**
	 * HTTP请求对象
	 * 
	 * @author YYmmiinngg
	 */
	public class HttpRequester {
		private String defaultContentEncoding;
		private int timeout = 30000;
		public static String TAG = "HttpRequester";
		public HttpRequester() {
			this.defaultContentEncoding = Charset.defaultCharset().name();
		}
	 
		/**
		 * 发送GET请求
		 * 
		 * @param urlString
		 *            URL地址
		 * @return 响应对象
		 * @throws IOException
		 */
		public HttpRespons sendGet(String urlString) throws IOException {
			return this.send(urlString, "GET", null, null);
		}
	 
		/**
		 * 发送GET请求
		 * 
		 * @param urlString
		 *            URL地址
		 * @param params
		 *            参数集合
		 * @return 响应对象
		 * @throws IOException
		 */
		public HttpRespons sendGet(String urlString, Map<String, String> params)
				throws IOException {
			return this.send(urlString, "GET", params, null);
		}
	 
		/**
		 * 发送GET请求
		 * 
		 * @param urlString
		 *            URL地址
		 * @param params
		 *            参数集合
		 * @param propertys
		 *            请求属性
		 * @return 响应对象
		 * @throws IOException
		 */
		public HttpRespons sendGet(String urlString, Map<String, String> params,
				Map<String, String> propertys) throws IOException {
			return this.send(urlString, "GET", params, propertys);
		}
	 
		/**
		 * 发送POST请求
		 * 
		 * @param urlString
		 *            URL地址
		 * @return 响应对象
		 * @throws IOException
		 */
		public HttpRespons sendPost(String urlString) throws IOException {
			return this.send(urlString, "POST", null, null);
		}
	 
		/**
		 * 发送POST请求
		 * 
		 * @param urlString
		 *            URL地址
		 * @param params
		 *            参数集合
		 * @return 响应对象
		 * @throws IOException
		 */
		public HttpRespons sendPost(String urlString, Map<String, String> params)
				throws IOException {
			return this.send(urlString, "POST", params, null);
		}
	 
		/**
		 * 发送POST请求
		 * 
		 * @param urlString
		 *            URL地址
		 * @param params
		 *            参数集合
		 * @param propertys
		 *            请求属性
		 * @return 响应对象
		 * @throws IOException
		 */
		public HttpRespons sendPost(String urlString, Map<String, String> params,
				Map<String, String> propertys) throws IOException {
			return this.send(urlString, "POST", params, propertys);
		}
	 
		/**
		 * 发送HTTP请求
		 * 
		 * @param urlString
		 * @return 响映对象
		 * @throws IOException
		 */
		private HttpRespons send(String urlString, String method,
				Map<String, String> parameters, Map<String, String> propertys)
				throws IOException {
			HttpURLConnection urlConnection = null;
			if (method.equalsIgnoreCase("GET") && parameters != null) {
				StringBuffer param = new StringBuffer();
				int i = 0;
				for (String key : parameters.keySet()) {
					if (i == 0)
						param.append("?");
					else
						param.append("&");
					param.append(key).append("=").append(parameters.get(key));
					i++;
				}
				urlString += param;
			}
			URL url = new URL(urlString);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setConnectTimeout(timeout);
			urlConnection.setRequestMethod(method);
			/*****setDoOutput（）会导致获取不到数据**********/
			//urlConnection.setDoOutput(true);
			//urlConnection.setDoInput(true);
			/***************/
			urlConnection.setUseCaches(false);
	 
			if (propertys != null)
				for (String key : propertys.keySet()) {
					urlConnection.addRequestProperty(key, propertys.get(key));
				}
	 
			if (method.equalsIgnoreCase("POST") && parameters != null) {
				StringBuffer param = new StringBuffer();
				for (String key : parameters.keySet()) {
					param.append("&");
					param.append(key).append("=").append(parameters.get(key));
				}
				urlConnection.getOutputStream().write(param.toString().getBytes());
				urlConnection.getOutputStream().flush();
				urlConnection.getOutputStream().close();
			}
	 
			return this.makeContent(urlString, urlConnection);
		}

		/**
		 * 得到响应对象
		 * 
		 * @param urlConnection
		 * @return 响应对象
		 * @throws IOException
		 */
		private HttpRespons makeContent(String urlString,
				HttpURLConnection urlConnection) throws IOException {
			HttpRespons httpResponser = new HttpRespons();
			try {
				InputStream in = urlConnection.getInputStream();
				BufferedReader bufferedReader = new BufferedReader(
						new InputStreamReader(in,"utf-8"));
				Log.e(TAG, "bufferedReader = "+ bufferedReader.toString());
				httpResponser.contentCollection = new Vector<String>();
				StringBuffer temp = new StringBuffer();
				String line = bufferedReader.readLine();

				while (line != null) {
					httpResponser.contentCollection.add(line);
					temp.append(line).append("\r\n");
					line = bufferedReader.readLine();
				}
				bufferedReader.close();
	 
				String ecod = urlConnection.getContentEncoding();
				if (ecod == null)
					ecod = this.defaultContentEncoding;
	 
				Log.e(TAG, "httpResponser.contentCollection ="+httpResponser.contentCollection.toString());
				httpResponser.urlString = urlString;
				Log.e(TAG, "httpResponser.urlString ="+httpResponser.urlString);
				httpResponser.defaultPort = urlConnection.getURL().getDefaultPort();
				Log.e(TAG, "httpResponser.defaultPort ="+httpResponser.defaultPort);
				httpResponser.file = urlConnection.getURL().getFile();
				Log.e(TAG, "httpResponser.file = "+httpResponser.file);
				httpResponser.host = urlConnection.getURL().getHost();
				Log.e(TAG, "httpResponser.host = "+httpResponser.host);
				httpResponser.path = urlConnection.getURL().getPath();
				Log.e(TAG, "httpResponser.path = "+httpResponser.path);
				httpResponser.port = urlConnection.getURL().getPort();
				Log.e(TAG, "httpResponser.port = "+httpResponser.port);
				httpResponser.protocol = urlConnection.getURL().getProtocol();
				Log.e(TAG, "httpResponser.protocol = "+httpResponser.protocol);
				httpResponser.query = urlConnection.getURL().getQuery();
				Log.e(TAG, "httpResponser.query = "+httpResponser.query);
				httpResponser.ref = urlConnection.getURL().getRef();
				Log.e(TAG, "httpResponser.ref = "+httpResponser.ref);
				httpResponser.userInfo = urlConnection.getURL().getUserInfo();
				Log.e(TAG, "httpResponser.userInfo = "+httpResponser.userInfo);
				httpResponser.content = new String(temp.toString().getBytes(), ecod);
				Log.e(TAG, "httpResponser.content = "+httpResponser.content);
				httpResponser.contentEncoding = ecod;
				Log.e(TAG, "httpResponser.contentEncoding = "+httpResponser.contentEncoding);
				httpResponser.code = urlConnection.getResponseCode();
				Log.e(TAG, "httpResponser.code = "+httpResponser.code);
				httpResponser.message = urlConnection.getResponseMessage();
				Log.e(TAG, "httpResponser.message = "+httpResponser.message);
				httpResponser.contentType = urlConnection.getContentType();
				Log.e(TAG, "httpResponser.contentType = "+httpResponser.contentType);
				httpResponser.method = urlConnection.getRequestMethod();
				Log.e(TAG, "httpResponser.method = "+httpResponser.method);
				httpResponser.connectTimeout = urlConnection.getConnectTimeout();
				Log.e(TAG, "httpResponser.connectTimeout = "+httpResponser.connectTimeout);
				httpResponser.readTimeout = urlConnection.getReadTimeout();
				Log.e(TAG, "httpResponser.readTimeout = "+httpResponser.readTimeout);
	 
				return httpResponser;
			} catch (IOException e) {
				throw e;
			} finally {
				if (urlConnection != null)
					urlConnection.disconnect();
			}
		}
	 
		/**
		 * 设置超时
		 */
		public void setTimeOut(int timeout) {
			this.timeout = timeout;
		}
		
		/**
		 * 默认的响应字符集
		 */
		public String getDefaultContentEncoding() {
			return this.defaultContentEncoding;
		}
	 
		/**
		 * 设置默认的响应字符集
		 */
		public void setDefaultContentEncoding(String defaultContentEncoding) {
			this.defaultContentEncoding = defaultContentEncoding;
		}
	
}
