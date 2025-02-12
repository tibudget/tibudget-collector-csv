package com.tibudget.plugins.csv;

import com.tibudget.api.exceptions.CollectError;
import com.tibudget.api.exceptions.ParameterError;
import com.tibudget.dto.OperationDto;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

public class ImportFromFileTest {

	private static final Logger LOG = Logger.getLogger(ImportFromFileTest.class.getName());

	private final static int TIMEOUT = 3000;
	
	private final static int TIMEOUT_LONG = 10000;

	static class BankOperationComparator implements Comparator<OperationDto> {
		@Override
		public int compare(OperationDto o1, OperationDto o2) {
			return o1.getLabel().compareTo(o2.getLabel());
		}
	}

	@Test
	public void testImportError1() throws CollectError, ParameterError {
		assertTimeout(Duration.ofSeconds(TIMEOUT_LONG), () -> {
			assertThrows(ParameterError.class, () -> {
				File csv = new File("target/test-classes/import-comma-dv-do-l-c-d.csv");
				CsvCollector collector = new CsvCollector(csv, false, 1, 2, 3, 4, 5, -1, CsvCollector.ColumnSeparator.COMMA, true, "MM-yy-dd", "#.#", CsvCollector.DecimalSeparator.DOT);
				collector.collect(null);
			});
		});
	}

