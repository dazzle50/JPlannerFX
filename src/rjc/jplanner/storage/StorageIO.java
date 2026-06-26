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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.plan.Plan;
import rjc.table.Utils;

/*************************************************************************************************/
/******************* Central gateway for application save and load operations ********************/
/*************************************************************************************************/

public class StorageIO
{
  private final XmlPlanMapper m_planMapper;
  // TODO add m_userConfigMapper and m_uiConfigMapper here when ready

  /****************************************** constructor ****************************************/
  public StorageIO()
  {
    // instantiate the specialised data mappers
    m_planMapper = new XmlPlanMapper();
  }

  /********************************************** open *******************************************/
  public Plan open( Path path )
  {
    // delegate to specialised mapper
    return m_planMapper.load( path );
  }

  /********************************************** save *******************************************/
  public boolean save( PlanContext context, File file )
  {
    // if file exists already, check file can be written
    if ( file.exists() && !file.canWrite() )
    {
      Utils.trace( "Could not write to '" + file.getPath() + "'" );
      return false;
    }

    try
    {
      XMLOutputFactory xof = XMLOutputFactory.newInstance();
      FileWriter writer = new FileWriter( file );
      XMLStreamWriter xml = xof.createXMLStreamWriter( writer );

      xml.writeStartDocument( "UTF-8", "1.0" );
      xml.writeEndDocument();

      xml.flush();
      xml.close();
    }
    catch ( XMLStreamException | IOException exception )
    {
      // some sort of exception thrown
      exception.printStackTrace();
      return false;
    }

    return true;
  }

}