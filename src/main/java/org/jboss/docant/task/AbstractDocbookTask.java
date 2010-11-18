/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.docant.task;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.jboss.jdocbook.Configuration;
import org.jboss.jdocbook.Environment;
import org.jboss.jdocbook.JDocBookComponentRegistry;
import org.jboss.jdocbook.JDocBookProcessException;
import org.jboss.jdocbook.MasterLanguageDescriptor;
import org.jboss.jdocbook.Profiling;
import org.jboss.jdocbook.ResourceDelegate;
import org.jboss.jdocbook.ValueInjection;
import org.jboss.jdocbook.profile.ProfilingSource;
import org.jboss.jdocbook.render.RenderingException;
import org.jboss.jdocbook.render.RenderingSource;
import org.jboss.jdocbook.util.ResourceDelegateSupport;
import org.jboss.jdocbook.util.TranslationUtils;
import org.jboss.jdocbook.util.XIncludeHelper;
import org.jboss.jdocbook.xslt.XSLTException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * abstract ant task for jdocbook
 *
 * @author Jeff Zhang
 */
public abstract class AbstractDocbookTask extends Task implements DirectoryLayout.BaseInfo
{
   private static final Logger log = LoggerFactory.getLogger(AbstractDocbookTask.class);

   /** The name of the master document */
   protected String sourceDocumentName;

   /** 
    * The directory where the sources are located.
    *  default is "${basedir}/src/main/docbook"
    */
   protected File sourceDirectory;

   /**
    * The base directory where output will be written.
    * default is "${basedir}/target"
    */
   protected File baseOutputDirectory;

   /**
    * The directory containing local fonts
    * default is "${basedir}/src/main/fonts"
    */
   protected File fontsDirectory;

   /**
    * The formats in which to perform rendering.
    */
   protected List<Format> formats = new ArrayList<Format>();

   /**
    * Profiling configuration
    */
   protected Profiling profiling = new Profiling();

   /**
    * Configurable options
    */
   protected Options options;

   // translation-specific config setting

   /**
    * The locale of the master translation.
    * default is "en-US"
    */
   protected String masterTranslation = "en-US";


   /**
    * Should we ignore translations?  This is useful for temporarily suspending processing of translations from a
    * profile or other environment specific means.  Note that this setting only affects the docbook processing
    * phases, not the PO/POT management goals.
    *
    * default-value="false"
    */
   protected boolean ignoreTranslations = false;

   /**
    * The locales of all non-master translations.
    */
   protected String[] translations;

   /**
    * directoryLayout
    */
   protected final DirectoryLayout directoryLayout = new DirectoryLayout(this);


   /**
    * jDocBookComponentRegistry
    */
   private JDocBookComponentRegistry jDocBookComponentRegistry;

   protected JDocBookComponentRegistry getComponentRegistry()
   {
      if (jDocBookComponentRegistry == null)
      {
         jDocBookComponentRegistry = buildComponentRegistry();
      }
      return jDocBookComponentRegistry;
   }

   private JDocBookComponentRegistry buildComponentRegistry()
   {
      return new JDocBookComponentRegistry(new EnvironmentImpl(), new ConfigurationImpl());
   }


   /**
    * masterLanguageLocale
    */
   private Locale masterLanguageLocale;

   protected Locale getMasterLanguageLocale()
   {
      if (masterLanguageLocale == null)
      {
         masterLanguageLocale = fromLanguageString(getMasterLanguage());
      }
      return masterLanguageLocale;
   }

   public Locale fromLanguageString(String languageStr)
   {
      return TranslationUtils.parse(languageStr, options.getLocaleSeparator());
   }

   private boolean isMasterLanguage(String language)
   {
      return getMasterLanguage().equals(language);
   }

   private boolean isMasterLanguage(Locale language)
   {
      return getMasterLanguageLocale().equals(language);
   }

   /**
    * masterLanguageDescriptor
    */
   private MasterLanguageDescriptorImpl masterLanguageDescriptor = new MasterLanguageDescriptorImpl();

   public MasterLanguageDescriptorImpl getMasterLanguageDescriptor()
   {
      return masterLanguageDescriptor;
   }

   private class MasterLanguageDescriptorImpl implements MasterLanguageDescriptor
   {
      public Locale getLanguage()
      {
         return fromLanguageString(masterTranslation);
      }

