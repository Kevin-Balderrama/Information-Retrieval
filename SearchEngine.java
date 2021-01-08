
/**
 * All code within SearchEngine class is original with the exception of:
 * 
 * All code in PorterAlgo and PorterCheck are part of porters algorithm external implementation as specified in Phase2
 * https://www.redicals.com/porter-stemming-algorithm-java-code.html
 * 
 */
import java.awt.Color;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;

import java.io.FileWriter;

public class SearchEngine {
	private static GUI gui = new GUI();
	public static void main(String[] args) throws IOException, FileNotFoundException {

//gui setup		
		gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		gui.setSize(250, 200);
		gui.setVisible(true);
		

// create and handle switches for flags
		String pathOfDir = System.getProperty("user.dir") + "PathOfDir";
		String nameOfIndexFile = System.getProperty("user.dir") + "NameOfIndexFile.txt";
		String nameOfStopListFile = System.getProperty("user.dir") + "NameOfStopListFile.txt";
		String queryFile = System.getProperty("user.dir") + "QueryFile.txt";
		String resultsFile = System.getProperty("user.dir") + "ResultsFile.txt";
		String outputOptions = System.getProperty("user.dir") + "dataFile.txt";
		boolean usePorter = false;
		int snippetLength = 0;
		int i = 0;
		String arg;
		while (i < args.length && args[i].startsWith("-")) {
			arg = args[i++];
			switch (arg) {
			case "-CorpusDir":
				System.out.println("Option CorpusDir Selected: PathOfDir");
				pathOfDir = System.getProperty("user.dir") + "\\" + args[i++];
				break;
			case "-InvertedIndex":
				System.out.println("Option InvertedIndex Selected: NameOfIndexFile");
				nameOfIndexFile = System.getProperty("user.dir") + "\\" + args[i++];
				break;
			case "-StopList":
				System.out.println("Option StopList Selected: NameOfStopListFile");
				nameOfStopListFile = System.getProperty("user.dir") + "\\" + args[i++];
				break;
			case "-Queries":
				System.out.println("Option Queries Selected: QueryFile");
				queryFile = System.getProperty("user.dir") + "\\" + args[i++];
				break;
			case "-Results":
				System.out.println("Option Results Selected: ResultsFile");
				resultsFile = System.getProperty("user.dir") + "\\" + args[i++];
				break;
			case "-UsePorter":
				System.out.println("Option Results Selected: UsePorter");
				usePorter = args[i++].equalsIgnoreCase("true");
				break;
			case "-snippetLength":
				System.out.println("Option Results Selected: snippetLength");
				snippetLength = Integer.parseInt(args[i++]);
				break;
			case "-output":
				System.out.println("Option Results Selected: output ");
				outputOptions = args[i++];
				if(outputOptions.equalsIgnoreCase("gui") || outputOptions.equalsIgnoreCase("file") || outputOptions.equalsIgnoreCase("both"))
					break;
				else
					System.err.println("ParseCmdLine: illegal option " + outputOptions);
			default:
				System.err.println("ParseCmdLine: illegal option " + arg);
				break;
			}
		}
		if (i == args.length)
			System.err.println("Usage: SearchEngine -CorpusDir PathOfDir " + "-InvertedIndex NameOfIndexFile "
					+ "-StopList NameOfStopListFile " + "-Queries QueryFile " + "-Results ResultsFile "
					+ "-UsePorter true|false " + "-snippetLength numberOfWordsInSnippet" + "-output gui|file|both");
		else
			System.out.println("Arguments Accepted!");

// make files based on flag switches of any given files
		File PathOfDir = new File(pathOfDir);
		File IndexFile = new File(nameOfIndexFile);
		File StopListFile = new File(nameOfStopListFile);
		File QueryFile = new File(queryFile);
		File ResultsFile = new File(resultsFile);
		
		

//Identify a Stoplist
		HashMap<String, Integer> wordMap = new HashMap<String, Integer>();
		HashMap<String, ArrayList<String>> index = new HashMap<String, ArrayList<String>>();
		System.out.println(pathOfDir);
		File[] files = PathOfDir.listFiles();
		
//create map of words to instances		
		gui.total = files.length;
		createWordMap(files, wordMap, index);
		gui.progressComplete("WordMap Complete");

// sorting word map		
		TreeMap<String, Integer> sorted = new TreeMap<>(wordMap);
		Set<Entry<String, Integer>> mappings = sorted.entrySet();
		Comparator<Entry<String, Integer>> valueComparator = new Comparator<Entry<String, Integer>>() {
			@Override
			public int compare(Entry<String, Integer> e1, Entry<String, Integer> e2) {
				Integer v1 = e1.getValue();
				Integer v2 = e2.getValue();
				return v1.compareTo(v2);
			}
		};
		List<Entry<String, Integer>> listOfEntries = new ArrayList<Entry<String, Integer>>(mappings);
		Collections.sort(listOfEntries, valueComparator);
		LinkedHashMap<String, Integer> sortedByValue = new LinkedHashMap<String, Integer>(listOfEntries.size());
		for (Entry<String, Integer> entry : listOfEntries) {
			sortedByValue.put(entry.getKey(), entry.getValue());
		}
		
// make and populate stopList and inverted index
		HashMap<String, Integer> stopList = new HashMap<String, Integer>();
		HashMap<String, ArrayList<String>> invertedIndex = new HashMap<String, ArrayList<String>>();
		int flag = 0;
		if (wordMap.size() * 99 / 100 < 50)
			flag = wordMap.size() * 99 / 100;
		else
			flag = wordMap.size() - 50;
		int counter = 0;

// push least frequent words to inverted index, and top75 or less to stopList
		for (String s : sortedByValue.keySet()) {
			if (counter <= flag)
				invertedIndex.put(s, index.get(s));
			else
				stopList.put(s, wordMap.get(s));
			counter++;
		}
		
// print word map		
		gui.setProgress(gui.progress, stopList.size(), "Printing Stoplist");
		printWordMap(stopList, StopListFile);
		gui.progressComplete("Stoplist Printed");

// print Inverted Index
		gui.setProgress(gui.progress, invertedIndex.size(), "Printing Inverted Index");
		printIndex(invertedIndex, IndexFile);
		gui.progressComplete("Inverted Index Printed");
		System.out.println("Complete Phase1 preprocess");

//Phase 2 Primary, other than switch starts here
//Task 1 is handled by external class PorterAlgo
//Algorithm use from https://www.redicals.com/porter-stemming-algorithm-java-code.html		
		if(usePorter)
		{
			File porterAlgo = new File("PorterIndex.txt");
			HashMap<String, ArrayList<String>> porterMap = new HashMap<String, ArrayList<String>>();
			gui.setProgress(gui.progress, invertedIndex.size(), "Loading Porter Map");
			if (!porterAlgo.isFile()) {
				for (String s : invertedIndex.keySet()) {
					gui.setProgress(++gui.progress, gui.total, "Loading Porter Map");
					String tokS = PorterCheck.stem(s);
					Set<String> next = new LinkedHashSet<>(invertedIndex.get(s));
					if (!porterMap.containsKey(tokS)) {
						porterMap.put(tokS, new ArrayList<String>());
					}
					next.removeAll(porterMap.get(tokS));
					porterMap.get(tokS).addAll(next);
				}
				printIndex(porterMap, porterAlgo);
			} else {
				for (String s : invertedIndex.keySet()) {
					gui.setProgress(++gui.progress, gui.total, "Loading Porter Map");
					String tokS = PorterCheck.stem(s);
					Set<String> next = new LinkedHashSet<>(invertedIndex.get(s));
					if (!porterMap.containsKey(tokS)) {
						porterMap.put(tokS, new ArrayList<String>());
					}
					next.removeAll(porterMap.get(tokS));
					porterMap.get(tokS).addAll(next);
				}
			}
			gui.progressComplete("Porter Map Loaded");
		}

//Task 3	
// when use of porter's algorithm is enabled
		int newTotal = invertedIndex.size();
		gui.setProgress(gui.progress, newTotal, "Processing Queries");
		if (usePorter) {
			FileWriter fw = new FileWriter(ResultsFile);
			PrintWriter pw = new PrintWriter(fw);
			Scanner scan = new Scanner(QueryFile);
			while (scan.hasNextLine()) {
				String next = scan.nextLine();
				String command = next.substring(0, next.indexOf(" "));
				String term = PorterCheck.stem(next.substring(next.indexOf(" ") + 1));
				String dne = term + " does not occur in any file, or has been selected as part of the stoplist";
				switch (command) {
				case "Query":
					pw.println(next);
					if (!invertedIndex.containsKey(term)) {
						pw.println(dne);
						break;
					}
					gui.setProgress(gui.progress, invertedIndex.get(term).size(), "Processing Queries");
					for (String s : invertedIndex.get(term))
					{
						pw.println(s);
						gui.setProgress(++gui.progress, gui.total, "Processing Queries With Porter's Algorithm");
					}
					pw.println();
					invertedIndex.get(term);
					break;
				case "Frequency":
					pw.println(next);
					pw.println(wordMap.get(term));
					pw.println();
					break;
				default:
					System.err.println("Usage: Query <term> | Frequency <term> ");
					break;
				}
				gui.setProgress(++gui.progress, gui.total, "Processing Queries With Porter's Algorithm");
			}
			scan.close();
			pw.close();
		}
// when use of porter's algorithm is disabled
		else {
			FileWriter fw = new FileWriter(ResultsFile);
			PrintWriter pw = new PrintWriter(fw);
			Scanner scan = new Scanner(QueryFile);
			while (scan.hasNextLine()) {
				String next = scan.nextLine();
				String command = next.substring(0, next.indexOf(" "));
				String term = next.substring(next.indexOf(" ") + 1);
				String dne = term + " does not occur in any file, or has been selected as part of the stoplist";
				switch (command) {
				case "Query":
					pw.println(next);
					if (!invertedIndex.containsKey(term)) {
						pw.println(dne);
						break;
					}
					gui.setProgress(gui.progress, invertedIndex.get(term).size(), "Processing Queries");
					for (String s : invertedIndex.get(term))
					{
						gui.setProgress(++gui.progress, gui.total, "Processing Queries Without Porter's Algorithm");
						pw.println(s);
					}
					pw.println();
					invertedIndex.get(term);
					break;
				case "Frequency":
					pw.println(next);
					pw.println(wordMap.get(term));
					pw.println();
					break;
				default:
					System.err.println("Usage: Query <term> | Frequency <term> ");
					break;
				}
				gui.setProgress(++gui.progress, gui.total, "Processing Index Without Porter's Algorithm");
			}
			scan.close();
			pw.close();
		}
		gui.progressComplete("Processing Queries Complete");	

//processing of snippets to display with words	
		
		System.out.println("Processing Snippets...");
		File snippets = new File("snippets.txt");
		FileWriter fw = new FileWriter(snippets);
		PrintWriter pw = new PrintWriter(fw);
		Scanner scan = new Scanner(QueryFile);
		HashMap<String, ArrayList<String>> returnMap = new HashMap<String, ArrayList<String>>();
		HashMap<String, HashMap<String, String>> snip = new HashMap<String, HashMap<String, String>>();
		String q = "";
		if (usePorter) {
			System.out.println(gui.progress);
			System.out.println(gui.total);
			newTotal = gui.total += index.size();
			gui.setProgress(gui.progress, newTotal, "Processing Snippets");
			if (scan.hasNextLine()) {
				String next = scan.nextLine();
				String command = next.substring(0, next.indexOf(" "));
				String term = PorterCheck.stem(next.substring(next.indexOf(" ") + 1));
				q= term;
				String dne = term + " does not occur in any file, or has been selected as part of the stoplist";

				
				switch (command) {
				case "Query":
					pw.println(next);
					String stem = term;
					boolean match = false;
					if (!index.keySet().isEmpty()) {
						gui.setProgress(gui.progress, gui.total += index.keySet().size(), "Processing Snippets");
						for (String x : index.keySet()) {
							if (x.matches(stem)) {
								match = true;
								Set<String> temp = new LinkedHashSet<>(index.get(x));
								if (!returnMap.containsKey(stem)) {
									returnMap.put(stem, new ArrayList<String>());
								}
								temp.removeAll(returnMap.get(stem));
								returnMap.get(stem).addAll(temp);
							}
							gui.setProgress(++gui.progress, gui.total, "Processing Snippets");
						}
					}
					if (!match) {
						pw.println(dne);
						break;
					}
					gui.setProgress(gui.progress, gui.total += returnMap.keySet().size(), "Processing Snippets");
					for (String y : returnMap.keySet()) {
						String key = y;
						gui.setProgress(gui.progress, gui.total += returnMap.get(y).size(), "Processing Snippets");
						for (String z : returnMap.get(y)) {
							gui.setProgress(++gui.progress, gui.total, "Processing Snippets");
							String[] glove = z.split(" word ");
							String docNo = glove[0];
							int wordPlace = Integer.parseInt(glove[1]);
							int start = wordPlace - snippetLength;
							if (start < 0)
								start = 0;
							int end = start + 2 * snippetLength + 1;
							if (!snip.containsKey(key))
								snip.put(key, new HashMap<String, String>());
							snip.get(key).put(docNo, next + "\n");
							String docPath = docNo.substring(0, docNo.indexOf("-"));
							File doc = new File(pathOfDir + "/" + docPath + "/" + docNo);
							Pattern p = Pattern.compile("[\\W\\?\\n\\t\\s\\\\/<>=!-/_0-9:;{}@|]+");
							Scanner scanSnip = new Scanner(doc);
							scanSnip.useDelimiter(p);
							int wordIndex = 0;
							
							while (scanSnip.hasNext() && wordIndex < end) {
								String capture = scanSnip.next();
								if (wordIndex >= start) {
									snip.get(key).put(docNo, snip.get(key).get(docNo).concat(capture.concat(" ")));
								}
								wordIndex++;
							}
							scanSnip.close();
						}
					}
					pw.close();
					printDoubleHash(snip, snippets);

				default:
					System.err.println("Usage: Query <term>");
					break;
				}
			}
		} else {
			if (scan.hasNextLine()) {
				String next = scan.nextLine();
				String command = next.substring(0, next.indexOf(" "));
				String term = next.substring(next.indexOf(" ") + 1);
				q= term;
				String dne = term + " does not occur in any file, or has been selected as part of the stoplist";
				newTotal = index.size();
				gui.setProgress(gui.progress, newTotal, "Processing Snippets");
				//HashMap<String, ArrayList<String>> returnMap = new HashMap<String, ArrayList<String>>();
				//HashMap<String, HashMap<String, String>> snip = new HashMap<String, HashMap<String, String>>();
				switch (command) {
				case "Query":
					pw.println(next);
					String stem = term;
					boolean match = false;
					if (!index.keySet().isEmpty()) {
						for (String x : index.keySet()) {
							if (x.matches(stem)) {
								match = true;
								Set<String> temp = new LinkedHashSet<>(index.get(x));
								if (!returnMap.containsKey(stem)) {
									returnMap.put(stem, new ArrayList<String>());
								}
								temp.removeAll(returnMap.get(stem));
								returnMap.get(stem).addAll(temp);
							}
							gui.setProgress(++gui.progress, gui.total, "Processing Snippets");
						}
					}
					if (!match) {
						pw.println(dne);
						break;
					}

					for (String y : returnMap.keySet()) {
						String key = y;
						for (String z : returnMap.get(y)) {
							String[] glove = z.split(" word ");
							String docNo = glove[0];
							int wordPlace = Integer.parseInt(glove[1]);
							int start = wordPlace - snippetLength;
							if (start < 0)
								start = 0;
							int end = start + 2 * snippetLength + 1;
							if (!snip.containsKey(key))
								snip.put(key, new HashMap<String, String>());
							snip.get(key).put(docNo, next + "\n");
							String docPath = docNo.substring(0, docNo.indexOf("-"));
							File doc = new File(pathOfDir + "/" + docPath + "/" + docNo);
							Pattern p = Pattern.compile("[\\W\\?\\n\\t\\s\\\\/<>=!-/_0-9:;{}@|]+");
							Scanner scanSnip = new Scanner(doc);
							scanSnip.useDelimiter(p);
							int wordIndex = 0;
							while (scanSnip.hasNext() && wordIndex < end) {
								String capture = scanSnip.next();
								if (wordIndex >= start) {
									snip.get(key).put(docNo, snip.get(key).get(docNo).concat(capture.concat(" ")));
								}
								wordIndex++;
							}
							scanSnip.close();
						}
					}
					pw.close();
					printDoubleHash(snip, snippets);

				default:
					System.err.println("Usage: Query <term>");
					break;
				}
			}
		}
		scan.close();
		pw.close();
// end screen when all processing is done and query as well as analytics are processed and displayed
		gui.progressComplete("Snippets Complete");
		System.out.println("Snippets Complete");
		gui.finalPush();
		double tp = ((double)snip.get(q).size());
		double p = ((double)invertedIndex.get(q).size());
		double fp = files.length;
		
// fall though as to display of data
		if(outputOptions.equals("gui") || outputOptions.equals("both"))
		{
			
			gui.print("\nRecall: " + (tp/p) + "\n");
			gui.print("Precision: " + (tp/(tp+fp)) + "\n");
			printToGUI(snip, gui);
		}
		if(outputOptions.equals("file"))
		{
			File data = new File(System.getProperty("user.dir") + "\\" +"data.txt");
			FileWriter dfw = new FileWriter(data);
			PrintWriter dpw = new PrintWriter(dfw);
			dpw.print("\nRecall: " + (tp/p) + "\n");
			dpw.print("Precision: " + (tp/(tp+fp)) + "\n");
			for (String outer : snip.keySet()) {
				for (String inner : snip.get(outer).keySet()) {
					dpw.println("\n\n"+snip.get(outer).get(inner) + "\n->\n" +inner);
					dpw.println("sssss");
					//inner + "->" + snip.get(outer).get(inner)
				}
			}
			dpw.close();
			gui.dispatchEvent(new WindowEvent(gui, WindowEvent.WINDOW_CLOSING));
		}
		
		File data = new File(System.getProperty("user.dir") + "\\" +"data.txt");
		FileWriter dfw = new FileWriter(data);
		PrintWriter dpw = new PrintWriter(dfw);
		dpw.print("\nRecall: " + (tp/p) + "\n");
		dpw.print("Precision: " + (tp/(tp+fp)) + "\n");
		for (String outer : snip.keySet()) {
			for (String inner : snip.get(outer).keySet()) {
				dpw.println("\n\n"+snip.get(outer).get(inner) + "\n->\n" +inner);
				dpw.println("sssss");
				//inner + "->" + snip.get(outer).get(inner)
			}
		}
		dpw.close();
	}

