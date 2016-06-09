package it.xargon.xmp;

import it.xargon.streams.Printable;
import it.xargon.streams.Streamable;

public interface XmpMessage extends Streamable, Printable {   
   public XmpMessageType getType();
   public boolean isChildOf(XmpMessage request);
   public void setContents(byte[] contents);
   public byte[] getContents();
}