      public File getPotDirectory()
      {
         return directoryLayout.getPotSourceDirectory();
      }

      public File getBaseSourceDirectory()
      {
         return directoryLayout.getMasterSourceDirectory();
      }

      private File rootMasterFile;

      public File getRootDocumentFile()
      {
         if (rootMasterFile == null)
         {
            rootMasterFile = new File(getBaseSourceDirectory(), sourceDocumentName);
         }
         return rootMasterFile;
      }

      private Set<File> masterFiles;

      public Set<File> getDocumentFiles()
      {
         if (masterFiles == null)
         {
            File rootMasterFile = getRootDocumentFile();
            final Set<File> files = new TreeSet<File>();
            files.add(rootMasterFile);
            XIncludeHelper.findAllInclusionFiles(rootMasterFile, files);
            this.masterFiles = Collections.unmodifiableSet(files);
         }
         return masterFiles;
      }
   }

   /**
    * The override method to perform the actual processing
    *
    * @throws RenderingException Indicates problem performing rendering
    * @throws XSLTException Indicates problem building or executing XSLT transformer
    */
   protected void process() throws JDocBookProcessException
   {
   }


   protected void doExecute() throws JDocBookProcessException
   {
      process();
   }
   
   /**
    * Execute
    * @exception BuildException If the build fails
    */
   @Override
   public void execute() throws BuildException
   {
      if (options == null)
      {
         options = new Options();
      }

      if (translations == null)
      {
         translations = new String[0];
      }

      try
      {

         doExecute();

      }
      catch (XSLTException e)
      {
         throw new BuildException("XSLT problem", e);
      }
      catch (RenderingException e)
      {
         throw new BuildException("Rendering problem", e);
      }
      catch (JDocBookProcessException e)
      {
         throw new BuildException("Unexpected problem", e);
      }
   }


   private class EnvironmentImpl implements Environment
   {
      private final ResourceDelegateImpl resourceDelegate = new ResourceDelegateImpl();

      public ResourceDelegate getResourceDelegate()
      {
         return (ResourceDelegate) resourceDelegate;
      }

      public MasterLanguageDescriptor getMasterLanguageDescriptor()
      {
         return getMasterLanguageDescriptor();
      }

      public File getWorkDirectory()
      {
         return directoryLayout.getRootJDocBookWorkDirectory();
      }

      public File getStagingDirectory()
      {
         return directoryLayout.getStagingDirectory();
      }

      public List<File> getFontDirectories()
      {
         return Arrays.asList(AbstractDocbookTask.this.getFontDirectories());
      }

      public DocBookXsltResolutionStrategy getDocBookXsltResolutionStrategy()
      {
         return DocBookXsltResolutionStrategy.NAMED;
      }
   }


   private class ResourceDelegateImpl extends ResourceDelegateSupport
   {
      private ClassLoader loader;

      @Override
      protected ClassLoader getResourceClassLoader()
      {
         if (loader == null)
         {
            loader = buildResourceDelegateClassLoader();
         }

         for (URL url : ((URLClassLoader) loader).getURLs())
         {
            log.info("cl url: {}", url.toString());
            System.err.println("cl url: " + url.toString());
         }
         return loader;
      }
   }

   @SuppressWarnings(
   {"unchecked"})
   private ClassLoader buildResourceDelegateClassLoader()
   {
      // There are three sources for resolver base urls:
      //      1) staging dir
      //      2) project dependencies
      //      3) plugin dependencies (this should be plugin *injected* dependencies)
      List<URL> urls = new ArrayList<URL>();

      //      1) staging dir
      log.info("$$ stage {}", directoryLayout.getStagingDirectory().toString());
      if (directoryLayout.getStagingDirectory().exists())
      {
         try
         {
            urls.add(directoryLayout.getStagingDirectory().toURI().toURL());
         }
         catch (MalformedURLException e)
         {
            throw new JDocBookProcessException("Unable to resolve staging directory to URL", e);
         }
      }
      /*
             //      2) project dependencies
             for ( Artifact artifact : (Set<Artifact>) project.getArtifacts() ) {
                 if ( artifact.getFile() != null ) {
                     try {
                         urls.add( artifact.getFile().toURI().toURL() );
                     }
                     catch ( MalformedURLException e ) {
                         getLog().warn( "Uanble to retrieve artifact url [" + artifact.getId() + "]" );
                     }
                 }
             }

             //      3) plugin dependencies (this should be plugin *injected* dependencies)
             if ( pluginArtifacts != null ) {
                 for ( Artifact artifact : (List<Artifact>) pluginArtifacts ) {
                     if ( artifact.getFile() != null ) {
                         try {
                             urls.add( artifact.getFile().toURI().toURL() );
                         }
                         catch ( MalformedURLException e ) {
                             getLog().warn( "Uanble to retrieve artifact url [" + artifact.getId() + "]" );
                         }
                     }
                 }
             }
      */
      return new URLClassLoader(urls.toArray(new URL[urls.size()]), Thread.currentThread().getContextClassLoader());
   }

