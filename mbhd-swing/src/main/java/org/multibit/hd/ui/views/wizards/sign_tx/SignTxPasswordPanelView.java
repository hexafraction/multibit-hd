package org.multibit.hd.ui.views.wizards.sign_tx;

import com.google.common.base.Optional;
import net.miginfocom.swing.MigLayout;
import org.bitcoinj.core.*;
import org.bitcoinj.crypto.KeyCrypterException;
import org.bitcoinj.wallet.KeyChain;
import org.multibit.hd.core.dto.CoreMessageKey;
import org.multibit.hd.core.dto.SignMessageResult;
import org.multibit.hd.core.dto.WalletSummary;
import org.multibit.hd.core.managers.WalletManager;
import org.multibit.hd.core.utils.BitcoinMessages;
import org.multibit.hd.core.utils.BitcoinNetwork;
import org.multibit.hd.ui.events.view.ViewEvents;
import org.multibit.hd.ui.languages.Languages;
import org.multibit.hd.ui.languages.MessageKey;
import org.multibit.hd.ui.utils.ClipboardUtils;
import org.multibit.hd.ui.utils.WhitespaceTrimmer;
import org.multibit.hd.ui.views.components.*;
import org.multibit.hd.ui.views.components.borders.TextBubbleBorder;
import org.multibit.hd.ui.views.components.enter_password.EnterPasswordModel;
import org.multibit.hd.ui.views.components.enter_password.EnterPasswordView;
import org.multibit.hd.ui.views.components.panels.PanelDecorator;
import org.multibit.hd.ui.views.components.text_fields.FormattedBitcoinAddressField;
import org.multibit.hd.ui.views.fonts.AwesomeIcon;
import org.multibit.hd.ui.views.themes.Themes;
import org.multibit.hd.ui.views.wizards.AbstractWizard;
import org.multibit.hd.ui.views.wizards.AbstractWizardPanelView;
import org.multibit.hd.ui.views.wizards.WizardButton;
import org.spongycastle.crypto.params.KeyParameter;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * <p>Wizard to provide the following to UI:</p>
 * <ul>
 * <li>Sign message: Enter details and password</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class SignTxPasswordPanelView extends AbstractWizardPanelView<SignTxWizardModel, String> {

    // Labels
    JLabel signingAddressLabel;
    JLabel messageLabel;
    JLabel signatureLabel;

    FormattedBitcoinAddressField signingAddress;
    JTextArea signature;
    JTextArea txnEntryArea;

    JLabel reportLabel;

    // Panel specific components
    private ModelAndView<EnterPasswordModel, EnterPasswordView> enterPasswordMaV;

    /**
     * @param wizard    The wizard managing the states
     * @param panelName The panel name to allow event filtering
     */
    public SignTxPasswordPanelView(AbstractWizard<SignTxWizardModel> wizard, String panelName) {

        super(wizard, panelName, AwesomeIcon.PENCIL, MessageKey.SIGN_MESSAGE_TITLE);

    }

    @Override
    public void newPanelModel() {

        enterPasswordMaV = Components.newEnterPasswordMaV(getPanelName());
        setPanelModel("");
    }

    @Override
    public void initialiseContent(JPanel contentPanel) {

        contentPanel.setLayout(new MigLayout(
                Panels.migXYLayout(),
                "[][][][]", // Column constraints
                "[][80][][30][30][20]" // Row constraints
        ));

        // Labels (also used in clipboard)
        signingAddressLabel = Labels.newBitcoinAddress();
        messageLabel = new JLabel("Raw transaction:");
        signatureLabel = new JLabel("Signed transaction");

        signingAddress = TextBoxes.newEnterBitcoinAddress(getWizardModel(), false);

        // Suggest the current receiving address for signing
        WalletSummary currentWalletSummary = WalletManager.INSTANCE.getCurrentWalletSummary().get();
        ECKey newKey = currentWalletSummary.getWallet().currentKey(KeyChain.KeyPurpose.RECEIVE_FUNDS);
        String address = newKey.toAddress(BitcoinNetwork.current().get()).toString();
        signingAddress.setText(address);

        txnEntryArea = TextBoxes.newEnterRawTxn();

        // The message is a wall of text so needs scroll bars in many cases
        txnEntryArea.setBorder(null);

        // Message requires its own scroll pane
        JScrollPane messageScrollPane = new JScrollPane();
        messageScrollPane.setOpaque(true);
        messageScrollPane.setBackground(Themes.currentTheme.dataEntryBackground());
        messageScrollPane.setBorder(null);
        messageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        messageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // View port requires special handling
        messageScrollPane.setViewportView(txnEntryArea);
        messageScrollPane.getViewport().setBackground(Themes.currentTheme.dataEntryBackground());
        messageScrollPane.setViewportBorder(new TextBubbleBorder(Themes.currentTheme.dataEntryBorder()));

        // Ensure we maintain the overall theme
        ScrollBarUIDecorator.apply(messageScrollPane, true);

        signature = TextBoxes.newEnterRawTxn();
        signature.setEditable(false);
        AccessibilityDecorator.apply(signature, MessageKey.SIGNATURE, MessageKey.SIGNATURE_TOOLTIP);

        // Add them to the panel
        contentPanel.add(signingAddressLabel);
        contentPanel.add(signingAddress, "growx,span 3,push,wrap");

        contentPanel.add(messageLabel);
        contentPanel.add(messageScrollPane, "grow,span 3,push,wrap");

        contentPanel.add(enterPasswordMaV.getView().newComponentPanel(), "growx,span 3,wrap");

        contentPanel.add(Buttons.newSignMessageButton(getSignMessageAction()), "cell 1 3,align right");
        contentPanel.add(Buttons.newCopyAllButton(getCopyClipboardAction()), "cell 2 3");
        contentPanel.add(Buttons.newClearAllButton(getClearAllAction()), "cell 3 3,wrap");

        contentPanel.add(signatureLabel);
        contentPanel.add(signature, "grow,span 3,push,wrap");

        reportLabel = Labels.newStatusLabel(Optional.<MessageKey>absent(), null, Optional.<Boolean>absent());
        AccessibilityDecorator.apply(reportLabel, MessageKey.NOTES);
        contentPanel.add(reportLabel, "growx,span 4");
    }

    @Override
    protected void initialiseButtons(AbstractWizard<SignTxWizardModel> wizard) {
        PanelDecorator.addFinish(this, wizard);
    }

    @Override
    public void fireInitialStateViewEvents() {
        // Apply button starts off enabled
        ViewEvents.fireWizardButtonEnabledEvent(getPanelName(), WizardButton.APPLY, true);
    }

    @Override
    public void afterShow() {
        txnEntryArea.requestFocusInWindow();
    }

    @Override
    public boolean beforeHide(boolean isExitCancel) {

        if (!isExitCancel) {

            // Ensure the wizard model correctly reflects the contents of the components
            updateFromComponentModels(Optional.absent());

        }

        // Must be OK to proceed
        return true;
    }

    @Override
    public void updateFromComponentModels(Optional componentModel) {

        // Update models from component values
    }


    /**
     * @return A new action for signing the message
     */
    private Action getSignMessageAction() {

        // Sign the message
        return new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {

                signMessage();

            }

        };
    }

    /**
     * @return A new action for clearing the signing address, message text and signature
     */
    private Action getClearAllAction() {

        // Clear the fields and set focus
        return new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                signingAddress.setText("");
                txnEntryArea.setText("");
                enterPasswordMaV.getModel().setPassword("".toCharArray());
                enterPasswordMaV.getModel().setValue("");
                enterPasswordMaV.getView().updateViewFromModel();

                // Clear the credentials on the UI as update view from model does not work
                Component passwordField = enterPasswordMaV.getView().currentComponentPanel().getComponent(1);
                if (passwordField instanceof JPasswordField) {
                    ((JPasswordField) passwordField).setText("");
                }
                signature.setText("");
                reportLabel.setText("");
                reportLabel.setIcon(null);

                // Reset focus
                signingAddress.requestFocusInWindow();
            }
        };
    }

    /**
     * Sign the message text with the address specified and update UI
     */
    private void signMessage() {
        try {
            String addressText = WhitespaceTrimmer.trim(signingAddress.getText());
            String txnHex = txnEntryArea.getText();
            String walletPassword = enterPasswordMaV.getModel().getValue();

            try {
                Address signingAddress = new Address(BitcoinNetwork.current().get(), addressText);

                Optional<WalletSummary> walletSummaryOptional = WalletManager.INSTANCE.getCurrentWalletSummary();

                if (walletSummaryOptional.isPresent()) {
                    WalletSummary walletSummary = walletSummaryOptional.get();

                    Wallet wallet = walletSummary.getWallet();

                    ECKey signingKey = wallet.findKeyFromPubHash(signingAddress.getHash160());
                    if (signingKey != null) {
                        if (signingKey.getKeyCrypter() != null) {
                            KeyParameter aesKey = signingKey.getKeyCrypter().deriveKey(walletPassword);
                            Wallet.SendRequest req = Wallet.SendRequest
                                    .forTx(new Transaction(BitcoinNetwork.current().get(), Utils.HEX.decode(txnHex)));
                            req.aesKey = aesKey;
                            wallet.signTransaction(req);
                            String hexSigned = Utils.HEX.encode(req.tx.unsafeBitcoinSerialize());
                            signature.setText(hexSigned);
                            if(txnHex.equals(hexSigned)) signature.setBackground(Color.YELLOW);
                            else signature.setBackground(Color.GREEN);
                        } else {
                            // The signing key is not encrypted but it should be
                            throw new RuntimeException("Signing key not encrypted");
                        }
                    } else {
                        // No signing key found.
                        throw new RuntimeException("Signing key not found");
                    }
                } else {
                    throw new RuntimeException("No wallet");
                }
            } catch (KeyCrypterException | AddressFormatException e) {
                throw new RuntimeException(e);
            }
        } catch (Exception e) {
            signature.setText("Error: "+e.getClass().getName() + "\n"+e.getMessage());
        }
    }

    /**
     * @return A new action for copying the view contents to the clipboard
     */
    private Action getCopyClipboardAction() {

        return new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {

                String clipboard = BitcoinMessages.formatAsBitcoinSignedMessage(
                        signingAddress.getText(),
                        txnEntryArea.getText(),
                        signature.getText()
                );

                // Copy the image to the clipboard
                ClipboardUtils.copyStringToClipboard(clipboard);

            }

        };
    }

}
