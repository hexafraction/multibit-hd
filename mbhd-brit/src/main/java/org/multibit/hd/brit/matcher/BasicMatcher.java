package org.multibit.hd.brit.matcher;

import com.google.common.base.Optional;
import org.multibit.hd.brit.crypto.AESUtils;
import org.multibit.hd.brit.crypto.PGPUtils;
import org.multibit.hd.brit.dto.*;
import org.spongycastle.crypto.params.KeyParameter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

/**
 *  <p>Class to provide the following to BRIT:<br>
 *  <ul>
 *  <li>ability to match redeemers and payers</li>
 *  </ul>
 * <p/>
 *  </p>
 *  
 */
public class BasicMatcher implements Matcher {

  private MatcherConfig matcherConfig;

  /**
   * The last payerRequest received.
   * (Having a single last payerRequest won't work in a multithreaded environment)
   */
  private PayerRequest lastPayerRequest;

  /**
   * The matcher store containing all the bitcoin address information
   */
  private MatcherStore matcherStore;


  public BasicMatcher(MatcherConfig matcherConfig) {
    this.matcherConfig = matcherConfig;

    // Create a new matcher store and populate it
    matcherStore = MatcherStores.newBasicMatcherStore(matcherConfig.getMatcherStoreLocation());
  }

  @Override
  public MatcherConfig getConfig() {
    return matcherConfig;
  }

  @Override
  public PayerRequest decryptPayerRequest(EncryptedPayerRequest encryptedPayerRequest) throws Exception {

    ByteArrayInputStream serialisedPayerRequestEncryptedInputStream = new ByteArrayInputStream(encryptedPayerRequest.getPayload());

    ByteArrayOutputStream serialisedPayerRequestOutputStream = new ByteArrayOutputStream(1024);

    // PGP encrypt the file
    PGPUtils.decryptFile(serialisedPayerRequestEncryptedInputStream, serialisedPayerRequestOutputStream,
            new FileInputStream(matcherConfig.getMatcherSecretKeyringFile()), matcherConfig.getPassword());


    return PayerRequest.parse(serialisedPayerRequestOutputStream.toByteArray());
  }

  @Override
  public MatcherResponse process(PayerRequest payerRequest) {
    lastPayerRequest = payerRequest;

    WalletToEncounterDateLink previousEncounter = matcherStore.lookupWalletToEncounterDateLink(payerRequest.getBRITWalletId());

    // The replay date is the earliest of:
    // + the payerRequest.firstTreatmentDate in the PayerRequest (if available)
    // + the firstTransactionDate in the previousEncounter (which would have been supplied in the past for this wallet)
    // + the previousEncounterDate (the first time this wallet was seen by the Matcher
    // BRITWalletId
    Date replayDate = new Date();   // If this is a brand new wallet, never used or seen before by the matcher you can replay from now.
    if (previousEncounter != null && previousEncounter.getEncounterDateOptional().isPresent()) {
      replayDate = previousEncounter.getEncounterDateOptional().get();
    }
    if (previousEncounter != null && previousEncounter.getFirstTransactionDate().isPresent()) {
      replayDate = previousEncounter.getFirstTransactionDate().get().before(replayDate) ? previousEncounter.getFirstTransactionDate().get() : replayDate;
    }
    if (payerRequest.getFirstTransactionDate().isPresent()) {
      replayDate = payerRequest.getFirstTransactionDate().get().before(replayDate) ? payerRequest.getFirstTransactionDate().get() : replayDate;
    }

    // TODO update record if replay date coming in is earleir than the one on the existing record (or if it is absent)
    
    // If the previousEncounter was null then store this encounter
    if (previousEncounter == null) {
      WalletToEncounterDateLink thisEncounter = new WalletToEncounterDateLink(payerRequest.getBRITWalletId(), Optional.of(new Date()), Optional.of(replayDate));
      matcherStore.storeWalletToEncounterDateLink(thisEncounter);
    }
    // Lookup the current valid set of Bitcoin addresses to return to the payer
    List<String> currentBitcoinAddressList = matcherStore.getBitcoinAddressListForDate(new Date());
    return new MatcherResponse(replayDate, currentBitcoinAddressList);
  }

  @Override
  public EncryptedMatcherResponse encryptMatcherResponse(MatcherResponse matcherResponse) throws NoSuchAlgorithmException {
    // Stretch the 20 byte britWalletId to 32 bytes (256 bits)
    byte[] stretchedBritWalletId = MessageDigest.getInstance("SHA-256").digest(lastPayerRequest.getBRITWalletId().getBytes());

    // Create an AES key from the stretchedBritWalletId and the sessionKey and decrypt the payload
    byte[] encryptedMatcherResponsePayload = AESUtils.encrypt(matcherResponse.serialise(), new KeyParameter(stretchedBritWalletId), lastPayerRequest.getSessionKey());

    return new EncryptedMatcherResponse(encryptedMatcherResponsePayload);
  }

}