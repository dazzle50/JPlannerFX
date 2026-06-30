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
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.plan.Plan;
import rjc.table.Utils;
import rjc.table.data.types.DateTime;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/******************* Central gateway for application save and load operations ********************/
/*************************************************************************************************/

public class StorageIO
{

  /********************************************** open *******************************************/
  public Plan open( Path path )
  {
    // TODO delegate to specialised mapper
    return null;
  }

  /********************************************** save *******************************************/
  public boolean save( PlanContext context, File file )
  {
    // check context is valid
    if ( context == null || context.getPlan() == null )
      throw new IllegalArgumentException( "PlanContext and Plan must not be null" );

    // if file exists already, check file can be written
    if ( file.exists() && !file.canWrite() )
    {
      context.getStatus().update( Level.ERROR, "Could not write to '" + file.getPath() + "'" );
      return false;
    }

    // attempt to write JPlannerFX file to the specified file
    Utils.trace( "Starting to write '" + file.getPath() + "'" );
    try ( var writer = Files.newBufferedWriter( file.toPath(), Charset.forName( XmlLabels.ENCODING ) );
        var xml = new XmlWriter( writer ) )
    {
      writeStartDocument( writer );
      writeMetaData( xml, file );

      var planMapper = new XmlPlanMapper( context.getPlan() );
      planMapper.write( xml );

      xml.endDocument();
      xml.flush();
    }
    catch ( Exception exception )
    {
      // some sort of exception thrown
      Utils.trace( "ERROR writing '" + file.getPath() + "'" );
      exception.printStackTrace();
      context.getStatus().update( Level.ERROR,
          "Error writing to '" + file.getPath() + "' : " + exception.getMessage() );
      return false;
    }

    // successfully wrote file
    Utils.trace( "Successfully wrote '" + file.getPath() + "'" );
    return true;
  }

  /************************************** writeStartDocument *************************************/
  private void writeStartDocument( BufferedWriter writer ) throws Exception
  {
    // write the XML declaration using the same character set as the output writer
    writer.write( "<?xml version=\"" );
    writer.write( XmlLabels.VERSION );
    writer.write( "\" encoding=\"" );
    writer.write( XmlLabels.ENCODING );
    writer.write( "\"?>" );
  }

  /**************************************** writeMetaData ****************************************/
  private void writeMetaData( XmlWriter xml, File file ) throws Exception
  {
    // write the JPlannerFX meta-data to the XML stream
    xml.startElement( XmlLabels.XML_JPLANNER );
    xml.attribute( XmlLabels.XML_FORMAT, XmlLabels.FORMAT );
    String saveUser = System.getProperty( "user.name" );
    xml.attribute( XmlLabels.XML_SAVEUSER, saveUser );
    DateTime saveWhen = DateTime.now();
    xml.attribute( XmlLabels.XML_SAVEWHEN, saveWhen.toString() );
    xml.attribute( XmlLabels.XML_SAVENAME, file.getName() );
    xml.attribute( XmlLabels.XML_SAVEWHERE, file.getParent() );
  }

}