	public static void createWordMap(File[] files, HashMap<String, Integer> wordMap,
			HashMap<String, ArrayList<String>> invertedIndex) {
		int newTotal = gui.total += files.length;
		gui.setProgress(gui.progress, newTotal, "Processing PathOfDir");
		for (File file : files) {			
			if (file.isDirectory()) {
				System.out.print("Processing Directory: " + file.getName() + "...");
				createWordMap(file.listFiles(), wordMap, invertedIndex);
			} else {
				try {
					int wordCounter = 0;
					Pattern p = Pattern.compile("[\\W\\?\\n\\t\\s\\\\/<>=!-/_0-9:;{}@|]+");
					Scanner scan = new Scanner(file);
					scan.useDelimiter(p);
					while (scan.hasNext()) {
						String next = scan.next().toLowerCase();
						if (!wordMap.containsKey(next))
							wordMap.put(next, 0);
						wordMap.replace(next, wordMap.get(next) + 1);
						if (!invertedIndex.containsKey(next))
							invertedIndex.put(next, new ArrayList<String>());
						invertedIndex.get(next).add(file.getName() + " word " + wordCounter);
						wordCounter++;
					}
					scan.close();
					gui.setProgress(++gui.progress, gui.total, "Processing PathOfDir");
				} catch (FileNotFoundException e) {
					System.out.println("failed in create");
					e.printStackTrace();
				}
			}
			
		}
		System.out.println("Complete");
		
	}

