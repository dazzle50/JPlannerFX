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

/*************************************************************************************************/
/*********************** Choose a destination for saving plan data as XML ************************/
/*************************************************************************************************/

public class FileSaveAs
{
  private boolean m_successful; // whether a destination was chosen and saved

  /***************************************** constructor *****************************************/
  public FileSaveAs( PlanContext context )
  {
    // finish the current edit before opening the file chooser
    context.getGui().finishEditing();

    // use file-chooser defaulting (if available) to current plan location and filename
    FileChooser fc = new FileChooser();
    fc.setTitle( "Save plan" );

    File initialDirectory = new File( context.getPlan().getFileLocation() );
    if ( initialDirectory.isDirectory() )
      fc.setInitialDirectory( initialDirectory );

    fc.setInitialFileName( context.getPlan().getFilename() );
    fc.getExtensionFilters().add( new FileChooser.ExtensionFilter( "JPlannerFX files (*.xml)", "*.xml" ) );
    fc.getExtensionFilters().add( new FileChooser.ExtensionFilter( "All files (*.*)", "*.*" ) );

    File file = fc.showSaveDialog( context.getGui().getWindow() );

    // if user cancels file is null, so exit immediately
    if ( file == null )
      return;

    // attempt to save to user specified file
    m_successful = new FileSave( context, file ).isSuccessful();
  }

  /**************************************** isSuccessful *****************************************/
  public boolean isSuccessful()
  {
    return m_successful;
  }

}
