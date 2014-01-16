package com.tibudget.plugins.csv;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;

import org.junit.Test;

import com.tibudget.api.exceptions.CollectError;
import com.tibudget.plugins.csv.CsvFormat;
import com.tibudget.plugins.csv.CsvFormatAnalyzer;

public class CsvFormatAnalyzerTest {
	
	private final static int TIMEOUT = 10000000;
	
	@Test(timeout=TIMEOUT)
	public void testImport1() throws CollectError, IOException {
		File csv = new File("src/test/resources/import-comma-dv-do-l-c-d.csv");
		CsvFormat guessedFormat = CsvFormatAnalyzer.findFormat(csv);
		assertNotNull("Cannot guess format", guessedFormat);
		assertEquals("char separator", ',', guessedFormat.getColSeparator());
		assertEquals("skip first row", true, guessedFormat.isSkipFirstRow());
		assertEquals("date op index", 1, guessedFormat.getDateOperationIndex());
		assertEquals("date value index", 2, guessedFormat.getDateValueIndex());
		assertEquals("label index", 3, guessedFormat.getLabelIndex());
		assertEquals("credit index", 4, guessedFormat.getCreditIndex());
		assertEquals("debit index", 5, guessedFormat.getDebitIndex());
		assertEquals("value index", -1, guessedFormat.getValueIndex());
		assertEquals("date format", "yyyy-MM-dd", guessedFormat.getDateFormat());
		assertEquals("decimal separator", '.', guessedFormat.getValueFormat().getDecimalFormatSymbols().getDecimalSeparator());
	}

	@Test(timeout=TIMEOUT)
	public void testImport2() throws CollectError, IOException {
		File csv = new File("src/test/resources/import-semi-colon-do-dv-m-l.csv");
		CsvFormat guessedFormat = CsvFormatAnalyzer.findFormat(csv);
		assertNotNull("Cannot guess format", guessedFormat);
		assertEquals("char separator", ';', guessedFormat.getColSeparator());
		assertEquals("skip first row", true, guessedFormat.isSkipFirstRow());
		assertEquals("date op index", 2, guessedFormat.getDateOperationIndex());
		assertEquals("date value index", 1, guessedFormat.getDateValueIndex());
		assertEquals("label index", 4, guessedFormat.getLabelIndex());
		assertEquals("credit index", -1, guessedFormat.getCreditIndex());
		assertEquals("debit index", -1, guessedFormat.getDebitIndex());
		assertEquals("value index", 3, guessedFormat.getValueIndex());
		assertEquals("date format", "yyyy-MM-dd", guessedFormat.getDateFormat());
		assertEquals("decimal separator", '.', guessedFormat.getValueFormat().getDecimalFormatSymbols().getDecimalSeparator());
	}

	@Test(timeout=TIMEOUT)
	public void testImport3() throws CollectError, IOException {
		File csv = new File("src/test/resources/import-tab-dv-l-c-d.csv");
		CsvFormat guessedFormat = CsvFormatAnalyzer.findFormat(csv);
		assertNotNull("Cannot guess format", guessedFormat);
		assertEquals("char separator", '\t', guessedFormat.getColSeparator());
		assertEquals("skip first row", true, guessedFormat.isSkipFirstRow());
		assertEquals("date op index", 1, guessedFormat.getDateOperationIndex());
		assertEquals("date value index", 1, guessedFormat.getDateValueIndex());
		assertEquals("label index", 2, guessedFormat.getLabelIndex());
		assertEquals("credit index", 3, guessedFormat.getCreditIndex());
		assertEquals("debit index", 4, guessedFormat.getDebitIndex());
		assertEquals("value index", -1, guessedFormat.getValueIndex());
		assertEquals("date format", "yyyy-MM-dd", guessedFormat.getDateFormat());
		assertEquals("decimal separator", '.', guessedFormat.getValueFormat().getDecimalFormatSymbols().getDecimalSeparator());
	}

	@Test(timeout=TIMEOUT)
	public void testImport4() throws CollectError, IOException {
		File csv = new File("src/test/resources/import-nrow.csv");
		CsvFormat guessedFormat = CsvFormatAnalyzer.findFormat(csv);
		assertNotNull("Cannot guess format", guessedFormat);
		assertEquals("char separator", '\t', guessedFormat.getColSeparator());
		assertEquals("skip first row", true, guessedFormat.isSkipFirstRow());
		assertEquals("date op index", 2, guessedFormat.getDateOperationIndex());
		assertEquals("date value index", 1, guessedFormat.getDateValueIndex());
		assertEquals("label index", 3, guessedFormat.getLabelIndex());
		assertEquals("credit index", 5, guessedFormat.getCreditIndex());
		assertEquals("debit index", 4, guessedFormat.getDebitIndex());
		assertEquals("value index", -1, guessedFormat.getValueIndex());
		assertEquals("date format", "dd/MM/yyyy", guessedFormat.getDateFormat());
		assertEquals("decimal separator", ',', guessedFormat.getValueFormat().getDecimalFormatSymbols().getDecimalSeparator());
	}

}
