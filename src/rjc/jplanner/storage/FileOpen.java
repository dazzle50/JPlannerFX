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

import javafx.stage.FileChooser;
import rjc.jplanner.gui.PlanContext;
import rjc.table.Utils;
import rjc.table.signal.ObservableStatus.Level;

/*************************************************************************************************/
/**************** Use FileChooser to choose file and load previously saved Plan ******************/
/*************************************************************************************************/

public class FileOpen
{

  /***************************************** constructor *****************************************/
  public FileOpen( PlanContext context )
  {
    // use file-chooser defaulting (if available) to current plan location
    FileChooser fc = new FileChooser();
    fc.setTitle( "Open plan" );

    File initialDirectory = new File( context.getPlan().getFileLocation() );
    if ( initialDirectory.isDirectory() )
      fc.setInitialDirectory( initialDirectory );

    fc.getExtensionFilters().add( new FileChooser.ExtensionFilter( "JPlannerFX files (*.xml)", "*.xml" ) );
    fc.getExtensionFilters().add( new FileChooser.ExtensionFilter( "All files (*.*)", "*.*" ) );

    File file = fc.showOpenDialog( context.getGui().getWindow() );

    // if user cancels file is null, so exit immediately
    if ( file == null )
      return;

    // ensure file exists
    if ( !file.exists() )
    {
      context.getStatus().update( Level.ERROR, "File '" + file.getPath() + "' does not exist" );
      return;
    }

    // ensure file can be read
    if ( !file.canRead() )
    {
      context.getStatus().update( Level.ERROR, "Could not read from '" + file.getPath() + "'" );
      return;
    }

    // ensure file is JPlanner XML
    if ( !validJPlannerXml( file ) )
    {
      context.getStatus().update( Level.ERROR, "File '" + file.getPath() + "' not valid plan." );
      return;
    }

    // attempt to launch new GUI in separate JVM (probably using ProcessBuilder) to display this plan
    Utils.trace( "NOT IMPLEMENTED" );
  }

  /************************************** validJPlannerXml ***************************************/
  private boolean validJPlannerXml( File file )
  {
    // TODO Auto-generated method stub
    return false;
  }

}
