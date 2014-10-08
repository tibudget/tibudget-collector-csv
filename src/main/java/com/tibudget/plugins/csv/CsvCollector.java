package com.tibudget.plugins.csv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import au.com.bytecode.opencsv.CSVReader;

import com.tibudget.api.ICollectorPlugin;
import com.tibudget.api.Input;
import com.tibudget.api.exceptions.CollectError;
import com.tibudget.api.exceptions.ParameterError;
import com.tibudget.dto.BankAccountDto;
import com.tibudget.dto.BankOperationDto;
import com.tibudget.dto.BankOperationDto.Type;
import com.tibudget.dto.MessageDto;

public class CsvCollector implements ICollectorPlugin {

	private static Logger LOG = LoggerFactory.getLogger(CsvCollector.class);
	
	public static final Charset DEFAULT_CHARSET = Charset.forName("utf-8");

	// messages: <pluginid>_fr.properties + file.label= + file.help=

	@Input(fieldset="fs1", hideFieldset="fsmanual", order=2, required=true)
	private boolean auto = true;

	@Input(fieldset="fs1", order=0, required=false)
	private BankAccountDto bankAccount;

	@Input(fieldset="fs1", order=1, required=true)
	private File file;

	@Input(fieldset="fsmanual", order=5, required=true)
	private int dateOperationIndex = -1;

	@Input(fieldset="fsmanual", order=6, required=false)
	private int dateValueIndex = -1;

	@Input(fieldset="fsmanual", order=7, required=true)
	private int labelIndex = -1;

	@Input(fieldset="fsmanual", order=9, required=false)
	private int creditIndex = -1;

	@Input(fieldset="fsmanual", order=10, required=false)
	private int debitIndex = -1;

	@Input(fieldset="fsmanual", order=8, required=true)
	private int valueIndex = -1;

	@Input(fieldset="fsmanual", order=11, required=true)
	private String dateFormat = null;

	@Input(fieldset="fsmanual", order=13, required=true)
	private char decimalSeparator = '.';

	@Input(fieldset="fsmanual", order=12, required=true)
	private String numberFormat = "#.#";

	@Input(fieldset="fsmanual", order=3, required=true)
	private boolean skipFirstRow = true;

	@Input(fieldset="fsmanual", order=4, required=true)
	private char colSeparator = ',';

	private List<BankOperationDto> bankOperationsDtos;

	private int progress = 0;

	public CsvCollector() {
		super();
	}

	public CsvCollector(File file, boolean auto, int dateOperationIndex,
			int dateValueIndex, int labelIndex, int creditIndex,
			int debitIndex, int valueIndex, char colSeparator,
			boolean skipFirstRow, String dateFormat, String numberFormat, char decimalSeparator) {
		super();
		this.file = file;
		this.auto = auto;
		this.dateOperationIndex = dateOperationIndex;
		this.dateValueIndex = dateValueIndex;
		this.labelIndex = labelIndex;
		this.creditIndex = creditIndex;
		this.debitIndex = debitIndex;
		this.valueIndex = valueIndex;
		this.colSeparator = colSeparator;
		this.skipFirstRow = skipFirstRow;
		this.dateFormat = dateFormat;
		this.numberFormat = numberFormat;
		this.decimalSeparator = decimalSeparator;
	}