	public static void printWordMap(HashMap<String, Integer> wordMap, File output) {
		try {
			FileWriter fw = new FileWriter(output);
			PrintWriter pw = new PrintWriter(fw);
			wordMap.entrySet().stream().forEach(e -> {
				pw.println(e.getValue() + ": instance(s) of " + e.getKey());
				gui.setProgress(++gui.progress, gui.total, "Printing to "+output.getName());
				});
			pw.close();
		} catch (IOException e2) {
			System.out.println("failed in print");
			e2.printStackTrace();
		}
	}

	public static void printDoubleHash(HashMap<String, HashMap<String, String>> doubleMap, File output) {
		try {
			FileWriter fw = new FileWriter(output);
			PrintWriter pw = new PrintWriter(fw);
			gui.setProgress(++gui.progress, gui.total+= doubleMap.size(), "Printing to "+output.getName());
			for (String outer : doubleMap.keySet()) {
				gui.setProgress(++gui.progress, gui.total+= doubleMap.get(outer).size(), "Printing to "+output.getName());
				for (String inner : doubleMap.get(outer).keySet()) {
					gui.setProgress(++gui.progress, gui.total, "Printing to "+output.getName());
					pw.println(inner + "->" + doubleMap.get(outer).get(inner));
				}
			}
			pw.close();
		} catch (IOException e2) {
			System.out.println("failed in print");
			e2.printStackTrace();
		}
	}

