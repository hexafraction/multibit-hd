package org.multibit.hd.ui.views.wizards.sign_tx;

import com.google.common.base.Optional;
import org.multibit.hd.ui.views.wizards.AbstractWizard;
import org.multibit.hd.ui.views.wizards.AbstractWizardPanelView;

import java.util.Map;

/**
 * <p>Wizard to provide the following to UI for "sign message":</p>
 * <ol>
 * <li>Enter details</li>
 * </ol>
 *
 * @since 0.0.1
 *
 */
public class SignTxWizard extends AbstractWizard<SignTxWizardModel> {

  public SignTxWizard(SignTxWizardModel model, boolean isExiting) {
    super(model, isExiting, Optional.absent());
  }

  @Override
  protected void populateWizardViewMap(Map<String, AbstractWizardPanelView> wizardViewMap) {

    wizardViewMap.put(
      SignTxState.SIGN_TX_PASSWORD.name(),
      new SignTxPasswordPanelView(this, SignTxState.SIGN_TX_PASSWORD.name())
    );



  }

}
