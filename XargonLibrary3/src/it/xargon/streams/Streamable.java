package it.xargon.streams;

import java.io.OutputStream;
import java.io.IOException;

public interface Streamable {
   public void marshal(OutputStream ostream) throws IOException;
}
