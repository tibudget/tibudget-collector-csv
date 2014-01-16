package com.tibudget.plugins.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

public class CsvFormatAnalyzer {

	private static Logger LOG = LoggerFactory.getLogger(CsvFormatAnalyzer.class);

	private static final int ANALYZE_LINE_COUNT = 100;

	private static final String[] DATE_FORMATS = new String[] {
		"yyyyddMM",
		"MMddyyyy",
		"yyyyMMdd",
		"ddMMyyyy",
		"yyyy-dd-MM",
		"MM-dd-yyyy",
		"yyyy-MM-dd",
		"dd-MM-yyyy",
		"yyyy/dd/MM",
		"MM/dd/yyyy",
		"yyyy/MM/dd",
		"dd/MM/yyyy",
		"MMM dd yyyy",
		"MMMM dd yyyy",
		"dd MMM yyyy",
	"dd MMMM yyyy"};

	private static class ColumnStats {
		int index;
		DescriptiveStatistics lengthStat;
		DescriptiveStatistics digitPercentStat;
		DescriptiveStatistics letterPercentStat;
		DescriptiveStatistics numberStat;
		DescriptiveStatistics dateStat;

		public ColumnStats(int index) {
			super();
			this.index = index;
			this.lengthStat = new DescriptiveStatistics();
			this.digitPercentStat = new DescriptiveStatistics();
			this.letterPercentStat = new DescriptiveStatistics();
			this.numberStat = new DescriptiveStatistics();
			this.dateStat = new DescriptiveStatistics();
		}

		public int getIndex() {
			return this.index;
		}

		public DescriptiveStatistics getLengthStat() {
			return this.lengthStat;
		}

		public DescriptiveStatistics getDigitPercentStat() {
			return this.digitPercentStat;
		}

		public DescriptiveStatistics getLetterPercentStat() {
			return this.letterPercentStat;
		}

		public DescriptiveStatistics getNumberStat() {
			return this.numberStat;
		}

		public DescriptiveStatistics getDateStat() {
			return this.dateStat;
		}
	}

	private static class ProbabilityDateComparator implements Serializable, Comparator<ColumnStats> {

		private static final long serialVersionUID = -1460741040462838258L;

		@Override
		public int compare(ColumnStats o1, ColumnStats o2) {
			return (int) ((computeDateProbability(o2) - computeDateProbability(o1)) * 100);
		}
	}

	private static class ProbabilityNumberComparator implements Serializable, Comparator<ColumnStats> {

		private static final long serialVersionUID = 7286857458628814954L;

		@Override
		public int compare(ColumnStats o1, ColumnStats o2) {
			return (int) ((computeNumberProbability(o2) - computeNumberProbability(o1)) * 100);
		}
	}

	private static class ProbabilityLabelComparator implements Serializable, Comparator<ColumnStats> {

		private static final long serialVersionUID = 6002038164929049592L;

		@Override
		public int compare(ColumnStats o1, ColumnStats o2) {
			return (int) ((computeLabelProbability(o2) - computeLabelProbability(o1)) * 100);
		}
	}

