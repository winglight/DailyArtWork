package net.yihabits.artwork;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Source;
import net.yihabits.artwork.db.ArtDAO;
import net.yihabits.artwork.db.ArtModel;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.database.Cursor;
import android.os.Environment;

public class DownloadUtil {

	private String basePath; // external storage path
	private int taskAmount = 0;

	private String random_url = "http://www.wga.hu/cgi-bin/search.cgi?random=1";
	private String domain_url = "http://www.wga.hu";
	
	private ArrayList<Runnable> threadList = new ArrayList<Runnable>();

	private DailyArtWorkActivity activity;

	public DownloadUtil(DailyArtWorkActivity activity) {
		this.basePath = initBaseDir();
		this.activity = activity;
	}

	public void downloadImages(final ArrayList<String> urls,
			final boolean refresh) {
		final String dir = getDir();

		taskAmount++;

		Runnable saveUrl = new Runnable() {
			public void run() {
				for (String url : urls) {
					// save one link
					saveUrl(url, dir);
					if (refresh) {
						activity.refreshGallery();
					}
				}
				taskAmount--;
				
				threadList.remove(this);
			}
		};
		threadList.add(saveUrl);
		new Thread(saveUrl).start();
	}
	
	public void stopAllThreads(){
		synchronized (threadList) {
			for(Runnable t : threadList){
				try {
					t.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				t = null;
			}
			threadList = null;
		}
	}

	public void saveArts(final ArrayList<ArtModel> artList) {

		Runnable saveUrl = new Runnable() {
			public void run() {
				ArtDAO dao = ArtDAO.getInstance(activity);
				dao.open();

				for (ArtModel art : artList) {
					// save art
					String imageUrl = art.getImageUrl();
					Cursor c = dao.getArtByImgUrl(imageUrl);
					if (!c.moveToFirst()) {
						// insert a record into db
						dao.insert(art);
					}
					c.close();
				}

				dao.close();
			}
		};
		new Thread(saveUrl).start();
	}

	private void saveUrl(String url, String dir) {
		HttpEntity resEntity = null;

		String path = convertUrl2Path(url);
		path = dir + path;
		File tmp = new File(path);
		if (tmp.exists()) {
			return;
		}

		try {
			HttpClient httpclient = new DefaultHttpClient();

			HttpGet httpget = new HttpGet(url);

			HttpResponse response = httpclient.execute(httpget);

			int status = response.getStatusLine().getStatusCode();

			if (status == HttpStatus.SC_OK) {

				resEntity = response.getEntity();

				// save to sdcard
				save2card(EntityUtils.toByteArray(resEntity), path);

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			if (resEntity != null) {
				try {
					resEntity.consumeContent();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public String convertUrl2Path(String url) {
		int start = url.indexOf(".hu");
		if (start > 0) {
			String tmp = url.substring(start + 3);
			tmp = tmp.replace("?", "_");
			tmp = tmp.replace("/", "_");
			return tmp;
		} else {
			return null;
		}
	}

	public ArrayList<ArtModel> getRandomImages() {
		// 1.remove ? & / from url and get the path
		String dir = getDir();
		initDir(dir);

		HttpEntity entity = null;
		ArrayList<ArtModel> artList = new ArrayList<ArtModel>();
		ArrayList<String> urls = new ArrayList<String>();
		ArrayList<String> pre_urls = new ArrayList<String>();

		try {
			HttpClient httpclient = new DefaultHttpClient();

			// download the random page
			HttpGet httpget = new HttpGet(random_url);
			HttpResponse response = httpclient.execute(httpget);

			int status = response.getStatusLine().getStatusCode();

			if (status == HttpStatus.SC_OK) {
				entity = response.getEntity();
				if (entity != null) {
					String content = EntityUtils.toString(entity);
					Source source = new Source(content);

					// 1.parse arts
					List<Element> list = source
							.getAllElements(HTMLElementName.TR);
					int i = 0;
					for (Element trele : list) {
						Element firstTd = trele
								.getFirstElement(HTMLElementName.TD);

						if (firstTd != null) {
							Element scriptEle = firstTd
									.getFirstElement(HTMLElementName.SCRIPT);
							if (scriptEle != null) {
								// this is the right TR
								i++;
								String scriptStr = scriptEle.toString();
								int start = scriptStr
										.lastIndexOf("<img src=\"");
								if (start > 0) {
									ArtModel model = new ArtModel();

									start += 10;
									scriptStr = scriptStr.substring(start);
									int end = scriptStr.indexOf("\"");
									// image urls
									String preImgUrl = domain_url
											+ scriptStr.substring(0, end);
									String imgUrl = preImgUrl.replace(
											"preview", "art");
									String preImgLocation = dir
											+ convertUrl2Path(preImgUrl);
									String imgLocation = preImgLocation
											.replace("preview", "art");

									model.setPreImageUrl(preImgUrl);
									model.setImageUrl(imgUrl);
									model.setPreImageLocation(preImgLocation);
									model.setImageLocation(imgLocation);

									// put images into download list
									pre_urls.add(preImgUrl);
									urls.add(imgUrl);

									// parse second TD
									Element secondTd = trele.getAllElements(
											HTMLElementName.TD).get(1);
									parseDetails(model, secondTd.toString());

									// put the model into list
									artList.add(model);

									if(i >9){
										break;
									}
								}
							}
						}
					}

				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			if (entity != null) {
				try {
					entity.consumeContent();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// start the thread to download images
		// pre_urls.addAll(urls);
		downloadImages(pre_urls, true);
			downloadImages(urls, false);

		// start save all arts into DB
		saveArts(artList);

		return artList;
	}

	private void parseDetails(ArtModel model, String tdStr) {
		tdStr = tdStr.replace("\n", "");
		String[] list = tdStr.split("<BR>");
		if (list.length >= 5) {
			model.setAuthor(list[0].substring(7, list[0].length() - 4));
			model.setName(list[1]);
			model.setYear(list[2]);
			model.setDetails(list[3]);
			model.setLocation(list[4]);
		}

	}

	public String saveImg(HttpClient httpclient, String url) {
		boolean flag = httpclient == null;
		if (flag) {
			httpclient = new DefaultHttpClient();
		}
		String dir = initBaseDir() + "/temp/";
		String path = dir + parseFileNameByUrl(url);
		File tmp = new File(path);
		if (tmp.exists()) {
			return "file://" + path;
		}
		initDir(dir);
		HttpEntity entity = null;
		try {
			// go to the current page
			HttpGet httpget = new HttpGet(url);
			HttpResponse response = httpclient.execute(httpget);
			entity = response.getEntity();

			// save to sdcard
			save2card(EntityUtils.toByteArray(entity), path);
			return "file://" + path;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {

			if (entity != null) {
				try {
					entity.consumeContent();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private String getPageDir() {
		SimpleDateFormat formatter4datetime = new SimpleDateFormat(
				"yyyy-MM-dd-HH-mm-ss");

		String dir = formatter4datetime.format(new Date());
		return basePath + "/html/" + dir + "/";
	}

	private String getPicDir() {
		SimpleDateFormat formatter4datetime = new SimpleDateFormat(
				"yyyy-MM-dd-HH-mm-ss");

		String dir = formatter4datetime.format(new Date());
		return basePath + "/pic/" + dir + "/";
	}

	private String parseFileNameByUrl(String url) {
		String res = "index.html";
		int start = url.indexOf("p=") + 2;
		String fileName = url.substring(start);
		start = fileName.indexOf("=");
		if (start >= 0) {
			res = fileName.substring(start + 1) + ".html";
		}
		start = fileName.lastIndexOf("/");
		if (start >= 0) {
			res = fileName.substring(start + 1);
		}
		res = res.replace("&", "_");
		res = res.replace("=", "_");
		res = res.replace("%", "_");
		return res;

	}

	private void save2card(byte[] bytes, String path) {
		try {
			// save to sdcard
			FileOutputStream fos = new FileOutputStream(new File(path));
			IOUtils.write(bytes, fos);

			// release all instances
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void save2card(String content, String path, String encode) {
		try {
			// save to sdcard
			FileOutputStream fos = new FileOutputStream(new File(path));
			IOUtils.write(content, fos, encode);

			// release all instances
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private String getDir() {
		return basePath + "/arts/";
	}

	public String initBaseDir() {
		File sdDir = Environment.getExternalStorageDirectory();
		File uadDir = null;
		if (sdDir.exists() && sdDir.canWrite()) {

		} else {
			sdDir = Environment.getDataDirectory();

		}
		uadDir = new File(sdDir.getAbsolutePath() + "/download/");
		if (uadDir.exists() && uadDir.canWrite()) {

			String path = getIndexPath();
			path = path.substring(0, path.indexOf("index.html"));
			String content = "<html><head><title>Favorite Downloaded</title><meta "
					+ "http-equiv='Content-Type' content='text/html; charset=UTF-8'>"
					+ "</head><body><table border='0'><tr><td><a href='"
					+ "file://"
					+ path
					+ "index.html"
					+ "'>All</a></td>"
					+ "<td><a href='"
					+ "file://"
					+ path
					+ "html/index.html"
					+ "'>Html</a></td>"
					+ "<td><a href='"
					+ "file://"
					+ path
					+ "pic/index.html"
					+ "'>Picture</td></tr><tr><td></td><tr></table><br><br></body></html>";

			// top index initiation
			File index = new File(path + "index.html");
			if (index.exists() && uadDir.canWrite()) {

			} else {

				save2card(content, path + "index.html", "UTF-8");
			}
			// html index initiation
			index = new File(path + "html/index.html");
			if (index.exists() && uadDir.canWrite()) {

			} else {
				save2card(content, path + "html/index.html", "UTF-8");
			}
			// picture index initiation
			index = new File(path + "pic/index.html");
			if (index.exists() && uadDir.canWrite()) {

			} else {
				save2card(content, path + "pic/index.html", "UTF-8");
			}
		} else {
			uadDir.mkdir();
			File hDir = new File(sdDir.getAbsolutePath() + "/download/html/");
			hDir.mkdir();
			File pDir = new File(sdDir.getAbsolutePath() + "/download/pic/");
			pDir.mkdir();
		}
		return uadDir.getAbsolutePath();
	}

	private void saveRecord2Index(String title, String url) {

		String type = "";
		if (url.indexOf("/html/") > 0) {
			type = "Html";
		} else {
			type = "Image";
		}

		try {
			String path = getIndexPath();
			String dir = path.substring(0, path.indexOf("index.html"));
			String content = IOUtils.toString(new FileInputStream(
					new File(path)));
			SimpleDateFormat formatter4datetime = new SimpleDateFormat(
					"yyyy-MM-dd-HH-mm-ss");
			int allTitle = content.indexOf(">All</a>");
			if (allTitle < 0) {
				content = content.replace("<tr><td></td><tr>",
						"<tr><td><a href='" + "file://" + dir + "index.html"
								+ "'>All</a></td>" + "<td><a href='"
								+ "file://" + dir + "html/index.html"
								+ "'>Html</a></td>" + "<td><a href='"
								+ "file://" + dir + "pic/index.html"
								+ "'>Picture</td></tr><tr><td></td><tr>");
			}

			// top index file updated
			content = content.replace(
					"<tr><td></td><tr>",
					"<tr><td></td><tr><tr><td><a href=\"" + url + "\">" + title
							+ "</td><td>"
							+ formatter4datetime.format(new Date())
							+ "</td><td>" + type + "</td></tr>");
			save2card(content, path, "UTF-8");

			// second level index updated
			if (type.equals("Html")) {
				path = dir + "html/index.html";
				content = IOUtils.toString(new FileInputStream(new File(path)));
			} else {
				path = dir + "pic/index.html";
				content = IOUtils.toString(new FileInputStream(new File(path)));
			}
			content = content.replace(
					"<tr><td></td><tr>",
					"<tr><td></td><tr><tr><td><a href=\"" + url + "\">" + title
							+ "</td><td>"
							+ formatter4datetime.format(new Date())
							+ "</td><td>" + type + "</td></tr>");
			save2card(content, path, "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void initDir(String dir) {
		File sdDir = new File(dir);
		if (sdDir.exists() && sdDir.canWrite()) {

		} else {
			sdDir.mkdirs();

		}
	}

	public static String getIndexPath() {
		File sdDir = Environment.getExternalStorageDirectory();
		if (sdDir.exists() && sdDir.canWrite()) {

		} else {
			sdDir = Environment.getDataDirectory();

		}
		File index = new File(sdDir.getAbsolutePath() + "/download/index.html");
		return index.getAbsolutePath();
	}

	public int getTaskAmount() {
		return taskAmount;
	}

}
