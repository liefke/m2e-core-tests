/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.maven.ide.eclipse.scm;

import java.io.File;
import java.io.InputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IProgressMonitor;

import org.maven.ide.eclipse.MavenPlugin;
import org.maven.ide.eclipse.project.MavenProjectScmInfo;

/**
 * An SCM handler base class
 *
 * @author Eugene Kuleshov
 */
public abstract class ScmHandler implements Comparable, IExecutableExtension {

  public static final String ATTR_CLASS = "class";
  public static final String ATTR_TYPE = "type";
  public static final String ATTR_PRIORITY = "priority";
  
  private String type;
  private int priority;

  public String getType() {
    return type;
  }

  public int getPriority() {
    return priority;
  }
  
  /**
   * Opens resource from SCM
   * 
   * @param url an url in maven-scm format for the resource to open
   * @param revision a resource revision to open 
   *  
   * @see http://maven.apache.org/scm/scm-url-format.html
   */
  public InputStream open(String url, String revision) throws CoreException {
    return null;
  }
  
  /**
   * @param info
   * @param location
   * @param monitor
   */
  public abstract void checkoutProject(MavenProjectScmInfo info, //
      File location, IProgressMonitor monitor) throws CoreException, InterruptedException;
  
  // IExecutableExtension  
  
  public void setInitializationData(IConfigurationElement config, String propertyName, Object data) {
    String handlerClass = config.getAttribute(propertyName);
    String type = config.getAttribute(ATTR_TYPE);
    String priority = config.getAttribute(ATTR_PRIORITY);

    this.type = type;
    
    if(priority!=null) {
      try {
        this.priority = Integer.parseInt(priority);
      } catch(Exception ex) {
        MavenPlugin.log("Unable to parse priority for " + handlerClass, ex);
      }
    }
  }
  
  // Comparable
  
  public int compareTo(Object o) {
    if(o instanceof ScmHandler) {
      ScmHandler handler = (ScmHandler) o;
      int res = getType().compareTo(handler.getType());
      if(res==0) {
        res = getPriority() - handler.getPriority();
      }
      return res; 
    }
    return -1;
  }

}
