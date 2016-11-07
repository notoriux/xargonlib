package it.xargon.streams;

import it.xargon.util.*;
import it.xargon.util.Debug.Printable;

import java.io.*;
import java.security.*;
import java.security.spec.*;

import javax.crypto.*;
import javax.crypto.spec.*;


public class EncryptionModel implements Streamable, Printable {
   public enum Default {LOW_SEC, MID_SEC, HIGH_SEC}
   
   private String ikeyPairAlgorithm=null;
   private Integer ikeyPairSize=null;
   private String iasyCipherAlgorithm=null;
   private String isecretKeyAlgorithm=null;
   private Integer isecretKeySize=null;
   private String isymCipherAlgorithm=null;
   
   private KeyFactory keyPairFactory=null;
   private KeyPairGenerator keyPairGenerator=null;
   private SecretKeyFactory keyFactory=null;
   private KeyGenerator keyGenerator=null;
   
   private boolean sealed=false;
   
   public EncryptionModel() {reset();}
   
   public static EncryptionModel getDefaultModel(Default def) {
      EncryptionModel result=new EncryptionModel();
      
      try {
         switch (def) {
            case LOW_SEC:
               result.setKeyPairAlgorithm("RSA");
               result.setKeyPairSize(512);
               result.setAsyCipherAlgorithm("RSA/ECB/PKCS1PADDING");
               result.setSecretKeyAlgorithm("DES");
               result.setSecretKeySize(0);
               result.setSymCipherAlgorithm("DES/CFB8/NOPADDING");
               break;
            case MID_SEC:
               result.setKeyPairAlgorithm("RSA");
               result.setKeyPairSize(1024);
               result.setAsyCipherAlgorithm("RSA/ECB/PKCS1PADDING");
               result.setSecretKeyAlgorithm("DESede");
               result.setSecretKeySize(112);
               result.setSymCipherAlgorithm("DESede/CFB8/NOPADDING");
               break;
            case HIGH_SEC:
               result.setKeyPairAlgorithm("RSA");
               result.setKeyPairSize(2048);
               result.setAsyCipherAlgorithm("RSA/ECB/PKCS1PADDING");
               result.setSecretKeyAlgorithm("DESede");
               result.setSecretKeySize(168);
               result.setSymCipherAlgorithm("DESede/CFB8/NOPADDING");
               break;
         }
      } catch (NoSuchAlgorithmException ex) {
         result=null;
      }
      
      result.seal();      
      return result;
   }
      
   public void marshal(OutputStream os) throws IOException {
      checkComplete();
      @SuppressWarnings("resource")
      ChecksumOutputStream cos=new ChecksumOutputStream(os);
      byte[] cache=null;
      
      cache=Bitwise.stringToByteArray(ikeyPairAlgorithm);
      cos.write(cache.length); cos.write(cache);
      
      cos.write(Bitwise.intToByteArray(ikeyPairSize.intValue()));
      
      cache=Bitwise.stringToByteArray(iasyCipherAlgorithm);
      cos.write(cache.length); cos.write(cache); 
      
      cache=Bitwise.stringToByteArray(isecretKeyAlgorithm);
      cos.write(cache.length); cos.write(cache);
      
      cos.write(Bitwise.intToByteArray(isecretKeySize.intValue()));
      
      cache=Bitwise.stringToByteArray(isymCipherAlgorithm);;
      cos.write(cache.length); cos.write(cache);
      
      cos.write((sealed)?1:0);
      
      cos.writeChecksum();
      
      cos.flush();
   }
   
   public static EncryptionModel unmarshal(InputStream is) throws IOException {
      EncryptionModel result=new EncryptionModel();
      SimpleChecksum chk=new SimpleChecksum();
      int len=0;
      byte[] cache=new byte[4];
      
      try {
         len=is.read(); chk.feed(len);
         cache=new byte[len]; is.read(cache); chk.feed(cache);
         result.setKeyPairAlgorithm(Bitwise.byteArrayToString(cache));
         
         cache=new byte[4]; is.read(cache); chk.feed(cache);
         result.setKeyPairSize(Bitwise.byteArrayToInt(cache));
         
         len=is.read(); chk.feed(len);
         cache=new byte[len]; is.read(cache); chk.feed(cache);
         result.setAsyCipherAlgorithm(Bitwise.byteArrayToString(cache));
         
         len=is.read(); chk.feed(len);
         cache=new byte[len]; is.read(cache); chk.feed(cache);
         result.setSecretKeyAlgorithm(Bitwise.byteArrayToString(cache));
         
         cache=new byte[4]; is.read(cache); chk.feed(cache);
         result.setSecretKeySize(Bitwise.byteArrayToInt(cache));
         
         len=is.read(); chk.feed(len);
         cache=new byte[len]; is.read(cache); chk.feed(cache);
         result.setSymCipherAlgorithm(Bitwise.byteArrayToString(cache));

         len=is.read(); chk.feed(len);
         if (len==1) result.seal();
         
         len=is.read();
         if (len!=chk.get()) throw new IOException("Checksum error");
      } catch (NoSuchAlgorithmException ex) {
         IOException ioex=new IOException();
         ioex.initCause(ex);
         throw ioex;
      }
      
      return result;
   }

