package com.intellij.localvcs;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class PutLabelChange extends Change {
  private long myTimestamp;
  private String myName;

  protected PutLabelChange(long timestamp, String name) {
    myTimestamp = timestamp;
    myName = name;
  }

  public PutLabelChange(Stream s) throws IOException {
    myTimestamp = s.readLong();
    myName = s.readString();
  }

  @Override
  public void write(Stream s) throws IOException {
    s.writeLong(myTimestamp);
    s.writeString(myName);
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public long getTimestamp() {
    return myTimestamp;
  }

  @Override
  public void applyTo(RootEntry r) {
  }

  @Override
  public void revertOn(RootEntry r) {
  }

  public boolean affects(Entry e) {
    return true;
  }

  public boolean isCreationalFor(Entry e) {
    return false;
  }

  public List<Content> getContentsToPurge() {
    return Collections.emptyList();
  }
}
