package org.multibit.hd.ui.views.components.tables;

import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.multibit.hd.core.config.Configurations;
import org.multibit.hd.core.dto.FiatPayment;
import org.multibit.hd.core.dto.PaymentData;
import org.multibit.hd.core.dto.RAGStatus;
import org.multibit.hd.ui.languages.Languages;
import org.multibit.hd.ui.languages.MessageKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.table.AbstractTableModel;
import java.util.List;
import java.util.Set;

/**
 * <p>TableModel to provide the following to contact JTable:</p>
 * <ul>
 * <li>Adapts a list of payments into a table model</li>
 * </ul>
 *
 * @since 0.0.1
 *
 */
public class PaymentTableModel extends AbstractTableModel {

  public static final int DATE_COLUMN_INDEX = 0;
  public static final int STATUS_COLUMN_INDEX = 1;
  public static final int TYPE_COLUMN_INDEX = 2;
  public static final int DESCRIPTION_COLUMN_INDEX = 3;
  public static final int AMOUNT_BTC_COLUMN_INDEX = 4;
  public static final int AMOUNT_FIAT_COLUMN_INDEX = 5;

  private static final Logger log = LoggerFactory.getLogger(PaymentTableModel.class);

  private String[] columnNames = {
          Languages.safeText(MessageKey.DATE),
          Languages.safeText(MessageKey.STATUS),
          Languages.safeText(MessageKey.TYPE),
          Languages.safeText(MessageKey.DESCRIPTION),
          Languages.safeText(MessageKey.LOCAL_AMOUNT) + " ", // BTC symbol added later
          Languages.safeText(MessageKey.LOCAL_AMOUNT) + " " + Configurations.currentConfiguration.getBitcoin().getLocalCurrencySymbol()
  };

  private Object[][] data;

  private List<PaymentData> paymentDataList;

  public PaymentTableModel(Set<PaymentData> paymentDataList) {
    setPaymentData(paymentDataList, false);
  }

  /**
   * Set the payment data into the table
   *
   * @param paymentData The paymentData to show in the table as a Set
   */
  public void setPaymentData(Set<PaymentData> paymentData, boolean fireTableDataChanged) {
    this.setPaymentData(Lists.newArrayList(paymentData), fireTableDataChanged);
  }

  /**
   * Set the payment data into the table
   *
   * @param paymentData The paymentData to show in the table as a List
   */
  public void setPaymentData(List<PaymentData> paymentData, boolean fireTableDataChanged) {
    this.paymentDataList = paymentData;

    data = new Object[paymentData.size()][];

    int row = 0;
    for (PaymentData payment : paymentData) {

      Object[] rowData = new Object[]{
              payment.getDate(),
              payment.getStatus(),
              payment.getType(),
              payment.getDescription(),
              payment.getAmountCoin(),
              payment.getAmountFiat()
      };

      data[row] = rowData;

      row++;
    }
    if (fireTableDataChanged) {
      fireTableDataChanged();
    }
  }

  public int getColumnCount() {
    return columnNames.length;
  }

  public int getRowCount() {
    return data.length;
  }

  public String getColumnName(int col) {
    return columnNames[col];
  }

  public Object getValueAt(int row, int col) {
    if (data.length == 0) {
      return "";
    }
    try {
      return data[row][col];
    } catch (NullPointerException npe) {
      log.error("NullPointerException reading row = " + row + ", column = " + col);
      return "";
    }
  }

  /**
   * JTable uses this method to determine the default renderer/
   * editor for each cell.  If we didn't implement this method,
   * then the last column would contain text ("true"/"false"),
   * rather than a check box.
   */
  public Class getColumnClass(int c) {
    switch (c) {
      case DATE_COLUMN_INDEX : return DateTime.class;
      case STATUS_COLUMN_INDEX : return RAGStatus.class;
      case TYPE_COLUMN_INDEX : return String.class;
      case DESCRIPTION_COLUMN_INDEX : return String.class;
      case AMOUNT_BTC_COLUMN_INDEX : return String.class;
      case AMOUNT_FIAT_COLUMN_INDEX : return FiatPayment.class;
      default: return String.class;
     }
  }

  /**
   * Handle changes to the data
   */
  public void setValueAt(Object value, int row, int col) {
    // No table updates allowed
  }

  public List<PaymentData> getPaymentDataList() {
    return paymentDataList;
  }

}