	public static CsvFormat findFormat(File csvFile) throws IOException {

		// Guess char separator
		Character charSepartor = findCharSeparator(csvFile);
		if (charSepartor == null) {
			return null;
		}

		// Guess date format
		String datePattern = findDatePattern(csvFile, charSepartor.charValue());
		if (datePattern == null) {
			return null;
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);

		// Guess values format
		DecimalFormat decimalFormat = findDecimalFormat(csvFile, charSepartor.charValue());
		if (decimalFormat == null) {
			return null;
		}

		DescriptiveStatistics colCountStats = new DescriptiveStatistics();
		Map<Integer, ColumnStats> colStats = new HashMap<Integer, ColumnStats>();
		boolean skipFirstLine = true;
		int lineCount = 0;
		Reader reader = null;
		CSVReader csvReader = null;
		FileInputStream is = null;
		try {
			is = new FileInputStream(csvFile);
			reader = new InputStreamReader(is, CsvCollector.DEFAULT_CHARSET);
			csvReader = new CSVReader(reader, charSepartor.charValue());
			// Analyze first line
			String[] nextLine = csvReader.readNext();
			for (String element : nextLine) {
				if (determineDateFormat(Collections.singletonList(element)).size() > 0) {
					skipFirstLine = false;
				}
			}
			// Parse next lines
			ParsePosition pp = new ParsePosition(0);
			while (lineCount < ANALYZE_LINE_COUNT && (nextLine = csvReader.readNext()) != null) {
				if (nextLine.length == 0 || (nextLine.length==1 && nextLine[0].length() == 0)) {
					// Ignore empty line
					continue;
				}
				lineCount++;
				colCountStats.addValue(nextLine.length);
				int colIndex = 1;
				for (String string : nextLine) {
					String value = string.trim();
					ColumnStats colStat = colStats.get(colIndex);
					if (colStat == null) {
						colStat = new ColumnStats(colIndex);
						colStats.put(colIndex, colStat);
					}
					colStat.getLengthStat().addValue(value.length());
					if (value.length() > 0) {
						double digitCount = 0;
						double letterCount = 0;
						// Value (Number)
						pp.setErrorIndex(-1); pp.setIndex(0);
						Number number = decimalFormat.parse(value, pp);
						if (value.length() == pp.getIndex() && number != null) {
							colStat.getNumberStat().addValue(number.doubleValue());
						}
						// Date
						try {
							Date date = dateFormat.parse(value);
							colStat.getDateStat().addValue(date.getTime());
						}
						catch (ParseException e) {
							// ignore
						}
						for (int i = 0; i < value.length(); i++) {
							char c = value.charAt(i);
							if (Character.isDigit(c)) {
								digitCount++;
							}
							else if (Character.isLetter(c)) {
								letterCount++;
							}
						}
						colStat.getDigitPercentStat().addValue(digitCount / value.length());
						colStat.getLetterPercentStat().addValue(letterCount / value.length());
					}
					colIndex++;
				}
			}
		}
		finally {
			if (csvReader != null) {
				try {
					csvReader.close();
				} catch (IOException e) {
					LOG.debug("Ignoring IOException: " + e.getMessage());
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					LOG.debug("Ignoring IOException: " + e.getMessage());
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					LOG.debug("Ignoring IOException: " + e.getMessage());
				}
			}
		}
		// Dump stats
		for (Map.Entry<Integer, ColumnStats> entry : colStats.entrySet()) {
			ColumnStats colStat = entry.getValue();
			LOG.info("index=" + entry.getKey()
					+ " probaDate=" + computeDateProbability(colStat)
					+ " probaNumber=" + computeNumberProbability(colStat)
					+ " probaLabel=" + computeLabelProbability(colStat)
					+ " length=" + colStat.getLengthStat().getMean()
					+ " length dev=" + colStat.getLengthStat().getStandardDeviation()
					+ " date symbol sum=" + colStat.getDateStat().getSum()
					+ " date symbol n=" + colStat.getDateStat().getN()
					+ " number symbol min=" + colStat.getNumberStat().getMin()
					+ " number symbol n=" + colStat.getNumberStat().getN()
					+ " letter percent=" + colStat.getLetterPercentStat().getMean()
					+ " letter percent dev=" + colStat.getLetterPercentStat().getStandardDeviation()
					+ " digit percent=" + colStat.getDigitPercentStat().getMean()
					+ " digit percent dev=" + colStat.getDigitPercentStat().getStandardDeviation()
					);
		}

		// Sort by 'date probability'
		List<ColumnStats> colStatsList = new ArrayList<ColumnStats>(colStats.values());
		Collections.sort(colStatsList, new ProbabilityDateComparator());
		int firstDateColIndex = colStatsList.get(0).getIndex();
		int secondDateColIndex = colStatsList.get(1).getIndex();

		// Sort by 'number probability'
		Collections.sort(colStatsList, new ProbabilityNumberComparator());
		int firstNumberColIndex = colStatsList.get(0).getIndex();
		int secondNumberColIndex = colStatsList.get(1).getIndex();

		// Sort by 'label probability'
		Collections.sort(colStatsList, new ProbabilityLabelComparator());
		int labelColIndex = colStatsList.get(0).getIndex();

		// Guess which column is 'operation date' or 'value date' and
		// which one is 'credit', 'debit' or 'value'
		CsvFormat format = new CsvFormat(charSepartor, skipFirstLine, datePattern);
		format.setLabelIndex(labelColIndex);
		format.setValueFormat(decimalFormat);

		if (colStats.get(secondDateColIndex).getDateStat().getN() < lineCount) {
			// No date datas for second column
			format.setDateValueIndex(firstDateColIndex);
			format.setDateOperationIndex(firstDateColIndex);
		}
		else {
			// Heuristic: operation date are usually after value date
			if (colStats.get(firstDateColIndex).getDateStat().getSum() > colStats.get(secondDateColIndex).getDateStat().getSum()) {
				format.setDateOperationIndex(firstDateColIndex);
				format.setDateValueIndex(secondDateColIndex);
			}
			else {
				format.setDateOperationIndex(secondDateColIndex);
				format.setDateValueIndex(firstDateColIndex);
			}
		}
		if (colStats.get(firstNumberColIndex).getNumberStat().getMin() < 0.0) {
			// Negative values mean that credit and debit are in the same column
			format.setValueIndex(firstNumberColIndex);
			format.setCreditIndex(-1);
			format.setDebitIndex(-1);
		}
		else {
			format.setValueIndex(-1);
			// Heuristics: more values in debit column :-)
			if (colStats.get(firstNumberColIndex).getNumberStat().getN() < colStats.get(secondNumberColIndex).getNumberStat().getN()) {
				format.setCreditIndex(firstNumberColIndex);
				format.setDebitIndex(secondNumberColIndex);
			}
			else {
				format.setCreditIndex(secondNumberColIndex);
				format.setDebitIndex(firstNumberColIndex);
			}
		}
		if (format.getLabelIndex() != format.getDateOperationIndex()
				&& format.getLabelIndex() != format.getDateValueIndex()
				&& format.getLabelIndex() != format.getDebitIndex()
				&& format.getLabelIndex() != format.getCreditIndex()
				&& format.getLabelIndex() != format.getValueIndex()
				&& format.getDateOperationIndex() != format.getDebitIndex()
				&& format.getDateOperationIndex() != format.getCreditIndex()
				&& format.getDateOperationIndex() != format.getValueIndex()
				&& format.getDateValueIndex() != format.getDebitIndex()
				&& format.getDateValueIndex() != format.getCreditIndex()
				&& format.getDateValueIndex() != format.getValueIndex()
				) {
			return format;
		}
		return null;
	}