	public static void printIndex(HashMap<String, ArrayList<String>> invertedIndex, File output) {
		TreeMap<String, ArrayList<String>> sorted = new TreeMap<>(invertedIndex);
		Set<Entry<String, ArrayList<String>>> mappings = sorted.entrySet();
		try {
			FileWriter fw = new FileWriter(output);
			PrintWriter pw = new PrintWriter(fw);
			for (Entry<String, ArrayList<String>> mapping : mappings) {
				pw.println(mapping.getKey() + "->" + mapping.getValue());
				gui.setProgress(++gui.progress, gui.total, "Printing to "+output.getName());
			}
			pw.close();
		} catch (IOException e2) {
			System.out.println("failed in print");
			e2.printStackTrace();
		}
	}
	
	public static void printToGUII(HashMap<String, ArrayList<String>> map, GUI output) {
		System.out.println();
		TreeMap<String, ArrayList<String>> sorted = new TreeMap<>(map);
		Set<Entry<String, ArrayList<String>>> mappings = sorted.entrySet();
		for (Entry<String, ArrayList<String>> mapping : mappings) {
			output.print(mapping.getKey() + "->" + mapping.getValue());
		}
	}
	
	public static void printToGUI(HashMap<String, HashMap<String, String>> snips, GUI output) {
		System.out.println();
		for (String outer : snips.keySet()) {
			for (String inner : snips.get(outer).keySet()) {
				System.out.println(inner + "->\n" + snips.get(outer).get(inner));
				output.print("\n\n"+snips.get(outer).get(inner) + "\n->\n" +inner );
			}
		}
		
	}
}

