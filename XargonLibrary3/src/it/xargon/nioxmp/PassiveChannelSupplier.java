package it.xargon.nioxmp;

import java.io.IOException;

public interface PassiveChannelSupplier extends ChannelSupplier {
   public SelectableByteChannel get() throws IOException;
}
