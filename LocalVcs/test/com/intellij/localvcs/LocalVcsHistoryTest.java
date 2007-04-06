package com.intellij.localvcs;

import static com.intellij.localvcs.Difference.Kind.*;
import org.junit.Test;

import java.util.List;

public class LocalVcsHistoryTest extends LocalVcsTestCase {
  // todo difference on root does not work!!!
  LocalVcs vcs = new TestLocalVcs();

  @Test
  public void testTreatingSeveralChangesDuringChangeSetAsOne() {
    vcs.beginChangeSet();
    vcs.createDirectory("dir");
    vcs.createFile("dir/one", null, -1);
    vcs.createFile("dir/two", null, -1);
    vcs.endChangeSet(null);

    assertEquals(1, vcs.getLabelsFor("dir").size());
  }

  @Test
  public void testTreatingSeveralChangesOutsideOfChangeSetAsSeparate() {
    vcs.createDirectory("dir");
    vcs.createFile("dir/one", null, -1);
    vcs.createFile("dir/two", null, -1);

    vcs.beginChangeSet();
    vcs.endChangeSet(null);

    vcs.createFile("dir/three", null, -1);
    vcs.createFile("dir/four", null, -1);

    assertEquals(5, vcs.getLabelsFor("dir").size());
  }

  @Test
  public void testIgnoringInnerChangeSets() {
    vcs.beginChangeSet();
    vcs.createDirectory("dir");
    vcs.beginChangeSet();
    vcs.createFile("dir/one", null, -1);
    vcs.endChangeSet("inner");
    vcs.createFile("dir/two", null, -1);
    vcs.endChangeSet("outer");

    List<Label> ll = vcs.getLabelsFor("dir");
    assertEquals(1, ll.size());
    assertEquals("outer", ll.get(0).getName());
  }

  @Test
  public void testIgnoringEmptyChangeSets() {
    vcs.beginChangeSet();
    vcs.createDirectory("dir");
    vcs.endChangeSet(null);

    assertEquals(1, vcs.getChangeList().getChanges().size());

    vcs.beginChangeSet();
    vcs.endChangeSet(null);

    assertEquals(1, vcs.getChangeList().getChanges().size());
  }

  @Test
  public void testNamedAndUnnamedLables() {
    vcs.beginChangeSet();
    vcs.createFile("file", null, -1);
    vcs.endChangeSet("name");

    vcs.changeFileContent("file", null, -1);

    List<Label> ll = vcs.getLabelsFor("file");
    assertEquals(2, ll.size());

    assertNull(ll.get(0).getName());
    assertEquals("name", ll.get(1).getName());
  }

  @Test
  public void testLabels() {
    vcs.createFile("file", b("old"), -1);
    vcs.changeFileContent("file", b("new"), -1);

    List<Label> ll = vcs.getLabelsFor("file");
    assertEquals(2, ll.size());
    assertEquals(c("new"), ll.get(0).getEntry().getContent());
    assertEquals(c("old"), ll.get(1).getEntry().getContent());
  }

  @Test
  public void testIncludingCurrentVersionIntoLabelsAfterPurge() {
    setCurrentTimestamp(10);
    vcs.createFile("file", null, -1);
    vcs.purgeUpTo(20);

    List<Label> ll = vcs.getLabelsFor("file");
    assertEquals(1, ll.size());

    assertEquals("file", ll.get(0).getEntry().getName());
  }

  @Test
  public void testIncludingVersionBeforeFirstChangeAfterPurge() {
    setCurrentTimestamp(10);
    vcs.createFile("file", b("one"), -1);
    setCurrentTimestamp(20);
    vcs.changeFileContent("file", b("two"), -1);

    vcs.purgeUpTo(15);

    List<Label> ll = vcs.getLabelsFor("file");
    assertEquals(2, ll.size());

    assertEquals(c("two"), ll.get(0).getEntry().getContent());
    assertEquals(c("one"), ll.get(1).getEntry().getContent());
  }

