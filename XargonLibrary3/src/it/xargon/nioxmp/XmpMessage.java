package it.xargon.nioxmp;

import java.nio.ByteBuffer;

import it.xargon.util.Debug;

public interface XmpMessage extends Debug.Printable {   
   public XmpMessageType getType();
   public boolean isChildOf(XmpMessage request);
   public void setContents(ByteBuffer contents);
   public ByteBuffer getContents();
}