class GUI extends JFrame {
	private Box processingBox = Box.createVerticalBox();
	private JPanel panel;
	private JLabel message;
	private JProgressBar progressBar;
	private JLabel percentage;
	private JTextArea jTextArea = new JTextArea(20, 20);
	private JScrollPane scrollable = new JScrollPane(jTextArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
	//private boolean usePorter;
	//private boolean wake = false;
	int total = 1;
	int progress = 0;

	public GUI() {
		panel = new JPanel();
		message = new JLabel();
		progressBar = new JProgressBar();
		percentage = new JLabel();
		

		setTitle("CS335-Kevin-Balderrama");
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setAlwaysOnTop(true);
		setMinimumSize(new java.awt.Dimension(500, 500));
		//setUndecorated(true);

		panel.setBackground(new java.awt.Color(102, 102, 102));
		panel.setBorder(new SoftBevelBorder(BevelBorder.RAISED, null, null, new Color(0, 0, 0), new Color(0, 0, 0)));
		panel.setForeground(new java.awt.Color(0, 0, 0));

		message.setFont(new java.awt.Font("Dialog", 1, 20)); // NOI18N
		message.setHorizontalAlignment(SwingConstants.CENTER);
		
		
		message.setText("Processing...");

		percentage.setText("0%");

		jTextArea.setBackground(new Color(102, 102, 102));
		jTextArea.setSize(400, 1000);
		jTextArea.setText("Results:");
		
		processingBox.add(message);
		processingBox.add(progressBar);
		processingBox.add(percentage);
		
		this.add(processingBox);
		
		setSize(new java.awt.Dimension(500, 500));
		setLocationRelativeTo(null);
	}
	public void setProgress(int soFar, int target, String message)
	{
		
		int percent = ((100*soFar)/target);
		//System.out.println(percent);
		total = target;
		progress = soFar;
		this.message.setText(message);
		percentage.setText(percent + "%");
		//System.out.println(percent + "%");
		progressBar.setValue(percent);
		this.repaint();
	}
	public void progressComplete(String message)
	{
		total = 1;
		progress = 0;
		int percent = 100;
		this.message.setText(message);
		percentage.setText(percent + "%");
		progressBar.setValue(percent);
		
		this.repaint();
	}
	
	public void finalPush()
	{
		processingBox.remove(message);
		processingBox.remove(percentage);
		processingBox.remove(progressBar);
		this.remove(processingBox);
		this.add(scrollable);
		setVisible(true);
		this.repaint();
	}
	
	public void print(String string)
	{
		jTextArea.append(string);
		this.repaint();
	}
}

/**
 * Following code is used from the below url=
 * 
 * @author https://www.redicals.com/porter-stemming-algorithm-java-code.html
 */
//Algorithm use from 
class NewString {
	public String str;

