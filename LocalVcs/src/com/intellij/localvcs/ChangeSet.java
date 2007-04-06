package com.intellij.localvcs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChangeSet extends Change {
  private String myName;
  private long myTimestamp;
  private List<Change> myChanges;

  public ChangeSet(long timestamp, String name, List<Change> changes) {
    myTimestamp = timestamp;
    myChanges = changes;
    myName = name;
  }

  public ChangeSet(Stream s) throws IOException {
    // todo get rid of null here
    myName = s.readStringOrNull();
    myTimestamp = s.readLong();

    int count = s.readInteger();
    myChanges = new ArrayList<Change>(count);
    while (count-- > 0) {
      myChanges.add(s.readChange());
    }
  }

  public void write(Stream s) throws IOException {
    s.writeStringOrNull(myName);
    s.writeLong(myTimestamp);

    s.writeInteger(myChanges.size());
    for (Change c : myChanges) {
      s.writeChange(c);
    }
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public long getTimestamp() {
    return myTimestamp;
  }

  // todo test support
  public List<Change> getChanges() {
    return myChanges;
  }

  @Override
  public void applyTo(RootEntry r) {
    for (Change c : myChanges) {
      c.applyTo(r);
    }
  }

  @Override
  public void revertOn(RootEntry e) {
    for (int i = myChanges.size() - 1; i >= 0; i--) {
      Change c = myChanges.get(i);
      c.revertOn(e);
    }
  }

  @Override
  public boolean affects(Entry e) {
    for (Change c : myChanges) {
      if (c.affects(e)) return true;
    }
    return false;
  }

  @Override
  public boolean isCreationalFor(Entry e) {
    for (Change c : myChanges) {
      if (c.isCreationalFor(e)) return true;
    }
    return false;
  }

  @Override
  public List<Content> getContentsToPurge() {
    List<Content> result = new ArrayList<Content>();
    for (Change c : myChanges) {
      result.addAll(c.getContentsToPurge());
    }
    return result;
  }
}
