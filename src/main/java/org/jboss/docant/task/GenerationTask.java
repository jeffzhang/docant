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

import org.apache.tools.ant.BuildException;

import org.jboss.jdocbook.profile.Profiler;
import org.jboss.jdocbook.render.FormatOptions;
import org.jboss.jdocbook.render.Renderer;

/**
 * generation ant task
 *
 * @author Jeff Zhang
 */
public class GenerationTask extends AbstractDocbookTask
{
   /**
    * process
    * @exception BuildException If the build fails
    */
   @Override
   public void process() throws BuildException
   {
      if (!sourceDirectory.exists())
      {
         System.out.println("sourceDirectory [" + sourceDirectory.getAbsolutePath() + "] did not exist");
         //getLog().info( "sourceDirectory [" + sourceDirectory.getAbsolutePath() + "] did not exist" );
         return;
      }

      final Profiler profiler = getComponentRegistry().getProfiler();
      final Renderer renderer = getComponentRegistry().getRenderer();

      //final Matcher<String> matcher = new Matcher<String>( getRequestedFormat() );

      for (PublishingSource publishingSource : resolvePublishingSources())
      {
         if (profiling.isEnabled())
         {
            profiler.profile(publishingSource);
         }
         for (FormatOptions formatOptions : getFormatOptionsList())
         {
            //if ( matcher.matches( formatOptions.getName() ) ) {
            renderer.render(publishingSource, formatOptions);
            //}
         }
      }
   }
}