	private static double computeDateProbability(ColumnStats colStats) {
		double proba = 0.0;
		if (colStats.getLengthStat().getMean() > 3.0 && colStats.getLengthStat().getStandardDeviation() == 0.0) {
			// Fix length
			proba = 0.7;
			if (colStats.getDateStat().getN() > ANALYZE_LINE_COUNT - 1) {
				// Parsed all rows
				proba = 1.0;
			}
		}
		else if (colStats.getDigitPercentStat().getMin() >= 2.0) {
			// At least two digit
			proba = 0.3;
		}
		return proba;
	}

	private static double computeNumberProbability(ColumnStats colStats) {
		double proba = 0.0;
		if (colStats.getNumberStat().getN() > ANALYZE_LINE_COUNT - 1) {
			// Parsed all rows
			proba = 1;
		}
		else if (colStats.getDateStat().getN() == 0) {
			proba = colStats.getDigitPercentStat().getMean();
		}
		return proba;
	}

	private static double computeLabelProbability(ColumnStats colStats) {
		double proba = (2.0 - (computeDateProbability(colStats) + computeNumberProbability(colStats))) / 2;
		return proba;
	}

	private static String findDatePattern(File file, char sep) throws IOException {
		Reader reader = null;
		CSVReader csvReader = null;
		FileInputStream is = null;
		String selectedFormat = null;
		Set<String> datesString = new HashSet<String>();
		int firstDateCol = -1, secondDateCol = -1;
		try {
			is = new FileInputStream(file);
			reader = new InputStreamReader(is, CsvCollector.DEFAULT_CHARSET);
			csvReader = new CSVReader(reader, sep);
			// Skip next line
			csvReader.readNext();
			String[] nextLine = csvReader.readNext();
			// Determine in which column we have dates
			for (int i = 0; i < nextLine.length; i++) {
				if (determineDateFormat(Collections.singletonList(nextLine[i].trim())).size() > 0) {
					if (firstDateCol >= 0) {
						secondDateCol = i;
						datesString.add(nextLine[secondDateCol].trim());
						break;
					}
					else {
						firstDateCol = i;
						datesString.add(nextLine[firstDateCol].trim());
					}
				}
			}
			// Get all date strings
			while ((nextLine = csvReader.readNext()) != null) {
				if (nextLine.length == 0 || (nextLine.length==1 && nextLine[0].length() == 0)) {
					// Ignore empty line
					continue;
				}
				if (firstDateCol >= 0 && nextLine.length >= firstDateCol + 1) {
					datesString.add(nextLine[firstDateCol].trim());
					if (secondDateCol >= 0 && nextLine.length >= secondDateCol + 1) {
						datesString.add(nextLine[secondDateCol].trim());
					}
				}
			}
			// Choose first compatible format
			Set<String> formats = determineDateFormat(datesString);
			if (formats.size() > 0) {
				selectedFormat = formats.iterator().next();
			}
		}
		finally {
			if (csvReader != null) {
				try {
					csvReader.close();
				} catch (IOException e) {
					LOG.debug("Ignoring IOException: " + e.getMessage());
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					LOG.debug("Ignoring IOException: " + e.getMessage());
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					LOG.debug("Ignoring IOException: " + e.getMessage());
				}
			}
		}
		return selectedFormat;
	}

