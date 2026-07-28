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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.plan.Plan;
import rjc.table.Utils;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/**************** Load previously saved in XML format Plan from specified file *******************/
/*************************************************************************************************/

public class FileLoad
{
  private XMLStreamReader m_xml;
  private String          m_savedby;   // who saved the file
  private String          m_savedWhen; // where file was saved

  /***************************************** constructor *****************************************/
  public FileLoad( PlanContext context, File file )
  {
    Utils.trace( "Starting to read '" + file.getPath() + "'" );

    try ( var input = Files.newInputStream( file.toPath() ) )
    {
      // open file as XML stream
      var factory = XMLInputFactory.newInstance();
      m_xml = factory.createXMLStreamReader( input );

      processJPlanner();
      var newPlan = processPlan();

      // plan-data was fully consumed, so loading was successful
      newPlan.setFilename( file.getName() );
      newPlan.setFileLocation( file.getParent() );
      // TODO do something with the new Plan !!!
      Utils.trace( "Successfully read '" + file.getPath() + "' " + newPlan );
    }
    catch ( Exception exception )
    {
      // loading failed before the end of plan-data was reached
      Utils.trace( "ERROR reading '" + file.getPath() + "'" );
      exception.printStackTrace();
      context.getStatus().update( Level.ERROR, "Error reading '" + file.getPath() + "' : " + exception.getMessage() );
    }
    finally
    {
      closeXmlReader();
    }
  }

  /*************************************** processJPlanner ***************************************/
  private void processJPlanner() throws Exception
  {
    // find and process the XML document wrapper
    findElementStart( XmlLabels.XML_JPLANNER );

    // document-level attributes include format version and compatibility information
    for ( int i = 0; i < m_xml.getAttributeCount(); i++ )
    {
      String attrib = m_xml.getAttributeLocalName( i );
      String value = m_xml.getAttributeValue( i );

      switch ( attrib )
      {
        case XmlLabels.XML_FORMAT -> {
          // ignored for now
        }
        case XmlLabels.XML_SAVENAME -> {
          // ignored
        }
        case XmlLabels.XML_SAVEWHERE -> {
          // ignored
        }
        case XmlLabels.XML_SAVEUSER -> m_savedby = value;
        case XmlLabels.XML_SAVEWHEN -> m_savedWhen = value;
        default -> Utils.trace( "Unhandled attribute '" + attrib + "' = '" + value + "'" );
      }
    }
  }

  /***************************************** processPlan *****************************************/
  private Plan processPlan() throws Exception
  {
    // position reader on plan-data and delegate its complete contents to the mapper
    findElementStart( XmlLabels.XML_PLAN_DATA );
    return new XmlPlanReader().read( m_xml );
  }

  /************************************** findElementStart ***************************************/
  private void findElementStart( String element ) throws Exception
  {
    // move forward until the requested document-level element is found
    while ( m_xml.hasNext() )
    {
      m_xml.next();
      if ( m_xml.isStartElement() && m_xml.getLocalName().equals( element ) )
        return;
    }

    throw new IOException( "Failed to find element '" + element + "'" );
  }

  /*************************************** closeXmlReader ****************************************/
  private void closeXmlReader()
  {
    // close XML reader without changing constructor control flow
    if ( m_xml == null )
      return;

    try
    {
      m_xml.close();
    }
    catch ( XMLStreamException exception )
    {
      exception.printStackTrace();
    }
  }

}
