package it.xargon.marshal;

import it.xargon.util.*;

public class MarIdentifier extends AbstractMarshaller {
   public MarIdentifier() {super("IDENT", Source.MEMORY, Identifier.class);}
      
   public byte[] marshalToMemory(Object obj) {return ((Identifier)obj).getData();}
   
   public Object unmarshalFromMemory(byte[] contents) {return new Identifier(contents);}
}
