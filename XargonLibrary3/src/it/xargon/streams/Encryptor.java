package it.xargon.streams;

import it.xargon.util.Bitwise;

import java.io.*;
import java.security.*;
import java.security.spec.*;
import java.util.Arrays;

import javax.crypto.*;

public class Encryptor {
   public static DuplexChannel secureChannel(DuplexChannel channel, EncryptionModel localModel) throws IOException {
      if (!localModel.isComplete()) throw new IllegalArgumentException("Modello di sicurezza incompleto");

      InputStream plain_is=channel.getInputStream();
      OutputStream plain_os=channel.getOutputStream();
      InputStream crypt_is=null;
      OutputStream crypt_os=null;
      
      DuplexChannel result=null;
      
      byte[] cache=new byte[4];
      
      try {
         //manda l'EncryptionModel locale
         localModel.marshal(plain_os);
         //ricevi l'EncryptionModel remoto
         EncryptionModel remoteModel=EncryptionModel.unmarshal(plain_is);
         
         //se ident è diverso da null, usa l'identificazione locale in esso contenuta
         //altrimenti genera keypair
         KeyPair kpair=null;
         if (channel.getIdentity()!=null) {
            kpair=localModel.regenKeyPair(channel.getIdentity().getPrivateLocalKey(), channel.getIdentity().getPublicLocalKey());
         } else {
            kpair=localModel.createKeyPair();            
         }
         PrivateKey localPrivateKey=kpair.getPrivate();
         
         //manda la chiave pubblica locale
         localModel.marshalPublicKey(kpair.getPublic(), plain_os);
         //con la chiave privata locale genera un decrittatore
         Cipher asyReceivingCipher=Cipher.getInstance(localModel.getAsyCipherAlgorithm());
         asyReceivingCipher.init(Cipher.DECRYPT_MODE, localPrivateKey);

         //ricevi e rigenera la chiave pubblica remota
         PublicKey remotePublicKey=remoteModel.unmarshalPublicKey(plain_is);
         //se ident è diverso da null, e contiene anche una chiave pubblica remota
         //confronta quest'ultima con quella ricevuta: devono essere uguali (se non
         //lo sono siamo di fronte ad un attacco "man in the middle")
         if (channel.getIdentity()!=null) {
            byte[] expectedRemoteKey=channel.getIdentity().getPublicRemoteKey();
            if (expectedRemoteKey!=null) {
               if (!(Arrays.equals(expectedRemoteKey, remoteModel.getPublicKeySpec(remotePublicKey))))
                     throw new IllegalStateException("Chave pubblica ricevuta diversa da quella attesa");
            }   
         }
         //con la chiave pubblica remota genera un crittatore
         Cipher asySendingCipher=Cipher.getInstance(remoteModel.getAsyCipherAlgorithm());
         asySendingCipher.init(Cipher.ENCRYPT_MODE, remotePublicKey);
         
         //genera secretkey locale (fase simmetrica)
         Key localSecKey=localModel.createSecretKey();         
         //ottieni una rappresentazione in byte
         byte[] localSecKeyBytes=localSecKey.getEncoded();
         //genera il cipher per l'invio (crittazione simmetrica)
         Cipher symSendingCipher=Cipher.getInstance(localModel.getSymCipherAlgorithm());
         symSendingCipher.init(Cipher.ENCRYPT_MODE, localSecKey);
         //ottieni i parametri di crittazione generati dall'inizializzazione (per essere inviati)
         byte[] localCipherParams=symSendingCipher.getParameters().getEncoded();
         
         //critta e manda i dati seguenti:
         //   lunghezza della secretkey simmetrica locale
         //   secretkey simmetrica locale
         //   lunghezza dei parametri di inizializzazione del cipher simmetrico
         //   parametri di inizializzazione del cipher simmetrico
         ByteArrayOutputStream bos=new ByteArrayOutputStream();    
         bos.write(asySendingCipher.update(Bitwise.intToByteArray(localSecKeyBytes.length)));
         bos.write(asySendingCipher.update(localSecKeyBytes));
         bos.write(asySendingCipher.update(Bitwise.intToByteArray(localCipherParams.length)));
         bos.write(asySendingCipher.doFinal(localCipherParams));
         bos.flush();bos.close();
         byte[] symLocalInitBlock=bos.toByteArray();
         plain_os.write(Bitwise.intToByteArray(symLocalInitBlock.length));
         plain_os.write(symLocalInitBlock);
         plain_os.flush();
         
         //Ricevi e decritta il blocco di inizializzazione
         plain_is.read(cache);
         byte[] symRemoteInitBlock=new byte[Bitwise.byteArrayToInt(cache)];
         plain_is.read(symRemoteInitBlock);
         ByteArrayInputStream bis=new ByteArrayInputStream(asyReceivingCipher.doFinal(symRemoteInitBlock));
         //   ricevi lunghezza della secretkey remota
         bis.read(cache);
         byte[] remoteSecKeyBytes=new byte[Bitwise.byteArrayToInt(cache)];
         //   ricevi la secretkey remota
         bis.read(remoteSecKeyBytes);
         //   ricevi lunghezza dei parametri di inizializzazione del cipher simmetrico
         bis.read(cache);
         byte[] remoteCipherParams=new byte[Bitwise.byteArrayToInt(cache)];
         //   ricevi i parametri di inizializzazione del cipher simmetrico
         bis.read(remoteCipherParams);
         bis.close();
         
         //Rigenera la secretkey remota
         Key remoteSecKey=remoteModel.regenSecretKey(remoteSecKeyBytes);
                  
         //con la secretkey remota e i parametri ricevuti
         //crea un cipher per decrittare la ricezione (symReceivingCipher)
         AlgorithmParameters aParams=AlgorithmParameters.getInstance(remoteModel.getSecretKeyAlgorithm());
         aParams.init(remoteCipherParams);
         Cipher symReceivingCipher=Cipher.getInstance(remoteModel.getSymCipherAlgorithm());
         symReceivingCipher.init(Cipher.DECRYPT_MODE, remoteSecKey, aParams);
         
         //genera gli stream criptati
         //   symReceivingCipher -> InputStream
         crypt_is=new CipherInputStream(plain_is, symReceivingCipher);
         //   sendingCipher -> OutputStream
         crypt_os=new CipherOutputStream(plain_os, symSendingCipher);
         
         //associa i due nuovi stream in un DuplexChannel
         Identity finalId=new Identity();
         finalId.setPublicRemoteKey(remoteModel.getPublicKeySpec(remotePublicKey));
         finalId.setPublicLocalKey(localModel.getPublicKeySpec(kpair.getPublic()));
         finalId.setPrivateLocalKey(localModel.getPrivateKeySpec(kpair.getPrivate()));
         result=new DuplexChannel(crypt_is, crypt_os, finalId);
      } catch (NoSuchAlgorithmException
             | InvalidKeySpecException
             | NoSuchPaddingException
             | InvalidKeyException
             | IllegalBlockSizeException
             | BadPaddingException
             | InvalidAlgorithmParameterException ex) {
         IOException ioex=new IOException();
         ioex.initCause(ex);
         throw ioex;
      }
      
      return result;
   }
}
