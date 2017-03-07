package org.thunlp.thulac.data;

import org.junit.Test;
import org.thunlp.thulac.util.StringUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * A class which converts {@link Dat} files generated by {@link DatMaker} inversely to a
 * list
 * of words.
 */
public class Dat2WordsConverter {
	/**
	 * Converts the given {@link Dat} file generated by {@link DatMaker} to words plus
	 * line numbers and output them through {@code writer}.
	 *
	 * @param dat
	 * 		The {@link Dat} file to convert.
	 * @param writer
	 * 		The {@link PrintWriter} to output words plus line numbers to.
	 * @param ln
	 * 		Whether to output line numbers.
	 */
	private static void convert(Dat dat, PrintWriter writer, boolean ln) {
		traverseTrieTree(dat, writer, 0, new Stack<>(), ln);
	}

	/**
	 * Traverse within the Trie Tree specified by the {@link Dat} file. The file is
	 * assumed to be generated correctly using {@link DatMaker}, otherwise the behavior
	 * is undefined. Along the traversing, the words plus line numbers stored within this
	 * Trie Tree are output using {@linkplain PrintWriter#println(String)
	 * writer.println()}.<br>
	 * This method calls itself recursively.
	 *
	 * @param dat
	 * 		The {@link Dat} file.
	 * @param writer
	 * 		The {@link PrintWriter} to output words to.
	 * @param index
	 * 		The index of the node to traverse.
	 * @param prefix
	 * 		The current prefix of this node, as a list of code points.
	 * @param ln
	 * 		Whether to output line numbers
	 */
	private static void traverseTrieTree(
			Dat dat, PrintWriter writer, int index, Stack<Integer> prefix, boolean ln) {
		int[] d = dat.dat;
		int base = d[index << 1], length = dat.datSize;
		if (d[(base << 1) + 1] == index && !prefix.isEmpty()) {
			writer.print(toString(prefix));
			if (ln) {
				writer.print(' ');
				writer.println(d[base << 1]); // line number
			} else writer.println();
		}
		for (int i = base + 1; i < length; ++i)
			if (d[(i << 1) + 1] == index) {
				prefix.push(i - base);
				traverseTrieTree(dat, writer, i, prefix, ln);
				prefix.pop();
			}
	}

	/**
	 * Converts an list of code points to a {@link String}.
	 *
	 * @param codePoints
	 * 		The list of code pointe.
	 *
	 * @return The converted {@link String}.
	 *
	 * @see StringUtils#toString(int...)
	 */
	private static String toString(List<Integer> codePoints) {
		StringBuilder sb = new StringBuilder();
		for (int codePoint : codePoints) sb.appendCodePoint(codePoint);
		return sb.toString();
	}

	/**
	 * Convert dat file at models/&lt;name&gt;.dat to words and save converted result
	 * to build/tmp/tests/&lt;name&gt;_text.txt.
	 *
	 * @param name
	 * 		The name of the DAT file.
	 * @param ln
	 * 		Whether to output line numbers.
	 *
	 * @throws IOException
	 * 		If an I/O error occurs.
	 */
	private static void convertAndSave(String name, boolean ln) throws IOException {
		Dat dat = new Dat("models/" + name + ".dat");
		PrintWriter writer = new PrintWriter(Files.newBufferedWriter(
				Paths.get("build/tmp/tests/" + name + "_text.txt")));
		convert(dat, writer, ln);
		writer.close();
	}

	private static Pattern LINE_PATTERN = Pattern.compile("^(.*)\\s(\\d+)$");

	/**
	 * Read file generated by {@link #convertAndSave(String, boolean)} at
	 * build/tmp/tests/&lt;name&gt;_text.txt and sort the words comparing the
	 * corresponding line numbers. Every line of the input file should match {@link
	 * #LINE_PATTERN}, while the first group being the word and the second
	 * group being the line number. The sorted result is output to
	 * build/tmp/tests/&lt;name&gt;_sorted.txt with the line numbers removed,
	 * containing the words only.<br>
	 * Since the {@link Dat} file as input to {@link #convertAndSave(String, boolean)} is
	 * assumed to be generated using {@link DatMaker#readFromTxtFile(String)}, which
	 * reads from a text file containing a word on each line, the file generated by
	 * this method should be identical to the input file provided to {@link
	 * DatMaker#readFromTxtFile(String)}.
	 *
	 * @param name
	 * 		The name of the converted file.
	 *
	 * @throws IOException
	 * 		If an I/O error occurs.
	 */
	private static void sortAndSave(String name) throws IOException {
		// This method makes excessive use of the Java 8 Stream API, advanced knowledge
		// of streams is required to read the following code.

		Files.write(Paths.get("build/tmp/tests/" + name + "_sorted.txt"),
				(Iterable<String>) Files.lines(
						Paths.get("build/tmp/tests/" + name + "_text.txt"))
						.map(line -> {
							Matcher matcher = LINE_PATTERN.matcher(line);
							if (!matcher.find()) return null;
							return new AbstractMap.SimpleEntry<>(
									Integer.parseInt(matcher.group(2)),
									matcher.group(1));
						})
						.sorted(Comparator.comparingInt(AbstractMap.SimpleEntry::getKey))
						.map(AbstractMap.SimpleEntry::getValue)::iterator);
	}

	/**
	 * Convert a stream of {@link Dat} files specified by {@code datFiles} to words plus
	 * line numbers using {@link #convertAndSave(String, boolean)} and then sort the
	 * lines using {@link #sortAndSave(String)}. This method output messages to {@link
	 * System#out} while executing.
	 *
	 * @param datFiles
	 * 		The stream of {@link Dat} files, for each {@link String} in {@code datFiles},
	 * 		for example, {@code "example"}, the input {@link Dat} file is at {@code
	 * 		models/example.dat}, the converted file is at {@code
	 * 		build/tmp/tests/example_text.txt}, and the sorted file is at {@code
	 * 		build/tmp/tests/example_sorted.txt}.
	 */
	private void convertAndSort(Stream<String> datFiles) {
		datFiles.forEach(datFile -> {
			try {
				System.out.printf("Converting dat file %s.dat\n", datFile);
				convertAndSave(datFile, true);
				System.out.printf("Sorting dat file build/tmp/tests/%s_text.dat\n",
						datFile);
				sortAndSave(datFile);
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	@Test
	public void test() throws IOException {
		convertAndSort(Files.list(Paths.get("models/"))
				.parallel()
				.map(Path::getFileName)
				.map(Path::toString)
				.map(String::toLowerCase)
				.filter(filename -> filename.endsWith(".dat"))
				.map(filename -> filename.substring(0, filename.length() - 4))
				.filter(filename -> !"t2s".equals(filename)) // not Dat file
				.filter(filename -> !"idiom".equals(filename))); // not DatMaker
		// idiom.dat is correct Dat file however not generated by DatMaker
		System.out.println("Converting dat file idiom.dat");
		convertAndSave("idiom", false);
	}
}