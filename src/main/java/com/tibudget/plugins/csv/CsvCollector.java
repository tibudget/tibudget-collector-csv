package com.tibudget.plugins.csv;

import com.tibudget.api.CollectorPlugin;
import com.tibudget.api.Input;
import com.tibudget.api.OTPProvider;
import com.tibudget.api.exceptions.CollectError;
import com.tibudget.api.exceptions.MessagesException;
import com.tibudget.api.exceptions.ParameterError;
import com.tibudget.dto.AccountDto;
import com.tibudget.dto.MessageDto;
import com.tibudget.dto.OperationDto;
import com.tibudget.dto.OperationDto.OperationDtoType;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.*;
import java.util.*;
import java.util.logging.Logger;

import static java.lang.Math.abs;

public class CsvCollector implements CollectorPlugin {

	private static final Logger LOG = Logger.getLogger(CsvCollector.class.getName());
	
	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	/**
	 * Enum representing common CSV column separators.
	 */
	public enum ColumnSeparator {
		COMMA(','),         // Comma ","
		SEMICOLON(';'),     // Semicolon ";"
		TAB('\t'),          // Tab character "\t"
		SPACE(' '),         // Space " "
		PIPE('|');          // Pipe "|"

		private final char character;

		/**
		 * Constructor for CsvSeparator.
		 * @param character The character used as a separator.
		 */
		ColumnSeparator(char character) {
			this.character = character;
		}

		/**
		 * Gets the separator character.
		 * @return The separator character.
		 */
		public char getCharacter() {
			return character;
		}

		/**
		 * Finds a CsvSeparator from a given character.
		 * @param separator The character to match.
		 * @return The corresponding CsvSeparator.
		 * @throws IllegalArgumentException if the character is not a known separator.
		 */
		public static ColumnSeparator fromChar(char separator) {
			for (ColumnSeparator columnSeparator : values()) {
				if (columnSeparator.character == separator) {
					return columnSeparator;
				}
			}
			return null;
		}
	}

	/**
	 * Enum representing common decimal separators in numeric values.
	 */
	public enum DecimalSeparator {
		DOT('.'),        // Dot "."
		COMMA(',');      // Comma ","

		private final char character;

		/**
		 * Constructor for DecimalSeparator.
		 * @param character The character used as a decimal separator.
		 */
		DecimalSeparator(char character) {
			this.character = character;
		}

		/**
		 * Gets the decimal separator character.
		 * @return The decimal separator character.
		 */
		public char getCharacter() {
			return character;
		}

		/**
		 * Finds a DecimalSeparator from a given character.
		 * @param separator The character to match.
		 * @return The corresponding DecimalSeparator.
		 * @throws IllegalArgumentException if the character is not a known decimal separator.
		 */
		public static DecimalSeparator fromChar(char separator) {
			for (DecimalSeparator decimalSeparator : values()) {
				if (decimalSeparator.character == separator) {
					return decimalSeparator;
				}
			}
			return null;
		}
	}

	// messages: <pluginid>_fr.properties + file.label= + file.help=

	@Input(fieldset="fs1", hideFieldset="fsmanual", order=2, required=false)
	private boolean auto = true;

	@Input(fieldset="fs1", order=0, required=false)
	private AccountDto account;

	@Input(fieldset="fs1", order=1)
	private File file;

	@Input(fieldset="fsmanual", order=5)
	private int dateOperationIndex = -1;

	@Input(fieldset="fsmanual", order=6, required=false)
	private int dateValueIndex = -1;

	@Input(fieldset="fsmanual", order=7)
	private int labelIndex = -1;

	@Input(fieldset="fsmanual", order=9, required=false)
	private int creditIndex = -1;

	@Input(fieldset="fsmanual", order=10, required=false)
	private int debitIndex = -1;

	@Input(fieldset="fsmanual", order=8)
	private int valueIndex = -1;

	@Input(fieldset="fsmanual", order=11)
	private String dateFormat = null;

	@Input(fieldset="fsmanual", order=13)
	private DecimalSeparator decimalSeparator = DecimalSeparator.DOT;