	/**
	 * {@inheritDoc}
	 * @param lastCollect Will be ignored here
	 * @throws CollectError
	 * @throws ParameterError
	 */
	@Override
	public void collect(Iterable<BankAccountDto> bankAccounts) throws CollectError, ParameterError {

		this.progress = 0;

		if (isAuto()) {
			initAuto();
		}

		this.bankOperationsDtos = new ArrayList<BankOperationDto>();
		int lineCount = CsvCollector.getLineCount(this.file);
		int count = 0;
		Reader reader = null;
		CSVReader csvReader = null;
		FileInputStream is = null;
		try {
			SimpleDateFormat fmt = new SimpleDateFormat(getDateFormat());
			is = new FileInputStream(this.file);
			reader = new InputStreamReader(is, DEFAULT_CHARSET);
			csvReader = new CSVReader(reader, getColSeparator());
			String [] nextLine;
			if (isSkipFirstRow()) {
				csvReader.readNext();
			}
			if (getDateOperationIndex() <= 0) {
				setDateOperationIndex(getDateValueIndex());
			}
			if (getDateValueIndex() <= 0) {
				setDateValueIndex(getDateOperationIndex());
			}
			while ((nextLine = csvReader.readNext()) != null) {
				if (nextLine.length == 0 || (nextLine.length==1 && nextLine[0].length() == 0)) {
					// Ignore empty line
					continue;
				}
				// Date value
				Date dateValue = null;
				try {
					String dateStr = nextLine[getDateValueIndex() - 1].trim();
					dateValue = fmt.parse(dateStr);
					if (!fmt.format(dateValue).equals(dateStr)) {
						if (auto) {
							throw new CollectError("collect.error.date", dateStr, dateValue);
						}
						else {
							throw new ParameterError("dateFormat", "form.error.dateFormat.parsing", dateStr, dateValue);
						}
					}
				} catch (ParseException e) {
					if (this.auto) {
						throw new CollectError("collect.error.date.parse.auto", nextLine[getDateValueIndex() - 1]);
					}
					else {
						throw new ParameterError("dateFormat", "form.error.dateFormat.parsing2", nextLine[getDateValueIndex() - 1]);
					}
				}
				// Date operation
				Date dateOperation = null;
				try {
					dateOperation = fmt.parse(nextLine[getDateOperationIndex() - 1]);
				} catch (ParseException e) {
					if (this.auto) {
						throw new CollectError("collect.error.date.parse.auto", nextLine[getDateValueIndex() - 1]);
					}
					else {
						throw new ParameterError("dateFormat", "form.error.dateFormat.parsing2", nextLine[getDateValueIndex() - 1]);
					}
				}
				// Label
				String label = nextLine[getLabelIndex() - 1].trim();
				// Value
				Double value = null;
				if (getValueIndex() > 0) {
					String valueStr = nextLine[getValueIndex() - 1].trim();
					value = parseNumber(valueStr);
				}
				else {
					Double credit = 0.0, debit = 0.0;
					if (getCreditIndex() <= nextLine.length) {
						String creditStr = nextLine[getCreditIndex() - 1].trim();
						if (creditStr.length() > 0) {
							credit = parseNumber(creditStr);
						}
					}
					if (getDebitIndex() <= nextLine.length) {
						String debitStr = nextLine[getDebitIndex() - 1].trim();
						if (debitStr.length() > 0) {
							debit = parseNumber(debitStr);
						}
					}
					value = credit - debit;
				}

				// Create operation
				BankOperationDto op = new BankOperationDto(Type.OTHER, dateValue, dateOperation, label, value);
				op.setAccountId(this.bankAccount.getTitle());
				this.bankOperationsDtos.add(op);
				
				// Balance will always be correct
				this.bankAccount.setCurrentBalance(this.bankAccount.getCurrentBalance() + op.getValue());

				// update progress
				count++;
				if (lineCount > 0) {
					this.progress = count / lineCount;
				}
			}

		}catch (FileNotFoundException e) {
			throw new CollectError("collect.error.filenotfound", e);
		} catch (IOException e) {
			if (this.auto) {
				throw new CollectError("collect.error.generic.auto", e);
			}
			else {
				throw new CollectError("collect.error.generic", e);
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
			this.progress = 100;
		}
	}

	private double parseNumber(String numberStr) throws ParameterError, CollectError {
		Double value = null;
		if (getNumberFormat() != null) {
			DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.US);
			symb.setDecimalSeparator(getDecimalSeparator());
			NumberFormat numberFormat = new DecimalFormat(getNumberFormat(), symb);
			ParsePosition pp = new ParsePosition(0);
			value = numberFormat.parse(numberStr, pp).doubleValue();
			if (numberStr.length() != pp.getIndex() || value == null) {
				if (auto) {
					throw new ParameterError("numberFormat", "form.error.numberFormat.parsing", value, numberStr);
				}
				else {
					throw new CollectError("collect.error.number", value, numberStr);
				}
			}
		}
		else {
			value = Double.parseDouble(numberStr);
		}
		return value;
	}

