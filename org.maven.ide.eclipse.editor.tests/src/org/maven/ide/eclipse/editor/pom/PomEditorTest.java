/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.editor.pom;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Label;

import com.windowtester.runtime.WT;
import com.windowtester.runtime.swt.condition.SWTIdleCondition;
import com.windowtester.runtime.swt.condition.eclipse.FileExistsCondition;
import com.windowtester.runtime.swt.condition.shell.ShellDisposedCondition;
import com.windowtester.runtime.swt.condition.shell.ShellShowingCondition;
import com.windowtester.runtime.swt.internal.condition.NotCondition;
import com.windowtester.runtime.swt.internal.condition.eclipse.DirtyEditorCondition;
import com.windowtester.runtime.swt.locator.ButtonLocator;
import com.windowtester.runtime.swt.locator.CTabItemLocator;
import com.windowtester.runtime.swt.locator.NamedWidgetLocator;
import com.windowtester.runtime.swt.locator.SWTWidgetLocator;
import com.windowtester.runtime.swt.locator.TableItemLocator;
import com.windowtester.runtime.swt.locator.TreeItemLocator;
import com.windowtester.runtime.swt.locator.eclipse.ViewLocator;
import com.windowtester.runtime.util.ScreenCapture;


/**
 * @author Eugene Kuleshov
 * @author Anton Kraev 
 */
public class PomEditorTest extends PomEditorTestBase {

  public void testUpdatingArtifactIdInXmlPropagatedToForm() throws Exception {
	  openPomFile(TEST_POM_POM_XML);

	  selectEditorTab(TAB_POM_XML);
	  
    replaceText("test-pom", "test-pom1");
    
    selectEditorTab(TAB_OVERVIEW);
    assertTextValue("artifactId", "test-pom1");
  }

  public void testFormToXmlAndXmlToFormInParentArtifactId() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    // test FORM->XML and XML->FORM update of parentArtifactId
    selectEditorTab(TAB_OVERVIEW);
    getUI().click(new SWTWidgetLocator(Label.class, "Parent"));
    setTextValue("parentArtifactId", "parent2");

    selectEditorTab(TAB_POM_XML);
    replaceText("parent2", "parent3");
    
    selectEditorTab(TAB_OVERVIEW);
    assertTextValue("parentArtifactId", "parent3");
  }

  public void testNewSectionCreation() throws Exception {
    openPomFile(TEST_POM_POM_XML);
    ScreenCapture.createScreenCapture();

    ScreenCapture.createScreenCapture();
    
    expandSectionIfRequired("organizationSection", "Organization");
    ScreenCapture.createScreenCapture();
		
    getUI().click(new NamedWidgetLocator("organizationName"));
    ScreenCapture.createScreenCapture();
    
		getUI().enterText("org.foo");
    ScreenCapture.createScreenCapture();
		
		selectEditorTab(TAB_POM_XML);
    replaceText("org.foo", "orgfoo1");
    
    selectEditorTab(TAB_OVERVIEW);
    assertTextValue("organizationName", "orgfoo1");
  }

  public void testUndoRedo() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    getUI().click(new NamedWidgetLocator("organizationName"));
    getUI().keyClick(SWT.CTRL, 'a');
    getUI().enterText("orgfoo");
    getUI().click(new NamedWidgetLocator("organizationUrl"));
    getUI().click(new NamedWidgetLocator("organizationName"));
    getUI().keyClick(SWT.CTRL, 'a');
    getUI().enterText("orgfoo1");
    
    // test undo
	  getUI().keyClick(SWT.CTRL, 'z');
	  assertTextValue("organizationName", "orgfoo");
	  // test redo
	  getUI().keyClick(SWT.CTRL, 'y');
	  assertTextValue("organizationName", "orgfoo1");
  }

  public void testDeletingScmSectionInXmlPropagatedToForm() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    selectEditorTab(TAB_OVERVIEW);
    getUI().click(new SWTWidgetLocator(Label.class, "SCM"));
    ScreenCapture.createScreenCapture();
    
    // XXX can't use "." in the url due to issue on Linux in WindowTester
    setTextValue("scmUrl", "http://m2eclipse");
    ScreenCapture.createScreenCapture();
    assertTextValue("scmUrl", "http://m2eclipse");
    selectEditorTab(TAB_POM_XML);
    delete("<scm>", "</scm>");
    selectEditorTab(TAB_OVERVIEW);
    getUI().wait(new SWTIdleCondition());
    assertTextValue("scmUrl", "");
    selectEditorTab(TAB_POM_XML);
    delete("<organization>", "</organization>");
    selectEditorTab(TAB_OVERVIEW);
    assertTextValue("organizationName", "");
    setTextValue("scmUrl", "http://m2eclipse");
    assertTextValue("scmUrl", "http://m2eclipse");
  }

  public void testExternalModificationEditorClean() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    // save editor
