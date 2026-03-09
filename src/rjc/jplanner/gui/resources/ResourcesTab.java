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

package rjc.jplanner.gui.resources;

import javafx.scene.control.Tab;
import rjc.jplanner.Main;
import rjc.jplanner.plan.Plan;

/*************************************************************************************************/
/************************* Tab showing table of available plan resources *************************/
/*************************************************************************************************/

public class ResourcesTab extends Tab
{
  private ResourcesView        m_view;

  private static ResourcesData m_data;

  /**************************************** constructor ******************************************/
  public ResourcesTab()
  {
    // construct tab
    super( "Resources" );
    setClosable( false );

    // showing table of available plan resources
    m_data = m_data == null ? new ResourcesData( Plan.getResources() ) : m_data;
    m_view = new ResourcesView( m_data, getText() );
    m_view.setUndostack( Main.getUndostack() );
    m_view.setStatus( Main.getStatus() );
    m_view.setFocusTraversable( true );

    // only have tab contents set if tab selected
    selectedProperty().addListener( ( property, oldValue, newValue ) ->
    {
      if ( newValue )
        setContent( m_view );
      else
        setContent( null );
    } );
  }
}
