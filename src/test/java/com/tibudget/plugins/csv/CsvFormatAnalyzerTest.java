package com.tibudget.plugins.csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import com.tibudget.api.exceptions.CollectError;

public class CsvFormatAnalyzerTest {

	private static final Duration TIMEOUT = Duration.ofSeconds(10); // Définition du timeout

	@Test
	public void testImport1() throws CollectError, IOException {
		assertTimeout(TIMEOUT, () -> {
			File csv = new File("src/test/resources/import-comma-dv-do-l-c-d.csv");
			CsvFormat guessedFormat = CsvFormatAnalyzer.findFormat(csv);
			assertNotNull(guessedFormat, "Cannot guess format");
			assertEquals(',', guessedFormat.getColSeparator(), "char separator");
			assertEquals(true, guessedFormat.isSkipFirstRow(), "skip first row");
			assertEquals(1, guessedFormat.getDateOperationIndex(), "date op index");
			assertEquals(2, guessedFormat.getDateValueIndex(), "date value index");
			assertEquals(3, guessedFormat.getLabelIndex(), "label index");
			assertEquals(4, guessedFormat.getCreditIndex(), "credit index");
			assertEquals(5, guessedFormat.getDebitIndex(), "debit index");
			assertEquals(-1, guessedFormat.getValueIndex(), "value index");
			assertEquals("yyyy-MM-dd", guessedFormat.getDateFormat(), "date format");
			assertEquals('.', guessedFormat.getValueFormat().getDecimalFormatSymbols().getDecimalSeparator(),
					"decimal separator");
		});
	}

	@Test
	public void testImport2() throws CollectError, IOException {
		assertTimeout(TIMEOUT, () -> {
			File csv = new File("src/test/resources/import-semi-colon-do-dv-m-l.csv");
			CsvFormat guessedFormat = CsvFormatAnalyzer.findFormat(csv);
			assertNotNull(guessedFormat, "Cannot guess format");
			assertEquals(';', guessedFormat.getColSeparator(), "char separator");
			assertEquals(true, guessedFormat.isSkipFirstRow(), "skip first row");
			assertEquals(2, guessedFormat.getDateOperationIndex(), "date op index");
			assertEquals(1, guessedFormat.getDateValueIndex(), "date value index");
			assertEquals(4, guessedFormat.getLabelIndex(), "label index");
			assertEquals(-1, guessedFormat.getCreditIndex(), "credit index");
			assertEquals(-1, guessedFormat.getDebitIndex(), "debit index");
			assertEquals(3, guessedFormat.getValueIndex(), "value index");
			assertEquals("yyyy-MM-dd", guessedFormat.getDateFormat(), "date format");
			assertEquals('.', guessedFormat.getValueFormat().getDecimalFormatSymbols().getDecimalSeparator(),
					"decimal separator");
		});
	}

	@Test
	public void testImport3() throws CollectError, IOException {
		assertTimeout(TIMEOUT, () -> {
			File csv = new File("src/test/resources/import-tab-dv-l-c-d.csv");
			CsvFormat guessedFormat = CsvFormatAnalyzer.findFormat(csv);
			assertNotNull(guessedFormat, "Cannot guess format");
			assertEquals('\t', guessedFormat.getColSeparator(), "char separator");
			assertEquals(true, guessedFormat.isSkipFirstRow(), "skip first row");
			assertEquals(1, guessedFormat.getDateOperationIndex(), "date op index");
			assertEquals(1, guessedFormat.getDateValueIndex(), "date value index");
			assertEquals(2, guessedFormat.getLabelIndex(), "label index");
			assertEquals(3, guessedFormat.getCreditIndex(), "credit index");
			assertEquals(4, guessedFormat.getDebitIndex(), "debit index");
			assertEquals(-1, guessedFormat.getValueIndex(), "value index");
			assertEquals("yyyy-MM-dd", guessedFormat.getDateFormat(), "date format");
			assertEquals('.', guessedFormat.getValueFormat().getDecimalFormatSymbols().getDecimalSeparator(),
					"decimal separator");
		});
	}

	@Test
	public void testImport4() throws CollectError, IOException {
		assertTimeout(TIMEOUT, () -> {
			File csv = new File("src/test/resources/import-nrow.csv");
			CsvFormat guessedFormat = CsvFormatAnalyzer.findFormat(csv);
			assertNotNull(guessedFormat, "Cannot guess format");
			assertEquals('\t', guessedFormat.getColSeparator(), "char separator");
			assertEquals(true, guessedFormat.isSkipFirstRow(), "skip first row");
			assertEquals(2, guessedFormat.getDateOperationIndex(), "date op index");
			assertEquals(1, guessedFormat.getDateValueIndex(), "date value index");
			assertEquals(3, guessedFormat.getLabelIndex(), "label index");
			assertEquals(5, guessedFormat.getCreditIndex(), "credit index");
			assertEquals(4, guessedFormat.getDebitIndex(), "debit index");
			assertEquals(-1, guessedFormat.getValueIndex(), "value index");
			assertEquals("dd/MM/yyyy", guessedFormat.getDateFormat(), "date format");
			assertEquals(',', guessedFormat.getValueFormat().getDecimalFormatSymbols().getDecimalSeparator(),
					"decimal separator");
		});
	}
}