	NewString() {
		str = "";
	}
}

class PorterAlgo {

	String Clean(String str) {
		int last = str.length();

		new Character(str.charAt(0));
		String temp = "";

		for (int i = 0; i < last; i++) {
			if (Character.isLetterOrDigit(str.charAt(i)))
				temp += str.charAt(i);
		}

		return temp;
	} // clean

	boolean hasSuffix(String word, String suffix, NewString stem) {

		String tmp = "";

		if (word.length() <= suffix.length())
			return false;
		if (suffix.length() > 1)
			if (word.charAt(word.length() - 2) != suffix.charAt(suffix.length() - 2))
				return false;

		stem.str = "";

		for (int i = 0; i < word.length() - suffix.length(); i++)
			stem.str += word.charAt(i);
		tmp = stem.str;

		for (int i = 0; i < suffix.length(); i++)
			tmp += suffix.charAt(i);

		if (tmp.compareTo(word) == 0)
			return true;
		else
			return false;
	}

	boolean vowel(char ch, char prev) {
		switch (ch) {
		case 'a':
		case 'e':
		case 'i':
		case 'o':
		case 'u':
			return true;
		case 'y': {

			switch (prev) {
			case 'a':
			case 'e':
			case 'i':
			case 'o':
			case 'u':
				return false;

			default:
				return true;
			}
		}

		default:
			return false;
		}
	}