  @Test
  public void testDoesNotIncludeLabelsForAnotherEntries() {
    vcs.beginChangeSet();
    vcs.createFile("file1", null, -1);
    vcs.endChangeSet("1");

    vcs.beginChangeSet();
    vcs.createFile("file2", null, -1);
    vcs.endChangeSet("2");

    List<Label> ll = vcs.getLabelsFor("file2");
    assertEquals(1, ll.size());
    assertEquals("2", ll.get(0).getName());
  }

  @Test
  public void testLabelsTimestamp() {
    setCurrentTimestamp(10);
    vcs.createFile("file", null, -1);

    setCurrentTimestamp(20);
    vcs.changeFileContent("file", null, -1);

    setCurrentTimestamp(30);
    vcs.changeFileContent("file", null, -1);

    List<Label> ll = vcs.getLabelsFor("file");
    assertEquals(30L, ll.get(0).getTimestamp());
    assertEquals(20L, ll.get(1).getTimestamp());
    assertEquals(10L, ll.get(2).getTimestamp());
  }

  @Test
  public void testTimestampForCurrentLabelAfterPurgeFromCurrentTimestamp() {
    setCurrentTimestamp(10);
    vcs.createFile("file", null, -1);
    vcs.purgeUpTo(20);

    setCurrentTimestamp(20);
    assertEquals(20L, vcs.getLabelsFor("file").get(0).getTimestamp());
  }

  @Test
  public void testTimestampForLastLabelAfterPurge() {
    setCurrentTimestamp(10);
    vcs.createFile("file", null, -1);

    setCurrentTimestamp(20);
    vcs.changeFileContent("file", null, -1);

    setCurrentTimestamp(30);
    vcs.changeFileContent("file", null, -1);

    vcs.purgeUpTo(15);

    List<Label> ll = vcs.getLabelsFor("file");
    assertEquals(30L, ll.get(0).getTimestamp());
    assertEquals(20L, ll.get(1).getTimestamp());
    assertEquals(20L, ll.get(2).getTimestamp());
  }

  @Test
  public void testHistoryForFileCreatedWithSameNameAsDeletedOne() {
    vcs.createFile("file", b("old"), -1);
    vcs.delete("file");
    vcs.createFile("file", b("new"), -1);

    List<Label> ll = vcs.getLabelsFor("file");
    assertEquals(1, ll.size());

    Entry e = ll.get(0).getEntry();
    assertEquals("file", e.getName());
    assertEquals(c("new"), e.getContent());
  }

  @Test
  public void testHistoryForFileCreatenInPlaceOfRenamedOne() {
    vcs.createFile("file1", b("content1"), -1);
    vcs.rename("file1", "file2");
    vcs.createFile("file1", b("content2"), -1);

    List<Label> ll = vcs.getLabelsFor("file1");
    assertEquals(1, ll.size());

    Entry e = ll.get(0).getEntry();
    assertEquals("file1", e.getName());
    assertEquals(c("content2"), e.getContent());

    ll = vcs.getLabelsFor("file2");
    assertEquals(2, ll.size());

    e = ll.get(0).getEntry();
    assertEquals("file2", e.getName());
    assertEquals(c("content1"), e.getContent());

    e = ll.get(1).getEntry();
    assertEquals("file1", e.getName());
    assertEquals(c("content1"), e.getContent());
  }

  @Test
  public void testGettingEntryFromLabel() {
    vcs.createFile("file", b("content"), 123L);
    vcs.changeFileContent("file", b("new content"), 456L);

    List<Label> ll = vcs.getLabelsFor("file");

    Entry e = ll.get(0).getEntry();
    assertEquals("file", e.getName());
    assertEquals(c("new content"), e.getContent());
    assertEquals(456L, e.getTimestamp());

    e = ll.get(1).getEntry();
    assertEquals("file", e.getName());
    assertEquals(c("content"), e.getContent());
    assertEquals(123L, e.getTimestamp());
  }

