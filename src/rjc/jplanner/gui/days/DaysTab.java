/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
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

package rjc.jplanner.gui.days;

import javafx.scene.control.Tab;
import rjc.jplanner.Main;
import rjc.table.view.TableView;

/*************************************************************************************************/
/************************* Tab showing table of available plan day-types *************************/
/*************************************************************************************************/

public class DaysTab extends Tab
{
  private TableView m_view;

  /**************************************** constructor ******************************************/
  public DaysTab()
  {
    // construct tab
    super( "Days" );
    setClosable( false );

    // showing table of available plan day-types
    m_view = new TableView( new DaysData( Main.getPlan().daytypes ), getText() );
    m_view.setUndostack( Main.getUndostack() );
    m_view.setStatus( Main.getStatus() );
    setContent( m_view );
  }
}
