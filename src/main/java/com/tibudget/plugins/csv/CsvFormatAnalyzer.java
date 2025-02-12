package com.tibudget.plugins.csv;

import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import java.io.*;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CsvFormatAnalyzer {

	private static final Logger LOG = Logger.getLogger(CsvFormatAnalyzer.class.getName());

	private static final int ANALYZE_LINE_COUNT = 100;

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
		CsvCollector.ColumnSeparator charSepartor = findCharSeparator(csvFile);
		if (charSepartor == null) {
			return null;
		}

		// Guess date format
		String datePattern = findDatePattern(csvFile, charSepartor.getCharacter());
		if (datePattern == null) {
			return null;
		}
		SimpleDateFormat dateFormat = new SimpleDateFormat(datePattern);

		// Guess values format
		DecimalFormat decimalFormat = findDecimalFormat(csvFile, charSepartor.getCharacter());
		if (decimalFormat == null) {
			return null;
		}

		DescriptiveStatistics colCountStats = new DescriptiveStatistics();
		Map<Integer, ColumnStats> colStats = new HashMap<Integer, ColumnStats>();
		boolean skipFirstLine = true;
		int lineCount = 0;
		CsvFileReader csvReader = null;
		try {
			csvReader = new CsvFileReader(csvFile.getAbsolutePath(), charSepartor.getCharacter());
			// Analyze first line
			String[] nextLine = csvReader.readNext();
			for (String element : nextLine) {
				if (!DateFormatUtils.determineDateFormat(Collections.singletonList(element)).isEmpty()) {
					skipFirstLine = false;
				}
			}
			// Parse next lines
			ParsePosition pp = new ParsePosition(0);
			while (lineCount < ANALYZE_LINE_COUNT && (nextLine = csvReader.readNext()) != null) {
				if (nextLine.length == 0 || (nextLine.length==1 && nextLine[0].isEmpty())) {
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
					if (!value.isEmpty()) {
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
		} finally {
			if (csvReader != null) {
				try {
					csvReader.close();
				} catch (IOException e) {
					LOG.fine("Ignoring IOException: " + e.getMessage());
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
					+ " length_avg=" + colStat.getLengthStat().getMean()
					+ " date_N=" + colStat.getDateStat().getN()
					+ " number_min=" + colStat.getNumberStat().getMin()
					+ " number_max=" + colStat.getNumberStat().getMax()
					+ " number_N=" + colStat.getNumberStat().getN()
					+ " letter_%_avg=" + colStat.getLetterPercentStat().getMean()
					+ " digit_%_avg=" + colStat.getDigitPercentStat().getMean()
					);
		}

		// Sort by 'date probability'
		List<ColumnStats> colStatsList = new ArrayList<ColumnStats>(colStats.values());
		colStatsList.sort(new ProbabilityDateComparator());
		int firstDateColIndex = colStatsList.get(0).getIndex();
		int secondDateColIndex = colStatsList.get(1).getIndex();

		// Sort by 'label probability'
		colStatsList.sort(new ProbabilityLabelComparator());
		int labelColIndex = colStatsList.get(0).getIndex();

		// Sort by 'number probability'
		List<ColumnStats> numberCols = colStatsList.stream().filter(
				columnStats -> computeNumberProbability(columnStats) > 0.5
		).sorted(
				(c1, c2) -> (int) (c2.getNumberStat().getN() - c1.getNumberStat().getN())
		).collect(Collectors.toList());
		int firstNumberColIndex = !numberCols.isEmpty() ? numberCols.get(0).getIndex() : -1;
		int secondNumberColIndex = numberCols.size() > 1 ? numberCols.get(1).getIndex() : -1;
		int thirdNumberColIndex = numberCols.size() > 2 ? numberCols.get(2).getIndex() : -1;

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
		// If a column represent the value (negative and positive) or the new amount of the account if will contains
		// ANALYZE_LINE_COUNT values and it will be sorted on first place because of probabilities.
		long n1 = firstNumberColIndex >= 0 ? colStats.get(firstNumberColIndex).getNumberStat().getN() : 0;
		long n2 = secondNumberColIndex >= 0 ? colStats.get(secondNumberColIndex).getNumberStat().getN() : 0;
		long n3 = thirdNumberColIndex >= 0 ? colStats.get(thirdNumberColIndex).getNumberStat().getN() : 0;
		if (n1 == lineCount && (n2 != lineCount || n3 != lineCount)) {
			// All lines have a value and not others so this column is the amount of the operation
			// or the new amount of the account
			format.setValueIndex(firstNumberColIndex);
			format.setCreditIndex(-1);
			format.setDebitIndex(-1);
			// If this is the new amount of the account that means credit and debit column are present; let's check
			if ((n2 + n3) == n1) {
				format.setValueIndex(-1);
				// Heuristics: more values in debit column :-)
				if (n2 < n3) {
					format.setCreditIndex(secondNumberColIndex);
					format.setDebitIndex(thirdNumberColIndex);
				}
				else {
					format.setCreditIndex(thirdNumberColIndex);
					format.setDebitIndex(secondNumberColIndex);
				}
			}
		}
		else {
			format.setValueIndex(-1);
			// Heuristics: more values in debit column :-)
			if (n1 < n2) {
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
		double lengthMean = colStats.getLengthStat().getMean();
		double digitMean = colStats.getDigitPercentStat().getMean();
		double letterMean = colStats.getLetterPercentStat().getMean();

		// Define ideal values for the parameters
		double idealLengthMin = 10; // Minimum value of the ideal range for length
		double idealLengthMax = 200; // Maximum value of the ideal range for length
		double idealDigitMean = 0.2;
		double idealLetterMean = 0.8;

		// Calculate individual probabilities
		double lengthProba = rangeGaussian(lengthMean, idealLengthMin, idealLengthMax, 4); // Handles a range for lengthMean
		double digitProba = gauss(digitMean, idealDigitMean, 0.2); // Narrow deviation for digitMean
		double letterProba = gauss(letterMean, idealLetterMean, 0.2); // Narrow deviation for letterMean

		// Combine the probabilities by multiplying them
        return lengthProba * digitProba * letterProba;
	}

	private static double gauss(double x, double mu, double sigma) {
		return Math.exp(-Math.pow(x - mu, 2) / (2 * Math.pow(sigma, 2)));
	}

	// Gaussian-like function for a range of ideal values
	public static double rangeGaussian(double x, double min, double max, double sigma) {
		if (x < min) {
			return gauss(x, min, sigma); // Penalize values below the minimum
		} else if (x > max) {
			return gauss(x, max, sigma); // Penalize values above the maximum
		} else {
			return 1.0; // Values within the range are ideal and return a probability of 1
		}
	}

	private static String findDatePattern(File file, char sep) throws IOException {
		CsvFileReader csvReader = null;
		String selectedFormat = null;
		Set<String> datesString = new HashSet<String>();
		int firstDateCol = -1, secondDateCol = -1;
		try {
			csvReader = new CsvFileReader(file.getAbsolutePath(), sep);
			// Skip next line
			csvReader.readNext();
			String[] nextLine = csvReader.readNext();
			// Determine in which column we have dates
			for (int i = 0; i < nextLine.length; i++) {
				if (DateFormatUtils.determineDateFormat(Collections.singletonList(nextLine[i].trim())).size() > 0) {
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
			Set<String> formats = DateFormatUtils.determineDateFormat(datesString);
			if (formats.size() > 0) {
				selectedFormat = formats.iterator().next();
			}
		} finally {
			if (csvReader != null) {
				try {
					csvReader.close();
				} catch (IOException e) {
					LOG.fine("Ignoring IOException: " + e.getMessage());
				}
			}
		}
		return selectedFormat;
	}

	private static DecimalFormat findDecimalFormat(File file, char sep) throws IOException {
		CsvFileReader csvReader = null;
		DecimalFormat selectedFormat = null;
		Set<String> valuesString = new HashSet<String>();
		int firstValueCol = -1, secondValueCol = -1;
		try {
			csvReader = new CsvFileReader(file.getAbsolutePath(), sep);
			// Skip next line
			csvReader.readNext();
			String[] nextLine = csvReader.readNext();
			// Determine in which column we have values
			for (int i = 0; i < nextLine.length; i++) {
				if (!ValueFormatUtils.determineValueFormat(Collections.singletonList(nextLine[i].trim())).isEmpty()) {
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
			Set<DecimalFormat> formats = ValueFormatUtils.determineValueFormat(valuesString);
			if (!formats.isEmpty()) {
				selectedFormat = formats.iterator().next();
			}
		} finally {
			if (csvReader != null) {
				try {
					csvReader.close();
				} catch (IOException e) {
					LOG.fine("Ignoring IOException: " + e.getMessage());
				}
			}
		}
		return selectedFormat;
	}

	private static CsvCollector.ColumnSeparator findCharSeparator(File csvFile) throws IOException {
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
				LOG.fine(c + "] geomean=" + stat.getGeometricMean()
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
		return CsvCollector.ColumnSeparator.fromChar(currentChar);
	}

}