	int measure(String stem) {

		int i = 0, count = 0;
		int length = stem.length();

		while (i < length) {
			for (; i < length; i++) {
				if (i > 0) {
					if (vowel(stem.charAt(i), stem.charAt(i - 1)))
						break;
				} else {
					if (vowel(stem.charAt(i), 'a'))
						break;
				}
			}

			for (i++; i < length; i++) {
				if (i > 0) {
					if (!vowel(stem.charAt(i), stem.charAt(i - 1)))
						break;
				} else {
					if (!vowel(stem.charAt(i), '?'))
						break;
				}
			}
			if (i < length) {
				count++;
				i++;
			}
		} // while

		return (count);
	}

	boolean containsVowel(String word) {

		for (int i = 0; i < word.length(); i++)
			if (i > 0) {
				if (vowel(word.charAt(i), word.charAt(i - 1)))
					return true;
			} else {
				if (vowel(word.charAt(0), 'a'))
					return true;
			}

		return false;
	}

	boolean cvc(String str) {
		int length = str.length();

		if (length < 3)
			return false;

		if ((!vowel(str.charAt(length - 1), str.charAt(length - 2))) && (str.charAt(length - 1) != 'w')
				&& (str.charAt(length - 1) != 'x') && (str.charAt(length - 1) != 'y')
				&& (vowel(str.charAt(length - 2), str.charAt(length - 3)))) {

			if (length == 3) {
				if (!vowel(str.charAt(0), '?'))
					return true;
				else
					return false;
			} else {
				if (!vowel(str.charAt(length - 3), str.charAt(length - 4)))
					return true;
				else
					return false;
			}
		}

		return false;
	}

	String step1(String str) {

		NewString stem = new NewString();

		if (str.charAt(str.length() - 1) == 's') {
			if ((hasSuffix(str, "sses", stem)) || (hasSuffix(str, "ies", stem))) {
				String tmp = "";
				for (int i = 0; i < str.length() - 2; i++)
					tmp += str.charAt(i);
				str = tmp;
			} else {
				if ((str.length() == 1) && (str.charAt(str.length() - 1) == 's')) {
					str = "";
					return str;
				}
				if (str.charAt(str.length() - 2) != 's') {
					String tmp = "";
					for (int i = 0; i < str.length() - 1; i++)
						tmp += str.charAt(i);
					str = tmp;
				}
			}
		}

		if (hasSuffix(str, "eed", stem)) {
			if (measure(stem.str) > 0) {
				String tmp = "";
				for (int i = 0; i < str.length() - 1; i++)
					tmp += str.charAt(i);
				str = tmp;
			}
		} else {
			if ((hasSuffix(str, "ed", stem)) || (hasSuffix(str, "ing", stem))) {
				if (containsVowel(stem.str)) {

					String tmp = "";
					for (int i = 0; i < stem.str.length(); i++)
						tmp += str.charAt(i);
					str = tmp;
					if (str.length() == 1)
						return str;

					if ((hasSuffix(str, "at", stem)) || (hasSuffix(str, "bl", stem)) || (hasSuffix(str, "iz", stem))) {
						str += "e";

					} else {
						int length = str.length();
						if ((str.charAt(length - 1) == str.charAt(length - 2)) && (str.charAt(length - 1) != 'l')
								&& (str.charAt(length - 1) != 's') && (str.charAt(length - 1) != 'z')) {

							tmp = "";
							for (int i = 0; i < str.length() - 1; i++)
								tmp += str.charAt(i);
							str = tmp;
						} else if (measure(str) == 1) {
							if (cvc(str))
								str += "e";
						}
					}
				}
			}
		}

		if (hasSuffix(str, "y", stem))
			if (containsVowel(stem.str)) {
				String tmp = "";
				for (int i = 0; i < str.length() - 1; i++)
					tmp += str.charAt(i);
				str = tmp + "i";
			}
		return str;
	}

	String step2(String str) {

		String[][] suffixes = { { "ational", "ate" }, { "tional", "tion" }, { "enci", "ence" }, { "anci", "ance" },
				{ "izer", "ize" }, { "iser", "ize" }, { "abli", "able" }, { "alli", "al" }, { "entli", "ent" },
				{ "eli", "e" }, { "ousli", "ous" }, { "ization", "ize" }, { "isation", "ize" }, { "ation", "ate" },
				{ "ator", "ate" }, { "alism", "al" }, { "iveness", "ive" }, { "fulness", "ful" }, { "ousness", "ous" },
				{ "aliti", "al" }, { "iviti", "ive" }, { "biliti", "ble" } };
		NewString stem = new NewString();
		for (int index = 0; index < suffixes.length; index++) {
			if (hasSuffix(str, suffixes[index][0], stem)) {
				if (measure(stem.str) > 0) {
					str = stem.str + suffixes[index][1];
					return str;
				}
			}
		}

		return str;
	}

