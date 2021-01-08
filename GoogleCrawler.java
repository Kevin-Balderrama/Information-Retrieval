
/**
 * https://mkyong.com/java/jsoup-send-search-query-to-google/
 * credit to above for the search engine core code
 * only made a few modifications to suit my purposes
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.Scanner;

public class GoogleCrawler {
	private static Pattern patternDomainName;
	private Matcher matcher;
	private static final String DOMAIN_NAME_PATTERN = "([a-zA-Z0-9]([a-zA-Z0-9\\\\-]{0,61}[a-zA-Z0-9])?\\\\.)+[a-zA-Z]{2,6}";
	static {
		patternDomainName = Pattern.compile(DOMAIN_NAME_PATTERN);
	}

	public static void main(String[] args) throws IOException {
		
		String dir = System.getProperty("user.dir");
		System.out.println(dir);
		File folder = new File(dir + "/links");
		boolean created = folder.mkdir();
		System.out.println(folder.getPath() + created);
		
		File corpusQueries = new File("queries.txt");
		Scanner scan = new Scanner(corpusQueries);

		while (scan.hasNextLine()) {
			String next = scan.nextLine();
			System.out.println(next);
			File file = new File(folder.getAbsolutePath()+ "/" + next);
			file.mkdir();/*
			FileWriter fw = new FileWriter(file);
			PrintWriter pw = new PrintWriter(fw);
			
				
			
			//System.out.println(result.size());
			pw.close();*/
		}
		scan.close();
		/*
		File[] files = folder.listFiles();
		for (int i = 0; i < files.length; i++) {
			if (files[i].isFile()) {
				Scanner scanlinks = new Scanner(files[i]);
				while (scanlinks.hasNextLine()) {
					String scanlink = scanlinks.nextLine();
					System.out.println(scanlink);
					GoogleCrawler obj2 = new GoogleCrawler();
					Set<String> result2 = obj2.getHtmlFromGoogle(scanlink);
					for (String temp : result2) {
						File newFile = new File(scanlinks + ".txt");
						FileWriter fw2 = new FileWriter(newFile);
						PrintWriter pw2 = new PrintWriter(fw2);
						pw2.print(temp);
					}
				}
			}
		}
		
		
				GoogleCrawler obj = new GoogleCrawler();
				Set<String> result = obj.getDataFromGoogle(next);
				for (String temp : result) {
					pw.println(temp);
					System.out.println(temp);
				}
		
		
		
		*/

	}

	public String getDomainName(String url) {

		String domainName = "";
		matcher = patternDomainName.matcher(url);
		if (matcher.find()) {
			domainName = matcher.group(0).toLowerCase().trim();
		}
		return domainName;

	}

	private Set<String> getDataFromGoogle(String query) {

		Set<String> result = new HashSet<String>();
		String request = "https://www.google.com/search?q=" + query + "&num=5";
		System.out.println("Sending request..." + request);

		try {

			// need http protocol, set this as a Google bot agent :)
			Document doc = Jsoup.connect(request)
					.userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)").timeout(5000)
					.get();

			// get all links
			Elements links = doc.select("a[href]");
			for (Element link : links) {

				String temp = link.attr("href");
				if (temp.startsWith("/url?q=")) {
					// use regex to get domain name
					result.add(getDomainName(temp));
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	private Set<String> getHtmlFromGoogle(String url) {

		Set<String> result = new HashSet<String>();
		String request = url;
		System.out.println("Sending request..." + request);

		try {

			Document doc = Jsoup.connect(request)
					.userAgent("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)").timeout(5000)
					.get();

			// get all links
			Elements links = doc.select("a[href]");
			for (Element link : links) {

				String temp = link.html();
				System.out.println(link.outerHtml());
				if (temp.startsWith("/url?q=")) {
					// use regex to get domain name
					result.add(getDomainName(temp));
				}

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

}