   public String toString(String indent) {
      StringBuilder sb=new StringBuilder();
      sb.append(indent).append("KeyPair: ").append(ikeyPairAlgorithm);
      sb.append(" (").append(ikeyPairSize.intValue()).append(" bits)\n");
      sb.append(indent).append("Asymetric Cipher: ").append(iasyCipherAlgorithm).append("\n");
      sb.append(indent).append("SecretKey: ").append(isecretKeyAlgorithm);
      sb.append(" (").append(isecretKeySize.intValue()).append(" bits)\n");
      sb.append(indent).append("Symetric Cipher: ").append(isymCipherAlgorithm).append("\n");
      sb.append(indent).append((sealed?"Sealed":"Editable") + " Encryption Model\n");
      return sb.toString();
   }
   
   private void checkComplete() {if (!isComplete()) throw new IllegalStateException();}
   private void checkSealed() {if (sealed) throw new IllegalStateException();}
   public void seal() {checkComplete();sealed=true;}
   
   public void reset() {
      checkSealed();
      ikeyPairAlgorithm=null;
      ikeyPairSize=null;
      iasyCipherAlgorithm=null;      
      isecretKeyAlgorithm=null;
      isecretKeySize=null;
      isymCipherAlgorithm=null;
      keyPairFactory=null;
      keyPairGenerator=null;
      keyFactory=null;
      keyGenerator=null;
   }
   
   public boolean isComplete() {
      return (ikeyPairAlgorithm!=null)
           && (ikeyPairSize!=null)
           && (iasyCipherAlgorithm!=null)
           && (isecretKeyAlgorithm!=null)
           && (isecretKeySize!=null)
           && (isymCipherAlgorithm!=null);   
   }
   
   public boolean equals(Object obj) {
      if (!(obj instanceof EncryptionModel)) return false;
      EncryptionModel eo=EncryptionModel.class.cast(obj);
      return (ikeyPairAlgorithm.equals(ikeyPairAlgorithm))
           && (ikeyPairSize.equals(eo.ikeyPairSize))
           && (iasyCipherAlgorithm.equals(iasyCipherAlgorithm))         
           && (isecretKeyAlgorithm.equals(isecretKeyAlgorithm))
           && (isymCipherAlgorithm.equals(isymCipherAlgorithm));            
   }
   
   public String getKeyPairAlgorithm() {return ikeyPairAlgorithm;}
   public void setKeyPairAlgorithm(String keyPairAlgorithm) throws NoSuchAlgorithmException {
      checkSealed();
      if (ikeyPairAlgorithm!=null) throw new IllegalStateException();
      if (keyPairAlgorithm.length()>127) throw new IllegalArgumentException();
      ikeyPairAlgorithm = keyPairAlgorithm;
      
      keyPairFactory=KeyFactory.getInstance(ikeyPairAlgorithm);
      keyPairGenerator=KeyPairGenerator.getInstance(ikeyPairAlgorithm);
      if ((ikeyPairSize!=null) && (ikeyPairSize.intValue()>0)) keyPairGenerator.initialize(ikeyPairSize.intValue());      
   }
   
   public int getKeyPairSize() {return (ikeyPairSize==null)?0:ikeyPairSize.intValue();}
   public void setKeyPairSize(int keyPairSize) {
      checkSealed();
      if (ikeyPairSize!=null) throw new IllegalStateException();
      ikeyPairSize=Integer.valueOf(keyPairSize);
      if ((ikeyPairSize.intValue()>0) && (keyPairGenerator!=null)) keyPairGenerator.initialize(ikeyPairSize.intValue());
   }
   
   public KeyPair createKeyPair() throws NoSuchAlgorithmException {
      checkComplete();
      return keyPairGenerator.generateKeyPair();
   }
   
   public Identity createIdentity() throws NoSuchAlgorithmException {
      checkComplete();
      Identity result=new Identity();
      KeyPair kpair=createKeyPair();
      try {
         result.setPrivateLocalKey(getPrivateKeySpec(kpair.getPrivate()));
         result.setPublicLocalKey(getPublicKeySpec(kpair.getPublic()));
      } catch (InvalidKeySpecException ex) {
         return null;
      }
      return result;
   }
   
