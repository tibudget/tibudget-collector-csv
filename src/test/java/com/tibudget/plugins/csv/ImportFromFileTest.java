package com.tibudget.plugins.csv;

import java.io.File;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import junit.framework.Assert;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tibudget.api.exceptions.CollectError;
import com.tibudget.api.exceptions.ParameterError;
import com.tibudget.dto.BankOperationDto;

public class ImportFromFileTest {

	private static Logger LOG = LoggerFactory.getLogger(ImportFromFileTest.class);
	
	private final static int TIMEOUT = 3000;
	
	private final static int TIMEOUT_LONG = 10000;

	static class BankOperationComparator implements Comparator<BankOperationDto> {
		@Override
		public int compare(BankOperationDto o1, BankOperationDto o2) {
			return o1.getLabel().compareTo(o2.getLabel());
		}
	}

	@Test(expected = ParameterError.class, timeout=TIMEOUT)
	public void testImportError1() throws CollectError, ParameterError {
		File csv = new File("target/test-classes/import-comma-dv-do-l-c-d.csv");
		CsvCollector collector = new CsvCollector(csv, false, 1, 2, 3, 4, 5, -1, ',', true, "MM-yy-dd", "#.#", '.');
		collector.collect(null);
	}

	@Test//(timeout=TIMEOUT)
	public void testImport1() throws CollectError, ParameterError {
		File csv = new File("target/test-classes/import-comma-dv-do-l-c-d.csv");
		DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.FRANCE);
		symb.setDecimalSeparator('.');
		CsvCollector collector = new CsvCollector(csv, false, 1, 2, 3, 4, 5, -1, ',', true, "yyyy-MM-dd", "#.#", '.');
		collector.validate();
		collector.collect(null);
		// Asserts...
		checkResult((List<BankOperationDto>) collector.getBankOperations());
		// Auto
		CsvCollector autoCollector = new CsvCollector();
		autoCollector.setFile(csv);
		autoCollector.setAuto(true);
		autoCollector.validate();
		autoCollector.collect(null);
		// Asserts...
		checkResult((List<BankOperationDto>) autoCollector.getBankOperations());
	}

	@Test(timeout=TIMEOUT)
	public void testImport2() throws CollectError, ParameterError {
		File csv = new File("target/test-classes/import-semi-colon-do-dv-m-l.csv");
		DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.FRANCE);
		symb.setDecimalSeparator('.');
		CsvCollector collector = new CsvCollector(csv, false, 2, 1, 4, -1, -1, 3, ';', true, "yyyy-MM-dd", "#.#", '.');
		collector.validate();
		collector.collect(null);
		// Asserts...
		checkResult((List<BankOperationDto>) collector.getBankOperations());
		// Auto
		CsvCollector autoCollector = new CsvCollector();
		autoCollector.setFile(csv);
		autoCollector.setAuto(true);
		autoCollector.validate();
		autoCollector.collect(null);
		// Asserts...
		checkResult((List<BankOperationDto>) autoCollector.getBankOperations());
	}

	@Test(timeout=TIMEOUT)
	public void testImport3() throws CollectError, ParameterError {
		File csv = new File("target/test-classes/import-tab-dv-l-c-d.csv");
		DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.FRANCE);
		symb.setDecimalSeparator('.');
		CsvCollector collector = new CsvCollector(csv, false, 1, -1, 2, 3, 4, -1, '\t', true, "yyyy-MM-dd", "#.#", '.');
		collector.validate();
		collector.collect(null);
		// Asserts...
		checkResult((List<BankOperationDto>) collector.getBankOperations(), true);
		// Auto
		CsvCollector autoCollector = new CsvCollector();
		autoCollector.setFile(csv);
		autoCollector.setAuto(true);
		autoCollector.validate();
		autoCollector.collect(null);
		// Asserts...
		checkResult((List<BankOperationDto>) autoCollector.getBankOperations(), true);
	}

	@Test(timeout=TIMEOUT_LONG)
	public void testImportPerf() throws CollectError, ParameterError {
		File csv = new File("target/test-classes/import-nrow.csv");
		long start = System.currentTimeMillis();
		CsvCollector collector = new CsvCollector(csv, false, 2, 1, 3, 5, 4, -1, '\t', true, "dd/MM/yyyy", "#.#", ',');
		collector.validate();
		collector.collect(null);
		long duration = System.currentTimeMillis() - start;
		System.out.println("Duration: " + duration);
		// Auto
		CsvCollector autoCollector = new CsvCollector();
		autoCollector.setFile(csv);
		autoCollector.setAuto(true);
		autoCollector.validate();
		autoCollector.collect(null);
	}

	private void checkResult(List<BankOperationDto> entities) {
		checkResult(entities, false);
	}
	
	private void checkResult(List<BankOperationDto> entities, boolean dateOpEqualDateValue) {
		Assert.assertEquals(11, entities.size());
		Collections.sort(entities, new BankOperationComparator());
		
		for (BankOperationDto dto : entities) {
			LOG.debug(dto.getLabel() + ", " + dto.getValue() + ", " + dto.getDateValue() + ", " + dto.getDateOperation());
		}

		int i = 0;
		checkBankOperation(entities.get(i++), 2012,02,20, 2012,02,18, "A", -100.0, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012,02,21, 2012,02,19, "B", -101.1, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012,02,22, 2012,02,20, "C", -102.2, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012,02,23, 2012,02,21, "D", -103.3, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012,02,24, 2012,02,22, "E", -104.4, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012,02,01, 2012,02,01, "F", -10.4, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012,03,20, 2012,03,18, "V", 100.0, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012,03,21, 2012,03,19, "W", 101.1, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012,04,22, 2012,04,20, "X", 102.2, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012,05,23, 2012,05,21, "Y", 103.3, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012,06,24, 2012,06,22, "Z", 104.4, dateOpEqualDateValue);
	}

	private void checkBankOperation(BankOperationDto dto, int opy, int opm, int opd, int vy, int vm, int vd, String label, Double value, boolean dateOpEqualDateValue) {
		Assert.assertEquals("label", label, dto.getLabel());
		Assert.assertEquals("value of "+label, value, dto.getValue());

		Calendar cal = Calendar.getInstance(Locale.FRANCE);
		cal.setTime(dto.getDateOperation());
		Assert.assertEquals("op year of "+label, opy, cal.get(Calendar.YEAR));
		Assert.assertEquals("op month of "+label, opm - 1, cal.get(Calendar.MONTH));
		Assert.assertEquals("op day of "+label, opd, cal.get(Calendar.DATE));
		Assert.assertEquals("time of "+label, 0, cal.get(Calendar.HOUR)+cal.get(Calendar.MINUTE)+cal.get(Calendar.SECOND)+cal.get(Calendar.MILLISECOND));
		
		cal.setTime(dto.getDateValue());
		Assert.assertEquals("value year of "+label, dateOpEqualDateValue ? opy : vy, cal.get(Calendar.YEAR));
		Assert.assertEquals("value month of "+label, dateOpEqualDateValue ? opm - 1 : vm - 1, cal.get(Calendar.MONTH));
		Assert.assertEquals("value day of "+label, dateOpEqualDateValue ? opd : vd, cal.get(Calendar.DATE));
		Assert.assertEquals("time of "+label, 0, cal.get(Calendar.HOUR)+cal.get(Calendar.MINUTE)+cal.get(Calendar.SECOND)+cal.get(Calendar.MILLISECOND));
	}
}
