/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
 *  https://github.com/dazzle50/JPlannerFX                                *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.jplanner.storage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.plan.Plan;
import rjc.table.Utils;
import rjc.table.data.types.DateTime;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/************************** Save plan data and metadata to an XML file ***************************/
/*************************************************************************************************/

public class FileSave
{
  private boolean m_successful; // whether writing and save bookkeeping completed

  /***************************************** constructor *****************************************/
  public FileSave( PlanContext context, File file )
  {
    // check context is valid
    if ( context == null || context.getPlan() == null )
      throw new IllegalArgumentException( "PlanContext and Plan must not be null" );

    // finish edits after any file chooser returns, including edits in additional plan windows
    context.getGui().finishEditing();
    Plan plan = context.getPlan();
    String savedBy = System.getProperty( "user.name", "" );
    DateTime savedWhen = DateTime.now();
    File target;
    Utils.trace( "Starting to write '" + file.getPath() + "'" );
    try
    {
      target = write( plan, file, savedBy, savedWhen );
    }
    catch ( Exception exception )
    {
      // failed writing leaves the destination, plan metadata and clean position unchanged
      Utils.trace( "ERROR writing '" + file.getPath() + "'", exception );
      context.getStatus().update( Level.ERROR,
          "Error writing to '" + file.getPath() + "' : " + exception.getMessage() );
      return;
    }

    // publish the same metadata written to XML only after the destination has been replaced
    plan.setFilename( target.getName() );
    plan.setFileLocation( target.getParent() );
    plan.setSavedBy( savedBy );
    plan.setSavedWhen( savedWhen );
    context.getUndoStack().setClean();
    m_successful = true;
    context.getGui().refreshPlanDetails();
    context.getStatus().update( Level.INFO, "Saved '" + target.getPath() + "'" );
    Utils.trace( "Successfully wrote '" + target.getPath() + "'" );
  }

  /**************************************** isSuccessful *****************************************/
  public boolean isSuccessful()
  {
    return m_successful;
  }

  /***************************************** saveCurrent *****************************************/
  public static boolean saveCurrent( PlanContext context )
  {
    // unnamed plans need a destination; named plans reuse their saved location
    Plan plan = context.getPlan();
    if ( plan.getFilename().isBlank() )
      return new FileSaveAs( context ).isSuccessful();

    return new FileSave( context, new File( plan.getFileLocation(), plan.getFilename() ) ).isSuccessful();
  }

  /******************************************** write ********************************************/
  static File write( Plan plan, File file, String savedBy, DateTime savedWhen ) throws Exception
  {
    File target = file.getCanonicalFile();
    if ( target.exists() && ( !target.isFile() || !target.canWrite() ) )
      throw new IOException( "Could not write to '" + target.getPath() + "'" );

    var destination = target.toPath();
    var temporary = Files.createTempFile( destination.getParent(), ".jplanner-", ".tmp" );
    try
    {
      // finish and close the temporary file before replacing the destination
      try ( var writer = Files.newBufferedWriter( temporary, Charset.forName( XmlLabels.ENCODING ) );
          var xml = new XmlWriter( writer ) )
      {
        writeStartDocument( writer );
        writeMetadata( xml, target, savedBy, savedWhen );
        new XmlPlanWriter().write( plan, xml );
        // display-data persistence is not implemented
        xml.endDocument();
        xml.flush();
      }

      // an unsupported atomic replacement fails the save instead of risking the existing file
      Files.move( temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING );
      return target;
    }
    finally
    {
      Files.deleteIfExists( temporary );
    }
  }

  /************************************** writeStartDocument *************************************/
  private static void writeStartDocument( BufferedWriter writer ) throws IOException
  {
    // write the XML declaration using the same character set as the output writer
    writer.write( "<?xml version=\"" + XmlLabels.VERSION + "\" encoding=\"" + XmlLabels.ENCODING + "\"?>" );
  }

  /**************************************** writeMetadata ****************************************/
  private static void writeMetadata( XmlWriter xml, File file, String savedBy, DateTime savedWhen ) throws IOException
  {
    // write the JPlannerFX metadata to the XML stream
    xml.startElement( XmlLabels.XML_JPLANNER );
    xml.attribute( XmlLabels.XML_FORMAT, XmlLabels.FORMAT );
    xml.attribute( XmlLabels.XML_SAVEUSER, savedBy );
    xml.attribute( XmlLabels.XML_SAVEWHEN, savedWhen );
    xml.attribute( XmlLabels.XML_SAVENAME, file.getName() );
    xml.attribute( XmlLabels.XML_SAVEWHERE, file.getParent() );
  }
}