	String step3(String str) {

		String[][] suffixes = { { "icate", "ic" }, { "ative", "" }, { "alize", "al" }, { "alise", "al" },
				{ "iciti", "ic" }, { "ical", "ic" }, { "ful", "" }, { "ness", "" } };
		NewString stem = new NewString();

		for (int index = 0; index < suffixes.length; index++) {
			if (hasSuffix(str, suffixes[index][0], stem))
				if (measure(stem.str) > 0) {
					str = stem.str + suffixes[index][1];
					return str;
				}
		}
		return str;
	}

	String step4(String str) {

		String[] suffixes = { "al", "ance", "ence", "er", "ic", "able", "ible", "ant", "ement", "ment", "ent", "sion",
				"tion", "ou", "ism", "ate", "iti", "ous", "ive", "ize", "ise" };

		NewString stem = new NewString();

		for (int index = 0; index < suffixes.length; index++) {
			if (hasSuffix(str, suffixes[index], stem)) {

				if (measure(stem.str) > 1) {
					str = stem.str;
					return str;
				}
			}
		}
		return str;
	}

	String step5(String str) {

		if (str.charAt(str.length() - 1) == 'e') {
			if (measure(str) > 1) {/* measure(str)==measure(stem) if ends in vowel */
				String tmp = "";
				for (int i = 0; i < str.length() - 1; i++)
					tmp += str.charAt(i);
				str = tmp;
			} else if (measure(str) == 1) {
				String stem = "";
				for (int i = 0; i < str.length() - 1; i++)
					stem += str.charAt(i);

				if (!cvc(stem))
					str = stem;
			}
		}

		if (str.length() == 1)
			return str;
		if ((str.charAt(str.length() - 1) == 'l') && (str.charAt(str.length() - 2) == 'l') && (measure(str) > 1))
			if (measure(str) > 1) {/* measure(str)==measure(stem) if ends in vowel */
				String tmp = "";
				for (int i = 0; i < str.length() - 1; i++)
					tmp += str.charAt(i);
				str = tmp;
			}
		return str;
	}

	String stripPrefixes(String str) {

		String[] prefixes = { "kilo", "micro", "milli", "intra", "ultra", "mega", "nano", "pico", "pseudo" };

		int last = prefixes.length;
		for (int i = 0; i < last; i++) {
			if (str.startsWith(prefixes[i])) {
				String temp = "";
				for (int j = 0; j < str.length() - prefixes[i].length(); j++)
					temp += str.charAt(j + prefixes[i].length());
				return temp;
			}
		}

		return str;
	}

	private String stripSuffixes(String str) {

		str = step1(str);
		if (str.length() >= 1)
			str = step2(str);
		if (str.length() >= 1)
			str = step3(str);
		if (str.length() >= 1)
			str = step4(str);
		if (str.length() >= 1)
			str = step5(str);

		return str;
	}

	public String stripAffixes(String str) {

		str = str.toLowerCase();
		str = Clean(str);

		if ((str != "") && (str.length() > 2)) {
			str = stripPrefixes(str);

			if (str != "")
				str = stripSuffixes(str);

		}

		return str;
	}

}

//Algorithm use from https://www.redicals.com/porter-stemming-algorithm-java-code.html
class PorterCheck {
//method to completely stem the words in an array list
	public static String stem(String tokens1) {
		PorterAlgo pa = new PorterAlgo();
		String s1 = pa.step1(tokens1);
		String s2 = pa.step2(s1);
		String s3 = pa.step3(s2);
		String s4 = pa.step4(s3);
		String s5 = pa.step5(s4);

		return s5;
	}

	public static ArrayList<String> completeStem(List<String> tokens1) {
		PorterAlgo pa = new PorterAlgo();
		ArrayList<String> arrstr = new ArrayList<String>();
		for (String i : tokens1) {
			String s1 = pa.step1(i);
			String s2 = pa.step2(s1);
			String s3 = pa.step3(s2);
			String s4 = pa.step4(s3);
			String s5 = pa.step5(s4);
			arrstr.add(s5);
		}
		return arrstr;
	}

	public static ArrayList<String> fileTokenizer() {
		StringTokenizer strtoken = new StringTokenizer("this is a book");
		ArrayList<String> filetoken = new ArrayList<String>();
		while (strtoken.hasMoreElements()) {
			filetoken.add(strtoken.nextToken());
		}
		return filetoken;
	}
}