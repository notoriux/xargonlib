package it.xargon.niomarshal;

import java.nio.ByteBuffer;

import it.xargon.util.*;

public class MarIdentifier extends AbstractMarshaller<Identifier> {
   public MarIdentifier() {super("IDENT");}
      
   public byte[] marshalToMemory(Object obj) {return ((Identifier)obj).getData();}
   
   public Object unmarshalFromMemory(byte[] contents) {return new Identifier(contents);}

   @Override
   public ByteBuffer marshal(Identifier obj) {
      ByteBuffer result=alloc(obj.getSize());
      result.put(obj.getData()).flip();
      return result;
   }

   @Override
   public Identifier unmarshal(ByteBuffer buffer) {
      byte[] tmp=new byte[buffer.remaining()];
      buffer.get(tmp);
      return new Identifier(tmp);
   }
}
