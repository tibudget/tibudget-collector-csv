package com.tibudget.plugins.csv;

import java.text.DecimalFormat;

public class CsvFormat {

    int dateOperationIndex = -1;

    int dateValueIndex = -1;

    int labelIndex = -1;

    int creditIndex = -1;

    int debitIndex = -1;

    int valueIndex = -1;

    CsvCollector.ColumnSeparator colSeparator = CsvCollector.ColumnSeparator.COMMA;

    boolean skipFirstRow = true;

    String dateFormat;

    DecimalFormat valueFormat;

    public CsvFormat(CsvCollector.ColumnSeparator colSeparator, boolean skipFirstRow, String dateFormat) {
        super();
        this.colSeparator = colSeparator;
        this.skipFirstRow = skipFirstRow;
        this.dateFormat = dateFormat;
    }

    public CsvFormat(int dateOperationIndex, int dateValueIndex, int labelIndex, int creditIndex, int debitIndex, int valueIndex, CsvCollector.ColumnSeparator colSeparator,
                     boolean skipFirstRow, String dateFormat, DecimalFormat valueFormat) {
        super();
        this.dateOperationIndex = dateOperationIndex;
        this.dateValueIndex = dateValueIndex;
        this.labelIndex = labelIndex;
        this.creditIndex = creditIndex;
        this.debitIndex = debitIndex;
        this.valueIndex = valueIndex;
        this.colSeparator = colSeparator;
        this.skipFirstRow = skipFirstRow;
        this.dateFormat = dateFormat;
        this.valueFormat = valueFormat;
    }

    public int getDateOperationIndex() {
        return dateOperationIndex;
    }

    public void setDateOperationIndex(int dateOperationIndex) {
        this.dateOperationIndex = dateOperationIndex;
    }

    public int getDateValueIndex() {
        return dateValueIndex;
    }

    public void setDateValueIndex(int dateValueIndex) {
        this.dateValueIndex = dateValueIndex;
    }

    public int getLabelIndex() {
        return labelIndex;
    }

    public void setLabelIndex(int labelIndex) {
        this.labelIndex = labelIndex;
    }

    public int getCreditIndex() {
        return creditIndex;
    }

    public void setCreditIndex(int creditIndex) {
        this.creditIndex = creditIndex;
    }

    public int getDebitIndex() {
        return debitIndex;
    }

    public void setDebitIndex(int debitIndex) {
        this.debitIndex = debitIndex;
    }

    public int getValueIndex() {
        return valueIndex;
    }

    public void setValueIndex(int valueIndex) {
        this.valueIndex = valueIndex;
    }

    public CsvCollector.ColumnSeparator getColSeparator() {
        return colSeparator;
    }

    public void setColSeparator(CsvCollector.ColumnSeparator colSeparator) {
        this.colSeparator = colSeparator;
    }

    public boolean isSkipFirstRow() {
        return skipFirstRow;
    }

    public void setSkipFirstRow(boolean skipFirstRow) {
        this.skipFirstRow = skipFirstRow;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public DecimalFormat getValueFormat() {
        return valueFormat;
    }

    public void setValueFormat(DecimalFormat valueFormat) {
        this.valueFormat = valueFormat;
    }
}