   private class ConfigurationImpl implements Configuration
   {
      private Options options()
      {
         return options;
      }

      public Properties getTransformerParameters()
      {
         return options().getTransformerParameters();
      }

      public boolean isUseRelativeImageUris()
      {
         return options().isUseRelativeImageUris();
      }

      public char getLocaleSeparator()
      {
         return options().getLocaleSeparator();
      }

      public boolean isAutoDetectFontsEnabled()
      {
         return options().isAutoDetectFontsEnabled();
      }

      public boolean isUseFopFontCacheEnabled()
      {
         return options().isUseFopFontCache();
      }

      private LinkedHashSet<ValueInjection> valueInjections;

      public LinkedHashSet<ValueInjection> getValueInjections()
      {
         if (valueInjections == null)
         {
            valueInjections = new LinkedHashSet<ValueInjection>();
            valueInjections.addAll(getValueInjections());

            if (options().isApplyStandardInjectionValues())
            {
               //valueInjections.add( new ValueInjection( "version", project.getVersion() ) );
               SimpleDateFormat dateFormat = new SimpleDateFormat(options().getInjectionDateFormat());
               valueInjections.add(new ValueInjection("today", dateFormat.format(new Date())));
            }
         }
         return valueInjections;
      }

      private LinkedHashSet<String> catalogSet;

      public LinkedHashSet<String> getCatalogs()
      {
         if (catalogSet == null)
         {
            catalogSet = new LinkedHashSet<String>();
            for (String catalog : options().getCatalogs())
            {
               catalogSet.add(catalog);
            }
         }
         return catalogSet;
      }

      public Profiling getProfiling()
      {
         return profiling;
      }

      public String getDocBookVersion()
      {
         return options.getDocbookVersion();
      }
   }

   /**
    * rootMasterSourceFile
    */
   private File rootMasterSourceFile;

   protected File getRootMasterSourceFile()
   {
      if (rootMasterSourceFile == null)
      {
         rootMasterSourceFile = new File(directoryLayout.getMasterSourceDirectory(), sourceDocumentName);
      }
      return rootMasterSourceFile;
   }

   protected String stringify(Locale locale)
   {
      return TranslationUtils.render(locale, options.getLocaleSeparator());
   }

   protected File getSourceDocument(Locale languageLocale)
   {
      return getSourceDocument(stringify(languageLocale));
   }

   protected File getSourceDocument(String language)
   {
      return isMasterLanguage(language) ? getRootMasterSourceFile() : new File(directoryLayout
            .getTranslationDirectory(language), sourceDocumentName);
   }

   protected File getProfiledDocument(Locale languageLocale)
   {
      return getProfiledDocument(stringify(languageLocale));
   }

   protected File getProfiledDocument(String language)
   {
      return new File(directoryLayout.getProfilingDirectory(language), sourceDocumentName);
   }

   protected List<Format> getFormatOptionsList()
   {
      return formats;
   }

   protected List<PublishingSource> resolvePublishingSources()
   {
      List<PublishingSource> sources = new ArrayList<PublishingSource>();

      if (ignoreTranslations)
      {
         //getLog().info( "Skipping all translations" );
         sources.add(new PublishingSource(getMasterLanguageLocale()));
      }
      else
      {
         //Matcher<Locale> matcher = new Matcher<Locale>( getRequestedLanguageLocale() );

         //if ( matcher.matches( getMasterLanguageLocale() ) ) {
         sources.add(new PublishingSource(getMasterLanguageLocale()));
         //}
         /*
         else {
             getLog().debug( "skipping master language" );
         }
         */
         for (String language : translations)
         {
            final Locale languageLocale = fromLanguageString(language);
            //if ( matcher.matches( languageLocale ) ) {
            sources.add(new PublishingSource(languageLocale));
            //}
            /*
            else {
                getLog().debug( "skipping language " + language );
            }
            */
         }
      }

      return sources;
   }

