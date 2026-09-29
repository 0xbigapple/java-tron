package org.tron.common.utils;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;

import com.google.protobuf.ByteString;
import java.util.Arrays;
import java.util.Collections;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;
import org.tron.common.parameter.CommonParameter;

public class WalletUtilTest {

  private String cryptoEngine;

  @Before
  public void setUp() {
    cryptoEngine = CommonParameter.getInstance().getCryptoEngine();
    CommonParameter.getInstance().setCryptoEngine("ECKey");
  }

  @After
  public void tearDown() {
    CommonParameter.getInstance().setCryptoEngine(cryptoEngine);
  }

  @Test
  public void preservesExistingEncodingsAndDoesNotModifyInput() {
    int[] sizes = {0, 1, 20, 21};
    String[] expected = {"3QJmnh", "8M5JoSg", "6x36TdFVhCQBxJz1MzEoySdejiLcwZMed",
        "TFvF6WzyeRKMM8NcbddxL88mZXTeuUWA8Z"};
    for (int i = 0; i < sizes.length; i++) {
      byte[] input = bytes(sizes[i]);
      byte[] original = input.clone();
      assertEquals(expected[i], StringUtil.encode58Check(input));
      assertEquals(expected[i], WalletUtil.getAddressString(input));
      assertArrayEquals(original, input);
    }
  }

  @Test
  public void formatsOversizedInputsWithoutHashingOrBase58Conversion() {
    for (int size : new int[]{22, 4096, 16384, 65536}) {
      byte[] input = bytes(size);
      try (MockedStatic<Sha256Hash> hash = mockStatic(Sha256Hash.class);
           MockedStatic<Base58> base58 = mockStatic(Base58.class)) {
        String expected = "invalid-address(41414141...,len=" + size + ")";
        assertEquals(expected, WalletUtil.getAddressString(input));
        assertEquals(expected, WalletUtil.getAddressString(ByteString.copyFrom(input)));
        hash.verifyNoInteractions();
        base58.verifyNoInteractions();
      }
    }
  }

  @Test
  public void preservesOrdinaryWitnessLists() {
    ByteString address = ByteString.copyFrom(bytes(21));
    assertEquals(Collections.nCopies(27, StringUtil.encode58Check(address.toByteArray())),
        WalletUtil.getAddressStringList(Collections.nCopies(27, address)));
    assertTrue(WalletUtil.getAddressStringList(Collections.emptyList()).isEmpty());
  }

  private static byte[] bytes(int size) {
    byte[] input = new byte[size];
    Arrays.fill(input, (byte) 0x41);
    return input;
  }
}