	private void initAuto() throws ParameterError {
		try {
			CsvFormat format = CsvFormatAnalyzer.findFormat(getFile());
			if (format == null) {
				throw new ParameterError("auto", "collect.error.auto");
			}
			setColSeparator(format.getColSeparator());
			setSkipFirstRow(format.isSkipFirstRow());
			setDateOperationIndex(format.getDateOperationIndex());
			setDateValueIndex(format.getDateValueIndex());
			setCreditIndex(format.getCreditIndex());
			setLabelIndex(format.getLabelIndex());
			setDebitIndex(format.getDebitIndex());
			setValueIndex(format.getValueIndex());
			setDateFormat(format.getDateFormat());
			setDecimalSeparator(format.getValueFormat().getDecimalFormatSymbols().getDecimalSeparator());
			setNumberFormat(format.getValueFormat().toPattern());
		} catch (IOException e) {
			throw new ParameterError("auto", "collect.error.auto", e);
		}
	}

	@Override
	public Iterable<BankAccountDto> getBankAccounts() {
		return Collections.singleton(this.bankAccount);
	}

	@Override
	public Iterable<BankOperationDto> getBankOperations() {
		return this.bankOperationsDtos;
	}

	public void setFile(File file) {
		this.file = file;
	}

	public void setDateOperationIndex(int dateOperationIndex) {
		this.dateOperationIndex = dateOperationIndex;
	}

	public void setDateValueIndex(int dateValueIndex) {
		this.dateValueIndex = dateValueIndex;
	}

	public void setLabelIndex(int labelIndex) {
		this.labelIndex = labelIndex;
	}

	public void setCreditIndex(int creditIndex) {
		this.creditIndex = creditIndex;
	}

	public void setDebitIndex(int debitIndex) {
		this.debitIndex = debitIndex;
	}

	public void setValueIndex(int valueIndex) {
		this.valueIndex = valueIndex;
	}

	public void setColSeparator(char colSeparator) {
		this.colSeparator = colSeparator;
	}