	@Input(fieldset="fsmanual", order=12)
	private String numberFormat = "#.#";

	@Input(fieldset="fsmanual", order=3, required=false)
	private boolean skipFirstRow = true;

	@Input(fieldset="fsmanual", order=4)
	private ColumnSeparator colSeparator = ColumnSeparator.COMMA;

	private List<OperationDto> operationsDtos;

	private int progress = 0;

	public CsvCollector() {
		super();
	}

	public CsvCollector(File file) {
		super();
		this.file = file;
	}

	public CsvCollector(File file, boolean auto, int dateOperationIndex,
			int dateValueIndex, int labelIndex, int creditIndex,
			int debitIndex, int valueIndex, ColumnSeparator colSeparator,
			boolean skipFirstRow, String dateFormat, String numberFormat, DecimalSeparator decimalSeparator) {
		this(file);
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
	public void collect(Iterable<AccountDto> lastCollect) throws CollectError, ParameterError {

		this.progress = 0;

		if (isAuto()) {
			initAuto();
		}

		this.operationsDtos = new ArrayList<>();
		int lineCount = CsvCollector.getLineCount(this.file);
		int count = 0;
		CsvFileReader csvReader = null;
		try {
			SimpleDateFormat fmt = new SimpleDateFormat(getDateFormat());
			csvReader = new CsvFileReader(this.file.getAbsolutePath(), getColSeparator().getCharacter());

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
				if (nextLine.length == 0 || (nextLine.length==1 && nextLine[0].isEmpty())) {
					// Ignore empty line
					continue;
				}
				try {
					// Date value
					Date dateValue;
					try {
						String dateStr = nextLine[getDateValueIndex() - 1].trim();
						dateValue = fmt.parse(dateStr);
						if (!fmt.format(dateValue).equals(dateStr)) {
							if (auto) {
								throw new CollectError("collect.error.date", dateStr, dateValue);
							} else {
								throw new ParameterError("dateFormat", "form.error.dateFormat.parsing", dateStr, dateValue);
							}
						}
					} catch (ParseException e) {
						if (this.auto) {
							throw new CollectError("collect.error.date.parse.auto", nextLine[getDateValueIndex() - 1]);
						} else {
							throw new ParameterError("dateFormat", "form.error.dateFormat.parsing2", nextLine[getDateValueIndex() - 1]);
						}
					}
					// Date operation
					Date dateOperation;
					try {
						dateOperation = fmt.parse(nextLine[getDateOperationIndex() - 1]);
					} catch (ParseException e) {
						if (this.auto) {
							throw new CollectError("collect.error.date.parse.auto", nextLine[getDateValueIndex() - 1]);
						} else {
							throw new ParameterError("dateFormat", "form.error.dateFormat.parsing2", nextLine[getDateValueIndex() - 1]);
						}
					}
					// Label
					String label = nextLine[getLabelIndex() - 1].trim();
					// Value
					Double value;
					if (getValueIndex() > 0) {
						String valueStr = nextLine[getValueIndex() - 1].trim();
						value = parseNumber(valueStr);
					} else {
						double credit = 0.0, debit = 0.0;
						if (getCreditIndex() <= nextLine.length) {
							String creditStr = nextLine[getCreditIndex() - 1].trim();
							if (!creditStr.isEmpty()) {
								credit = parseNumber(creditStr);
							}
						}
						if (getDebitIndex() <= nextLine.length) {
							String debitStr = nextLine[getDebitIndex() - 1].trim();
							if (!debitStr.isEmpty()) {
								// Yes, some files contains negative values in the debit column so I prefer take the
								// absolute value
								debit = abs(parseNumber(debitStr));
							}
						}
						value = credit - debit;
					}

					// Create operation
					OperationDto op = new OperationDto(
							this.account.getUuid(),
							"",
							OperationDtoType.PAYMENT,
							dateOperation,
							dateValue,
							label,
							"",
							value
					);
					this.operationsDtos.add(op);

					// Balance will always be correct
					this.account.setCurrentBalance(this.account.getCurrentBalance() + op.getValue());

				} catch (MessagesException e) {
					long lineNumber = count + 1;
					if (isSkipFirstRow()) {
						lineNumber++;
					}
					LOG.info("Ignored line #" + lineNumber + ": "+String.join(String.valueOf(getColSeparator()), nextLine)+"(" + e.getMessage() + ")");
					throw e;
				}

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
		} finally {
			if (csvReader != null) {
				try {
					csvReader.close();
				} catch (IOException e) {
					LOG.fine("Ignoring IOException: " + e.getMessage());
				}
			}
			this.progress = 100;
		}
	}

	private double parseNumber(String numberStr) throws ParameterError, CollectError {
		Double value;
		if (getNumberFormat() != null) {
			DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.US);
			symb.setDecimalSeparator(getDecimalSeparator().getCharacter());
			NumberFormat numberFormat = new DecimalFormat(getNumberFormat(), symb);
			ParsePosition pp = new ParsePosition(0);
			value = numberFormat.parse(numberStr, pp).doubleValue();
			if (numberStr.length() != pp.getIndex()) {
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
			setDecimalSeparator(DecimalSeparator.fromChar(format.getValueFormat().getDecimalFormatSymbols().getDecimalSeparator()));
			setNumberFormat(format.getValueFormat().toPattern());
		} catch (IOException e) {
			throw new ParameterError("auto", "collect.error.auto", e);
		}
	}

	@Override
	public List<AccountDto> getAccounts() {
		return Collections.singletonList(this.account);
	}

	@Override
	public List<OperationDto> getOperations() {
		return this.operationsDtos;
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

	public void setColSeparator(ColumnSeparator colSeparator) {
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

	public ColumnSeparator getColSeparator() {
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

	public DecimalSeparator getDecimalSeparator() {
		return this.decimalSeparator;
	}

	public void setDecimalSeparator(DecimalSeparator decimalSeparator) {
		this.decimalSeparator = decimalSeparator;
	}

	@Override
	public void setOTPProvider(OTPProvider otpProvider) {
		// Not needed here
	}

	@Override
	public void setCookies(Map<String, String> map) {
		// Not needed here
	}

	@Override
	public Map<String, String> getCookies() {
		return Map.of();
	}

	@Override
	public List<MessageDto> validate() {
		List<MessageDto> msg = new ArrayList<>();
		if (this.account == null) {
			this.account = new AccountDto(UUID.randomUUID().toString(), AccountDto.AccountDtoType.PAYMENT, "CSV account", "Import CSV", Currency.getInstance(Locale.getDefault()).getCurrencyCode(), 0.0);
		}
		if (this.file == null) {
			// Do not check file existance since platform is storing files in
			// a restricted area
			msg.add(new MessageDto("file", "form.error.file.null"));
		}
		if (!this.auto) {
			if (this.colSeparator == null) {
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
			if (this.dateFormat == null || this.dateFormat.trim().isEmpty()) {
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
			if (this.decimalSeparator == null) {
				msg.add(new MessageDto("decimalSeparator", "form.error.decimalSeparator.null"));
			}
			else if (this.numberFormat == null || this.numberFormat.trim().isEmpty()) {
				msg.add(new MessageDto("decimalSeparator", "form.error.numberFormat.null"));
			}
			else {
				try {
					DecimalFormatSymbols symb = new DecimalFormatSymbols(Locale.US);
					symb.setDecimalSeparator(getDecimalSeparator().getCharacter());
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
		} catch (IOException e) {
			count = -1;
		}
		finally {
			if (buffer != null) {
				try {
					buffer.close();
				} catch (IOException e) {
					LOG.fine("Ignoring IOException: " + e.getMessage());
				}
			}
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					LOG.fine("Ignoring IOException: " + e.getMessage());
				}
			}
			if (is != null) {
				try {
					is.close();
				} catch (IOException e) {
					LOG.fine("Ignoring IOException: " + e.getMessage());
				}
			}
		}
		return count;
	}
}