   protected class PublishingSource implements ProfilingSource, RenderingSource
   {
      private final Locale languageLocale;

      public PublishingSource(Locale languageLocale)
      {
         this.languageLocale = languageLocale;
      }

      // ProfilingSource impl
      
      public Locale getLanguage()
      {
         return languageLocale;
      }

      public File resolveDocumentFile()
      {
         return getSourceDocument(getLanguage());
      }

      public File resolveProfiledDocumentFile()
      {
         return getProfiledDocument(languageLocale);
      }

      // RenderingSource impl 
      
      public File resolveSourceDocument()
      {
         return profiling.isEnabled() ? getProfiledDocument(languageLocale) : getSourceDocument(languageLocale);
      }

      public File resolvePublishingBaseDirectory()
      {
         return directoryLayout.getPublishBaseDirectory(stringify(languageLocale));
      }

      public File getXslFoDirectory()
      {
         // n/a
         return null;
      }
   }

   /**
    * Get the sourceDocumentName.
    * 
    * @return the sourceDocumentName.
    */
   public String getSourceDocumentName()
   {
      return sourceDocumentName;
   }

   /**
    * Set the sourceDocumentName.
    * 
    * @param sourceDocumentName The sourceDocumentName to set.
    */
   public void setSourceDocumentName(String sourceDocumentName)
   {
      this.sourceDocumentName = sourceDocumentName;
   }

   public File getBaseSourceDirectory()
   {
      return sourceDirectory;
   }
   
   /**
    * Get the sourceDirectory.
    * 
    * @return the sourceDirectory.
    */
   public File getSourceDirectory()
   {
      return sourceDirectory;
   }

   /**
    * Set the sourceDirectory.
    * 
    * @param sourceDirectory The sourceDirectory to set.
    */
   public void setSourceDirectory(File sourceDirectory)
   {
      this.sourceDirectory = sourceDirectory;
   }

   public File getBaseOutputDirectory()
   {
      return baseOutputDirectory;
   }

   /**
    * Set the baseOutputDirectory.
    * 
    * @param baseOutputDirectory The baseOutputDirectory to set.
    */
   public void setBaseOutputDirectory(File baseOutputDirectory)
   {
      this.baseOutputDirectory = baseOutputDirectory;
   }


   public File[] getFontDirectories()
   {
      List<File> directories = new ArrayList<File>();

      if (fontsDirectory != null && fontsDirectory.exists())
      {
         directories.add(fontsDirectory);
      }

      File stagedFontsDirectory = new File(directoryLayout.getStagingDirectory(), "fonts");
      if (stagedFontsDirectory.exists())
      {
         directories.add(stagedFontsDirectory);
      }

      return directories.toArray(new File[directories.size()]);
   }
   
   /**
    * Get the fontsDirectory.
    * 
    * @return the fontsDirectory.
    */
   public File getFontsDirectory()
   {
      return fontsDirectory;
   }

   /**
    * Set the fontsDirectory.
    * 
    * @param fontsDirectory The fontsDirectory to set.
    */
   public void setFontsDirectory(File fontsDirectory)
   {
      this.fontsDirectory = fontsDirectory;
   }

   /**
    * Add a new formats
    * @return Format
    */
   public Format createFormat()
   {
      Format f = new Format();
      formats.add(f);
      return f;
   }
   
   public String getMasterLanguage()
   {
      return masterTranslation;
   }
   
   /**
    * Get the masterTranslation.
    * 
    * @return the masterTranslation.
    */
   public String getMasterTranslation()
   {
      return masterTranslation;
   }

   /**
    * Set the masterTranslation.
    * 
    * @param masterTranslation The masterTranslation to set.
    */
   public void setMasterTranslation(String masterTranslation)
   {
      this.masterTranslation = masterTranslation;
   }
}