	public void setSkipFirstRow(boolean skipFirstRow) {
		this.skipFirstRow = skipFirstRow;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public File getFile() {
		return this.file;
	}

	public int getDateOperationIndex() {
		return this.dateOperationIndex;
	}

	public int getDateValueIndex() {
		return this.dateValueIndex;
	}

	public int getLabelIndex() {
		return this.labelIndex;
	}

	public int getCreditIndex() {
		return this.creditIndex;
	}

	public int getDebitIndex() {
		return this.debitIndex;
	}

	public int getValueIndex() {
		return this.valueIndex;
	}

	public char getColSeparator() {
		return this.colSeparator;
	}

	public boolean isSkipFirstRow() {
		return this.skipFirstRow;
	}

	public String getDateFormat() {
		return this.dateFormat;
	}

	public boolean isAuto() {
		return this.auto;
	}

	public void setAuto(boolean auto) {
		this.auto = auto;
	}

	public String getNumberFormat() {
		return this.numberFormat;
	}

	public void setNumberFormat(String numberFormat) {
		this.numberFormat = numberFormat;
	}

	public char getDecimalSeparator() {
		return this.decimalSeparator;
	}

	public void setDecimalSeparator(char decimalSeparator) {
		this.decimalSeparator = decimalSeparator;
	}

	@Override
	public Collection<MessageDto> validate() {
		List<MessageDto> msg = new ArrayList<MessageDto>();
		if (this.bankAccount == null) {
			this.bankAccount = new BankAccountDto(UUID.randomUUID().toString(), com.tibudget.dto.BankAccountDto.Type.OTHER, "CSV account", "Import CSV", 0.0);
		}
		if (this.file == null) {
			// Do not check file existance since platform is storing files in
			// a restricted area
			msg.add(new MessageDto("file", "form.error.file.null"));
		}
		if (!this.auto) {
			if (this.colSeparator == Character.MIN_VALUE) {
				msg.add(new MessageDto("colSeparator", "form.error.colSeparator.null"));
			}
			if (this.labelIndex < 0) {
				msg.add(new MessageDto("labelIndex", "form.error.labelIndex.null"));
			}
			if (this.valueIndex < 0 && (this.creditIndex < 0 || this.debitIndex < 0)) {
				msg.add(new MessageDto("valueIndex", "form.error.valueIndex.null"));
			}
			if (this.dateOperationIndex < 0 && this.dateValueIndex < 0) {
				msg.add(new MessageDto("dateOperationIndex", "form.error.dateOperationIndex.null"));
			}
			if (this.labelIndex > 0 && (this.labelIndex == this.valueIndex || this.labelIndex == this.creditIndex || this.labelIndex == this.debitIndex || this.labelIndex == this.dateOperationIndex || this.labelIndex == this.dateValueIndex)) {
				msg.add(new MessageDto("labelIndex", "form.error.labelIndex.alreadyused"));
			}
			if (this.valueIndex > 0 && (this.valueIndex == this.dateOperationIndex || this.valueIndex == this.dateValueIndex)) {
				msg.add(new MessageDto("valueIndex", "form.error.valueIndex.alreadyused"));
			}
			if (this.creditIndex > 0 && (this.creditIndex == this.dateOperationIndex || this.creditIndex == this.dateValueIndex)) {
				msg.add(new MessageDto("creditIndex", "form.error.creditIndex.alreadyused"));
			}
			if (this.debitIndex > 0 && (this.debitIndex == this.dateOperationIndex || this.debitIndex == this.dateValueIndex)) {
				msg.add(new MessageDto("debitIndex", "form.error.debitIndex.alreadyused"));
			}
			if (this.dateFormat == null || this.dateFormat.trim().length() == 0) {
				msg.add(new MessageDto("dateFormat", "form.error.dateFormat.null"));
			}
			else {
				try {
					new SimpleDateFormat(this.dateFormat);
				}
				catch (IllegalArgumentException e) {
					msg.add(new MessageDto("dateFormat", "form.error.dateFormat.invalid", this.dateFormat));
				}
			}
			if (this.decimalSeparator == Character.MIN_VALUE) {
				msg.add(new MessageDto("decimalSeparator", "form.error.decimalSeparator.null"));
			}
			else if (this.numberFormat == null || this.numberFormat.trim().length() == 0) {
				msg.add(new MessageDto("decimalSeparator", "form.error.numberFormat.null"));
			}
			else {
				try {
					DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.US);
					symb.setDecimalSeparator(getDecimalSeparator());
					new DecimalFormat(getNumberFormat(), symb);
				}
				catch (IllegalArgumentException e) {
					msg.add(new MessageDto("numberFormat", "form.error.numberFormat.invalid", this.numberFormat, this.decimalSeparator));
				}
			}
		}
		return msg;
	}

	@Override
	public int getProgress() {
		return this.progress;
	}

	private static int getLineCount(File file) {
		int count = 0;
		FileInputStream is = null;
		InputStreamReader reader = null;
		BufferedReader buffer = null;
		try {
			is = new FileInputStream(file);
			reader = new InputStreamReader(is, CsvCollector.DEFAULT_CHARSET);
			buffer = new BufferedReader(reader);
			while (buffer.readLine() != null) {
				count++;
			}
		} catch (FileNotFoundException e) {
			count = -1;
		} catch (IOException e) {
			count = -1;
		}
		finally {
			if (buffer != null) {
				try {
					buffer.close();
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
		return count;
	}

	public void setBankAccount(BankAccountDto bankAccount) {
		this.bankAccount = bankAccount;
	}
}