	@Test
	public void testImport1() throws CollectError, ParameterError {
		File csv = new File("target/test-classes/import-comma-dv-do-l-c-d.csv");
		DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.FRANCE);
		symb.setDecimalSeparator('.');
		CsvCollector collector = new CsvCollector(csv, false, 1, 2, 3, 4, 5, -1, CsvCollector.ColumnSeparator.COMMA, true, "yyyy-MM-dd", "#.#", CsvCollector.DecimalSeparator.DOT);
		collector.validate();
		collector.collect(null);
		// Asserts...
		checkResult((List<OperationDto>) collector.getOperations());
		// Auto
		CsvCollector autoCollector = new CsvCollector();
		autoCollector.setFile(csv);
		autoCollector.setAuto(true);
		autoCollector.validate();
		autoCollector.collect(null);
		// Asserts...
		checkResult((List<OperationDto>) autoCollector.getOperations());
	}

	@Test
	public void testImport2() throws CollectError, ParameterError {
		assertTimeout(Duration.ofSeconds(TIMEOUT_LONG), () -> {
			File csv = new File("target/test-classes/import-semi-colon-do-dv-m-l.csv");
			DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.FRANCE);
			symb.setDecimalSeparator('.');
			CsvCollector collector = new CsvCollector(csv, false, 2, 1, 4, -1, -1, 3, CsvCollector.ColumnSeparator.SEMICOLON, true, "yyyy-MM-dd", "#.#", CsvCollector.DecimalSeparator.DOT);
			collector.validate();
			collector.collect(null);
			// Asserts...
			checkResult((List<OperationDto>) collector.getOperations());
			// Auto
			CsvCollector autoCollector = new CsvCollector();
			autoCollector.setFile(csv);
			autoCollector.setAuto(true);
			autoCollector.validate();
			autoCollector.collect(null);
			// Asserts...
			checkResult((List<OperationDto>) autoCollector.getOperations());
		});
	}

	@Test
	public void testImport3() throws CollectError, ParameterError {
		assertTimeout(Duration.ofSeconds(TIMEOUT_LONG), () -> {
			File csv = new File("target/test-classes/import-tab-dv-l-c-d.csv");
			DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.FRANCE);
			symb.setDecimalSeparator('.');
			CsvCollector collector = new CsvCollector(csv, false, 1, -1, 2, 3, 4, -1, CsvCollector.ColumnSeparator.TAB, true, "yyyy-MM-dd", "#.#", CsvCollector.DecimalSeparator.DOT);
			collector.validate();
			collector.collect(null);
			// Asserts...
			checkResult((List<OperationDto>) collector.getOperations(), true);
			// Auto
			CsvCollector autoCollector = new CsvCollector();
			autoCollector.setFile(csv);
			autoCollector.setAuto(true);
			autoCollector.validate();
			autoCollector.collect(null);
			// Asserts...
			checkResult((List<OperationDto>) autoCollector.getOperations(), true);
		});
	}

	@Test
	public void testImportPerf() throws CollectError, ParameterError {
		assertTimeout(Duration.ofSeconds(TIMEOUT_LONG), () -> {
			File csv = new File("target/test-classes/import-nrow.csv");
			long start = System.currentTimeMillis();
			CsvCollector collector = new CsvCollector(csv, false, 2, 1, 3, 5, 4, -1, CsvCollector.ColumnSeparator.TAB, true, "dd/MM/yyyy", "#.#", CsvCollector.DecimalSeparator.COMMA);
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
		});
	}

	@Test
	public void testImportCic() throws CollectError, ParameterError {
		File csv = new File("target/test-classes/import-cic.csv");
		CsvCollector collector = new CsvCollector(csv);
		collector.validate();
		collector.collect(null);
		List<OperationDto> ops = (List<OperationDto>) collector.getOperations();
		assertEquals(27, ops.size());
		checkBankOperation(ops.get(0), 2024, 11, 27, 2024, 11, 27, "PRLV SEPA FREE MOBILE FMPMT-123456789", -74.91, true);
		checkBankOperation(ops.get(4), 2024, 11, 27, 2024, 11, 27, "VIR MME OU MR SMITH VIREMENT DE COMPTE MME", 40.0, true);
		checkBankOperation(ops.get(22), 2024, 11, 29, 2024, 11, 29, "FRAIS PAIE CB OP 49,99 USD", -1.72, true);
		checkBankOperation(ops.get(16), 2024, 11, 28, 2024, 11, 16, "VIR SUPTRESO 00020187402 REF 123456789123456789", 1226.92, false);
	}

	private void checkResult(List<OperationDto> entities) {
		checkResult(entities, false);
	}
	
	private void checkResult(List<OperationDto> entities, boolean dateOpEqualDateValue) {
		assertEquals(11, entities.size());
		entities.sort(new BankOperationComparator());
		
		for (OperationDto dto : entities) {
			LOG.fine(dto.getLabel() + ", " + dto.getValue() + ", " + dto.getDateValue() + ", " + dto.getDateOperation());
		}

		int i = 0;
		checkBankOperation(entities.get(i++), 2012, 2,20, 2012, 2,18, "A", -100.0, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012, 2,21, 2012, 2,19, "B", -101.1, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012, 2,22, 2012, 2,20, "C", -102.2, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012, 2,23, 2012, 2,21, "D", -103.3, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012, 2,24, 2012, 2,22, "E", -104.4, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012, 2, 1, 2012, 2, 1, "F", -10.4, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012, 3,20, 2012, 3,18, "V", 100.0, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012, 3,21, 2012, 3,19, "W", 101.1, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012, 4,22, 2012, 4,20, "X", 102.2, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012, 5,23, 2012, 5,21, "Y", 103.3, dateOpEqualDateValue);
		checkBankOperation(entities.get(i++), 2012, 6,24, 2012, 6,22, "Z", 104.4, dateOpEqualDateValue);
	}

	private void checkBankOperation(OperationDto dto, int opy, int opm, int opd, int vy, int vm, int vd, String label, Double value, boolean dateOpEqualDateValue) {
		assertEquals(label, dto.getLabel(), "label");
		assertEquals(value, dto.getValue(), "value of "+label);

		Calendar cal = Calendar.getInstance(Locale.FRANCE);
		cal.setTime(dto.getDateOperation());
		assertEquals(opy, cal.get(Calendar.YEAR), "op year of "+label);
		assertEquals(opm - 1, cal.get(Calendar.MONTH), "op month of "+label);
		assertEquals(opd, cal.get(Calendar.DATE), "op day of "+label);
		assertEquals(0, cal.get(Calendar.HOUR)+cal.get(Calendar.MINUTE)+cal.get(Calendar.SECOND)+cal.get(Calendar.MILLISECOND), "time of "+label);
		
		cal.setTime(dto.getDateValue());
		assertEquals(dateOpEqualDateValue ? opy : vy, cal.get(Calendar.YEAR), "value year of "+label);
		assertEquals(dateOpEqualDateValue ? opm - 1 : vm - 1, cal.get(Calendar.MONTH), "value month of "+label);
		assertEquals(dateOpEqualDateValue ? opd : vd, cal.get(Calendar.DATE), "value day of "+label);
		assertEquals(0, cal.get(Calendar.HOUR)+cal.get(Calendar.MINUTE)+cal.get(Calendar.SECOND)+cal.get(Calendar.MILLISECOND), "time of "+label);
	}
}