   public KeyPair regenKeyPair(byte[] privateKeySpec, byte[] publicKeySpec) throws InvalidKeySpecException {
      checkComplete();
      PrivateKey privKey=keyPairFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeySpec));
      PublicKey pubKey=keyPairFactory.generatePublic(new X509EncodedKeySpec(publicKeySpec));
      return new KeyPair(pubKey, privKey);
   }

   public void marshalPublicKey(PublicKey pubKey, OutputStream ostream) throws IOException {
      checkComplete();
      
      byte[] pubKeyBytes=null;
      try {pubKeyBytes=getPublicKeySpec(pubKey);}
      catch (InvalidKeySpecException ex) {
         IOException ioex=new IOException();
         ioex.initCause(ex);
         throw ioex;
      }
      
      @SuppressWarnings("resource")
      ChecksumOutputStream costream=new ChecksumOutputStream(ostream);
      
      costream.write(Bitwise.intToByteArray(pubKeyBytes.length));
      costream.write(pubKeyBytes);
      costream.writeChecksum();
      costream.flush();
   }
   
   public PublicKey unmarshalPublicKey(InputStream istream) throws IOException, InvalidKeySpecException {
      checkComplete();
      SimpleChecksum chk=new SimpleChecksum();
      
      byte[] cache=new byte[4];
      istream.read(cache); chk.feed(cache);
      cache=new byte[Bitwise.byteArrayToInt(cache)];
      istream.read(cache); chk.feed(cache);
      if (istream.read()!=chk.get()) throw new IOException("Checksum error");
      return regenPublicKey(cache);
   }
   
   public byte[] getPublicKeySpec(PublicKey pubKey) throws InvalidKeySpecException  {
      checkComplete();
      return keyPairFactory.getKeySpec(pubKey, X509EncodedKeySpec.class).getEncoded();
   }
   
   public byte[] getPrivateKeySpec(PrivateKey prvKey) throws InvalidKeySpecException  {
      checkComplete();
      return keyPairFactory.getKeySpec(prvKey, PKCS8EncodedKeySpec.class).getEncoded();
   }

   public PublicKey regenPublicKey(byte[] publicKeySpec) throws InvalidKeySpecException {
      checkComplete();
      return keyPairFactory.generatePublic(new X509EncodedKeySpec(publicKeySpec));
   }
      
   public String getAsyCipherAlgorithm() {return iasyCipherAlgorithm;}
   public void setAsyCipherAlgorithm(String asyCipherAlgorithm) {
      checkSealed();
      if (iasyCipherAlgorithm!=null) throw new IllegalStateException();
      if (asyCipherAlgorithm.length()>127) throw new IllegalArgumentException();
      iasyCipherAlgorithm = asyCipherAlgorithm;
   }   
   
   public String getSecretKeyAlgorithm() {return isecretKeyAlgorithm;}
   public void setSecretKeyAlgorithm(String secretKeyAlgorithm) throws NoSuchAlgorithmException {
      checkSealed();
      if (isecretKeyAlgorithm!=null) throw new IllegalStateException();
      if (secretKeyAlgorithm.length()>127) throw new IllegalArgumentException();
      isecretKeyAlgorithm = secretKeyAlgorithm;
      
      keyFactory=SecretKeyFactory.getInstance(isecretKeyAlgorithm);
      keyGenerator=KeyGenerator.getInstance(isecretKeyAlgorithm);
      if ((isecretKeySize!=null) && (isecretKeySize.intValue()>0)) keyGenerator.init(isecretKeySize.intValue());      
   }
   
   public int getSecretKeySize() {return (isecretKeySize==null)?0:isecretKeySize.intValue();}
   public void setSecretKeySize(int secretKeySize) {
      checkSealed();
      if (isecretKeySize!=null) throw new IllegalStateException();
      isecretKeySize=Integer.valueOf(secretKeySize);
   }
   
   public SecretKey createSecretKey() throws NoSuchAlgorithmException {
      checkComplete();
      return keyGenerator.generateKey();
   }
   
   public SecretKey regenSecretKey(byte[] keyspec) throws NoSuchAlgorithmException, InvalidKeyException {
      checkComplete();
      SecretKeySpec secKeySpec=new SecretKeySpec(keyspec, isecretKeyAlgorithm);
      return keyFactory.translateKey(secKeySpec);      
   }

   public String getSymCipherAlgorithm() {return isymCipherAlgorithm;}
   public void setSymCipherAlgorithm(String symCipherAlgorithm) {
      checkSealed();
      if (isymCipherAlgorithm!=null) throw new IllegalStateException();
      if (symCipherAlgorithm.length()>127) throw new IllegalArgumentException();
      isymCipherAlgorithm = symCipherAlgorithm;
   }   
}