	private static DecimalFormat findDecimalFormat(File file, char sep) throws IOException {
		Reader reader = null;
		CSVReader csvReader = null;
		FileInputStream is = null;
		DecimalFormat selectedFormat = null;
		Set<String> valuesString = new HashSet<String>();
		int firstValueCol = -1, secondValueCol = -1;
		try {
			is = new FileInputStream(file);
			reader = new InputStreamReader(is, CsvCollector.DEFAULT_CHARSET);
			csvReader = new CSVReader(reader, sep);
			// Skip next line
			csvReader.readNext();
			String[] nextLine = csvReader.readNext();
			// Determine in which column we have values
			for (int i = 0; i < nextLine.length; i++) {
				if (determineValueFormat(Collections.singletonList(nextLine[i].trim())).size() > 0) {
					if (firstValueCol >= 0) {
						secondValueCol = i;
						valuesString.add(nextLine[secondValueCol].trim());
						break;
					}
					else {
						firstValueCol = i;
						valuesString.add(nextLine[firstValueCol].trim());
					}
				}
			}
			// Get all date strings
			while ((nextLine = csvReader.readNext()) != null) {
				if (nextLine.length == 0 || (nextLine.length==1 && nextLine[0].length() == 0)) {
					// Ignore empty line
					continue;
				}
				if (firstValueCol >= 0 && nextLine.length >= firstValueCol + 1) {
					valuesString.add(nextLine[firstValueCol].trim());
					if (secondValueCol >= 0 && nextLine.length >= secondValueCol + 1) {
						valuesString.add(nextLine[secondValueCol].trim());
					}
				}
			}
			// Choose first compatible format
			Set<DecimalFormat> formats = determineValueFormat(valuesString);
			if (formats.size() > 0) {
				selectedFormat = formats.iterator().next();
			}
		}
		finally {
			if (csvReader != null) {
				try {
					csvReader.close();
				} catch (IOException e) {
					LOG.debug("Ignoring IOException: " + e.getMessage());
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					LOG.debug("Ignoring IOException: " + e.getMessage());
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					LOG.debug("Ignoring IOException: " + e.getMessage());
				}
			}
		}
		return selectedFormat;
	}