//    ui.keyClick(SWT.CTRL, 's');
//    Thread.sleep(2000);

    // externally replace file contents
    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    IFile file = root.getFile(new Path(TEST_POM_POM_XML));
    File f = new File(file.getLocation().toOSString());
    String text = getContents(f);
    setContents(f, text.replace("parent3", "parent4"));

    // reload the file
    getUI().click(new CTabItemLocator("Package Explorer"));
    getUI().click(new CTabItemLocator(TEST_POM_POM_XML));
    // ui.contextClick(new TreeItemLocator(TEST_POM_POM_XML, new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")), "Refresh");
    
    getUI().wait(new ShellShowingCondition("File Changed"));
    getUI().click(new ButtonLocator("&Yes"));
    
    assertTextValue("parentArtifactId", "parent4");

    // verify that value changed in xml and in the form
    selectEditorTab(TAB_POM_XML);
    String editorText = getEditorText();
    assertTrue(editorText, editorText.contains("<artifactId>parent4</artifactId>"));
    
    // XXX verify that value changed on a page haven't been active before
  }

  // test that form and xml is not updated when refused to pickup external changes
//  public void testExternalModificationNotUpdate() throws Exception {
//    // XXX test that form and xml are not updated when refused to pickup external changes
//  }
  
  // XXX update for new modification code 
  public void testExternalModificationEditorDirty() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    // make editor dirty
    getUI().click(new CTabItemLocator(TEST_POM_POM_XML));
    selectEditorTab(TAB_POM_XML);
    replaceText("parent4", "parent5");
    selectEditorTab(TAB_OVERVIEW);

    // externally replace file contents
    IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(TEST_POM_POM_XML));
    File f = new File(file.getLocation().toOSString());
    String text = getContents(f);
    setContents(f, text.replace("parent4", "parent6"));

    // reload the file
    getUI().click(new CTabItemLocator("Package Explorer"));
    // ui.click(new CTabItemLocator("*" + TEST_POM_POM_XML));  // take dirty state into the account
    getUI().keyClick(SWT.F12);
    
    // ui.contextClick(new TreeItemLocator(TEST_POM_POM_XML, new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")), "Refresh");
    
    getUI().wait(new ShellShowingCondition("File Changed"));
    getUI().click(new ButtonLocator("&Yes"));
    
    assertTextValue("parentArtifactId", "parent6");
    
    // verify that value changed in xml and in the form
    selectEditorTab(TAB_POM_XML);
    String editorText = getEditorText();
    assertTrue(editorText, editorText.contains("<artifactId>parent6</artifactId>"));

    // XXX verify that value changed on a page haven't been active before
  }

  public void testNewEditorIsClean() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    // close/open the file 
    getUI().close(new CTabItemLocator(TEST_POM_POM_XML));
    // ui.click(2, new TreeItemLocator(TEST_POM_POM_XML, new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));
    openPomFile(TEST_POM_POM_XML);

    // test the editor is clean
    getUI().assertThat(new NotCondition(new DirtyEditorCondition()));
  }

  //MNGECLIPSE-874
  public void testUndoAfterSave() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    // make a change 
    getUI().click(new CTabItemLocator(TEST_POM_POM_XML));
    selectEditorTab(TAB_POM_XML);
    replaceText("parent6", "parent7");
    selectEditorTab(TAB_OVERVIEW);
    
    //save file
    getUI().keyClick(SWT.CTRL, 's');

    // test the editor is clean
    getUI().assertThat(new NotCondition(new DirtyEditorCondition()));

    // undo change
    getUI().keyClick(SWT.CTRL, 'z');

    // test the editor is dirty
    getUI().assertThat(new DirtyEditorCondition());

    //test the value
    assertTextValue("parentArtifactId", "parent6");

    //save file
    //ui.keyClick(SWT.CTRL, 's');
  }

  public void testAfterUndoEditorIsClean() throws Exception {
    openPomFile(TEST_POM_POM_XML);

    // make a change 
    getUI().click(new CTabItemLocator(TEST_POM_POM_XML));
    selectEditorTab(TAB_POM_XML);
    replaceText("parent6", "parent7");
    selectEditorTab(TAB_OVERVIEW);
    // undo change
    getUI().keyClick(SWT.CTRL, 'z');

    // test the editor is clean
    getUI().assertThat(new NotCondition(new DirtyEditorCondition()));
  }

  public void testEmptyFile() throws Exception {
    String name = PROJECT_NAME + "/test.pom";
    createFile(name, "");
    openPomFile(name);
    
	  assertTextValue("artifactId", "");
	  setTextValue("artifactId", "artf1");
	  selectEditorTab(TAB_POM_XML);
	  replaceText("artf1", "artf2");
	  selectEditorTab(TAB_OVERVIEW);
	  assertTextValue("artifactId", "artf2");
	  
  }

	//MNGECLIPSE-834
