package org.multibit.hd.ui.views.wizards.sign_tx;

import org.bitcoinj.core.*;
import org.multibit.hd.ui.views.wizards.AbstractHardwareWalletWizardModel;
import org.multibit.hd.ui.views.wizards.AbstractWizardModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * <p>Model object to provide the following to "sign txHex" wizard:</p>
 * <ul>
 * <li>Storage of panel data</li>
 * <li>State transition management</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class SignTxWizardModel extends AbstractWizardModel<SignTxState> {

  private static final Logger log = LoggerFactory.getLogger(SignTxWizardModel.class);

  private Address signingAddress = null;
  private String txHex;
  private byte[] signature;

  /**
   * @param state The state object
   */
  public SignTxWizardModel(SignTxState state) {
    super(state);
  }

  @Override
  public void showNext() {

    switch (state) {
      case SIGN_TX_PASSWORD:
        break;
      default:
        // Do nothing
    }
  }

  @Override
  public void showPrevious() {
    switch (state) {
      case SIGN_TX_PASSWORD:
        break;
      default:
        throw new IllegalStateException("Unexpected state:" + state);
    }
  }



  /**
   * @return The txHex
   */
  public String getTxHex() {
    return txHex;
  }

  public void setTxHex(String message) {
    this.txHex = message;
  }

  /**
   * @return The signing address
   */
  public Address getSigningAddress() {
    return signingAddress;
  }

  public void setSigningAddress(Address signingAddress) {
    this.signingAddress = signingAddress;
  }

  /**
   * @return The signature
   */
  public byte[] getSignature() {
    return Arrays.copyOf(signature, signature.length);
  }

  public void setSignature(byte[] signature) {
    this.signature = Arrays.copyOf(signature, signature.length);
  }




}