	private static Character findCharSeparator(File csvFile) throws IOException {
		Map<Character, DescriptiveStatistics> stats = new HashMap<Character, DescriptiveStatistics>();
		final int mincol = 3;
		final int maxcol = 8;
		char[] separators = new char[] { ',', '\t', ';', '|' };
		for (char c : separators) {
			DescriptiveStatistics stat = new DescriptiveStatistics();
			FileInputStream is = null;
			InputStreamReader reader = null;
			BufferedReader buffer = null;
			try {
				is = new FileInputStream(csvFile);
				reader = new InputStreamReader(is, CsvCollector.DEFAULT_CHARSET);
				buffer = new BufferedReader(reader);
				int readline = 0;
				while (readline < ANALYZE_LINE_COUNT) {
					String line = buffer.readLine();
					if (line == null) {
						// No more line
						break;
					}
					readline++;

					// Count character occurence
					int count = 0;
					int lastIndex = -1;
					do {
						lastIndex = line.indexOf(c, lastIndex + 1);
						if (lastIndex >= 0) {
							count++;
						}
					} while (lastIndex >= 0);

					if (count < (mincol - 1) || count > maxcol) {
						// This cannot be this character
						break;
					}

					// Make statistics on this character
					stat.addValue(count);
				}
				stats.put(c, stat);
				LOG.debug(c + "] geomean=" + stat.getGeometricMean()
						+ " mean=" + stat.getMean()
						+ " variance=" + stat.getVariance()
						+ " deviation=" + stat.getStandardDeviation()
						+ " min=" + stat.getMin()
						+ " min=" + stat.getMin()
						+ " max=" + stat.getMax());
			} finally {
				if (buffer != null) {
					buffer.close();
				}
				if (reader != null) {
					reader.close();
				}
				if (is != null) {
					is.close();
				}
			}
		}

		// Select best character (smallest deviation)
		Character currentChar = null;
		double currentDeviation = -1;
		for (Map.Entry<Character, DescriptiveStatistics> entry : stats.entrySet()) {
			DescriptiveStatistics stat = entry.getValue();
			// Accept some format error (last column can be ommited)
			if (stat.getMean() >= (mincol-1) && stat.getMean() <= maxcol && Math.abs(stat.getStandardDeviation()) < Math.abs(currentDeviation)) {
				currentChar = entry.getKey();
				currentDeviation = stat.getStandardDeviation();
			}
		}
		return currentChar;
	}

	/**
	 * Return a set of accepted date format for this collection of date strings
	 * @param datesString List of dates string
	 * @return A set of accepted date format for this collection of date strings
	 */
	private static Set<String> determineDateFormat(Iterable<String> datesString) {
		Set<String> acceptFormats = new HashSet<String>();
		Set<String> rejectFormats = new HashSet<String>();
		ParsePosition pp = new ParsePosition(0);
		for (String dateString : datesString) {
			for (String format : DATE_FORMATS) {
				SimpleDateFormat simpleDateFormat = new SimpleDateFormat(format);
				// Don't automatically convert invalid date.
				simpleDateFormat.setLenient(false);
				pp.setErrorIndex(-1); pp.setIndex(0);
				Date date = simpleDateFormat.parse(dateString, pp);
				if (dateString.length() != pp.getIndex() || date == null) {
					// Error parsing (add to rejected and remove from accepted)
					rejectFormats.add(format);
					acceptFormats.remove(format);
				}
				else {
					if (!rejectFormats.contains(format)) {
						// Add to accepted only if not already rejected
						acceptFormats.add(format);
					}
				}
			}
		}
		return acceptFormats;
	}

	private static Set<DecimalFormat> determineValueFormat(Iterable<String> valuesString) {

		List<DecimalFormat> NUMBER_FORMATS = new ArrayList<DecimalFormat>();
		{
			DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.US);
			symb.setDecimalSeparator('.');
			NUMBER_FORMATS.add(new DecimalFormat("#.#", symb));
		}
		{
			DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.US);
			symb.setDecimalSeparator(',');
			NUMBER_FORMATS.add(new DecimalFormat("#.#", symb));
		}

		Set<DecimalFormat> acceptFormats = new HashSet<DecimalFormat>();
		Set<DecimalFormat> rejectFormats = new HashSet<DecimalFormat>();
		ParsePosition pp = new ParsePosition(0);
		for (String valueString : valuesString) {
			if (valueString.length() > 0) {
				for (DecimalFormat format : NUMBER_FORMATS) {
					pp.setErrorIndex(-1); pp.setIndex(0);
					Number value = format.parse(valueString, pp);
					if (valueString.length() != pp.getIndex() || value == null) {
						// Error parsing (add to rejected and remove from accepted)
						rejectFormats.add(format);
						acceptFormats.remove(format);
					}
					else {
						if (!rejectFormats.contains(format)) {
							// Add to accepted only if not already rejected
							acceptFormats.add(format);
						}
					}
				}
			}
		}
		return acceptFormats;
	}
}