//	public void testDiscardedFileDeletion() throws Exception {
//		String name = PROJECT_NAME + "/another.pom";
//		createFile(name, "");
//		openPomFile(name);
//		
//		getUI().keyClick(SWT.CTRL, 's');
//		getUI().close(new CTabItemLocator(name));
//		
//		// ui.click(2, new TreeItemLocator(name, new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));
//		openPomFile(name);
//		
//		getUI().click(new NamedWidgetLocator("groupId"));
//		getUI().enterText("1");
//		getUI().close(new CTabItemLocator("*" + name));
//		getUI().wait(new ShellDisposedCondition("Progress Information"));
//		getUI().wait(new ShellShowingCondition("Save Resource"));
//		getUI().click(new ButtonLocator("&No"));
//		
//    ScreenCapture.createScreenCapture();
//    
//		getUI().click(new TreeItemLocator(PROJECT_NAME, new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")));
//    ScreenCapture.createScreenCapture();
//		    
//		getUI().contextClick(new TreeItemLocator(name, //
//        new ViewLocator("org.eclipse.jdt.ui.PackageExplorer")), "Delete");
//    ScreenCapture.createScreenCapture();
//		getUI().wait(new ShellDisposedCondition("Progress Information"));
//		getUI().wait(new ShellShowingCondition("Confirm Delete"));
//		getUI().keyClick(WT.CR);
//		
//		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(name));
//		getUI().wait(new FileExistsCondition(file, false));
//	}
	
	// MNGECLIPSE-833
	public void testSaveAfterPaste() throws Exception {
		String name = PROJECT_NAME + "/another2.pom";
		String str = "<project xmlns=\"http://maven.apache.org/POM/4.0.0\" " //
        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
        + "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\">" //
        + "<modelVersion>4.0.0</modelVersion>" //
        + "<groupId>test</groupId>" //
        + "<artifactId>parent</artifactId>" //
        + "<packaging>pom</packaging>" //
        + "<version>0.0.1-SNAPSHOT</version>" //
        + "</project>";
		createFile(name, str);
//		IFile file = root.getFile(new Path(name));
//		file.create(new ByteArrayInputStream(str.getBytes()), true, null);

		openPomFile(name);
		
	  selectEditorTab(TAB_POM_XML);
		getUI().wait(new NotCondition(new DirtyEditorCondition()));
		findText("</project>");
		getUI().keyClick(WT.ARROW_LEFT);
		
		putIntoClipboard("<properties><sample>sample</sample></properties>");
		getUI().keyClick(SWT.CTRL, 'v');
		getUI().wait(new DirtyEditorCondition());
		
		getUI().keyClick(SWT.CTRL, 's');
		getUI().wait(new NotCondition(new DirtyEditorCondition()));
		getUI().keyClick(SWT.CTRL, 'w');
	}

	// MNGECLIPSE-835
  public void testModulesEditorActivation() throws Exception {
    openPomFile(TEST_POM_POM_XML);
    
    getUI().keyClick(SWT.CTRL, 'm');
    
    getUI().click(new SWTWidgetLocator(Label.class, "Parent"));
    // ui.click(new SWTWidgetLocator(Label.class, "Properties"));
    
    selectEditorTab(TAB_OVERVIEW);
    ScreenCapture.createScreenCapture();
  
    getUI().click(new ButtonLocator("Add..."));
    ScreenCapture.createScreenCapture();
    getUI().click(new TableItemLocator("?"));
    ScreenCapture.createScreenCapture();
    getUI().enterText("foo1");
    getUI().keyClick(WT.CR);
    getUI().keyClick(WT.CR);
    
    getUI().click(new ButtonLocator("Add..."));
    getUI().click(new TableItemLocator("?"));
    getUI().enterText("foo2");
    getUI().keyClick(WT.CR);
    getUI().keyClick(WT.CR);
  
    // save
    getUI().keyClick(SWT.CTRL, 's');
    
    getUI().click(new TableItemLocator("foo1"));
    getUI().click(new TableItemLocator("foo2"));

    getUI().keyClick(SWT.CTRL, 'm');
    
    // test the editor is clean
    getUI().assertThat(new NotCondition(new DirtyEditorCondition()));
  }

}