  @Test
  public void testGettingEntryFromLabelInRenamedDir() {
    vcs.createDirectory("dir");
    vcs.createFile("dir/file", null, -1);
    vcs.rename("dir", "newDir");
    vcs.changeFileContent("newDir/file", null, -1);

    List<Label> ll = vcs.getLabelsFor("newDir/file");
    assertEquals(3, ll.size());

    assertEquals("newDir/file", ll.get(0).getEntry().getPath());
    assertEquals("newDir/file", ll.get(1).getEntry().getPath());
    assertEquals("dir/file", ll.get(2).getEntry().getPath());
  }

  @Test
  public void testGettingEntryFromLabelDoesNotChangeRootEntry() {
    vcs.createFile("file", b("content"), -1);
    vcs.changeFileContent("file", b("new content"), -1);

    List<Label> ll = vcs.getLabelsFor("file");

    assertEquals(c("content"), ll.get(1).getEntry().getContent());
    assertEquals(c("new content"), vcs.getEntry("file").getContent());
  }

  @Test
  public void testGettingDifferenceBetweenLablels() {
    vcs.createFile("file", b("content"), -1);
    vcs.changeFileContent("file", b("new content"), -1);

    List<Label> ll = vcs.getLabelsFor("file");

    Label recent = ll.get(0);
    Label prev = ll.get(1);

    Difference d = prev.getDifferenceWith(recent);
    assertEquals(MODIFIED, d.getKind());
    assertEquals(c("content"), d.getLeft().getContent());
    assertEquals(c("new content"), d.getRight().getContent());
  }

  @Test
  public void testNoDifferenceBetweenLabels() {
    vcs.createFile("file", b("content"), -1);

    List<Label> ll = vcs.getLabelsFor("file");

    Label one = ll.get(0);
    Label two = ll.get(0);

    Difference d = one.getDifferenceWith(two);
    assertFalse(d.hasDifference());
  }

  @Test
  public void testDifferenceForDirectory() {
    vcs.createDirectory("dir");
    vcs.createFile("dir/file", null, -1);

    List<Label> ll = vcs.getLabelsFor("dir");
    assertEquals(2, ll.size());

    Label recent = ll.get(0);
    Label prev = ll.get(1);

    Difference d = prev.getDifferenceWith(recent);
    assertEquals(NOT_MODIFIED, d.getKind());
    assertEquals(1, d.getChildren().size());

    d = d.getChildren().get(0);
    assertEquals(CREATED, d.getKind());
    assertNull(d.getLeft());
    assertEquals("file", d.getRight().getName());
  }

  @Test
  public void testNoDifferenceForDirectoryWithEqualContents() {
    vcs.createDirectory("dir");
    vcs.createFile("dir/file", null, -1);
    vcs.delete("dir/file");

    List<Label> ll = vcs.getLabelsFor("dir");

    Difference d = ll.get(0).getDifferenceWith(ll.get(2));
    assertFalse(d.hasDifference());
  }

  @Test
  public void testDoesNotIncludeNotModifiedDifferences() {
    vcs.beginChangeSet();
    vcs.createDirectory("dir1");
    vcs.createDirectory("dir1/dir2");
    vcs.createDirectory("dir1/dir3");
    vcs.createFile("dir1/dir2/file", b(""), -1);
    vcs.endChangeSet(null);

    vcs.createFile("dir1/dir3/file", null, -1);

    List<Label> ll = vcs.getLabelsFor("dir1");
    Label recent = ll.get(0);
    Label prev = ll.get(1);

    Difference d = prev.getDifferenceWith(recent);

    assertEquals("dir1", d.getLeft().getName());
    assertEquals(NOT_MODIFIED, d.getKind());
    assertEquals(1, d.getChildren().size());

    d = d.getChildren().get(0);

    assertEquals("dir3", d.getLeft().getName());
    assertEquals(NOT_MODIFIED, d.getKind());
    assertEquals(1, d.getChildren().size());
  }
}
